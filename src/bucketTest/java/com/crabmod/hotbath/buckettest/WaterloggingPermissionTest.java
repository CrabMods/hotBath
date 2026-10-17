package com.crabmod.hotbath.buckettest;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@GameTestHolder("hotbath_bucket_capabilities")
@PrefixGameTestTemplate(false)
public final class WaterloggingPermissionTest {
    @GameTest(template = "empty_32x32x32", timeoutTicks = 100)
    public static void test_waterlogging_respects_adventure_permissions(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 4, 4));
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "bath-permission"));
        var original = new LinkedHashMap<BlockPos, BlockState>();
        var owned = new ArrayList<>(List.of(pos, pos.above(), pos.below()));
        for (var face : Direction.Plane.HORIZONTAL) owned.add(pos.relative(face));
        var baths = List.of(ResourceLocation.parse("hotbath:golden_bath"),
                ResourceLocation.parse("hotbath:slime_bath"), ResourceLocation.parse("hotbath:blazing_bath"));
        try {
            for (var block : owned) {
                check(level.getBlockEntity(block) == null, "Existing fixture entity");
                original.put(block, level.getBlockState(block));
            }
            // Three baths x dry/wet slabs x four permissions, plus four vanilla controls.
            for (int variant = 0; variant < 4; variant++) {
                for (int wet = 0; wet < (variant == 3 ? 1 : 2); wet++) {
                    for (int permission = 0; permission < 4; permission++) {
                        boolean allowed = permission == 0 || permission == 2;
                        player.setGameMode(permission == 0 ? GameType.SURVIVAL : GameType.ADVENTURE);
                        player.getAbilities().invulnerable = true;
                        player.setNoGravity(true);
                        player.setPos(pos.getX() + .5, pos.getY() + 1.2, pos.getZ() + .5);
                        player.setYRot(0);
                        player.setYHeadRot(0);
                        player.setXRot(90);
                        for (var block : owned) {
                            HotbathWaterloggingHelper.removeFluidType(level, block);
                            HotbathWaterloggingHelper.removeCustomFluidId(level, block);
                            level.setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
                        }
                        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
                        level.setBlockAndUpdate(pos, Blocks.OAK_SLAB.defaultBlockState()
                                .setValue(BlockStateProperties.WATERLOGGED, wet == 1));
                        var input = variant == 3 ? new ItemStack(Items.WATER_BUCKET)
                                : CustomFluidAPI.createBucket(baths.get(variant));
                        if (permission >= 2) {
                            var permittedBlock = permission == 2 ? Blocks.OAK_SLAB : Blocks.DIRT;
                            input.set(DataComponents.CAN_PLACE_ON, new AdventureModePredicate(
                                    List.of(BlockPredicate.Builder.block().of(permittedBlock).build()), true));
                        }
                        player.setItemInHand(InteractionHand.MAIN_HAND, input);
                        var before = input.copy();
                        var eye = player.getEyePosition();
                        var hit = level.clip(new ClipContext(eye, eye.add(player.getViewVector(1).scale(5)),
                                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
                        check(hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)
                                && hit.getDirection() == Direction.UP, "Native item ray missed slab");
                        check(level.mayInteract(player, pos)
                                && player.mayUseItemAt(pos.above(), Direction.UP, input) == allowed,
                                "Fixture permission mismatch");
                        var result = player.gameMode.useItem(player, level, input, InteractionHand.MAIN_HAND);
                        var held = player.getMainHandItem();
                        var customId = HotbathWaterloggingHelper.getStoredCustomFluidId(level, pos);
                        boolean waterlogged = level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED);
                        if (allowed) {
                            check(held.is(Items.BUCKET) && held.getCount() == 1 && waterlogged,
                                    "Authorized waterlogging failed");
                            check(variant == 3 ? customId == null : baths.get(variant).equals(customId),
                                    "Authorized waterlogging changed fluid identity");
                        } else {
                            check(result == InteractionResult.FAIL && ItemStack.isSameItemSameComponents(before, held)
                                            && held.getCount() == 1 && waterlogged == (wet == 1) && customId == null,
                                    "Denied Adventure use changed the bucket or slab");
                        }
                        player.getInventory().clearContent();
                    }
                }
            }
        } finally {
            player.getInventory().clearContent();
            player.discard();
            for (var entry : original.entrySet()) {
                HotbathWaterloggingHelper.removeFluidType(level, entry.getKey());
                HotbathWaterloggingHelper.removeCustomFluidId(level, entry.getKey());
                level.setBlockAndUpdate(entry.getKey(), entry.getValue());
            }
            for (var entry : original.entrySet()) {
                check(level.getBlockState(entry.getKey()).equals(entry.getValue())
                                && HotbathWaterloggingHelper.getStoredCustomFluidId(level, entry.getKey()) == null,
                        "Fixture restoration failed");
            }
        }
        helper.succeed();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
