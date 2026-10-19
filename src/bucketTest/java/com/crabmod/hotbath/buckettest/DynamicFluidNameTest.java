package com.crabmod.hotbath.buckettest;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.custom_fluid.CustomFluidRegistry;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

@GameTestHolder("hotbath_bucket_capabilities")
@PrefixGameTestTemplate(false)
public final class DynamicFluidNameTest {
    private static FluidStack golden() {
        return CustomFluidAPI.createCustomFluidStack(new ResourceLocation("hotbath:golden_bath"), 1250);
    }

    private static Component name(FluidStack stack) {
        return stack.getDisplayName();
    }

    @GameTest(template = "empty_1x1", batch = "fluid_names")
    public static void names_resolve_each_definition_without_changing_stack(GameTestHelper helper) {
        helper.assertTrue(!FMLEnvironment.dist.isClient(), "Dedicated-server name path required");
        var golden = golden();
        var blazing = CustomFluidAPI.createCustomFluidStack(new ResourceLocation("hotbath:blazing_bath"), 750);
        var snapshot = golden.copy();
        helper.assertTrue(name(golden).getString().equals("Golden Bath"), "Golden definition name missing");
        helper.assertTrue(name(blazing).getString().equals("Blazing Bath"), "Distinct definition name missing");
        helper.assertTrue(name(golden).getContents() instanceof TranslatableContents text
                && text.getKey().equals("fluid.hotbath.golden_bath"), "Lost the resource translation key");
        helper.assertTrue(golden.isFluidEqual(snapshot) && golden.getAmount() == snapshot.getAmount(), "Reading the name changed identity or volume");
        helper.assertTrue(name(new FluidStack(Fluids.WATER, 1000)).getString().equals("Water"), "Vanilla name changed");
        helper.succeed();
    }

    @GameTest(template = "empty_1x1", batch = "fluid_names")
    public static void missing_and_unknown_ids_use_existing_unknown_label(GameTestHelper helper) {
        var missing = new FluidStack(DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get(), 1000);
        var unknown = CustomFluidAPI.createCustomFluidStack(new ResourceLocation("hotbath:missing_name_test"), 1000);
        for (var stack : new FluidStack[]{missing, unknown}) {
            var copy = stack.copy();
            helper.assertTrue(name(stack).getContents() instanceof TranslatableContents text
                    && text.getKey().equals("item.hotbath.custom_fluid.unknown"), "Unknown stack leaked a type key");
            helper.assertTrue(stack.isFluidEqual(copy) && stack.getAmount() == copy.getAmount(), "Unknown name changed the stack");
        }
        helper.succeed();
    }

    @GameTest(template = "empty_1x1", batch = "fluid_names")
    public static void definition_reload_refreshes_existing_stack_name(GameTestHelper helper) {
        var original = new ArrayList<>(CustomFluidRegistry.getAllDefinitions());
        var stack = golden();
        try {
            var json = CustomFluidDefinition.CODEC.encodeStart(JsonOps.INSTANCE,
                    CustomFluidAPI.getCustomFluidDefinition(stack).orElseThrow()).result().orElseThrow().getAsJsonObject();
            json.getAsJsonObject("translations").addProperty("en_us", "Reloaded Golden Bath");
            var updated = CustomFluidDefinition.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
            var definitions = new ArrayList<>(original);
            definitions.removeIf(d -> d.id().equals(updated.id()));
            definitions.add(updated);
            CustomFluidRegistry.onFluidsReloaded(definitions);
            helper.assertTrue(name(stack).getString().equals("Reloaded Golden Bath"), "Name cached across reload");
        } finally {
            CustomFluidRegistry.onFluidsReloaded(original);
        }
        helper.assertTrue(name(stack).getString().equals("Golden Bath"), "Definition restoration failed");
        helper.succeed();
    }

    @GameTest(template = "empty_1x1", batch = "fluid_names")
    public static void resource_translation_has_priority_over_definition_fallback(GameTestHelper helper) {
        var original = new ArrayList<>(CustomFluidRegistry.getAllDefinitions());
        try {
            var json = JsonParser.parseString("{\"id\":\"hotbath:resource_name_test\",\"color\":16777215,"
                    + "\"name_key\":\"block.minecraft.stone\",\"translations\":{\"en_us\":\"Fallback\"}}");
            var definition = CustomFluidDefinition.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
            var definitions = new ArrayList<>(original);
            definitions.add(definition);
            CustomFluidRegistry.onFluidsReloaded(definitions);
            var stack = CustomFluidAPI.createCustomFluidStack(definition.id(), 1000);
            helper.assertTrue(name(stack).getString().equals("Stone"), "Resource language lost priority");
        } finally {
            CustomFluidRegistry.onFluidsReloaded(original);
        }
        helper.succeed();
    }
}
