package com.crabmod.hotbath.buckettest;

import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.crabmod.hotbath.registers.FluidsRegister;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@GameTestHolder("hotbath_bucket_capabilities")
@PrefixGameTestTemplate(false)
public final class CreateWaterloggedPipeDrainingTest {
    @GameTest(template = "empty_1x1", timeoutTicks = 50)
    public static void test_create_waterlogged_pipe_draining(GameTestHelper helper) {
        if (!net.neoforged.fml.ModList.get().isLoaded("create")) {
            check(!Boolean.getBoolean("hotbath.requireCreateBucketTests"), "Create is required for this run");
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        var original = new LinkedHashMap<BlockPos, BlockState>();
        var inputs = List.of(CustomFluidStackHelper.createStack(id("hotbath:logged_first"), 1000),
                CustomFluidStackHelper.createStack(id("hotbath:logged_second"), 1000),
                new FluidStack(FluidsRegister.HOT_WATER_FLUID.get(), 1000), new FluidStack(Fluids.WATER, 1000));
        try {
            for (int mode = 0; mode < 8; mode++) {
                BlockPos pipePos = helper.absolutePos(new BlockPos((mode % 4) * 2, 3, (mode / 4) * 3 + 1));
                BlockPos pos = pipePos.below();
                check(level.getBlockEntity(pos) == null
                        && HotbathWaterloggingHelper.getStoredFluidType((LevelAccessor) level, pos) == null
                        && HotbathWaterloggingHelper.getStoredCustomFluidId(level, pos) == null, "Dirty test fixture");
                original.put(pos, level.getBlockState(pos));
                var input = inputs.get(mode % 4);
                var wet = (mode < 4 ? Blocks.STONE_SLAB : Blocks.OAK_STAIRS).defaultBlockState()
                        .setValue(BlockStateProperties.WATERLOGGED, true);
                level.setBlockAndUpdate(pos, wet);
                if (input.getFluid() != Fluids.WATER) HotbathWaterloggingHelper.storeFluidType(level, pos, input.getFluid());
                var fluidId = CustomFluidStackHelper.getFluidId(input);
                if (fluidId != null) HotbathWaterloggingHelper.storeCustomFluidId(level, pos, fluidId);
                check(level.getFluidState(pos).getType() == input.getFluid(), "World fixture type missing");
                var saved = savedPipe(pipePos);
                var pipe = OpenEndedPipe.fromNBT(saved, level.registryAccess(), pipePos);
                pipe.manageSource(level, null);
                var handler = pipe.provideHandler().getCapability();
                var simulated = mode < 4 ? handler.drain(1000, FluidAction.SIMULATE)
                        : handler.drain(input.copy(), FluidAction.SIMULATE);
                check(matches(simulated, input, 1000), "Simulated waterlogged drain changed fluid identity");
                check(level.getBlockState(pos).equals(wet)
                        && Objects.equals(fluidId, HotbathWaterloggingHelper.getStoredCustomFluidId(level, pos)),
                        "Simulation modified source");
                var drained = mode < 4 ? handler.drain(1000, FluidAction.EXECUTE)
                        : handler.drain(input.copy(), FluidAction.EXECUTE);
                check(matches(drained, input, 1000), "Executed waterlogged drain changed fluid identity or amount");
                check(level.getBlockState(pos).equals(wet.setValue(BlockStateProperties.WATERLOGGED, false)),
                        "Draining did not preserve the dry block");
                check(HotbathWaterloggingHelper.getStoredFluidType((LevelAccessor) level, pos) == null
                        && HotbathWaterloggingHelper.getStoredCustomFluidId(level, pos) == null, "Drained metadata retained");
                level.setBlockAndUpdate(pos, wet);
                check(level.getFluidState(pos).getType() == Fluids.WATER, "Plain water inherited the previous bath");
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            BlockPos pipePos = helper.absolutePos(new BlockPos(0, 3, 9));
            BlockPos pos = pipePos.below();
            check(level.getBlockEntity(pos) == null, "Dirty partial fixture");
            original.put(pos, level.getBlockState(pos));
            level.setBlockAndUpdate(pos, Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
            var input = inputs.get(0);
            HotbathWaterloggingHelper.storeFluidType(level, pos, input.getFluid());
            HotbathWaterloggingHelper.storeCustomFluidId(level, pos, CustomFluidStackHelper.getFluidId(input));
            var saved = savedPipe(pipePos);
            var pipe = OpenEndedPipe.fromNBT(saved, level.registryAccess(), pipePos);
            pipe.manageSource(level, null);
            var handler = pipe.provideHandler().getCapability();
            var first = handler.drain(250, FluidAction.EXECUTE);
            var buffered = handler.getFluidInTank(0).copy();
            var remaining = handler.drain(750, FluidAction.EXECUTE);
            check(matches(first, input, 250) && matches(buffered, input, 750) && matches(remaining, input, 750),
                    "Partial waterlogged extraction lost identity or volume");
            check(HotbathWaterloggingHelper.getStoredFluidType((LevelAccessor) level, pos) == null
                    && HotbathWaterloggingHelper.getStoredCustomFluidId(level, pos) == null, "Partial drain retained metadata");
        } finally {
            for (var entry : original.entrySet()) {
                HotbathWaterloggingHelper.removeFluidType(level, entry.getKey());
                level.removeBlockEntity(entry.getKey());
                level.setBlockAndUpdate(entry.getKey(), entry.getValue());
            }
        }
        helper.succeed();
    }
    private static boolean matches(FluidStack actual, FluidStack expected, int amount) {
        return actual.getFluid() == expected.getFluid() && actual.getAmount() == amount
                && Objects.equals(CustomFluidStackHelper.getFluidId(actual), CustomFluidStackHelper.getFluidId(expected));
    }
    private static CompoundTag savedPipe(BlockPos pos) {
        var location = new CompoundTag();
        location.put("Pos", NbtUtils.writeBlockPos(pos));
        location.putString("Face", Direction.DOWN.name());
        var tag = new CompoundTag();
        tag.put("Location", location);
        return tag;
    }
    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
