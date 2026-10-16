package com.crabmod.hotbath.buckettest;

import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** Tests the loaded Create recipe objects, including fluid amounts and results. */
@GameTestHolder("hotbath_bucket_capabilities")
@PrefixGameTestTemplate(false)
public final class CreateMixingRecipeTest {
    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_create_mixing_recipe_inputs(GameTestHelper helper) {
        if (!net.neoforged.fml.ModList.get().isLoaded("create")) {
            check(!Boolean.getBoolean("hotbath.requireCreateBucketTests"), "Create is required for this run");
            helper.succeed();
            return;
        }

        List<String> names = List.of("honey_bath", "herbal_bath", "milk_bath", "peony_bath", "rose_bath");
        List<Item> materials = List.of(Items.HONEYCOMB, BuiltInRegistries.ITEM.get(id("hotbath:bath_herb")),
                Items.MILK_BUCKET, Items.PEONY, Items.ROSE_BUSH);
        for (int i = 0; i < names.size(); i++) {
            var loaded = helper.getLevel().getRecipeManager().byKey(id("hotbath:create/mixing_" + names.get(i)))
                    .orElseThrow(() -> new AssertionError("Missing Create bath mixing recipe"));
            var recipe = loaded.value();
            check(recipe instanceof MixingRecipe, "Bath recipe is not a Create mixing recipe");
            MixingRecipe mixing = (MixingRecipe) recipe;
            check(mixing.getRequiredHeat() == HeatCondition.HEATED, "Bath mixing must require heat");

            var items = mixing.getIngredients();
            check(items.size() == (i == 2 ? 0 : 2), "Wrong bath material count");
            for (var ingredient : items) {
                check(ingredient.test(new ItemStack(materials.get(i))), "Wrong bath material");
                check(!ingredient.test(new ItemStack(Items.DIAMOND)), "Unrelated material accepted");
            }

            var fluids = mixing.getFluidIngredients();
            check(fluids.size() == (i == 2 ? 2 : 1), "Wrong fluid ingredient count");
            check(fluids.get(0).test(new FluidStack(Fluids.WATER, 1000)), "Water ingredient missing");
            check(fluids.get(0).amount() == 1000, "Water amount must be 1000 mB");
            check(!fluids.get(0).test(new FluidStack(Fluids.LAVA, 1000)), "Lava must not replace water");
            if (i == 2) {
                check(fluids.get(1).amount() == 250, "Milk amount must be 250 mB");
                var milk = BuiltInRegistries.FLUID.getTag(TagKey.create(Registries.FLUID, id("c:milk")))
                        .orElseThrow(() -> new AssertionError("Milk fluid tag missing")).iterator();
                check(milk.hasNext() && fluids.get(1).test(new FluidStack(milk.next().value(), 250)),
                        "Milk ingredient must accept the registered milk fluid");
                check(!fluids.get(1).test(new FluidStack(Fluids.WATER, 1000)), "Milk must differ from water");
            }
            var outputs = mixing.getFluidResults();
            check(outputs.size() == 1, "Expected exactly one bath fluid output");
            check(outputs.get(0).getFluid() == BuiltInRegistries.FLUID.get(id("hotbath:" + names.get(i) + "_fluid"))
                    && outputs.get(0).getAmount() == 1000, "Wrong bath result or volume");
            check(mixing.getRollableResults().isEmpty(), "Fluid mixing must not create extra item outputs");
        }
        helper.succeed();
    }

    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
