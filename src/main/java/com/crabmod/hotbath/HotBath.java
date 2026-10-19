package com.crabmod.hotbath;

import com.crabmod.hotbath.compat.CompatManager;
import com.crabmod.hotbath.compat.ColdSweatCompat;
import com.crabmod.hotbath.compat.ColdSweatIntegration;
import com.crabmod.hotbath.compat.LegendarySurvivalOverhaulIntegration;
import com.crabmod.hotbath.compat.LSOCompat;
import com.crabmod.hotbath.compat.ToughAsNailsCompat;
import com.crabmod.hotbath.compat.ToughAsNailsIntegration;
import com.crabmod.hotbath.compat.AlexsMobsCompat;
import com.crabmod.hotbath.compat.AlexsMobsIntegration;
import com.crabmod.hotbath.compat.AlexsCavesCompat;
import com.crabmod.hotbath.compat.AlexsCavesIntegration;
import com.crabmod.hotbath.compat.CreateCompat;
import com.crabmod.hotbath.compat.TwilightForestCompat;
import com.crabmod.hotbath.compat.TwilightForestIntegration;
import com.crabmod.hotbath.compat.FarmersDelightCompat;
import com.crabmod.hotbath.compat.FarmersDelightIntegration;
import com.crabmod.hotbath.compat.EpicFightCompat;
import com.crabmod.hotbath.compat.EpicFightIntegration;
import com.crabmod.hotbath.compat.SereneSeasonsCompat;
import com.crabmod.hotbath.compat.SereneSeasonsIntegration;
import com.crabmod.hotbath.custom_fluid.CustomFluidBrewingRecipe;
import com.crabmod.hotbath.custom_fluid.CustomFluidCraftingRecipe;
import com.crabmod.hotbath.custom_fluid.CustomFluidItems;
import com.crabmod.hotbath.custom_fluid.CustomFluidNetworking;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.crabmod.hotbath.custom_fluid.DynamicFluidTypeRegistry;
import com.crabmod.hotbath.dirtiness.DirtinessNetworking;
import com.crabmod.hotbath.fluid_details.HotbathFluidType;
import com.crabmod.hotbath.item.ItemGroup;
import com.crabmod.hotbath.registers.BlockEntityRegister;
import com.crabmod.hotbath.registers.BlocksRegister;
import com.crabmod.hotbath.registers.CustomFluidBlocksRegister;
import com.crabmod.hotbath.registers.EntityRegister;
import com.crabmod.hotbath.registers.FluidsRegister;
import com.crabmod.hotbath.registers.ItemRegister;
import com.crabmod.hotbath.registers.ParticleRegister;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(HotBath.MOD_ID)
public class HotBath {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "hotbath";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    private final IEventBus modEventBus;

    @SuppressWarnings("removal")
    public HotBath() {
        this(FMLJavaModLoadingContext.get());
    }

    public HotBath(FMLJavaModLoadingContext context) {
        this.modEventBus = context.getModEventBus();
        IEventBus modEventBus = this.modEventBus;
        
        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HotBathConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HotBathClientConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.crabmod.hotbath.client.HotBathSettingsScreen.register();
        }
        
        ItemGroup.register(modEventBus);
        FluidsRegister.register(modEventBus);
        BlocksRegister.register(modEventBus);
        ItemRegister.register(modEventBus);
        ParticleRegister.register(modEventBus);
        EntityRegister.register(modEventBus);
        HotbathFluidType.register(modEventBus);
        CustomFluidItems.register(modEventBus);
        CustomFluidCraftingRecipe.register(modEventBus);
        
        // Register dynamic custom fluid system
        DynamicFluidTypeRegistry.register(modEventBus);
        DynamicFluidRegistry.register(modEventBus);
        CustomFluidBlocksRegister.register(modEventBus);
        BlockEntityRegister.register(modEventBus);
        
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::enqueueIMC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
//    modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        
        // Register dirtiness networking (always register, handler checks config at runtime)
        DirtinessNetworking.register();
        
        // Register custom fluid networking
        CustomFluidNetworking.register();

        // Skip all mod integrations if disabled
        if (!HotBathConfig.isModIntegrationsEnabled()) {
            LOGGER.info("Mod integrations disabled in config - skipping all mod integrations.");
            registerBrewingRecipes(event);
            return;
        }

        // Register all compat modules with the CompatManager
        registerCompatModules();
        
        // Initialize all registered compats safely
        CompatManager.initializeAll();
        
