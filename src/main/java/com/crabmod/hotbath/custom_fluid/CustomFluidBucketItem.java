package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.registers.CustomFluidBlocksRegister;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A dynamic bucket item that can hold any custom fluid defined in data packs.
 * The fluid type is stored in the item's data components, allowing a single
 * item registration to represent all custom fluids.
 * 
 * <p>This bucket behaves similarly to a water bucket:
 * <ul>
 *   <li>Right-click on a block to place the fluid</li>
 *   <li>Returns an empty bucket after use</li>
 *   <li>Displays the custom fluid name and color</li>
 *   <li>Supports waterlogging blocks</li>
 * </ul>
 */
public class CustomFluidBucketItem extends Item {
    
    public CustomFluidBucketItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Get the fluid definition
        CustomFluidDefinition definition = CustomFluidDataComponents.getFluidDefinition(stack);
        if (definition == null) {
            return InteractionResultHolder.fail(stack);
        }

        // Ray trace to find where to place fluid
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        
        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }
        
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            BlockState clickedState = level.getBlockState(pos);
            
            // Check if the clicked block can be waterlogged
            if (clickedState.getBlock() instanceof SimpleWaterloggedBlock
                    && clickedState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                
                boolean isAlreadyWaterlogged = clickedState.getValue(BlockStateProperties.WATERLOGGED);
                
                if (!level.mayInteract(player, pos)
                        || !player.mayUseItemAt(pos.relative(hitResult.getDirection()), hitResult.getDirection(), stack)) {
                    return InteractionResultHolder.fail(stack);
                }
                
                if (!level.isClientSide) {
                    if (!isAlreadyWaterlogged) {
                        // Waterlog the block
                        level.setBlock(pos, clickedState.setValue(BlockStateProperties.WATERLOGGED, true), 3);
                    }
                    // Whether it was already waterlogged or not, update the fluid type
                    // This allows replacing existing water/fluid with custom fluid
                    
                    // Store the fluid type in waterlogging helper
                    HotbathWaterloggingHelper.storeFluidType(level, pos, 
                            DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get());
                    
                    // Store the custom fluid ID for this waterlogged position
                    // This triggers network sync and client re-render
                    HotbathWaterloggingHelper.storeCustomFluidId(level, pos, definition.id());
                    
                    // Trigger fluid spread update to propagate to blocks below
                    HotbathWaterloggingHelper.triggerFluidSpreadUpdate(level, pos, 
                            DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get(), definition.id());
                    
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
                }
                
                // Return empty bucket
                if (!player.getAbilities().instabuild) {
                    return InteractionResultHolder.sidedSuccess(new ItemStack(Items.BUCKET), level.isClientSide());
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
            
            // Normal fluid placement
            BlockPos placePos = pos.relative(hitResult.getDirection());
            
            if (!level.isInWorldBounds(placePos) || !level.mayInteract(player, pos)
                    || !player.mayUseItemAt(placePos, hitResult.getDirection(), stack)) {
                return InteractionResultHolder.fail(stack);
            }
            
            BlockState targetState = level.getBlockState(placePos);
            FluidState targetFluid = targetState.getFluidState();
            
            // Can only place in air or replaceable blocks
            if (targetState.isAir() || targetState.canBeReplaced()) {
                if (!level.isClientSide) {
                    // Check if target is already a SOURCE custom fluid block - just update the fluid ID
                    // If it's flowing fluid (not source), we need to replace it with a source block
                    BlockEntity existingBe = level.getBlockEntity(placePos);
                    boolean isExistingSource = targetFluid.isSource() && existingBe instanceof CustomFluidBlockEntity;
                    
                    if (isExistingSource) {
                        // Directly update the existing source BlockEntity - this triggers render refresh
                        ((CustomFluidBlockEntity) existingBe).setFluidId(definition.id());
                    } else {
                        // Place dynamic custom fluid block (source) with BlockEntity to store fluid ID
                        BlockState fluidState = CustomFluidBlocksRegister.CUSTOM_FLUID_BLOCK.get().defaultBlockState();
                        if (!level.setBlock(placePos, fluidState, 11)) {
                            return InteractionResultHolder.fail(stack);
                        }
                        
                        // Set the fluid ID in the BlockEntity
                        BlockEntity be = level.getBlockEntity(placePos);
                        if (be instanceof CustomFluidBlockEntity newBe) {
                            newBe.setFluidId(definition.id());
                        }
                    }
                    
                    level.playSound(null, placePos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(player, GameEvent.FLUID_PLACE, placePos);
                }
                
                // Return empty bucket
                if (!player.getAbilities().instabuild) {
                    return InteractionResultHolder.sidedSuccess(new ItemStack(Items.BUCKET), level.isClientSide());
                }
                
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }
        
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        CustomFluidDefinition definition = CustomFluidDataComponents.getFluidDefinition(stack);
        if (definition != null) {
            // Use the dynamic translation system to get the translated fluid name
            String translatedFluidName = definition.getTranslatedName();
            Component fluidName = Component.literal(translatedFluidName);
            return Component.translatable("item.hotbath.custom_fluid_bucket", fluidName);
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, 
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        CustomFluidDefinition definition = CustomFluidDataComponents.getFluidDefinition(stack);
        if (definition != null) {
            // Show custom description if provided in translations (key format: description.{lang})
            // Look for description in current language from translations
            String descriptionKey = definition.getTranslationKey() + ".desc";
            String translatedDesc = CustomFluidTranslationManager.getTranslation(descriptionKey);
            if (!translatedDesc.equals(descriptionKey)) {
                // Translation found, use it
                tooltipComponents.add(Component.literal(translatedDesc)
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            
            // Show effects count
            if (!definition.effects().isEmpty()) {
                tooltipComponents.add(Component.translatable("tooltip.hotbath.effects_count", 
                        definition.effects().size())
                        .withStyle(ChatFormatting.GREEN));
            }
        }
    }

    /**
     * Gets the custom fluid color for item rendering.
     * This is called by the item color handler.
     * 
     * @param stack The item stack
     * @param tintIndex The tint layer index
     * @return The color for rendering
     */
    public static int getItemColor(ItemStack stack, int tintIndex) {
        if (tintIndex == 1) { // Fluid layer
            int color = CustomFluidDataComponents.getFluidColor(stack);
            if (color != -1) {
                return color;
            }
        }
        return 0xFFFFFFFF;
    }
}
