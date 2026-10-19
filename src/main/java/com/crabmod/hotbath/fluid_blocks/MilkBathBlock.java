package com.crabmod.hotbath.fluid_blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

/**
 * Milk Bath Block
 */
public class MilkBathBlock extends AbstractHotbathBlock implements IInsideAreaTracker {
    public MilkBathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
        super(supplier, properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.bathe("milk_bath_fluid", entity, this);
    }
}
