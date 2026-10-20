package com.crabmod.hotbath.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotBathApiContractTest {

    private static final Set<String> EXPECTED_BATH_FLUIDS = Set.of(
            "hotbath:hot_water_fluid",
            "hotbath:hot_water_flowing",
            "hotbath:honey_bath_fluid",
            "hotbath:honey_bath_flowing",
            "hotbath:milk_bath_fluid",
            "hotbath:milk_bath_flowing",
            "hotbath:peony_bath_fluid",
            "hotbath:peony_bath_flowing",
            "hotbath:rose_bath_fluid",
            "hotbath:rose_bath_flowing",
            "hotbath:herbal_bath_fluid",
            "hotbath:herbal_bath_flowing",
            "hotbath:dynamic_custom_fluid",
            "hotbath:dynamic_custom_fluid_flowing"
    );

    @Test
    void publicApiExposesMechanismLevelBathContainerContract() throws IOException {
        String api = read("src/main/java/com/crabmod/hotbath/api/HotBathApi.java");
        String handler = read("src/main/java/com/crabmod/hotbath/dirtiness/DirtinessHandler.java");

        assertAll(
                () -> assertTrue(api.contains("package com.crabmod.hotbath.api;"),
                        "External integrations should have a stable API package"),
                () -> assertTrue(api.contains("public static final TagKey<Fluid> BATH_FLUIDS"),
                        "API should expose the bath fluid tag key"),
                () -> assertTrue(api.contains("public static final TagKey<Fluid> CLEANSING_FLUIDS"),
                        "API should expose the dirtiness-cleaning fluid tag key"),
                () -> assertTrue(api.contains("public static boolean isBathFluid(Fluid fluid)"),
                        "External containers should be able to identify Hot Bath fluids"),
                () -> assertTrue(api.contains("public static boolean isCleansingFluid(Fluid fluid)"),
                        "External containers should be able to identify fluids that clean dirtiness"),
                () -> assertTrue(api.contains("public static boolean applyDirtinessCleaning(ServerPlayer player, boolean isMoving)"),
                        "External containers should be able to apply gradual dirtiness cleaning"),
                () -> assertTrue(api.contains("public static boolean applyDirtinessCleaning(ServerPlayer player, Fluid fluid, boolean isMoving)"),
                        "External containers should have a fluid-gated cleaning helper"),
                () -> assertTrue(api.contains("public static float getDirtiness(Player player)"),
                        "External integrations should read dirtiness through HotBathApi instead of DirtinessHandler"),
                () -> assertTrue(api.contains("public static boolean addDirtiness(ServerPlayer player, float percentage)"),
                        "External integrations should add dirtiness through HotBathApi instead of DirtinessHandler"),
                () -> assertTrue(api.contains("HotBathConfig.isDirtinessEnabled()"),
                        "Public dirtiness mutation should respect the Hot Bath dirtiness toggle"),
                () -> assertTrue(api.contains("fluid.defaultFluidState().is(tag) || HotbathFluidHelper.isHotbathFluid(fluid)"),
                        "API should honor datapack tags while preserving built-in HotBath detection"),
                () -> assertTrue(api.contains("new ResourceLocation(HotBath.MOD_ID, name)")&&!api.contains("ResourceLocation.fromNamespaceAndPath"),
                        "The public API must use the ResourceLocation constructor available on the minimum supported Forge runtime"),
                () -> assertTrue(handler.contains("public static boolean applyExternalBathing(ServerPlayer player, boolean isMoving)"),
                        "DirtinessHandler should expose a gradual cleaning hook for mechanism-level integrations"),
                () -> assertTrue(handler.contains("progressBathing(player, data, gameTime, isMoving)"),
                        "External and built-in cleaning should share the same gradual cleaning logic"),
                () -> assertTrue(api.contains("DirtinessHandler.getDirtiness(player)")
                                && api.contains("DirtinessHandler.addDirtToPlayer(player, percentage)"),
                        "HotBathApi should delegate to the existing dirtiness implementation"),
                () -> assertFalse(api.toLowerCase().contains("refurbished"),
                        "HotBath API must not depend on a specific furniture mod"),
                () -> assertFalse(api.toLowerCase().contains("mrcrayfish"),
                        "HotBath API must stay mechanism-level, not item-specific")
        );
    }

    @Test
    void dedicatedFluidTagsMirrorExistingWaterCompatibilityList() throws IOException {
        Set<String> bathValues = tagValues("src/main/resources/data/hotbath/tags/fluids/bath_fluids.json");
        Set<String> cleansingValues = tagValues("src/main/resources/data/hotbath/tags/fluids/cleansing_fluids.json");
        Set<String> waterValues = tagValues("src/main/resources/data/minecraft/tags/fluids/water.json");

        assertAll(
                () -> assertEquals(EXPECTED_BATH_FLUIDS, bathValues,
                        "bath_fluids should include every built-in and dynamic HotBath fluid"),
                () -> assertEquals(EXPECTED_BATH_FLUIDS, cleansingValues,
                        "cleansing_fluids should initially match HotBath bath fluids"),
                () -> assertTrue(waterValues.containsAll(EXPECTED_BATH_FLUIDS),
                        "Adding dedicated tags must not remove existing vanilla water compatibility"),
                () -> assertFalse(read("src/main/resources/data/hotbath/tags/fluids/bath_fluids.json").contains("\"replace\": true"),
                        "The bath fluid tag should be extendable by datapacks and other mods"),
                () -> assertFalse(read("src/main/resources/data/hotbath/tags/fluids/cleansing_fluids.json").contains("\"replace\": true"),
                        "The cleansing fluid tag should be extendable by datapacks and other mods")
        );
    }

    private static Set<String> tagValues(String path) throws IOException {
        String json = read(path);
        Matcher matcher = Pattern.compile("\"([a-z0-9_.-]+:[a-z0-9_/.-]+)\"").matcher(json);
        Set<String> values = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
