package com.crabmod.hotbath.buckettest;

import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.LinkedHashMap;

@GameTestHolder("hotbath_bucket_capabilities")
@PrefixGameTestTemplate(false)
public final class CreateOpenPipeDrainingTest {
    @GameTest(template = "empty_32x32x32", timeoutTicks = 40)
    public static void test_create_open_pipe_draining_identity(GameTestHelper helper) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("create")) {
            check(!Boolean.getBoolean("hotbath.requireCreateBucketTests"), "Create is required for this run");
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        var originals = new LinkedHashMap<BlockPos, BlockState>();
        var first = id("hotbath:drain_identity_first");
        var second = id("hotbath:drain_identity_second");
        try {
            for (int mode = 0; mode < 4; mode++) {
                BlockPos pipePos = helper.absolutePos(new BlockPos(mode * 2, 3, 1));
                BlockPos output = pipePos.below();
                check(level.getBlockEntity(output) == null, "Existing test block entity");
                originals.put(output, level.getBlockState(output));
                level.setBlockAndUpdate(output, DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get()
                        .defaultFluidState().createLegacyBlock());
                ResourceLocation expected = mode % 2 == 0 ? first : second;
                check(CustomFluidStackHelper.setFluidIdAt(level, output, expected), "Fixture identity missing");
                var before = level.getBlockState(output);
                var saved = savedPipe(pipePos);
                var pipe = OpenEndedPipe.fromNBT(saved, pipePos);
                pipe.manageSource(level);
                var handler = pipe.provideHandler().orElseThrow(() -> new AssertionError("Missing open pipe handler"));
                var input = CustomFluidStackHelper.createStack(expected, 1000);
                var wrong = CustomFluidStackHelper.createStack(expected.equals(first) ? second : first, 1000);
                check(handler.drain(wrong, FluidAction.SIMULATE).isEmpty(), "Wrong-ID simulation accepted");
                var simulated = mode < 2 ? handler.drain(1000, FluidAction.SIMULATE)
                        : handler.drain(input.copy(), FluidAction.SIMULATE);
                check(simulated.getAmount() == 1000 && expected.equals(CustomFluidStackHelper.getFluidId(simulated)),
                        "Simulated drain lost bath identity");
                check(level.getBlockState(output).equals(before)
                        && expected.equals(CustomFluidStackHelper.getFluidIdAt(level, output)), "Simulation changed source");
                var drained = mode < 2 ? handler.drain(1000, FluidAction.EXECUTE)
                        : handler.drain(input.copy(), FluidAction.EXECUTE);
                check(drained.getAmount() == 1000 && expected.equals(CustomFluidStackHelper.getFluidId(drained)),
                        "Executed drain lost bath identity");
                check(!level.getFluidState(output).isSource(), "Drained source still present");
            }
            BlockPos pipePos = helper.absolutePos(new BlockPos(0, 3, 5));
            BlockPos output = pipePos.below();
            check(level.getBlockEntity(output) == null, "Existing buffered fixture");
            originals.put(output, level.getBlockState(output));
            var saved = savedPipe(pipePos);
            var pipe = OpenEndedPipe.fromNBT(saved, pipePos);
            pipe.manageSource(level);
            var handler = pipe.provideHandler().orElseThrow(() -> new AssertionError("Missing open pipe handler"));
            for (var expected : java.util.List.of(first, second)) {
                level.removeBlockEntity(output);
                level.setBlockAndUpdate(output, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(output, DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get()
                        .defaultFluidState().createLegacyBlock());
                check(CustomFluidStackHelper.setFluidIdAt(level, output, expected)
                        && expected.equals(CustomFluidStackHelper.getFluidIdAt(level, output)), "Buffered source fixture ID missing");
                var small = handler.drain(250, FluidAction.EXECUTE);
                var buffered = handler.getFluidInTank(0).copy();
                var simulated = handler.drain(250, FluidAction.SIMULATE);
                check(handler.getFluidInTank(0).getAmount() == 750, "Buffer simulation consumed fluid");
                var remaining = handler.drain(750, FluidAction.EXECUTE);
                check(small.getAmount() == 250 && buffered.getAmount() == 750
                        && simulated.getAmount() == 250 && remaining.getAmount() == 750
                        && handler.getFluidInTank(0).isEmpty(), "Partial drain changed volume");
                for (var stack : java.util.List.of(small, buffered, simulated, remaining)) {
                    check(expected.equals(CustomFluidStackHelper.getFluidId(stack)), "Buffered drain lost or reused another ID: expected " + expected
                            + ", actual " + CustomFluidStackHelper.getFluidId(stack));
                }
            }
            pipePos = helper.absolutePos(new BlockPos(4096, 3, 4096));
            check(!level.isLoaded(pipePos.below()), "Unloaded fixture already loaded");
            saved = savedPipe(pipePos);
            pipe = OpenEndedPipe.fromNBT(saved, pipePos);
            pipe.manageSource(level);
            handler = pipe.provideHandler().orElseThrow(() -> new AssertionError("Missing open pipe handler"));
            check(handler.drain(1000, FluidAction.SIMULATE).isEmpty()
                    && handler.drain(1000, FluidAction.EXECUTE).isEmpty()
                    && !level.isLoaded(pipePos.below()), "Drain loaded an unavailable outlet chunk");
        } finally {
            for (var entry : originals.entrySet()) {
                level.removeBlockEntity(entry.getKey());
                level.setBlockAndUpdate(entry.getKey(), entry.getValue());
            }
        }
        helper.succeed();
    }

    private static CompoundTag savedPipe(BlockPos pos) {
        var location = new CompoundTag();
        location.put("Pos", NbtUtils.writeBlockPos(pos));
        location.putString("Face", Direction.DOWN.name());
        var saved = new CompoundTag();
        saved.put("Location", location);
        return saved;
    }
    private static ResourceLocation id(String value) { return new ResourceLocation(value); }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
