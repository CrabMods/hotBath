package com.crabmod.hotbath.buckettest;

import com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity;
import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.List;

@GameTestHolder("hotbath_bucket_capabilities")
@PrefixGameTestTemplate(false)
public final class CustomFluidSourceTest {
    @GameTest(template = "empty_32x32x32", batch = "custom_source_stacking", timeoutTicks = 200)
    public static void test_stacked_sources_and_downward_flow(GameTestHelper helper) {
        var level = helper.getLevel();
        var original = new LinkedHashMap<BlockPos, BlockState>();
        var first = id("hotbath:source_test_first");
        var second = id("hotbath:source_test_second");
        var roots = List.of(helper.absolutePos(new BlockPos(2, 3, 2)),
                helper.absolutePos(new BlockPos(6, 3, 2)),
                helper.absolutePos(new BlockPos(10, 3, 2)),
                helper.absolutePos(new BlockPos(14, 3, 2)),
                helper.absolutePos(new BlockPos(18, 3, 2)));
        try {
            for (int i = 0; i < roots.size(); i++) {
                var root = roots.get(i);
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        for (int y = -1; y <= 2; y++) {
                            var pos = root.offset(x, y, z);
                            check(level.getBlockEntity(pos) == null, "Existing fixture entity");
                            original.put(pos, level.getBlockState(pos));
                            level.setBlockAndUpdate(pos, (x != 0 || z != 0 || y == -1)
                                    ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                        }
                    }
                }
                if (i != 2) source(level, root, first);
                if (i >= 3) {
                    level.setBlockAndUpdate(root.above(), (i == 3 ? Fluids.WATER : Fluids.LAVA)
                            .defaultFluidState().createLegacyBlock());
                } else {
                    source(level, root.above(), i == 1 ? second : first);
                }
            }
        } catch (Throwable failure) {
            restore(level, original);
            throw failure;
        }
        helper.runAfterDelay(80, () -> {
            try {
                for (int i = 0; i < roots.size(); i++) {
                    if (i == 2) continue;
                    var root = roots.get(i);
                    check(level.getFluidState(root).isSource()
                            && level.getFluidState(root.above()).isSource(), "Stacked source was consumed");
                    check(first.equals(CustomFluidStackHelper.getFluidIdAt(level, root)),
                            "Lower source identity changed");
                    if (i < 2) {
                        check((i == 1 ? second : first).equals(
                                CustomFluidStackHelper.getFluidIdAt(level, root.above())),
                                "Upper source identity changed");
                    } else {
                        check(level.getFluidState(root.above()).getType().isSame(
                                i == 3 ? Fluids.WATER : Fluids.LAVA), "Upper vanilla source changed");
                    }
                }
                var flow = roots.get(2);
                check(!level.getFluidState(flow).isEmpty() && !level.getFluidState(flow).isSource()
                        && first.equals(CustomFluidStackHelper.getFluidIdAt(level, flow)),
                        "Downward flow lost its state or identity");
                level.setBlockAndUpdate(flow.above(), Blocks.AIR.defaultBlockState());
            } catch (Throwable failure) {
                restore(level, original);
                throw failure;
            }
            helper.runAfterDelay(80, () -> {
                try {
                    check(level.getFluidState(roots.get(2)).isEmpty(), "Orphan flow did not clear");
                } finally {
                    restore(level, original);
                }
                helper.succeed();
            });
        });
    }

    private static void source(ServerLevel level, BlockPos pos, ResourceLocation id) {
        level.setBlockAndUpdate(pos, DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get()
                .defaultFluidState().createLegacyBlock());
        ((CustomFluidBlockEntity) level.getBlockEntity(pos)).setFluidId(id);
    }

    private static void restore(ServerLevel level, LinkedHashMap<BlockPos, BlockState> original) {
        for (var entry : original.entrySet()) {
            level.removeBlockEntity(entry.getKey());
            level.setBlockAndUpdate(entry.getKey(), entry.getValue());
        }
        for (var entry : original.entrySet()) {
            check(level.getBlockState(entry.getKey()).equals(entry.getValue()), "Fixture restoration failed");
        }
    }

    private static ResourceLocation id(String value) { return new ResourceLocation(value); }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
