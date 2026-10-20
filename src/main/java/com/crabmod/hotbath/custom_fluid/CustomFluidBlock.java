package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.crabmod.hotbath.fluid_blocks.IInsideAreaTracker;
import com.crabmod.hotbath.util.ParticleGenerator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

/**
 * A custom fluid block that applies effects based on a CustomFluidDefinition.
 * This block is created dynamically based on data pack definitions.
 * 
 * <p>The block handles:
 * <ul>
 *   <li>Applying potion effects to players after they've been in the fluid for a configured time</li>
 *   <li>Generating steam and bubble particles based on configuration</li>
 *   <li>All standard hot bath behaviors from AbstractHotbathBlock</li>
 * </ul>
 */
public class CustomFluidBlock extends AbstractHotbathBlock implements IInsideAreaTracker {
    
    private static final int TICKS_PER_SECOND = 20;
    
    private final CustomFluidDefinition definition;

    public CustomFluidBlock(
            Supplier<? extends FlowingFluid> fluidSupplier,
            Properties properties,
            CustomFluidDefinition definition) {
        super(fluidSupplier, properties);
        this.definition = definition;
    }

    /**
     * Gets the fluid definition associated with this block.
     */
    public CustomFluidDefinition getDefinition() {
        return definition;
    }

    /**
     * Checks if this custom fluid is considered "hot" based on its temperature.
     * Only hot fluids (temperature >= 35°C) cause damage to ice mobs.
     * 
     * @return true if temperature >= HOT_TEMPERATURE_THRESHOLD
     */
    @Override
    public boolean isHotBath() {
        return definition.isHot();
    }

    @Override
    protected void applyBathEffects(@NotNull Level level,@NotNull BlockPos pos,@NotNull Entity entity) {
        com.crabmod.hotbath.api.HotBathApi.applyCustomFluidContact(entity,definition);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(
            @NotNull BlockState stateIn,
            @NotNull Level worldIn,
            @NotNull BlockPos pos,
            @NotNull RandomSource rand) {
        
        // Call parent for bubble column and underwater effects
        super.animateTick(stateIn, worldIn, pos, rand);
        
        // Generate steam particles only when:
        // 1. The fluid is hot (temperature >= threshold) AND
        // 2. The showSteam setting is enabled in the definition
        if (definition.isHot() && definition.showSteam()) {
            generateCustomSteamParticles(worldIn, pos, rand);
        }
    }

    /**
     * Generates steam particles at adjacent air blocks.
     */
    private void generateCustomSteamParticles(Level worldIn, BlockPos pos, RandomSource rand) {
        BlockPos[] adjacentPositions = new BlockPos[]{
                pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()
        };

        int airBlockCount = 0;
        BlockPos[] airBlocks = new BlockPos[adjacentPositions.length];

        for (BlockPos adjacentPos : adjacentPositions) {
            if (worldIn.getBlockState(adjacentPos).isAir()) {
                airBlocks[airBlockCount++] = adjacentPos;
            }
        }

        if (airBlockCount > 0 && rand.nextInt(5) == 0) {
            BlockPos selectedPos = airBlocks[rand.nextInt(airBlockCount)];
            ParticleGenerator.renderDefaultSteam((ClientLevel) worldIn, selectedPos, rand);
        }
    }

    /**
     * Gets the tint color for this custom fluid.
     * Used by the rendering system to apply color to the grayscale texture.
     */
    public int getTintColor() {
        return definition.color();
    }

    /**
     * Gets whether particles should be shown for this fluid.
     */
    public boolean shouldShowParticles() {
        return definition.showParticles();
    }

    /**
     * Gets whether bubbles should be shown for this fluid.
     */
    public boolean shouldShowBubbles() {
        return definition.showBubbles();
    }

    /**
     * Gets whether steam should be shown for this fluid.
     * Steam is shown when temperature >= HOT_TEMPERATURE_THRESHOLD (35°C) AND show_steam is true.
     */
    public boolean shouldShowSteam() {
        return definition.isHot() && definition.showSteam();
    }

    @Override protected boolean shouldShowSteam(Level level,BlockPos pos) {
        return shouldShowSteam();
    }
    @Override protected boolean shouldShowBubbles(Level level,BlockPos pos) {
        return shouldShowBubbles();
    }
}
