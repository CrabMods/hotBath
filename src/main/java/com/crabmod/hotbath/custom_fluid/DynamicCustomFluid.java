package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom FlowingFluid implementation that propagates BlockEntity data when spreading.
 * This ensures that flowing fluid blocks inherit the color and fluid ID from their source.
 */
public abstract class DynamicCustomFluid extends BaseFlowingFluid {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCustomFluid.class);
    
    protected DynamicCustomFluid(Properties properties) {
        super(properties);
    }

    /**
     * Override spreadTo to copy BlockEntity data from source to new flowing block.
     * This ensures flowing fluid maintains the same color as the source.
     * Also checks waterlogging storage for custom fluid ID when spreading from waterlogged blocks.
     */
    @Override
    protected void spreadTo(@NotNull LevelAccessor level, @NotNull BlockPos pos, 
                            @NotNull BlockState blockState, @NotNull Direction direction, 
                            @NotNull FluidState fluidState) {
        // Get the source position (where the fluid is spreading FROM)
        BlockPos sourcePos = pos.relative(direction.getOpposite());
        
        // Get fluid data from the source block - check BlockEntity first, then waterlogging storage
        ResourceLocation fluidId = null;
        
        // Try to get from BlockEntity first
        BlockEntity sourceBe = level.getBlockEntity(sourcePos);
        if (sourceBe instanceof CustomFluidBlockEntity sourceFluidBe) {
            fluidId = sourceFluidBe.getFluidId();
        }
        
        // If not found in BlockEntity, try waterlogging storage
        // This handles the case when fluid spreads from a waterlogged block
        if (fluidId == null) {
            BlockState sourceState = level.getBlockState(sourcePos);
            if (sourceState.hasProperty(BlockStateProperties.WATERLOGGED) 
                    && sourceState.getValue(BlockStateProperties.WATERLOGGED)) {
                fluidId = HotbathWaterloggingHelper.getStoredCustomFluidId(level, sourcePos);
            }
        }
        
        // Call parent to place the fluid block
        super.spreadTo(level, pos, blockState, direction, fluidState);
        
        // Copy the fluid ID to the new block's BlockEntity
        BlockEntity newBe = level.getBlockEntity(pos);
        if (fluidId != null && newBe instanceof CustomFluidBlockEntity newFluidBe) {
            newFluidBe.setFluidId(fluidId);
        }
    }
    
    /**
     * A falling stream must preserve an existing custom-fluid source, including its ID.
     * Flowing blocks can still be replaced as the stream updates.
     */
    @Override
    protected boolean canBeReplacedWith(@NotNull FluidState state, @NotNull BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull Fluid fluidIn,
                                        @NotNull Direction direction) {
        return direction == Direction.DOWN && !state.isSource();
    }

    /**
     * Get the customFluidId at a position (from BlockEntity or waterlogging storage).
     */
    private static ResourceLocation getCustomFluidIdAt(BlockGetter level, BlockPos pos) {
        // Try BlockEntity first
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe) {
            return customBe.getFluidId();
        }
        
        // Try waterlogging storage (only works if level is LevelAccessor)
        if (level instanceof LevelAccessor levelAccessor) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(BlockStateProperties.WATERLOGGED) 
                    && state.getValue(BlockStateProperties.WATERLOGGED)) {
                return HotbathWaterloggingHelper.getStoredCustomFluidId(levelAccessor, pos);
            }
        }
        
        return null;
    }
    
    /**
     * Check if fluid can pass through the wall between two blocks.
     * Simplified version of FlowingFluid.canPassThroughWall (which is private).
     */
    private boolean hotbath$canPassThroughWall(Direction direction, BlockGetter level, 
                                                BlockPos pos, BlockState state, 
                                                BlockPos spreadPos, BlockState spreadState) {
        // Simple check: if collision shapes don't block the face, fluid can pass
        net.minecraft.world.phys.shapes.VoxelShape shape1 = state.getCollisionShape(level, pos);
        net.minecraft.world.phys.shapes.VoxelShape shape2 = spreadState.getCollisionShape(level, spreadPos);
        return !net.minecraft.world.phys.shapes.Shapes.mergedFaceOccludes(shape1, shape2, direction);
    }
    
    /**
     * Check if a fluid state is a source block of this fluid type.
     * Equivalent to FlowingFluid.isSourceBlockOfThisType (which is private).
     */
    private boolean hotbath$isSourceBlockOfThisType(FluidState state) {
        return state.getType().isSame(this) && state.isSource();
    }
    
    /**
     * Override getNewLiquid to check customFluidId when determining if flowing fluid has valid source.
     * 
     * <p>Problem: The default implementation uses isSame() which returns true for all dynamic
     * custom fluids (since they share DYNAMIC_FLUID_STILL/FLOWING). This means a milk_tea
     * flowing fluid would consider a green_tea source as valid support, preventing it from
     * disappearing when it should.</p>
     * 
     * <p>Solution: Override this method to also compare customFluidIds. A flowing fluid
     * should only consider a neighboring fluid as valid support if both the base type
     * AND the customFluidId match.</p>
     */
    @Override
    protected @NotNull FluidState getNewLiquid(@NotNull Level level, @NotNull BlockPos pos, 
                                                @NotNull BlockState blockState) {
        // Get this fluid's customFluidId
        ResourceLocation myCustomId = getCustomFluidIdAt(level, pos);
        
        int maxAmount = 0;
        int sourceCount = 0;
        ResourceLocation sourceCustomId = myCustomId;
        
        // Check horizontal neighbors
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            FluidState neighborFluid = neighborState.getFluidState();
            
            // Check if neighbor has same base fluid type AND can pass through wall
            if (neighborFluid.getType().isSame(this) && hotbath$canPassThroughWall(direction, level, pos, blockState, neighborPos, neighborState)) {
                ResourceLocation neighborCustomId = getCustomFluidIdAt(level, neighborPos);
                
                // CustomFluidId matching logic:
                // - If myCustomId is null (flowing fluid not yet tagged), accept any same-type neighbor
                // - If myCustomId is not null, require exact match with neighbor
                // This allows flowing fluid to work normally while still detecting mismatches
                boolean customIdMatches = (myCustomId == null) 
                        || (neighborCustomId != null && myCustomId.equals(neighborCustomId));
                
                if (customIdMatches) {
                    if (neighborFluid.isSource() && neighborCustomId != null && net.neoforged.neoforge.event.EventHooks.canCreateFluidSource(level, neighborPos, neighborState)) {
                        if (sourceCustomId == null) sourceCustomId = neighborCustomId;
                        if (sourceCustomId.equals(neighborCustomId)) sourceCount++;
                    }
                    maxAmount = Math.max(maxAmount, neighborFluid.getAmount());
                }
            }
        }
        
        // Check if can become source (2+ source neighbors and solid below or source of same type below)
        if (com.crabmod.hotbath.HotBathConfig.areInfiniteBathSourcesEnabled() && sourceCount >= 2) {
            BlockState belowState = level.getBlockState(pos.below());
            FluidState belowFluid = belowState.getFluidState();
            if (belowState.isSolid() || hotbath$isSourceBlockOfThisType(belowFluid)) {
                // Also check customFluidId for the below fluid if it exists
                if (!belowFluid.isEmpty() && belowFluid.getType().isSame(this)) {
                    ResourceLocation belowCustomId = getCustomFluidIdAt(level, pos.below());
                    boolean customIdMatches = sourceCustomId != null && sourceCustomId.equals(belowCustomId);
                    if (customIdMatches) {
                        return getSource(false);
                    }
                } else if (belowState.isSolid()) {
                    return getSource(false);
                }
            }
        }
        
        // Check fluid above
        // For the fluid above check, we don't need canPassThroughWall because:
        // 1. If it's a fluid block above, liquid naturally flows down
        // 2. If it's a waterlogged block above, the fluid inside supports flow down
        // We only need to verify the customFluidId matches
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        FluidState aboveFluid = aboveState.getFluidState();
        if (!aboveFluid.isEmpty() && aboveFluid.getType().isSame(this)) {
            // Check customFluidId matches
            ResourceLocation aboveCustomId = getCustomFluidIdAt(level, abovePos);
            boolean customIdMatches = (myCustomId == null) 
                    || (aboveCustomId != null && myCustomId.equals(aboveCustomId));
            
            if (customIdMatches) {
                return getFlowing(8, true);
            }
            // If customFluidId doesn't match, this fluid has lost its source support
            // Fall through to return EMPTY or reduced flow
        }
        
        // Calculate new amount based on neighbors (minus drop-off)
        int newAmount = maxAmount - getDropOff(level);
        
        return newAmount <= 0 ? Fluids.EMPTY.defaultFluidState() : getFlowing(newAmount, false);
    }
    
    /**
     * Override tick to ensure flowing fluids inherit customFluidId from their source.
     * This handles the case where the fluid was created without going through spreadTo
     * (e.g., via vanilla mechanics or chunk loading).
     * 
     * Also schedules tick updates for neighboring flowing fluids when this fluid disappears,
     * ensuring the entire flow chain is updated.
     */
    @Override
    public void tick(@NotNull Level level, @NotNull BlockPos pos, @NotNull FluidState state) {
        // Before normal tick processing, ensure we have a customFluidId
        if (!state.isSource()) {
            ensureCustomFluidId(level, pos);
        }
        
        // Get the current state before tick
        FluidState beforeTick = level.getFluidState(pos);
        
        // Call parent tick
        super.tick(level, pos, state);
        
        // Check if the fluid disappeared or changed significantly after tick
        FluidState afterTick = level.getFluidState(pos);
        boolean fluidDisappeared = !beforeTick.isEmpty() && afterTick.isEmpty();
        boolean fluidReduced = !beforeTick.isEmpty() && !afterTick.isEmpty() 
                && afterTick.getAmount() < beforeTick.getAmount();
        
        // If this fluid disappeared or was reduced, schedule tick updates for neighboring flowing fluids
        // This ensures the entire flow chain is updated when a source is changed
        if (fluidDisappeared || fluidReduced) {
            scheduleNeighborFlowingFluidTicks(level, pos);
        }
    }
    
    /**
     * Schedule tick updates for all neighboring flowing fluid blocks.
     * This propagates the update through the entire flow chain.
     */
    private void scheduleNeighborFlowingFluidTicks(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            FluidState neighborFluid = level.getFluidState(neighborPos);
            
            // Only schedule for flowing fluids (not sources)
            if (!neighborFluid.isEmpty() 
                    && neighborFluid.getType().isSame(this) 
                    && !neighborFluid.isSource()) {
                level.scheduleTick(neighborPos, neighborFluid.getType(), 1);
            }
        }
    }
    
    /**
     * Ensure this fluid block has a customFluidId by inheriting from above if needed.
     */
    private void ensureCustomFluidId(Level level, BlockPos pos) {
        ResourceLocation myCustomId = getCustomFluidIdAt(level, pos);
        if (myCustomId != null) {
            return; // Already has ID
        }
        
        // Try to inherit from above
        BlockPos abovePos = pos.above();
        FluidState aboveFluid = level.getFluidState(abovePos);
        if (!aboveFluid.isEmpty() && aboveFluid.getType().isSame(this)) {
            ResourceLocation aboveCustomId = getCustomFluidIdAt(level, abovePos);
            if (aboveCustomId != null) {
                // Set our customFluidId to match the one above
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CustomFluidBlockEntity customBe) {
                    customBe.setFluidId(aboveCustomId);
                    LOGGER.debug("ensureCustomFluidId: Inherited {} from above at {}", aboveCustomId, pos);
                }
            }
        }
    }

    /**
     * Source (still) version of the dynamic custom fluid.
     */
    public static class Source extends DynamicCustomFluid {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(@NotNull FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(@NotNull FluidState state) {
            return true;
        }
    }

    /**
     * Flowing version of the dynamic custom fluid.
     */
    public static class Flowing extends DynamicCustomFluid {
        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.material.Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(@NotNull FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(@NotNull FluidState state) {
            return false;
        }
    }
}
