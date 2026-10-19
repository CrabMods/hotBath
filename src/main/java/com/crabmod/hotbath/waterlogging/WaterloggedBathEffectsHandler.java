package com.crabmod.hotbath.waterlogging;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.crabmod.hotbath.registers.CustomFluidBlocksRegister;
import com.crabmod.hotbath.registers.FluidsRegister;
import com.crabmod.hotbath.util.HotbathFluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = HotBath.MOD_ID)
public final class WaterloggedBathEffectsHandler {
    private static final double BOUNDS_EPSILON = 1.0E-4D;

    private WaterloggedBathEffectsHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }

        findWaterloggedBath(player)
                .ifPresent(match -> match.block().applyWaterloggedEntityInside(level, match.pos(), player));
    }

    @SubscribeEvent
    public static void onMobTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.LivingEntity entity
                && !entity.level().isClientSide() && entity.isInWater()
                && com.crabmod.hotbath.entity.FriendlyBathBenefits.isEligible(entity)) {
            findWaterloggedBath(entity).ifPresent(match ->
                    match.block().applyWaterloggedEntityInside(entity.level(), match.pos(), entity));
        }
    }

    private static Optional<WaterloggedBathMatch> findWaterloggedBath(net.minecraft.world.entity.LivingEntity player) {
        Level level = player.level();

        for (BlockPos pos : candidatePositions(player)) {
            BlockState state = level.getBlockState(pos);
            if (!state.hasProperty(BlockStateProperties.WATERLOGGED)
                    || !state.getValue(BlockStateProperties.WATERLOGGED)) {
                continue;
            }

            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(level, pos);
            AbstractHotbathBlock bathBlock = bathBlockForStoredFluid(storedFluid);
            if (bathBlock != null) {
                return Optional.of(new WaterloggedBathMatch(pos, bathBlock));
            }
        }

        return Optional.empty();
    }

    private static List<BlockPos> candidatePositions(net.minecraft.world.entity.LivingEntity player) {
        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
        addPosition(positions, player.blockPosition());
        addPosition(positions, BlockPos.containing(player.getEyePosition()));

        AABB bounds = player.getBoundingBox().deflate(BOUNDS_EPSILON);
        int minX = Mth.floor(bounds.minX);
        int minY = Mth.floor(bounds.minY);
        int minZ = Mth.floor(bounds.minZ);
        int maxX = Mth.floor(bounds.maxX);
        int maxY = Mth.floor(bounds.maxY);
        int maxZ = Mth.floor(bounds.maxZ);

        for (BlockPos candidate : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            addPosition(positions, candidate);
        }

        return new ArrayList<>(positions);
    }

    private static void addPosition(LinkedHashSet<BlockPos> positions, BlockPos pos) {
        if (pos != null) {
            positions.add(pos.immutable());
        }
    }

    private static AbstractHotbathBlock bathBlockForStoredFluid(Fluid storedFluid) {
        if (storedFluid == null) {
            return null;
        }

        Fluid sourceFluid = HotbathFluidHelper.getSourceFluid(storedFluid);
        if (sourceFluid == FluidsRegister.HOT_WATER_FLUID.get()) {
            return (AbstractHotbathBlock) FluidsRegister.HOT_WATER_BLOCK.get();
        }
        if (sourceFluid == FluidsRegister.HONEY_BATH_FLUID.get()) {
            return (AbstractHotbathBlock) FluidsRegister.HONEY_BATH_BLOCK.get();
        }
        if (sourceFluid == FluidsRegister.MILK_BATH_FLUID.get()) {
            return (AbstractHotbathBlock) FluidsRegister.MILK_BATH_BLOCK.get();
        }
        if (sourceFluid == FluidsRegister.HERBAL_BATH_FLUID.get()) {
            return (AbstractHotbathBlock) FluidsRegister.HERBAL_BATH_BLOCK.get();
        }
        if (sourceFluid == FluidsRegister.PEONY_BATH_FLUID.get()) {
            return (AbstractHotbathBlock) FluidsRegister.PEONY_BATH_BLOCK.get();
        }
        if (sourceFluid == FluidsRegister.ROSE_BATH_FLUID.get()) {
            return (AbstractHotbathBlock) FluidsRegister.ROSE_BATH_BLOCK.get();
        }
        if (sourceFluid == DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get()) {
            return CustomFluidBlocksRegister.CUSTOM_FLUID_BLOCK.get();
        }

        return null;
    }

    private record WaterloggedBathMatch(BlockPos pos, AbstractHotbathBlock block) {
    }
}
