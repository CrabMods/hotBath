package com.crabmod.hotbath.gametest;

import com.crabmod.hotbath.compat.EpicFightDirtinessPatchedLayer;
import com.crabmod.hotbath.dirtiness.DirtinessOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;

/**
 * Verifies that the Epic Fight dirtiness layer uses the same constants
 * as the vanilla {@link DirtinessOverlayRenderer}. If either class changes
 * a constant without updating the other, these tests catch the drift.
 */
@EventBusSubscriber(modid = "hotbath", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
@PrefixGameTestTemplate(false)
public class DirtinessRendererConsistencyTest {

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        event.register(DirtinessRendererConsistencyTest.class);
    }

    @GameTest(templateNamespace = "hotbath", template = "empty_1x1", timeoutTicks = 20)
    public static void test_num_patterns_match(GameTestHelper helper) {
        try {
            int vanillaPatterns = getStaticInt(DirtinessOverlayRenderer.class, "NUM_PATTERNS");
            int epicFightPatterns = getStaticInt(EpicFightDirtinessPatchedLayer.class, "NUM_PATTERNS");
            if (vanillaPatterns != epicFightPatterns) {
                helper.fail("NUM_PATTERNS mismatch: vanilla=" + vanillaPatterns
                        + " epicfight=" + epicFightPatterns);
                return;
            }
        } catch (Exception e) {
            helper.fail("Failed to read NUM_PATTERNS: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hotbath", template = "empty_1x1", timeoutTicks = 20)
    public static void test_min_dirtiness_match(GameTestHelper helper) {
        try {
            float vanillaMin = getStaticFloat(DirtinessOverlayRenderer.class, "MIN_DIRTINESS");
            float epicFightMin = getStaticFloat(EpicFightDirtinessPatchedLayer.class, "MIN_DIRTINESS");
            if (Float.compare(vanillaMin, epicFightMin) != 0) {
                helper.fail("MIN_DIRTINESS mismatch: vanilla=" + vanillaMin
                        + " epicfight=" + epicFightMin);
                return;
            }
        } catch (Exception e) {
            helper.fail("Failed to read MIN_DIRTINESS: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hotbath", template = "empty_1x1", timeoutTicks = 20)
    public static void test_dirt_color_match(GameTestHelper helper) {
        try {
            float efR = getStaticFloat(EpicFightDirtinessPatchedLayer.class, "DIRT_R");
            float efG = getStaticFloat(EpicFightDirtinessPatchedLayer.class, "DIRT_G");
            float efB = getStaticFloat(EpicFightDirtinessPatchedLayer.class, "DIRT_B");

            // Vanilla uses int conversion: (int)(0.30f * 255) = 76, then divides back
            // We verify the source floats match the vanilla literals
            if (Float.compare(efR, 0.30f) != 0 || Float.compare(efG, 0.22f) != 0
                    || Float.compare(efB, 0.15f) != 0) {
                helper.fail("Dirt color mismatch: epicfight RGB=(" + efR + "," + efG + "," + efB
                        + ") expected (0.30, 0.22, 0.15)");
                return;
            }
        } catch (Exception e) {
            helper.fail("Failed to read dirt color constants: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hotbath", template = "empty_1x1", timeoutTicks = 20)
    public static void test_dirt_textures_match(GameTestHelper helper) {
        try {
            ResourceLocation[] vanillaTextures = getStaticField(
                    DirtinessOverlayRenderer.class, "DIRT_TEXTURES", ResourceLocation[].class);
            ResourceLocation[] epicFightTextures = getStaticField(
                    EpicFightDirtinessPatchedLayer.class, "DIRT_TEXTURES", ResourceLocation[].class);

            if (vanillaTextures.length != epicFightTextures.length) {
                helper.fail("Texture array length mismatch: vanilla=" + vanillaTextures.length
                        + " epicfight=" + epicFightTextures.length);
                return;
            }
            for (int i = 0; i < vanillaTextures.length; i++) {
                if (!vanillaTextures[i].equals(epicFightTextures[i])) {
                    helper.fail("Texture mismatch at index " + i + ": vanilla=" + vanillaTextures[i]
                            + " epicfight=" + epicFightTextures[i]);
                    return;
                }
            }
        } catch (Exception e) {
            helper.fail("Failed to read DIRT_TEXTURES: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hotbath", template = "empty_1x1", timeoutTicks = 20)
    public static void test_alpha_formula_match(GameTestHelper helper) {
        // Both classes define calculateAlpha with the same formula.
        // Test at key points to catch any divergence.
        try {
            java.lang.reflect.Method vanillaAlpha = DirtinessOverlayRenderer.class
                    .getDeclaredMethod("calculateAlpha", float.class);
            vanillaAlpha.setAccessible(true);

            java.lang.reflect.Method efAlpha = EpicFightDirtinessPatchedLayer.class
                    .getDeclaredMethod("calculateAlpha", float.class);
            efAlpha.setAccessible(true);

            Minecraft client = Minecraft.getInstance();
            PlayerRenderer playerRenderer = (PlayerRenderer) client.getEntityRenderDispatcher()
                    .getRenderer(client.player);
            DirtinessOverlayRenderer vanillaLayer = new DirtinessOverlayRenderer(playerRenderer);

            float[] testPoints = {0.0f, 0.01f, 0.1f, 0.25f, 0.5f, 0.75f, 1.0f};
            for (float d : testPoints) {
                float vanillaVal = (float) vanillaAlpha.invoke(vanillaLayer, d);
                float efVal = (float) efAlpha.invoke(null, d);
                if (Math.abs(vanillaVal - efVal) > 1e-6f) {
                    helper.fail("Alpha formula divergence at dirtiness=" + d
                            + ": vanilla=" + vanillaVal + " epicfight=" + efVal);
                    return;
                }
            }
        } catch (NoSuchMethodException e) {
            helper.fail("calculateAlpha method not found in one of the classes: " + e.getMessage());
            return;
        } catch (Exception e) {
            helper.fail("Failed to compare alpha formulas: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    private static int getStaticInt(Class<?> cls, String fieldName) throws Exception {
        Field f = cls.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.getInt(null);
    }

    private static float getStaticFloat(Class<?> cls, String fieldName) throws Exception {
        Field f = cls.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.getFloat(null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getStaticField(Class<?> cls, String fieldName, Class<T> type) throws Exception {
        Field f = cls.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (T) f.get(null);
    }
}
