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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.List;

@GameTestHolder("hotbath_bucket_capabilities")
@PrefixGameTestTemplate(false)
public final class CreateOpenPipeIdentityTest {
    @GameTest(template = "empty_32x32x32", timeoutTicks = 30)
    public static void test_create_open_pipe_world_identity(GameTestHelper helper) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("create")) {
            check(!Boolean.getBoolean("hotbath.requireCreateBucketTests"), "Create is required for this run");
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        var original = new LinkedHashMap<BlockPos, BlockState>();
        // Identity transport must not depend on modifying the global definition registry.
        var first = id("hotbath:pipe_identity_first");
        var second = id("hotbath:pipe_identity_second");
        try {
            for (int mode = 0; mode < 4; mode++) {
                BlockPos pipePos = helper.absolutePos(new BlockPos(mode * 2, 3, 1));
                BlockPos output = pipePos.below();
                for (var pos : List.of(pipePos, output)) {
                    check(level.getBlockEntity(pos) == null, "Existing test block entity");
                    original.put(pos, level.getBlockState(pos));
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                if (mode > 0) {
                    level.setBlockAndUpdate(output, DynamicFluidRegistry.DYNAMIC_FLUID_FLOWING.get()
                            .getFlowing(4, false).createLegacyBlock());
                    check(CustomFluidStackHelper.setFluidIdAt(level, output, mode == 2 ? second : first),
                            "Existing fluid fixture missing identity");
                }
                var before = level.getBlockState(output);
                var savedPipe = savedPipe(pipePos);
                var pipe = OpenEndedPipe.fromNBT(savedPipe, pipePos);
                pipe.manageSource(level);
                var handler = pipe.provideHandler().orElseThrow(() -> new AssertionError("Missing open pipe handler"));
                var input = CustomFluidStackHelper.createStack(mode == 3 ? second : first, 1000);
                int simulated = handler.fill(input.copy(), FluidAction.SIMULATE);
                check(level.getBlockState(output).equals(before), "Simulation changed the world");
                boolean blocked = mode >= 2;
                check(blocked ? simulated == 0 : simulated > 0, "Wrong simulated transfer result");
                int filled = 0;
                while (filled < 1000) {
                    var remaining = input.copy();
                    remaining.setAmount(1000 - filled);
                    int accepted = handler.fill(remaining, FluidAction.EXECUTE);
                    if (accepted == 0) break;
                    check(accepted > 0 && accepted <= 1000 - filled, "Invalid transfer amount");
                    filled += accepted;
                }
                if (blocked) {
                    check(filled == 0 && level.getBlockState(output).equals(before), "Different fluid was replaced");
                    check((mode == 2 ? second : first).equals(CustomFluidStackHelper.getFluidIdAt(level, output)),
                            "Existing fluid identity changed");
                } else {
                    check(filled == 1000 && level.getFluidState(output).isSource(), "Pipe did not place a source");
                    check(first.equals(CustomFluidStackHelper.getFluidIdAt(level, output)), "Placed identity lost");
                }
            }
            BlockPos pipePos = helper.absolutePos(new BlockPos(4096, 3, 4096));
            check(!level.isLoaded(pipePos.below()), "Unloaded outlet fixture is already loaded");
            var savedPipe = savedPipe(pipePos);
            var pipe = OpenEndedPipe.fromNBT(savedPipe, pipePos);
            pipe.manageSource(level);
            var handler = pipe.provideHandler().orElseThrow(() -> new AssertionError("Missing open pipe handler"));
            var input = CustomFluidStackHelper.createStack(first, 1000);
            check(handler.fill(input.copy(), FluidAction.SIMULATE) == 0
                    && handler.fill(input, FluidAction.EXECUTE) == 0
                    && !level.isLoaded(pipePos.below()), "Open pipe loaded an unavailable outlet chunk");
        } finally {
            for (var entry : original.entrySet()) {
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
        var savedPipe = new CompoundTag();
        savedPipe.put("Location", location);
        return savedPipe;
    }

    private static ResourceLocation id(String value) { return new ResourceLocation(value); }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
