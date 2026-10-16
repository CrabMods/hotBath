package com.crabmod.hotbath.buckettest;

import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyBlockEntity;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@GameTestHolder("hotbath_bucket_capabilities")
@PrefixGameTestTemplate(false)
public final class CreateHosePulleyDrainingTest {
    @GameTest(template = "empty_1x1", timeoutTicks = 100)
    public static void test_create_hose_custom_draining(GameTestHelper helper) {
        if (!net.neoforged.fml.ModList.get().isLoaded("create")) {
            if (Boolean.getBoolean("hotbath.requireCreateBucketTests")) {
                throw new AssertionError("Create is required for this run");
            }
            helper.succeed();
            return;
        }
        CreateCases.run(helper);
    }

    private static final class CreateCases {
        private static void run(GameTestHelper helper) {
            var level = helper.getLevel();
            var root = helper.absolutePos(new BlockPos(3, 3, 3));
            var original = new LinkedHashMap<BlockPos, BlockState>();
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = -1; y <= 4; y++) {
                var pos = root.offset(x, y, z);
                check(level.getBlockEntity(pos) == null, "Pre-existing fixture block entity");
                original.put(pos, level.getBlockState(pos));
            }
            var baths = List.of(CustomFluidStackHelper.createStack(id("hotbath:hose_first"), 1000),
                    CustomFluidStackHelper.createStack(id("hotbath:hose_second"), 1000));
            try {
                for (int i = 0; i < baths.size(); i++) {
                    var first = baths.get(i);
                    var other = baths.get(1 - i);
                    clear(level, original);
                    checkSameFluid(level, root, first);
                    clear(level, original);
                    var handler = arrange(level, root, first, other, true);
                    check(matches(handler.drain(250, FluidAction.EXECUTE), first, 250), "Initial partial drain changed");
                    check(matches(handler.getFluidInTank(0), first, 750), "Partial buffer changed");
                    var before = level.getBlockState(root);
                    var beforeId = CustomFluidStackHelper.getFluidIdAt(level, root);
                    check(matches(handler.drain(1000, FluidAction.SIMULATE), first, 750), "Boundary simulation changed");
                    checkSource(level, root, before, beforeId);
                    check(matches(handler.drain(1000, FluidAction.EXECUTE), first, 750), "Boundary returned wrong bath");
                    checkSource(level, root, before, beforeId);
                    check(matches(handler.drain(1000, FluidAction.EXECUTE), other, 1000), "Different source was lost");

                    clear(level, original);
                    handler = arrange(level, root, first, first, false);
                    before = level.getBlockState(root);
                    beforeId = CustomFluidStackHelper.getFluidIdAt(level, root);
                    check(handler.drain(other.copy(), FluidAction.SIMULATE).isEmpty(), "Wrong-ID simulation accepted");
                    checkSource(level, root, before, beforeId);
                    check(handler.drain(other.copy(), FluidAction.EXECUTE).isEmpty(), "Wrong-ID execution accepted");
                    checkSource(level, root, before, beforeId);
                }
                for (var fluid : List.of(Fluids.WATER, Fluids.LAVA)) {
                    clear(level, original);
                    checkSameFluid(level, root, new FluidStack(fluid, 1000));
                }
            } finally {
                for (var entry : original.entrySet()) {
                    level.removeBlockEntity(entry.getKey());
                    level.setBlock(entry.getKey(), entry.getValue(), 2);
                }
            }
            helper.succeed();
        }

        private static void checkSameFluid(ServerLevel level, BlockPos root, FluidStack input) {
            var handler = arrange(level, root, input, input, true);
            check(matches(handler.drain(250, FluidAction.EXECUTE), input, 250), "First partial drain changed");
            check(matches(handler.drain(1000, FluidAction.EXECUTE), input, 1000), "Same-fluid top-up changed");
            check(matches(handler.drain(1000, FluidAction.EXECUTE), input, 750), "Remaining buffer changed");
        }

        private static void checkSource(ServerLevel level, BlockPos pos, BlockState state, ResourceLocation fluidId) {
            check(level.getBlockState(pos).equals(state)
                    && Objects.equals(CustomFluidStackHelper.getFluidIdAt(level, pos), fluidId),
                    "Rejected extraction removed or changed the source");
        }

        private static IFluidHandler arrange(ServerLevel level, BlockPos root, FluidStack upper,
                                            FluidStack lower, boolean twoSources) {
            for (var pos : List.of(root.below(), root.west(), root.east(), root.north(), root.south(),
                    root.above().west(), root.above().east(), root.above().north(), root.above().south())) {
                level.setBlock(pos, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 2);
            }
            source(level, root, lower);
            if (twoSources) source(level, root.above(), upper);
            var hosePos = root.above(4);
            level.setBlockAndUpdate(hosePos, BuiltInRegistries.BLOCK.get(id("create:hose_pulley")).defaultBlockState());
            var hose = (HosePulleyBlockEntity) level.getBlockEntity(hosePos);
            check(hose != null, "Native hose fixture missing");
            var drainer = new FluidDrainingBehaviour(hose);
            for (int i = 0; i < 16; i++) {
                drainer.pullNext(root, true);
                drainer.tick();
            }
            // Public native constructors keep Ponder out of the development test's build dependencies.
            var handler = new HosePulleyFluidHandler(new SmartFluidTank(1500, ignored -> {}),
                    new FluidFillingBehaviour(hose), drainer, () -> root, () -> true);
            check(matches(handler.getFluidInTank(0), upper, 1000), "Native search selected the wrong source");
            return handler;
        }

        private static void source(ServerLevel level, BlockPos pos, FluidStack input) {
            level.setBlock(pos, input.getFluid().defaultFluidState().createLegacyBlock(), 2);
            var fluidId = CustomFluidStackHelper.getFluidId(input);
            if (fluidId != null) {
                check(CustomFluidStackHelper.setFluidIdAt(level, pos, fluidId), "Fixture identity missing");
            }
        }

        private static void clear(ServerLevel level, Map<BlockPos, BlockState> original) {
            for (var pos : original.keySet()) {
                level.removeBlockEntity(pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }

        private static boolean matches(FluidStack actual, FluidStack expected, int amount) {
            return actual.getAmount() == amount && actual.getFluid() == expected.getFluid()
                    && Objects.equals(CustomFluidStackHelper.getFluidId(actual), CustomFluidStackHelper.getFluidId(expected));
        }

        private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
        private static void check(boolean value, String message) {
            if (!value) throw new AssertionError(message);
        }
    }
}
