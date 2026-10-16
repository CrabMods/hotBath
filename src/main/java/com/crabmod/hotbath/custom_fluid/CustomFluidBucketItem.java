package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.registers.CustomFluidBlocksRegister;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * A dynamic bucket item that can hold any custom fluid defined in data packs.
 * The fluid type is stored in the item's NBT tags, allowing a single
 * item registration to represent all custom fluids.
 * 
 * <p>This bucket behaves similarly to a water bucket:
 * <ul>
 *   <li>Right-click on a block to place the fluid</li>
 *   <li>Returns an empty bucket after use</li>
 *   <li>Displays the custom fluid name and color</li>
 * </ul>
 */
public class CustomFluidBucketItem extends Item {
    
    public CustomFluidBucketItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return CustomFluidCapabilities.createProvider(stack, true);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Get the fluid definition
        CustomFluidDefinition definition = CustomFluidNBTHelper.getFluidDefinition(stack);
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

            // Place fluid block at adjacent position (no waterlogging support in 1.20.1)
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
        CustomFluidDefinition definition = CustomFluidNBTHelper.getFluidDefinition(stack);
        if (definition != null) {
            // Use the dynamic translation system to get the translated fluid name
            String translatedFluidName = definition.getTranslatedName();
            Component fluidName = Component.literal(translatedFluidName);
            return Component.translatable("item.hotbath.custom_fluid_bucket", fluidName);
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, 
            @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        CustomFluidDefinition definition = CustomFluidNBTHelper.getFluidDefinition(stack);
        if (definition != null) {
            // Show custom description if provided in translations
            String descriptionKey = definition.getTranslationKey() + ".desc";
            String translatedDesc = CustomFluidTranslationManager.getTranslation(descriptionKey);
            if (!translatedDesc.equals(descriptionKey)) {
                tooltip.add(Component.literal(translatedDesc)
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }

            // Effects count
            if (!definition.effects().isEmpty()) {
                tooltip.add(Component.translatable("tooltip.hotbath.effects_count",
                        definition.effects().size())
                        .withStyle(ChatFormatting.GREEN));
            }
        }
    }

    /**
     * Creates a bucket item stack filled with the specified custom fluid.
     * 
     * @param fluidId The fluid ID to fill the bucket with
     * @return A new bucket item stack with the fluid data set
     */
    public ItemStack createFilledBucket(net.minecraft.resources.ResourceLocation fluidId) {
        ItemStack stack = new ItemStack(this);
        CustomFluidNBTHelper.setFluidId(stack, fluidId);
        return stack;
    }
}
