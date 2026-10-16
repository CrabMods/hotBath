package com.crabmod.hotbath.buckettest;

import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyBlockEntity;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

@GameTestHolder("hotbath_bucket_capabilities")
@PrefixGameTestTemplate(false)
public final class CreateHosePulleyFillingTest {
    @GameTest(template = "empty_1x1", timeoutTicks = 100)
    public static void test_create_hose_filling_large_area(GameTestHelper helper) {
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
            var root = helper.absolutePos(new BlockPos(3, 20, 3));
            var path = new ArrayList<BlockPos>();
            for (int x = 0; x <= 16; x++) path.add(root.east(x));
            for (int y = 1; y <= 16; y++) path.add(root.east(16).below(y));
            for (int z = 1; z <= 16; z++) path.add(root.east(16).below(16).south(z));
            var owned = new LinkedHashSet<BlockPos>();
            for (var pos : path) {
                owned.add(pos);
                for (var direction : Direction.values()) owned.add(pos.relative(direction));
            }
            var hosePos = root.above(4);
            owned.add(hosePos);
            var original = new LinkedHashMap<BlockPos, BlockState>();
            for (var pos : owned) {
                check(level.getBlockEntity(pos) == null, "Existing fixture block entity");
                original.put(pos, level.getBlockState(pos));
            }
            try {
                for (var bath : List.of("golden_bath", "slime_bath", "blazing_bath")) {
                    for (var pos : owned) {
                        level.removeBlockEntity(pos);
                        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
                    }
                    for (var pos : path) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    level.setBlockAndUpdate(hosePos, BuiltInRegistries.BLOCK.get(id("create:hose_pulley")).defaultBlockState());
                    var hose = (HosePulleyBlockEntity) level.getBlockEntity(hosePos);
                    var filler = new FluidFillingBehaviour(hose);
                    var tank = new SmartFluidTank(1500, ignored -> {});
                    var handler = new HosePulleyFluidHandler(tank, filler, new FluidDrainingBehaviour(hose), () -> root, () -> true);
                    var fluidId = id("hotbath:" + bath);
                    var input = CustomFluidStackHelper.createStack(fluidId, 1000);
                    check(!filler.tryDeposit(input.getFluid(), root, true), "Search initialization changed");
                    for (var pos : path) {
                        check(handler.fill(input.copy(), FluidAction.SIMULATE) == 1000 && tank.isEmpty(), "Simulation changed buffer");
                        check(level.isEmptyBlock(pos), "Simulation placed a source");
                        check(handler.fill(input.copy(), FluidAction.EXECUTE) == 1000 && tank.isEmpty(), "Deposit volume changed");
                    }
                    // 49 sources enclose a 17 x 17 x 17 bounding box, exceeding the old scan limit.
                    for (var pos : path) {
                        check(level.getFluidState(pos).isSource() && fluidId.equals(CustomFluidStackHelper.getFluidIdAt(level, pos)),
                                "Native hose source lost its bath ID at " + pos);
                    }
                }
            } finally {
                for (var entry : original.entrySet()) {
                    level.removeBlockEntity(entry.getKey());
                    level.setBlock(entry.getKey(), entry.getValue(), 2);
                }
                for (var entry : original.entrySet()) {
                    check(level.getBlockState(entry.getKey()).equals(entry.getValue()) && level.getBlockEntity(entry.getKey()) == null,
                            "Fixture restoration failed");
                }
            }
            helper.succeed();
        }

        private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
        private static void check(boolean value, String message) {
            if (!value) throw new AssertionError(message);
        }
    }
}
