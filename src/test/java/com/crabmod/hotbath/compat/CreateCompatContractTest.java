package com.crabmod.hotbath.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateCompatContractTest {

    @Test
    void createCompatIsRegisteredThroughApiFreeLoader() throws IOException {
        String source = read("src/main/java/com/crabmod/hotbath/HotBath.java");

        assertAll(
                () -> assertTrue(source.contains("import com.crabmod.hotbath.compat.CreateCompat;"),
                        "HotBath should import the API-free Create loader"),
                () -> assertFalse(source.contains("Create mod integration - temporarily disabled"),
                        "Create integration should not be left disabled"),
                () -> assertTrue(source.contains("\"create\""),
                        "Create compat should be registered with CompatManager"),
                () -> assertTrue(source.contains("CreateCompat::isCreateLoaded"),
                        "Create load check must avoid loading CreateIntegration before Create is present"),
                () -> assertTrue(source.contains("CreateCompat::init"),
                        "Create compat initializer should be registered"),
                () -> assertFalse(source.contains("CreateIntegration::isCreateLoaded"),
                        "CreateIntegration imports Create API and must not be used as the load check"),
                () -> assertTrue(source.contains("com.simibubi.create.api.effect.OpenPipeEffectHandler"),
                        "Create open pipe API should be verified before initialization"),
                () -> assertTrue(source.contains("com.simibubi.create.content.fluids.spout.FillingBySpout"),
                        "Create spout item filling support should be verified before initialization")
        );
    }

    @Test
    void createLoadCheckQueriesModListAndInitializationFailuresPropagate() throws IOException {
        String source = read("src/main/java/com/crabmod/hotbath/compat/CreateCompat.java");

        assertAll(
                () -> assertTrue(source.contains("return ModList.get().isLoaded(CREATE_MOD_ID);"),
                        "CreateCompat.isCreateLoaded should query the actual mod list, not the init flag"),
                () -> assertTrue(source.contains("catch (Throwable e)"),
                        "Create API linkage failures are Errors, not only Exceptions"),
                () -> assertTrue(source.contains("throw runtimeException;"),
                        "Runtime initialization failures must reach CompatManager"),
                () -> assertTrue(source.contains("throw error;"),
                        "NoClassDefFoundError and similar linkage errors must reach CompatManager")
        );
    }

    @Test
    void openPipeEffectsCoverSourceAndFlowingFluids() throws IOException {
        String source = read("src/main/java/com/crabmod/hotbath/compat/CreateIntegration.java");

        assertAll(
                () -> assertTrue(source.contains("private static void registerPipeEffect(Fluid sourceFluid, Fluid flowingFluid"),
                        "Open pipe handlers should be registered by a source/flowing helper"),
                () -> assertPipePair(source, "HOT_WATER_FLUID", "HOT_WATER_FLOWING"),
                () -> assertPipePair(source, "HONEY_BATH_FLUID", "HONEY_BATH_FLOWING"),
                () -> assertPipePair(source, "MILK_BATH_FLUID", "MILK_BATH_FLOWING"),
                () -> assertPipePair(source, "HERBAL_BATH_FLUID", "HERBAL_BATH_FLOWING"),
                () -> assertPipePair(source, "PEONY_BATH_FLUID", "PEONY_BATH_FLOWING"),
                () -> assertPipePair(source, "ROSE_BATH_FLUID", "ROSE_BATH_FLOWING"),
                () -> assertTrue(source.contains("DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get()")
                                && source.contains("DynamicFluidRegistry.DYNAMIC_FLUID_FLOWING.get()")
                                && source.contains("DynamicCustomFluidPipeEffect"),
                        "Open pipe handlers should cover data-pack custom fluids through their shared dynamic fluid"),
                () -> assertFalse(source.contains("import com.simibubi.create.api.behaviour.spouting.BlockSpoutingBehaviour;"),
                        "HotBath should not import block spouting API without a custom block behavior"),
                () -> assertFalse(source.contains("BlockSpoutingBehaviour.BY_BLOCK"),
                        "HotBath has no dedicated cauldron blocks, so spouts should rely on Create filling recipes")
        );
    }

    @Test
    void createRecipesUseNeoForgeCreateFormats() throws IOException {
        List<Path> recipes = listJson("src/main/resources/data/hotbath/recipe/create");

        assertEquals(17, recipes.size(), "HotBath should keep all Create mixing, filling, and emptying recipes");
        for (Path recipe : recipes) {
            String json = read(recipe.toString());
            String name = recipe.getFileName().toString();

            assertAll(name,
                    () -> assertTrue(json.contains("\"neoforge:conditions\""),
                            "NeoForge 1.21 recipes should use neoforge:conditions"),
                    () -> assertTrue(json.contains("\"modid\": \"create\""),
                            "Create recipes should be gated on Create being loaded"),
                    () -> assertFalse(json.contains("\"forge:conditions\""),
                            "NeoForge recipes must not use Forge 1.20 condition keys"),
                    () -> assertFalse(json.contains("\"conditions\""),
                            "NeoForge recipes should use neoforge:conditions, not Forge's conditions key")
            );

            if (name.startsWith("filling_")) {
                assertAll(name,
                    () -> assertTrue(json.contains("\"type\": \"create:filling\""),
                            "Bottle recipes should be Create filling recipes"),
                    () -> assertTrue(json.contains("\"type\": \"neoforge:single\""),
                            "NeoForge Create filling recipes use SizedFluidIngredient JSON"),
                    () -> assertTrue(json.contains("\"id\": \"hotbath:"),
                            "NeoForge recipe results should use id")
                );
            } else if (name.startsWith("emptying_")) {
                assertAll(name,
                        () -> assertTrue(json.contains("\"type\": \"create:emptying\""),
                                "Bottle drain recipes should be Create emptying recipes"),
                        () -> assertTrue(json.contains("\"id\": \"minecraft:glass_bottle\""),
                                "Emptying recipes should return a glass bottle"),
                        () -> assertTrue(json.contains("\"amount\": 250"),
                                "Bath bottles should contain 250 mB"),
                        () -> assertTrue(json.contains("\"id\": \"hotbath:"),
                                "NeoForge emptying fluid results should use id")
                );
            } else if (name.startsWith("mixing_")) {
                assertAll(name,
                        () -> assertTrue(json.contains("\"type\": \"create:mixing\""),
                                "Bath fluid recipes should be Create mixing recipes"),
                        () -> assertTrue(json.contains("\"heat_requirement\": \"heated\""),
                                "Create 1.21 mixing recipes use snake_case heat_requirement"),
                        () -> assertTrue(json.contains("\"id\": \"hotbath:"),
                                "NeoForge fluid results should use id"),
                        () -> assertFalse(json.contains("\"heatRequirement\""),
                                "Create 1.21 mixing recipes must not use the Forge 1.20 key")
                );
            }
        }
    }

    @Test
    void dynamicCustomFluidCreateContractIsExplicit() throws IOException {
        String helper = read("src/main/java/com/crabmod/hotbath/custom_fluid/CustomFluidStackHelper.java");
        String capabilities = read("src/main/java/com/crabmod/hotbath/custom_fluid/CustomFluidCapabilities.java")
                + read("src/main/java/com/crabmod/hotbath/custom_fluid/CompatibleFluidBucketWrapper.java");
        String context = read("src/main/java/com/crabmod/hotbath/custom_fluid/CustomFluidStackContext.java");
        String fillingMixin = read("src/main/java/com/crabmod/hotbath/mixin/create/FluidFillingBehaviourMixin.java");
        String drainingMixin = read("src/main/java/com/crabmod/hotbath/mixin/create/FluidDrainingBehaviourMixin.java");
        String hoseMixin = read("src/main/java/com/crabmod/hotbath/mixin/create/HosePulleyFluidHandlerMixin.java");
        String manipulationAccessor = read("src/main/java/com/crabmod/hotbath/mixin/create/FluidManipulationBehaviourAccessor.java");
        String mixinConfig = read("src/main/resources/hotbath.create.mixins.json");
        String modToml = read("src/main/resources/META-INF/neoforge.mods.toml");
        String build = read("build.gradle");
        String hotbath = read("src/main/java/com/crabmod/hotbath/HotBath.java");

        assertAll(
                () -> assertTrue(helper.contains("stack.set(CustomFluidDataComponents.CUSTOM_FLUID_ID.get(), fluidId.toString())"),
                        "NeoForge FluidStack should carry the custom fluid id as a data component"),
                () -> assertTrue(helper.contains("getFluidIdAt(LevelAccessor level, BlockPos pos)")
                                && helper.contains("CustomFluidBlockEntity")
                                && helper.contains("HotbathWaterloggingHelper.getStoredCustomFluidId"),
                        "Create world draining should be able to recover custom ids from blocks and waterlogging storage"),
                () -> assertTrue(capabilities.contains("Capabilities.FluidHandler.ITEM")
                                && capabilities.contains("Items.BUCKET")
                                && capabilities.contains("Items.GLASS_BOTTLE")
                                && capabilities.contains("CustomFluidAPI.createBucket")
                                && capabilities.contains("CustomFluidAPI.createBottle")
                                && capabilities.contains("CustomFluidStackHelper.hasSameFluidId"),
                        "Custom buckets and bottles should expose a fluid item capability for arbitrary datapack ids"),
                () -> assertTrue(helper.contains("hasSameFluidId")
                                && helper.contains("leftId.equals(getFluidId(right))"),
                        "Custom fluid capabilities should distinguish datapack ids sharing the same dynamic Fluid"),
                () -> assertTrue(helper.contains("hasDifferentFluidIdAt")
                                && helper.contains("existingId != null")
                                && helper.contains("!existingId.equals(fluidId)")
                                && helper.contains("return false;"),
                        "Create filling must not overwrite an existing different custom fluid id"),
                () -> assertTrue(context.contains("ThreadLocal<FluidStack>"),
                        "Create filling must preserve the full FluidStack while Create passes only Fluid to tryDeposit"),
                () -> assertTrue(fillingMixin.contains("FluidFillingBehaviour.class")
                                && fillingMixin.contains("remap = false")
                                && fillingMixin.contains("FluidManipulationBehaviourAccessor")
                                && fillingMixin.contains("BlockEntityBehaviour")
                                && fillingMixin.contains("CustomFluidStackContext.getCreateDepositStack")
                                && fillingMixin.contains("CustomFluidStackHelper.setFluidIdAt")
                                && fillingMixin.contains("@Inject(method = \"getAtPos\"")
                                && fillingMixin.contains("cancellable = true")
                                && fillingMixin.contains("CustomFluidStackHelper.hasDifferentFluidIdAt")
                                && fillingMixin.contains("cir.setReturnValue(hotbath$blockingSpaceType())"),
                        "Create world filling should restore the custom id onto placed HotBath dynamic fluid blocks"),
                () -> assertTrue(drainingMixin.contains("FluidDrainingBehaviour.class")
                                && drainingMixin.contains("remap = false")
                                && drainingMixin.contains("FluidManipulationBehaviourAccessor")
                                && drainingMixin.contains("BlockEntityBehaviour")
                                && drainingMixin.contains("queue.first().pos()")
                                && drainingMixin.contains("CustomFluidStackHelper.setFluidId"),
                        "Create world draining should write the recovered custom id into the returned FluidStack"),
                () -> assertTrue(hoseMixin.contains("HosePulleyFluidHandler.class")
                                && hoseMixin.contains("remap = false")
                                && hoseMixin.contains("setCreateDepositStack")
                                && hoseMixin.contains("clearCreateDepositStack"),
                        "Hose pulley filling should bracket Create deposit calls with the full FluidStack context"),
                () -> assertTrue(manipulationAccessor.contains("FluidManipulationBehaviour.class")
                                && manipulationAccessor.contains("@Accessor(value = \"affectedArea\", remap = false)"),
                        "Create inherited fluid search state should be read through a parent-class accessor"),
                () -> assertTrue(mixinConfig.contains("CreateMixinPlugin")
                                && mixinConfig.contains("FluidDrainingBehaviourMixin")
                                && mixinConfig.contains("FluidFillingBehaviourMixin")
                                && mixinConfig.contains("FluidManipulationBehaviourAccessor")
                                && mixinConfig.contains("HosePulleyFluidHandlerMixin"),
                        "Create-only mixins should be isolated in their own optional mixin config"),
                () -> assertTrue(modToml.contains("hotbath.create.mixins.json")
                                && build.contains("config 'hotbath.create.mixins.json'"),
                        "The Create mixin config should be included in runtime metadata and Gradle mixin processing"),
                () -> assertTrue(hotbath.contains("CustomFluidCapabilities::registerCapabilities"),
                        "Custom fluid item capabilities should be registered on the mod event bus")
        );
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static List<Path> listJson(String path) throws IOException {
        try (Stream<Path> stream = Files.walk(Path.of(path))) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".json"))
                    .toList();
        }
    }

    private static void assertPipePair(String source, String sourceFluid, String flowingFluid) {
        assertTrue(source.contains(
                "registerPipeEffect(\n" +
                        "            FluidsRegister." + sourceFluid + ".get(),\n" +
                        "            FluidsRegister." + flowingFluid + ".get(),"),
                sourceFluid + " should register the same open pipe effect for " + flowingFluid);
    }
}
