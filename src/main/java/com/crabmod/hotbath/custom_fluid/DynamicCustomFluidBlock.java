package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.crabmod.hotbath.fluid_blocks.IInsideAreaTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * </ul>
 */
public class DynamicCustomFluidBlock extends AbstractHotbathBlock implements EntityBlock, IInsideAreaTracker {
    
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
     * Only hot fluids (temperature >= 35閹虹煰) cause damage to ice mobs and gummy bears.
     * 
     * @param level The level
     * @param pos The position of the fluid block
     * @return true if the fluid at this position is hot (temperature >= 35閹虹煰)
     */
    @Override
    public boolean isHotBath(Level level, BlockPos pos) {
        return getFluidDefinition(level,pos).map(CustomFluidDefinition::isHot).orElse(false);
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
     */
    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, 
                        @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        
        if (level.isClientSide) {
            return;
        }
        
        // Schedule tick updates for neighboring flowing fluids
        // This ensures they re-evaluate their sources when a new source is placed
        scheduleNeighborFluidUpdates(level, pos);
    }
    
    /**
     * Schedule tick updates for neighboring flowing fluid blocks.
     * This ensures they re-evaluate their state when the source changes.
     */
    public void scheduleNeighborFluidUpdates(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            FluidState neighborFluid = level.getFluidState(neighborPos);
            
            // Schedule tick for all non-source fluids
            if (!neighborFluid.isEmpty() && !neighborFluid.isSource()) {
                level.scheduleTick(neighborPos, neighborFluid.getType(), 1);
            }
        }
    }

    /**
     * Override pickupBlock to return the correct custom fluid bucket.
     * This is called when a player uses an empty bucket on the fluid block.
     */
    @Override
    public @NotNull ItemStack pickupBlock(@NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockState state) {
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
        return super.pickupBlock(level, pos, state);
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

        return Optional.empty();
    }



    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        super.entityInside(state, level, pos, entity);
        applyImmersionEffects(level, pos, entity);
    }

    public void applyImmersionEffects(Level level, BlockPos pos, Entity entity) {
        getFluidDefinition(level,pos).ifPresent(definition->com.crabmod.hotbath.api.HotBathApi.applyCustomFluidContact(entity,definition));
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
