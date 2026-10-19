package com.crabmod.hotbath.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** The existing bath herb can be planted directly on dirt or farmland. */
public final class BathHerbCropBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape[] SHAPES = {Block.box(5,0,5,11,5,11),Block.box(3,0,3,13,9,13),
            Block.box(2,0,2,14,12,14),Block.box(1,0,1,15,15,15)};

    public BathHerbCropBlock(Properties properties) {
        super(properties);registerDefaultState(stateDefinition.any().setValue(AGE,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AGE);}
    @Override protected boolean mayPlaceOn(BlockState state,BlockGetter level,BlockPos pos){return state.is(BlockTags.DIRT)||state.is(Blocks.FARMLAND);}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPES[state.getValue(AGE)];}
    @Override public boolean isRandomlyTicking(BlockState state){return state.getValue(AGE)<3;}
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(level.getRawBrightness(pos,0)>=9&&state.getValue(AGE)<3&&random.nextInt(5)==0)
            level.setBlockAndUpdate(pos,state.setValue(AGE,state.getValue(AGE)+1));
    }
    @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state,boolean client){return state.getValue(AGE)<3;}
    @Override public boolean isBonemealSuccess(Level level,RandomSource random,BlockPos pos,BlockState state){return true;}
    @Override public void performBonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state){
        level.setBlockAndUpdate(pos,state.setValue(AGE,Math.min(3,state.getValue(AGE)+1+random.nextInt(2))));
    }
}