        registerBrewingRecipes(event);
    }
    
    /**
     * Register all compatibility modules with the CompatManager.
     * Each module is registered with its mod ID, display name, load check, and initializer.
     */
    private void registerCompatModules() {
        CompatManager.registerCompat(
            "cold_sweat",
            "Cold Sweat",
            ColdSweatIntegration::isColdSweatLoaded,
            ColdSweatCompat::init,
            "com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier",
            "com.momosoftworks.coldsweat.api.util.Temperature",
            "com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent",
            "com.momosoftworks.coldsweat.api.event.core.init.DefaultTempModifiersEvent",
            "com.momosoftworks.coldsweat.util.world.WorldHelper",
            "com.momosoftworks.coldsweat.api.util.placement.Matcher",
            "com.momosoftworks.coldsweat.api.util.placement.Placement"
        );
        
        CompatManager.registerCompat(
            "toughasnails",
            "Tough As Nails",
            ToughAsNailsIntegration::isToughAsNailsLoaded,
            ToughAsNailsCompat::init,
            "toughasnails.api.temperature.TemperatureHelper",
            "toughasnails.api.temperature.IPlayerTemperatureModifier",
            "toughasnails.api.temperature.TemperatureLevel",
            "toughasnails.api.thirst.ThirstHelper",
            "toughasnails.api.thirst.IThirst"
        );
        
        CompatManager.registerCompat(
            "legendarysurvivaloverhaul",
            "Legendary Survival Overhaul",
            LegendarySurvivalOverhaulIntegration::isLSOLoaded,
            LSOCompat::init,
            "sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureUtil",
            "sfiomn.legendarysurvivaloverhaul.api.thirst.ThirstUtil",
            "sfiomn.legendarysurvivaloverhaul.registry.MobEffectRegistry"
        );
        
        CompatManager.registerCompat(
            "alexsmobs",
            "Alex's Mobs",
            AlexsMobsIntegration::isAlexsMobsLoaded,
            AlexsMobsCompat::init,
            "com.github.alexthe666.alexsmobs.entity.EntityCapuchinMonkey",
            "com.github.alexthe666.alexsmobs.entity.EntityCockroach",
            "com.github.alexthe666.alexsmobs.entity.EntityCrimsonMosquito",
            "com.github.alexthe666.alexsmobs.entity.EntityFly"
        );
        
        CompatManager.registerCompat(
            "alexscaves",
            "Alex's Caves",
            AlexsCavesIntegration::isAlexsCavesLoaded,
            AlexsCavesCompat::init,
            "com.github.alexmodguy.alexscaves.server.entity.living.GammaroachEntity",
            "com.github.alexmodguy.alexscaves.server.entity.living.GummyBearEntity",
            "com.github.alexmodguy.alexscaves.server.entity.living.RaycatEntity",
            "com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry"
        );
        
        CompatManager.registerCompat(
            "twilightforest",
            "Twilight Forest",
            TwilightForestIntegration::isTwilightForestLoaded,
            TwilightForestCompat::init,
            "twilightforest.init.TFMobEffects",
            "twilightforest.init.TFParticleType"
        );
        
        CompatManager.registerCompat(
            "farmersdelight",
            "Farmer's Delight",
            FarmersDelightIntegration::isFarmersDelightLoaded,
            FarmersDelightCompat::init,
            "vectorwing.farmersdelight.common.registry.ModEffects"
        );
        
        CompatManager.registerCompat(
            "sereneseasons",
            "Serene Seasons",
            SereneSeasonsIntegration::isSereneSeasonsLoaded,
            SereneSeasonsCompat::init,
            "sereneseasons.api.season.SeasonHelper",
            "sereneseasons.api.season.Season",
            "sereneseasons.api.season.ISeasonState"
        );

        // Epic Fight is a client-only rendering integration. Do not verify its
        // client API classes on dedicated servers.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CompatManager.registerCompat(
                "epicfight",
                "Epic Fight",
                EpicFightIntegration::isEpicFightLoaded,
                () -> EpicFightCompat.init(this.modEventBus),
                "yesman.epicfight.client.renderer.patched.layer.PatchedLayer",
                "yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer",
                "yesman.epicfight.client.renderer.FirstPersonRenderer",
                "yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent$Modify",
                "yesman.epicfight.client.ClientEngine",
                "yesman.epicfight.client.events.engine.RenderEngine"
            );
        } else {
            LOGGER.info("Skipping Epic Fight compat registration on dedicated server (client-only rendering).");
        }
        
        CompatManager.registerCompat(
            "create",
            "Create",
            CreateCompat::isCreateLoaded,
            CreateCompat::init,
            "com.simibubi.create.api.effect.OpenPipeEffectHandler",
            "com.simibubi.create.content.fluids.spout.FillingBySpout"
        );
    }
    
    private void registerBrewingRecipes(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.HOT_WATER_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HOT_WATER_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.HONEY_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HONEY_BATH_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.MILK_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_MILK_BATH_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.HERBAL_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HERBAL_BATH_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.PEONY_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_PEONY_BATH_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.ROSE_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_ROSE_BATH_BOTTLE.get().getDefaultInstance());
            
            // Register custom fluid brewing recipe (bottle -> splash bottle with gunpowder)
            BrewingRecipeRegistry.addRecipe(new CustomFluidBrewingRecipe());
        });
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        // Skip mod integrations if disabled
        if (!HotBathConfig.isModIntegrationsEnabled()) {
            LOGGER.info("Mod integrations disabled - ToughAsNails integration skipped.");
            return;
        }
        
        if (ToughAsNailsIntegration.isToughAsNailsLoaded()) {
            LOGGER.info("Tough As Nails detected! Temperature integration enabled.");
            try {
                ToughAsNailsCompat.init();
                LOGGER.info("Tough As Nails integration registered successfully.");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize Tough As Nails integration: {}", e.getMessage(), e);
            }
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.HOT_WATER_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.HOT_WATER_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.HONEY_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.HONEY_BATH_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.MILK_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.MILK_BATH_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.PEONY_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.PEONY_BATH_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.ROSE_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.ROSE_BATH_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.HERBAL_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.HERBAL_BATH_FLOWING.get(), RenderType.translucent());
        }
    }

    /*
    @SubscribeEvent
    public void onBrewingRecipeRegister(net.minecraftforge.event.brewing.RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.HOT_WATER_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HOT_WATER_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.HONEY_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HONEY_BATH_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.MILK_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_MILK_BATH_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.HERBAL_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HERBAL_BATH_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.PEONY_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_PEONY_BATH_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.ROSE_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_ROSE_BATH_BOTTLE.get().getDefaultInstance());
    }
    */
}









