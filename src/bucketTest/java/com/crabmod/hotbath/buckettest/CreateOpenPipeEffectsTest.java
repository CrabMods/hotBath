package com.crabmod.hotbath.buckettest;

import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder("hotbath_bucket_capabilities")
@PrefixGameTestTemplate(false)
public final class CreateOpenPipeEffectsTest {
    @GameTest(template = "empty_32x32x32", batch = "open_pipe_effect_pulses", timeoutTicks = 100)
    public static void test_short_bath_pulses_at_every_clock_phase(GameTestHelper helper) {
        if (!net.neoforged.fml.ModList.get().isLoaded("create")) {
            check(!Boolean.getBoolean("hotbath.requireCreateBucketTests"), "Create is required");
            helper.succeed();
            return;
        }
        CreateCases.start(helper);
    }

    // Keep optional Create linkage out of no-Create test discovery.
    private static final class CreateCases {
        private static final List<String> NAMES = List.of("hot_water", "honey_bath", "milk_bath",
                "herbal_bath", "peony_bath", "rose_bath", "golden_bath");

        private static void start(GameTestHelper helper) {
            var root = helper.absolutePos(new BlockPos(3, 5, 3));
            check(helper.getLevel().getBlockState(root.below()).isAir(), "Outlet is occupied");
            var inside = pig(helper, root.below(3));
            var outside = pig(helper, root.below(3).east(5));
            step(helper, root, inside, outside, new HashSet<>());
        }

        private static void step(GameTestHelper helper, BlockPos root, Pig inside, Pig outside,
                                 Set<Integer> phases) {
            try {
                var level = helper.getLevel();
                check(phases.add((int) (level.getGameTime() % 20)), "Clock phase repeated");
                for (int i = 0; i < NAMES.size(); i++) {
                    var fluid = i < 6 ? new FluidStack(BuiltInRegistries.FLUID.get(
                            id("hotbath:" + NAMES.get(i) + "_fluid")), 250)
                            : CustomFluidStackHelper.createStack(id("hotbath:golden_bath"), 250);
                    var location = new CompoundTag();
                    location.put("Pos", NbtUtils.writeBlockPos(root));
                    location.putString("Face", Direction.DOWN.name());
                    var saved = new CompoundTag();
                    saved.put("Location", location);
                    var pipe = com.simibubi.create.content.fluids.OpenEndedPipe.fromNBT(saved, level.registryAccess(), root);
                    pipe.manageSource(level, null);
                    var handler = pipe.provideHandler().getCapability();
                    var primer = fluid.copy();
                    primer.setAmount(1);
                    check(handler.fill(primer, FluidAction.EXECUTE) == 1, "Native primer rejected");
                    inside.removeAllEffects();
                    outside.removeAllEffects();
                    inside.setAbsorptionAmount(0);
                    inside.setRemainingFireTicks(100);
                    outside.setRemainingFireTicks(100);
                    if (i == 2) {
                        inside.addEffect(new MobEffectInstance(MobEffects.POISON, 600));
                        inside.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600));
                        inside.addEffect(new MobEffectInstance(MobEffects.LUCK, 600));
                    }
                    int beforeCount = inside.getActiveEffects().size();
                    check(handler.fill(fluid.copy(), FluidAction.SIMULATE) == 250
                            && inside.getRemainingFireTicks() == 100
                            && inside.getActiveEffects().size() == beforeCount, "Simulation changed entity");
                    check(handler.fill(FluidStack.EMPTY, FluidAction.EXECUTE) == 0
                            && inside.getRemainingFireTicks() == 100
                            && inside.getActiveEffects().size() == beforeCount, "Empty input changed entity");
                    check(handler.fill(fluid, FluidAction.EXECUTE) == 250, "Native pulse rejected");
                    check(!inside.isOnFire(), "A real bath pulse did not extinguish the subject");
                    if (i == 2) {
                        check(inside.getActiveEffects().stream().filter(e -> !e.getEffect().value().isBeneficial()).count() == 1
                                && inside.hasEffect(MobEffects.LUCK), "Milk must remove one negative effect");
                    } else {
                        var expected = expected(i);
                        check(inside.getActiveEffects().size() == expected.size(), "Unexpected effect count");
                        for (var effect : expected) {
                            var actual = inside.getEffect(effect.getEffect());
                            check(actual != null && actual.getDuration() == effect.getDuration()
                                    && actual.getAmplifier() == effect.getAmplifier(), "Wrong bath effect");
                        }
                    }
                    check(outside.getActiveEffects().isEmpty() && outside.getRemainingFireTicks() == 100,
                            "Outside entity was affected");
                    check(handler.getFluidInTank(0).getAmount() == 251
                            && level.getFluidState(pipe.getOutputPos()).isEmpty(), "Pulse storage changed");
                    if (i == 2) {
                        check(handler.fill(fluid.copy(), FluidAction.EXECUTE) == 250
                                && inside.getActiveEffects().size() == 1
                                && inside.hasEffect(MobEffects.LUCK)
                                && handler.getFluidInTank(0).getAmount() == 501,
                                "A second milk pulse must remove the remaining negative effect only");
                    }
                }
                if (phases.size() < 20) {
                    helper.runAfterDelay(1, () -> step(helper, root, inside, outside, phases));
                } else {
                    inside.discard();
                    outside.discard();
                    check(inside.isRemoved() && outside.isRemoved(), "Fixture entity cleanup failed");
                    helper.succeed();
                }
            } catch (Throwable failure) {
                inside.discard();
                outside.discard();
                throw failure;
            }
        }

        private static Pig pig(GameTestHelper helper, BlockPos at) {
            var pig = EntityType.PIG.create(helper.getLevel());
            check(pig != null, "Missing fixture entity");
            pig.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
            pig.setNoAi(true);
            pig.setNoGravity(true);
            pig.setInvulnerable(true);
            check(helper.getLevel().addFreshEntity(pig), "Fixture entity was not added");
            return pig;
        }

        private static List<MobEffectInstance> expected(int i) {
            return switch (i) {
                case 0 -> List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
                case 1 -> List.of(new MobEffectInstance(MobEffects.ABSORPTION, 200),
                        new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100));
                case 3 -> List.of(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200),
                        new MobEffectInstance(MobEffects.REGENERATION, 100));
                case 4 -> List.of(new MobEffectInstance(MobEffects.LUCK, 200));
                case 5 -> List.of(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200));
                default -> CustomFluidStackHelper.getDefinition(CustomFluidStackHelper.createStack(
                        id("hotbath:golden_bath"), 250)).orElseThrow().createEffectInstances();
            };
        }

        private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
