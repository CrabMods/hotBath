package com.crabmod.hotbath.buckettest;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.custom_fluid.CustomFluidNBTHelper;
import com.crabmod.hotbath.custom_fluid.CustomFluidItems;
import com.crabmod.hotbath.custom_fluid.CustomFluidRegistry;
import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.crabmod.hotbath.registers.FluidsRegister;
import com.crabmod.hotbath.registers.ItemRegister;
import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Regression coverage for ordinary buckets and component-backed custom baths. */
@GameTestHolder("hotbath_bucket_capabilities")
@PrefixGameTestTemplate(false)
public class BucketCapabilityTest {
    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_bucket_capabilities(GameTestHelper helper) {
        boolean createLoaded = ModList.get().isLoaded("create");
        check(!Boolean.getBoolean("hotbath.requireCreateBucketTests") || createLoaded,
                "This validation requires Create to be loaded");
        verifyCapabilities(helper.getLevel());
        writeReceipt(createLoaded);
        helper.succeed();
    }

    private static void writeReceipt(boolean createLoaded) {
        String destination = System.getProperty("hotbath.bucketTestReceipt", "");
        if (destination.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(destination);
            Files.createDirectories(path.getParent());
            var receipt = Map.of(
                    "schemaVersion", 1,
                    "processStartCount", 1,
                    "worldLoadCount", 1,
                    "scenarioCount", 1,
                    "failedCount", 0,
                    "cleanupFailureCount", 0,
                    "allScenariosPassed", true,
                    "sessionId", System.getenv().getOrDefault("MINECRAFT_MOD_TEST_SESSION_ID", ""),
                    "requestedScenarios", List.of("bucketcapabilitytest.test_bucket_capabilities"),
                    "observations", Map.of("createLoaded", createLoaded,
                            "createSpoutTransactions", createLoaded ? 30 : 0,
                            "fluidKinds", 10,
                            "stackCounts", List.of(1, 2, 16),
                            "unknownAndMalformedIdsRejected", true,
                            "legacyReadSimulationUnchanged", true,
                            "registryRestored", true,
                            "process", Map.of("pid", ProcessHandle.current().pid(),
                                    "maxHeapBytes", Runtime.getRuntime().maxMemory(),
                                    "availableProcessors", Runtime.getRuntime().availableProcessors()),
                            "createVersion", createLoaded ? ModList.get().getModContainerById("create")
                                    .orElseThrow().getModInfo().getVersion().toString() : "absent"));
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(receipt),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (Exception error) {
            throw new AssertionError("Could not seal bucket test evidence", error);
        }
    }

    public static void verifyCapabilities() {
        verifyCapabilities(null);
    }

    private static void verifyCapabilities(Level level) {
        verifyRoundTrip(new FluidStack(Fluids.WATER, 1000), new ItemStack(Items.WATER_BUCKET));
        verifyRoundTrip(new FluidStack(Fluids.LAVA, 1000), new ItemStack(Items.LAVA_BUCKET));
        List<FluidStack> builtInFluids = List.of(
                new FluidStack(FluidsRegister.HOT_WATER_FLUID.get(), 1000),
                new FluidStack(FluidsRegister.HONEY_BATH_FLUID.get(), 1000),
                new FluidStack(FluidsRegister.MILK_BATH_FLUID.get(), 1000),
                new FluidStack(FluidsRegister.HERBAL_BATH_FLUID.get(), 1000),
                new FluidStack(FluidsRegister.PEONY_BATH_FLUID.get(), 1000),
                new FluidStack(FluidsRegister.ROSE_BATH_FLUID.get(), 1000));
        List<ItemStack> builtInBuckets = List.of(
                new ItemStack(ItemRegister.HOT_WATER_BUCKET.get()),
                new ItemStack(ItemRegister.HONEY_BATH_BUCKET.get()),
                new ItemStack(ItemRegister.MILK_BATH_BUCKET.get()),
                new ItemStack(ItemRegister.HERBAL_BATH_BUCKET.get()),
                new ItemStack(ItemRegister.PEONY_BATH_BUCKET.get()),
                new ItemStack(ItemRegister.ROSE_BATH_BUCKET.get()));
        for (int i = 0; i < builtInFluids.size(); i++) {
            verifyRoundTrip(builtInFluids.get(i), builtInBuckets.get(i));
        }

        // Add two distinct fixture IDs without removing existing data-pack definitions.
        // Restore the exact previous definitions even when an assertion fails.
        var previous = new ArrayList<>(CustomFluidRegistry.getAllDefinitions());
        var fixture = CustomFluidDefinition.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"id\":\"hotbath:bucket_capability_test\",\"effects\":[]}"))
                .result().orElseThrow();
        var second = CustomFluidDefinition.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"id\":\"hotbath:bucket_capability_test_second\",\"effects\":[]}"))
                .result().orElseThrow();
        try {
            var definitions = new ArrayList<>(previous);
            definitions.removeIf(def -> def.id().equals(fixture.id()) || def.id().equals(second.id()));
            definitions.add(fixture);
            definitions.add(second);
            CustomFluidRegistry.onFluidsReloaded(definitions);
            FluidStack fluid = CustomFluidStackHelper.createStack(fixture.id(), 1000);
            ItemStack expected = CustomFluidAPI.createBucket(fixture.id());
            verifyRoundTrip(fluid, expected);
            FluidStack other = CustomFluidStackHelper.createStack(second.id(), 1000);
            verifyRoundTrip(other, CustomFluidAPI.createBucket(second.id()));
            verifyBottle(fixture.id(), second.id());
            verifyBottle(second.id(), fixture.id());
            IFluidHandlerItem filled = handler(expected.copy());
            check(filled.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 0,
                    "Ordinary fluid must not overwrite a custom bath");
            check(fixture.id().equals(CustomFluidStackHelper.getFluidId(filled.getFluidInTank(0))),
                    "Reacquiring the capability must preserve the custom fluid ID");
            check(filled.drain(other, FluidAction.EXECUTE).isEmpty(), "Different custom ID drained");
            verifyInvalidIdentities();
            verifyLegacySimulation(fixture.id());
            verifyMixedRefills(fluid, other);

            // This is Create's real public execution path, including its bucket
            // shortcut and the optional Hot Bath mixin, rather than a mock handler.
            if (level != null && ModList.get().isLoaded("create")) {
                CreateChecks.verifySpout(level, new FluidStack(Fluids.WATER, 1000), new ItemStack(Items.WATER_BUCKET));
                CreateChecks.verifySpout(level, new FluidStack(Fluids.LAVA, 1000), new ItemStack(Items.LAVA_BUCKET));
                for (int i = 0; i < builtInFluids.size(); i++) {
                    CreateChecks.verifySpout(level, builtInFluids.get(i), builtInBuckets.get(i));
                }
                CreateChecks.verifySpout(level, fluid, expected);
                CreateChecks.verifySpout(level, other, CustomFluidAPI.createBucket(second.id()));
            }
        } finally {
            CustomFluidRegistry.onFluidsReloaded(previous);
            check(new HashSet<>(previous).equals(new HashSet<>(CustomFluidRegistry.getAllDefinitions())),
                    "Custom fluid registry was not restored");
        }
    }

    private static void verifyRoundTrip(FluidStack fluid, ItemStack expected) {
        IFluidHandlerItem handler = handler(new ItemStack(Items.BUCKET));
        check(handler.fill(withAmount(fluid, 999), FluidAction.EXECUTE) == 0, "Partial fill accepted");
        check(handler.fill(fluid, FluidAction.SIMULATE) == 1000, "Fill simulation rejected");
        check(handler.getContainer().is(Items.BUCKET), "Simulation changed the container");
        check(handler.fill(fluid, FluidAction.EXECUTE) == 1000, "Fill rejected");
        check(ItemStack.isSameItemSameTags(handler.getContainer(), expected), "Incorrect filled bucket");
        check(sameFluid(handler.getFluidInTank(0), fluid), "Fluid identity lost");
        check(handler.fill(fluid, FluidAction.EXECUTE) == 0, "Full bucket accepted more fluid");
        check(handler.drain(999, FluidAction.EXECUTE).isEmpty(), "Partial drain accepted");
        check(handler.drain(fluid, FluidAction.SIMULATE).getAmount() == 1000, "Drain simulation rejected");
        check(ItemStack.isSameItemSameTags(handler.getContainer(), expected), "Drain simulation changed container");
        check(handler.drain(fluid, FluidAction.EXECUTE).getAmount() == 1000, "Drain rejected");
        check(handler.getContainer().is(Items.BUCKET), "Drain did not return an empty bucket");
        check(handler.fill(fluid, FluidAction.EXECUTE) == 1000, "Refill after drain rejected");
        // Consumers reacquire a capability from the resulting item, so the six
        // BucketItem subclasses need their own providers as well as empty buckets.
        IFluidHandlerItem reacquired = handler(expected.copy());
        check(sameFluid(reacquired.getFluidInTank(0), fluid), "Requery lost fluid");
        check(reacquired.drain(1000, FluidAction.EXECUTE).getAmount() == 1000, "Requeried bucket cannot drain");
        check(reacquired.getContainer().is(Items.BUCKET), "Requeried drain lost empty bucket");
        check(handler(new ItemStack(Items.BUCKET, 2)).fill(fluid, FluidAction.EXECUTE) == 0,
                "Direct capability access must not consume a stack of buckets");
    }

    private static void verifyInvalidIdentities() {
        FluidStack unknown = CustomFluidStackHelper.createStack(
                new ResourceLocation("hotbath", "unregistered_bucket_test"), 1000);
        FluidStack missing = new FluidStack(unknown.getFluid(), 1000);
        FluidStack malformed = missing.copy();
        malformed.getOrCreateChildTag(CustomFluidNBTHelper.TAG_CUSTOM_FLUID).putString(CustomFluidNBTHelper.TAG_FLUID_ID, "Invalid ID!");
        for (FluidStack invalid : List.of(unknown, missing, malformed)) {
            IFluidHandlerItem empty = handler(new ItemStack(Items.BUCKET));
            check(!empty.isFluidValid(0, invalid), "Invalid fluid claimed valid");
            check(empty.fill(invalid, FluidAction.SIMULATE) == 0, "Invalid fill simulation accepted");
            check(empty.fill(invalid, FluidAction.EXECUTE) == 0, "Invalid fill consumed fluid");
            check(empty.getContainer().is(Items.BUCKET), "Invalid ID deleted the bucket");
        }
        ItemStack unknownBucket = new ItemStack(CustomFluidItems.CUSTOM_FLUID_BUCKET.get());
        unknownBucket.getOrCreateTagElement(CustomFluidNBTHelper.TAG_CUSTOM_FLUID).putString(CustomFluidNBTHelper.TAG_FLUID_ID, "hotbath:unregistered_bucket_test");
        ItemStack before = unknownBucket.copy();
        IFluidHandlerItem invalid = handler(unknownBucket);
        check(invalid.getFluidInTank(0).isEmpty(), "Unknown container exposed a fluid");
        check(invalid.drain(1000, FluidAction.EXECUTE).isEmpty(), "Unknown container drained");
        check(invalid.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 0,
                "Unknown custom bucket overwritten");
        check(ItemStack.isSameItemSameTags(invalid.getContainer(), before), "Unknown item mutated");
    }

    private static void verifyLegacySimulation(ResourceLocation id) {
        for (var item : List.of(CustomFluidItems.CUSTOM_FLUID_BUCKET.get(), CustomFluidItems.CUSTOM_FLUID_BOTTLE.get())) {
            ItemStack legacy = new ItemStack(item);
            CompoundTag data = new CompoundTag();
            CompoundTag identity = new CompoundTag();
            identity.putString("FluidId", id.toString());
            data.put("HotbathCustomFluid", identity);
            legacy.setTag(data);
            ItemStack before = legacy.copy();
            IFluidHandlerItem capability = handler(legacy);
            check(id.equals(CustomFluidStackHelper.getFluidId(capability.getFluidInTank(0))), "Legacy ID unreadable");
            check(!capability.drain(1000, FluidAction.SIMULATE).isEmpty(), "Legacy simulation rejected");
            check(ItemStack.isSameItemSameTags(legacy, before), "Read/SIMULATE migrated the original item");
        }
    }

    private static void verifyMixedRefills(FluidStack first, FluidStack second) {
        IFluidHandlerItem capability = handler(new ItemStack(Items.BUCKET));
        for (FluidStack fluid : List.of(first, new FluidStack(Fluids.WATER, 1000), second,
                new FluidStack(Fluids.LAVA, 1000), first)) {
            check(capability.fill(fluid, FluidAction.EXECUTE) == 1000, "Mixed refill rejected");
            check(sameFluid(capability.getFluidInTank(0), fluid), "Mixed refill ID stale");
            check(sameFluid(capability.drain(1000, FluidAction.EXECUTE), fluid),
                    "Mixed refill drained a stale fluid");
            check(capability.getContainer().is(Items.BUCKET), "Mixed refill did not reset bucket");
        }
    }

    private static void verifyBottle(ResourceLocation id, ResourceLocation otherId) {
        FluidStack fluid = CustomFluidStackHelper.createStack(id, 250);
        ItemStack expected = CustomFluidAPI.createBottle(id);
        IFluidHandlerItem empty = handler(new ItemStack(Items.GLASS_BOTTLE));
        check(empty.fill(withAmount(fluid, 249), FluidAction.EXECUTE) == 0, "Partial bottle fill accepted");
        check(empty.fill(fluid, FluidAction.SIMULATE) == 250 && empty.getContainer().is(Items.GLASS_BOTTLE),
                "Bottle fill simulation changed the item");
        check(empty.fill(fluid, FluidAction.EXECUTE) == 250, "Bottle fill rejected");
        check(ItemStack.isSameItemSameTags(empty.getContainer(), expected), "Bottle identity lost");
        IFluidHandlerItem filled = handler(empty.getContainer());
        check(filled.drain(CustomFluidStackHelper.createStack(otherId, 250), FluidAction.EXECUTE).isEmpty(),
                "Bottle drained a different custom ID");
        check(filled.drain(249, FluidAction.EXECUTE).isEmpty(), "Partial bottle drain accepted");
        check(sameFluid(filled.drain(250, FluidAction.SIMULATE), fluid)
                        && ItemStack.isSameItemSameTags(filled.getContainer(), expected),
                "Bottle drain simulation changed the item");
        check(sameFluid(filled.drain(250, FluidAction.EXECUTE), fluid)
                        && filled.getContainer().is(Items.GLASS_BOTTLE), "Bottle drain lost fluid or item");
        check(handler(new ItemStack(Items.GLASS_BOTTLE, 2)).fill(fluid, FluidAction.EXECUTE) == 0,
                "Stacked bottles consumed");
    }

    private static final class CreateChecks {
        private static void verifySpout(Level level, FluidStack fluid, ItemStack expected) {
            for (int count : List.of(1, 2, 16)) {
                ItemStack input = new ItemStack(Items.BUCKET, count);
                ItemStack before = input.copy();
                FluidStack available = withAmount(fluid, 2000);
                check(com.simibubi.create.content.fluids.spout.FillingBySpout.canItemBeFilled(level, input),
                        "Create rejected empty buckets");
                int amount = com.simibubi.create.content.fluids.spout.FillingBySpout.getRequiredAmountForItem(
                        level, input, available);
                check(amount == 1000, "Create did not request exactly one bucket");
                check(input.getCount() == count && ItemStack.isSameItemSameTags(input, before)
                        && available.getAmount() == 2000, "Create amount query mutated input");
                ItemStack result = com.simibubi.create.content.fluids.spout.FillingBySpout.fillItem(
                        level, amount, input, available);
                check(result.getCount() == 1 && ItemStack.isSameItemSameTags(result, expected),
                        "Create produced the wrong bucket or custom ID");
                check(input.getCount() == count - 1 && available.getAmount() == 1000,
                        "Create consumed the wrong item/fluid amount");
            }
        }
    }

    private static IFluidHandlerItem handler(ItemStack stack) {
        IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        check(handler != null, "Missing bucket fluid capability");
        return handler;
    }

    private static FluidStack withAmount(FluidStack fluid, int amount) {
        FluidStack copy = fluid.copy();
        copy.setAmount(amount);
        return copy;
    }

    private static boolean sameFluid(FluidStack left, FluidStack right) {
        return left.isFluidEqual(right) && left.getAmount() == right.getAmount();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
