package com.crabmod.hotbath.gametest;

import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Regression coverage for RaccoonTamingMixin's @Shadow / injection contracts against
 * AlexsMobs' EntityRaccoon. If AlexsMobs rewrites any of these signatures, the mixin
 * apply fails at server startup with "was not located in the target class" — these
 * tests catch that class of mismatch early.
 *
 * <p>Historical breakage this guards: RaccoonTamingMixin previously shadowed
 * {@code Optional<BlockPos> getWashPos()}, but AlexsMobs 1.22.9 ships
 * {@code BlockPos getWashPos()} — the mixin failed to apply and crashed the server.
 */
@EventBusSubscriber(modid = "hotbath", bus = EventBusSubscriber.Bus.MOD)
public class RaccoonTamingMixinTest {

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        if (ModList.get().isLoaded("alexsmobs")) {
            event.register(Tests.class);
        }
    }

    // Keep optional class references out of test discovery when Alex's Mobs is absent.
    @PrefixGameTestTemplate(false)
    public static class Tests {

        // ==========================================
        // @Shadow method: getWashPos() → BlockPos
        // ==========================================

        @GameTest(templateNamespace = "hotbath", template = "empty_1x1", timeoutTicks = 20)
        public static void test_raccoon_getWashPos_signature(GameTestHelper helper) {
            Method m;
            try {
                m = EntityRaccoon.class.getDeclaredMethod("getWashPos");
            } catch (NoSuchMethodException e) {
                helper.fail("EntityRaccoon.getWashPos() no longer exists — RaccoonTamingMixin @Shadow will fail to apply");
                return;
            }
            if (!m.getReturnType().equals(BlockPos.class)) {
                helper.fail("EntityRaccoon.getWashPos() return type changed to "
                        + m.getReturnType().getName()
                        + " — RaccoonTamingMixin @Shadow expects BlockPos. Mixin will fail to apply.");
                return;
            }
            helper.succeed();
        }

        // ==========================================
        // @Inject target: postWashItem(ItemStack)
        // ==========================================

        @GameTest(templateNamespace = "hotbath", template = "empty_1x1", timeoutTicks = 20)
        public static void test_raccoon_postWashItem_target_exists(GameTestHelper helper) {
            try {
                Method m = EntityRaccoon.class.getDeclaredMethod("postWashItem", ItemStack.class);
                if (!m.getReturnType().equals(void.class)) {
                    helper.fail("EntityRaccoon.postWashItem return type changed to "
                            + m.getReturnType().getName()
                            + " — RaccoonTamingMixin @Inject expects void");
                    return;
                }
            } catch (NoSuchMethodException e) {
                helper.fail("EntityRaccoon.postWashItem(ItemStack) no longer exists — RaccoonTamingMixin @Inject will fail");
                return;
            }
            helper.succeed();
        }

        // ==========================================
        // @Shadow field: eggThrowerUUID
        // ==========================================

        @GameTest(templateNamespace = "hotbath", template = "empty_1x1", timeoutTicks = 20)
        public static void test_raccoon_eggThrowerUUID_field(GameTestHelper helper) {
            Field f;
            try {
                f = EntityRaccoon.class.getDeclaredField("eggThrowerUUID");
            } catch (NoSuchFieldException e) {
                helper.fail("EntityRaccoon.eggThrowerUUID field no longer exists — RaccoonTamingMixin @Shadow will fail");
                return;
            }
            if (!f.getType().equals(UUID.class)) {
                helper.fail("eggThrowerUUID field type changed to " + f.getType().getName() + " — expected UUID");
                return;
            }
            helper.succeed();
        }

        // ==========================================
        // Superclass methods the mixin inherits from TamableAnimal
        // ==========================================

        @GameTest(templateNamespace = "hotbath", template = "empty_1x1", timeoutTicks = 20)
        public static void test_tamable_animal_methods_present(GameTestHelper helper) {
            // isTame()
            try {
                Method m = TamableAnimal.class.getDeclaredMethod("isTame");
                if (!m.getReturnType().equals(boolean.class)) {
                    helper.fail("TamableAnimal.isTame() return type changed");
                    return;
                }
            } catch (NoSuchMethodException e) {
                helper.fail("TamableAnimal.isTame() missing — RaccoonTamingMixin relies on it via inheritance");
                return;
            }

            // setTame(boolean, boolean)  -- 1.21's two-arg overload
            try {
                TamableAnimal.class.getDeclaredMethod("setTame", boolean.class, boolean.class);
            } catch (NoSuchMethodException e) {
                helper.fail("TamableAnimal.setTame(boolean, boolean) missing — mixin's call site would not compile");
                return;
            }

            // setOwnerUUID(UUID)
            try {
                TamableAnimal.class.getDeclaredMethod("setOwnerUUID", UUID.class);
            } catch (NoSuchMethodException e) {
                helper.fail("TamableAnimal.setOwnerUUID(UUID) missing — mixin's call site would not compile");
                return;
            }

            // EntityRaccoon must still extend TamableAnimal for the mixin to shadow-inherit
            if (!TamableAnimal.class.isAssignableFrom(EntityRaccoon.class)) {
                helper.fail("EntityRaccoon no longer extends TamableAnimal — RaccoonTamingMixin's extends-pattern will break");
                return;
            }

            helper.succeed();
        }

        // ==========================================
        // Mixin apply smoke test: our injected method must be present on EntityRaccoon
        // ==========================================

        @GameTest(templateNamespace = "hotbath", template = "empty_1x1", timeoutTicks = 20)
        public static void test_mixin_injection_applied(GameTestHelper helper) {
            // Mixin renames @Inject handlers but preserves the original handler name as a substring.
            // If the mixin failed to apply, no such method will exist on the target class.
            boolean found = false;
            for (Method m : EntityRaccoon.class.getDeclaredMethods()) {
                if (m.getName().contains("hotbath$bonusTamingChance")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                helper.fail("RaccoonTamingMixin.hotbath$bonusTamingChance handler not found on EntityRaccoon — mixin did not apply. "
                        + "Check server log for @Shadow / @Inject mismatch errors.");
                return;
            }
            helper.succeed();
        }
    }
}
