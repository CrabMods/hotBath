package com.crabmod.hotbath.gametest;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.custom_fluid.CustomFluidRegistry;
import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Regression coverage for ordinary buckets and component-backed custom baths. */
@GameTestHolder("hotbath")
@PrefixGameTestTemplate(false)
public class BucketCapabilityTest {
    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_bucket_capabilities(GameTestHelper helper) {
        verifyCapabilities();
        helper.succeed();
    }

    public static void verifyCapabilities() {
        verifyRoundTrip(new FluidStack(Fluids.WATER, 1000), new ItemStack(Items.WATER_BUCKET));
        verifyRoundTrip(new FluidStack(Fluids.LAVA, 1000), new ItemStack(Items.LAVA_BUCKET));

        // Supply a deterministic data-pack fluid and restore the registry even on failure.
        var previous = new ArrayList<>(CustomFluidRegistry.getAllDefinitions());
        var fixture = CustomFluidDefinition.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"id\":\"hotbath:bucket_capability_test\",\"effects\":[]}"))
                .getOrThrow();
        try {
            CustomFluidRegistry.onFluidsReloaded(List.of(fixture));
            FluidStack fluid = CustomFluidStackHelper.createStack(fixture.id(), 1000);
            ItemStack expected = CustomFluidAPI.createBucket(fixture.id());
            verifyRoundTrip(fluid, expected);
            IFluidHandlerItem filled = handler(expected.copy());
            check(filled.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 0,
                    "Ordinary fluid must not overwrite a custom bath");
            check(fixture.id().equals(CustomFluidStackHelper.getFluidId(filled.getFluidInTank(0))),
                    "Reacquiring the capability must preserve the custom fluid ID");
        } finally {
            CustomFluidRegistry.onFluidsReloaded(previous);
        }
    }

    private static void verifyRoundTrip(FluidStack fluid, ItemStack expected) {
        IFluidHandlerItem handler = handler(new ItemStack(Items.BUCKET));
        check(handler.fill(fluid.copyWithAmount(999), FluidAction.EXECUTE) == 0, "Partial fill accepted");
        check(handler.fill(fluid, FluidAction.SIMULATE) == 1000, "Fill simulation rejected");
        check(handler.getContainer().is(Items.BUCKET), "Simulation changed the container");
        check(handler.fill(fluid, FluidAction.EXECUTE) == 1000, "Fill rejected");
        check(ItemStack.isSameItemSameComponents(handler.getContainer(), expected), "Incorrect filled bucket");
        check(FluidStack.isSameFluidSameComponents(handler.getFluidInTank(0), fluid), "Fluid identity lost");
        check(handler.fill(fluid, FluidAction.EXECUTE) == 0, "Full bucket accepted more fluid");
        check(handler.drain(999, FluidAction.EXECUTE).isEmpty(), "Partial drain accepted");
        check(handler.drain(fluid, FluidAction.SIMULATE).getAmount() == 1000, "Drain simulation rejected");
        check(ItemStack.isSameItemSameComponents(handler.getContainer(), expected), "Drain simulation changed container");
        check(handler.drain(fluid, FluidAction.EXECUTE).getAmount() == 1000, "Drain rejected");
        check(handler.getContainer().is(Items.BUCKET), "Drain did not return an empty bucket");
        check(handler.fill(fluid, FluidAction.EXECUTE) == 1000, "Refill after drain rejected");
        check(handler(new ItemStack(Items.BUCKET, 2)).fill(fluid, FluidAction.EXECUTE) == 0,
                "Direct capability access must not consume a stack of buckets");
    }

    private static IFluidHandlerItem handler(ItemStack stack) {
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        check(handler != null, "Missing bucket fluid capability");
        return handler;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
