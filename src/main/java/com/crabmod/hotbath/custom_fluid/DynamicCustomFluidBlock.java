package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.crabmod.hotbath.fluid_blocks.IInsideAreaTracker;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A dynamic custom fluid block that uses BlockEntity to store fluid data from data packs.
 * This allows each placed fluid block to have its own unique fluid definition.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Stores fluid ID in BlockEntity for per-block customization</li>
 *   <li>Generates steam particles only when fluid is hot (temperature >= threshold)</li>
 *   <li>Applies effects from the stored fluid definition</li>
 *   <li>Supports tinted rendering based on fluid color</li>
 *   <li>Recovers custom fluid ID from waterlogging storage when created from broken waterlogged blocks</li>
 * </ul>
 */
public class DynamicCustomFluidBlock extends AbstractHotbathBlock implements EntityBlock, IInsideAreaTracker {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCustomFluidBlock.class);
    private static final int TICKS_PER_SECOND = 20;

    public DynamicCustomFluidBlock(Supplier<? extends FlowingFluid> fluidSupplier, Properties properties) {
        super(fluidSupplier, properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CustomFluidBlockEntity(pos, state);
    }
    
    /**
     * Checks if this custom fluid at the given position is considered "hot".
     * Looks up the BlockEntity to get the fluid definition and checks its temperature.
     * Only hot fluids (temperature >= 35閹虹煰) cause damage to ice mobs.
     * 
     * @param level The level
     * @param pos The position of the fluid block
     * @return true if the fluid at this position is hot
     */
    @Override
    public boolean isHotBath(Level level, BlockPos pos) {
        return getFluidDefinition(level,pos).map(CustomFluidDefinition::isHot).orElse(false);
    }
    
    /**
     * Indicates that this block has dynamic light emission that depends on BlockEntity data.
     * This tells NeoForge to call getLightEmission() for each block instance.
     */
    @Override
    public boolean hasDynamicLightEmission(@NotNull BlockState state) {
        return true;
    }
    
    /**
     * Gets the light emission value for this fluid block based on the stored fluid definition.
     * This allows each custom fluid to have its own luminosity value.
     */
    @Override
    public int getLightEmission(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe) {
            return customBe.getFluidDefinition()
                    .map(CustomFluidDefinition::luminosity)
                    .orElse(2); // Default luminosity
        }
        return 2; // Default luminosity if BlockEntity not found
    }
    
    /**
     * Called when this block is placed in the world.
     * Checks if there was a stored custom fluid ID in waterlogging storage at this position,
     * which happens when a waterlogged block is broken and releases the fluid.
     * Also schedules tick updates for neighboring flowing fluids to ensure they recalculate
     * whether their source is still valid.
     */
    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, 
                        @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        
        if (level.isClientSide) {
            return;
        }
        
        // Check if there's a stored custom fluid ID from waterlogging at this position
        // This handles the case when a waterlogged block is broken and fluid is released
        ResourceLocation customFluidId = HotbathWaterloggingHelper.getStoredCustomFluidId(level, pos);
        if (customFluidId != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CustomFluidBlockEntity customBe) {
                // Only set if the BlockEntity doesn't already have a fluid ID
                if (customBe.getFluidId() == null) {
                    customBe.setFluidId(customFluidId);
                }
            }
            // Clean up the waterlogging storage since we've moved the fluid to a regular block
            HotbathWaterloggingHelper.removeFluidType(level, pos);
            HotbathWaterloggingHelper.removeFromClientCache(pos);
        }
        
        // Schedule tick updates for neighboring flowing fluids
        // This ensures that when a source block is replaced with a different custom fluid,
        // the neighboring flowing fluids will recalculate and disappear if their source is no longer valid
        scheduleNeighborFluidUpdates(level, pos);
    }
    
    /**
     * Schedule fluid tick updates for all neighboring blocks that contain flowing custom fluid.
     * This is called when a source block is placed to ensure that any adjacent flowing fluids
     * with a different customFluidId will recalculate and potentially disappear.
     */
    private void scheduleNeighborFluidUpdates(Level level, BlockPos pos) {
        // Check all 6 directions
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            FluidState neighborFluid = level.getFluidState(neighborPos);
            
            // Check if neighbor is a dynamic custom fluid that is flowing (not source)
            if (!neighborFluid.isEmpty() 
                    && neighborFluid.getType() instanceof DynamicCustomFluid 
                    && !neighborFluid.isSource()) {
                // Schedule a tick for this flowing fluid to recalculate its state
                level.scheduleTick(neighborPos, neighborFluid.getType(), 1);
                LOGGER.debug("Scheduled tick for flowing fluid at {} due to source change at {}", neighborPos, pos);
            }
        }
    }

    /**
     * Override pickupBlock to return the correct custom fluid bucket.
     * This is called when a player uses an empty bucket on the fluid block.
     */
    @Override
    public @NotNull ItemStack pickupBlock(@Nullable Player player, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockState state) {
        // Only allow pickup of source blocks (level 0)
        if (state.getValue(LEVEL) != 0) {
            return ItemStack.EMPTY;
        }

        // Get fluid definition from BlockEntity before removing it
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe) {
            Optional<CustomFluidDefinition> definitionOpt = customBe.getFluidDefinition();
            if (definitionOpt.isPresent()) {
                // Remove the fluid block
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                // Return the filled custom fluid bucket
                return CustomFluidAPI.createBucket(definitionOpt.get());
            }
        }

        // Fallback to parent behavior if no definition found
        return super.pickupBlock(player, level, pos, state);
    }

    /**
     * Override getPickupSound to return the correct sound for bucket pickup.
     */
    @Override
    public @NotNull Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL);
    }

    /**
     * Gets the fluid definition from the BlockEntity at the given position.
     */
    public Optional<CustomFluidDefinition> getFluidDefinition(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe) {
            return customBe.getFluidDefinition();
        }

        if(be instanceof com.crabmod.hotbath.api.BathFluidContainer container)return container.getCustomFluidDefinition();

        ResourceLocation customFluidId = HotbathWaterloggingHelper.getStoredCustomFluidId(level, pos);
        return customFluidId == null
                ? Optional.empty()
                : CustomFluidAPI.getFluidDefinition(customFluidId);
    }

    public void applyImmersionEffects(Level level, BlockPos pos, Entity entity) {
        applyBathEffects(level, pos, entity);
    }

    @Override
    protected void applyBathEffects(@NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        if (level.isClientSide) {
            return;
        }
        
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity player)
                || (!(player instanceof ServerPlayer) && !com.crabmod.hotbath.entity.FriendlyBathBenefits.isEligible(player))) {
            return;
        }
        
        if (!player.isAlive()) {
            return;
        }
        
        // Get fluid definition from BlockEntity
        Optional<CustomFluidDefinition> definitionOpt = getFluidDefinition(level, pos);
        if (definitionOpt.isEmpty()) {
            return;
        }
        
        CustomFluidDefinition definition = definitionOpt.get();
        
        InsideAreaResult result = trackInside(player);
        
        if (!result.shouldProcess()) {
            return;
        }
        
        int stayedTicks = result.stayedTicks();
        int triggerTicks = definition.triggerTimeSeconds() * TICKS_PER_SECOND;
        
        // Apply effects after the trigger time has passed
        if (stayedTicks >= triggerTicks) {
            applyEffects(player, definition);
        }
    }

    /**
     * Applies configured effects to an eligible bather.
     */
    private void applyEffects(net.minecraft.world.entity.LivingEntity player, CustomFluidDefinition definition) {
        List<MobEffectInstance> effects = definition.createEffectInstances();
        for (MobEffectInstance effect : effects) {
            player.addEffect(new MobEffectInstance(
                    effect.getEffect(),
                    effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            ));
        }
    }

    /**
     * Override to check BlockEntity for steam display (based on temperature).
     * Parent class will call this before generating steam particles.
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    protected boolean shouldShowSteam(Level level, BlockPos pos) {
        return getFluidDefinition(level,pos).map(def->def.isHot()&&def.showSteam()).orElse(false);
    }
    
    /**
     * Override to check BlockEntity for bubble display setting.
     * Parent class will call this before generating bubble particles.
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    protected boolean shouldShowBubbles(Level level, BlockPos pos) {
        return getFluidDefinition(level,pos).map(CustomFluidDefinition::showBubbles).orElse(false);
    }

    /**
     * Gets the tint color for this fluid block at the given position.
     * Used by the rendering system to apply color to the grayscale texture.
     */
    public int getTintColor(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe) {
            return customBe.getFluidColor();
        }
        return 0x45E1E9; // Default cyan
    }
}
