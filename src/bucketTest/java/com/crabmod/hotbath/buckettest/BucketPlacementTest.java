package com.crabmod.hotbath.buckettest;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@GameTestHolder("hotbath_bucket_capabilities")
@PrefixGameTestTemplate(false)
public final class BucketPlacementTest {
    @GameTest(template = "empty_1x1", timeoutTicks = 100)
    public static void test_rejected_bucket_placement_retains_contents(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(3, 3, 3));
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "bucket-boundary"));
        var original = new LinkedHashMap<BlockPos, BlockState>();
        var baths = List.of(id("hotbath:golden_bath"), id("hotbath:slime_bath"), id("hotbath:blazing_bath"));
        try {
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().invulnerable = true;
            player.setNoGravity(true);
            for (int mode = 0; mode < 3; mode++) for (int variant = 0; variant < 5; variant++) {
                int y = mode == 0 ? level.getMaxBuildHeight() - 1 : mode == 1 ? level.getMinBuildHeight() : 100;
                var clicked = new BlockPos(origin.getX(), y, origin.getZ());
                var face = mode == 1 ? Direction.DOWN : Direction.UP;
                var target = clicked.relative(face);
                for (var pos : List.of(clicked, target)) {
                    check(level.getBlockEntity(pos) == null, "Existing fixture block entity");
                    original.putIfAbsent(pos, level.getBlockState(pos));
                }
                level.setBlock(clicked, Blocks.STONE.defaultBlockState(), 2);
                if (!level.isOutsideBuildHeight(target)) level.setBlock(target, Blocks.AIR.defaultBlockState(), 2);
                player.getInventory().clearContent();
                player.setPos(clicked.getX() + .5, y + (mode == 1 ? -3.2 : 1.0), clicked.getZ() + .5);
                player.setXRot(mode == 1 ? -90 : 90);
                player.setYRot(0);
                var eye = player.getEyePosition();
                var hit = level.clip(new ClipContext(eye, eye.add(player.getViewVector(1).scale(5)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
                check(hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(clicked)
                        && hit.getDirection() == face, "Native use ray missed the expected face");
                var input = variant < 3 ? CustomFluidAPI.createBucket(baths.get(variant))
                        : new ItemStack(variant == 3 ? Items.WATER_BUCKET : Items.LAVA_BUCKET);
                var before = input.copy();
                player.setItemInHand(InteractionHand.MAIN_HAND, input);
                var result = player.gameMode.useItem(player, level, input, InteractionHand.MAIN_HAND);
                var held = player.getMainHandItem();
                if (mode < 2) {
                    check(result == InteractionResult.FAIL && ItemStack.isSameItemSameComponents(before, held)
                                    && before.getCount() == held.getCount() && level.getBlockState(target).isAir(),
                            "Rejected placement changed bucket contents");
                } else {
                    check(held.is(Items.BUCKET) && held.getCount() == 1 && level.getFluidState(target).isSource(),
                            "Valid placement no longer exchanges one bucket for one source");
                    check(variant < 3 ? baths.get(variant).equals(CustomFluidStackHelper.getFluidIdAt(level, target))
                                    : level.getFluidState(target).getType().isSame(variant == 3 ? Fluids.WATER : Fluids.LAVA),
                            "Valid placement changed fluid identity");
                }
                for (var pos : List.of(clicked, target)) {
                    level.removeBlockEntity(pos);
                    level.setBlock(pos, original.get(pos), 2);
                }
            }
        } finally {
            player.getInventory().clearContent();
            player.discard();
            for (var entry : original.entrySet()) {
                level.removeBlockEntity(entry.getKey());
                level.setBlock(entry.getKey(), entry.getValue(), 2);
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
