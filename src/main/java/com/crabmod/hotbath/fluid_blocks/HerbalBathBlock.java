package com.crabmod.hotbath.fluid_blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

/**
 * Herbal Bath Block
 */
public class HerbalBathBlock extends AbstractHotbathBlock implements IInsideAreaTracker {
    public HerbalBathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
        super(supplier, properties);
    }

    @Override
    protected void applyBathEffects(Level level, BlockPos pos, Entity entity) {
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.bathe("herbal_bath_fluid", entity);
    }
}
