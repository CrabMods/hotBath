package com.crabmod.hotbath;

import com.crabmod.hotbath.client.particle.CustomDripParticle;
import com.crabmod.hotbath.client.particle.FlyParticle;
import com.crabmod.hotbath.client.particle.HotBathBubbleParticle;
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
import com.crabmod.hotbath.dirtiness.DirtinessAttachment;
import com.crabmod.hotbath.fluid_details.HotbathFluidType;
import com.crabmod.hotbath.item.ItemGroup;
import com.crabmod.hotbath.registers.BlockEntityRegister;
import com.crabmod.hotbath.registers.BlocksRegister;
import com.crabmod.hotbath.registers.CustomFluidBlocksRegister;
import com.crabmod.hotbath.registers.EntityRegister;
import com.crabmod.hotbath.registers.ExtraEventsRegister;
import com.crabmod.hotbath.registers.FluidsRegister;
import com.crabmod.hotbath.registers.ItemRegister;
import com.crabmod.hotbath.registers.ParticleRegister;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.BubbleParticle;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.brewing.BrewingRecipeRegistry;
import com.crabmod.hotbath.custom_fluid.CustomFluidDataComponents;
import com.crabmod.hotbath.custom_fluid.CustomFluidCapabilities;
import com.crabmod.hotbath.custom_fluid.CustomFluidItems;
import com.crabmod.hotbath.custom_fluid.CustomFluidCraftingRecipe;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.crabmod.hotbath.custom_fluid.DynamicFluidTypeRegistry;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(HotBath.MOD_ID)
public class HotBath {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "hotbath";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public HotBath(ModContainer modContainer, IEventBus modEventBus) {
        // Register config
        modContainer.registerConfig(ModConfig.Type.COMMON, HotBathConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, HotBathClientConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.crabmod.hotbath.client.HotBathSettingsScreen.register(modContainer);
        }
        
        ItemGroup.register(modEventBus);
        FluidsRegister.register(modEventBus);
        DynamicFluidTypeRegistry.register(modEventBus);
        DynamicFluidRegistry.register(modEventBus);
        BlocksRegister.register(modEventBus);
        CustomFluidBlocksRegister.register(modEventBus);
        BlockEntityRegister.register(modEventBus);
        ItemRegister.register(modEventBus);
        ParticleRegister.register(modEventBus);
        EntityRegister.register(modEventBus);
        HotbathFluidType.register(modEventBus);
        DirtinessAttachment.register(modEventBus);
        ExtraEventsRegister.register(modEventBus);
        
        // Register custom fluid system
        CustomFluidDataComponents.register(modEventBus);
        CustomFluidItems.register(modEventBus);
        CustomFluidCraftingRecipe.register(modEventBus);
        modEventBus.addListener(CustomFluidCapabilities::registerCapabilities);
        
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);
//    modEventBus.addListener(this::addCreative);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientModEvents::onClientSetup);
            modEventBus.addListener(ClientModEvents::registerParticles);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        // Skip all mod integrations if disabled
        if (!HotBathConfig.isModIntegrationsEnabled()) {
            LOGGER.info("Mod integrations disabled in config - skipping all mod integrations.");
            return;
        }

        // Register all compat modules with the CompatManager
        registerCompatModules();
        
        // Initialize all registered compats safely
        CompatManager.initializeAll();
    }
    
    /**
     * Register all compatibility modules with the CompatManager.
     * Each module is registered with its mod ID, display name, load check, and initializer.
     */
    private void registerCompatModules() {
        // Cold Sweat - temperature system integration
        CompatManager.registerCompat(
            "cold_sweat",
            "Cold Sweat",
            ColdSweatIntegration::isColdSweatLoaded,
            ColdSweatCompat::init,
            // Required API classes - verified before init() runs
            "com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier",
            "com.momosoftworks.coldsweat.api.util.Temperature",
            "com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent",
            "com.momosoftworks.coldsweat.api.event.core.init.DefaultTempModifiersEvent",
            "com.momosoftworks.coldsweat.util.world.WorldHelper",
            "com.momosoftworks.coldsweat.api.util.placement.Placement",
            "com.momosoftworks.coldsweat.api.util.placement.Matcher"
        );
        
        // Tough As Nails - temperature & thirst integration
        CompatManager.registerCompat(
            "toughasnails",
            "Tough As Nails",
            ToughAsNailsIntegration::isToughAsNailsLoaded,
            ToughAsNailsCompat::init,
            "toughasnails.api.temperature.IPlayerTemperatureModifier",
            "toughasnails.api.temperature.TemperatureLevel",
            "toughasnails.api.temperature.TemperatureHelper",
            "toughasnails.api.thirst.ThirstHelper",
            "toughasnails.api.thirst.IThirst"
        );
        
        // Legendary Survival Overhaul - temperature & thirst integration
        CompatManager.registerCompat(
            "legendarysurvivaloverhaul",
            "Legendary Survival Overhaul",
            LegendarySurvivalOverhaulIntegration::isLSOLoaded,
            LSOCompat::init,
            "sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureUtil",
            "sfiomn.legendarysurvivaloverhaul.api.thirst.ThirstUtil",
            "sfiomn.legendarysurvivaloverhaul.registry.MobEffectRegistry"
        );
        
        // Alex's Mobs - entity behavior integration
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
        
        // Alex's Caves - entity behavior integration
        CompatManager.registerCompat(
            "alexscaves",
            "Alex's Caves",
            AlexsCavesIntegration::isAlexsCavesLoaded,
            AlexsCavesCompat::init,
            "com.github.alexmodguy.alexscaves.server.entity.living.GummyBearEntity",
            "com.github.alexmodguy.alexscaves.server.entity.living.GammaroachEntity",
            "com.github.alexmodguy.alexscaves.server.entity.living.RaycatEntity",
            "com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry"
        );
        
        // Twilight Forest - effects & particles integration
        CompatManager.registerCompat(
            "twilightforest",
            "Twilight Forest",
            TwilightForestIntegration::isTwilightForestLoaded,
            TwilightForestCompat::init,
            "twilightforest.init.TFMobEffects",
            "twilightforest.init.TFParticleType"
        );
        
        // Farmer's Delight - effects integration
        CompatManager.registerCompat(
            "farmersdelight",
            "Farmer's Delight",
            FarmersDelightIntegration::isFarmersDelightLoaded,
            FarmersDelightCompat::init,
            "vectorwing.farmersdelight.common.registry.ModEffects"
        );
        
        // Serene Seasons - season-based temperature integration
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
                EpicFightCompat::init,
                "yesman.epicfight.client.renderer.patched.layer.PatchedLayer",
                "yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer",
                "yesman.epicfight.client.renderer.FirstPersonRenderer",
                "yesman.epicfight.api.client.event.EpicFightClientEventHooks",
                "yesman.epicfight.client.events.engine.RenderEngine"
            );
        } else {
            LOGGER.info("Skipping Epic Fight compat registration on dedicated server (client-only rendering).");
        }

        // Create - mechanical fluid integration
        CompatManager.registerCompat(
            "create",
            "Create",
            CreateCompat::isCreateLoaded,
            CreateCompat::init,
            "com.simibubi.create.api.effect.OpenPipeEffectHandler",
            "com.simibubi.create.content.fluids.spout.FillingBySpout"
        );
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class
    // annotated with @SubscribeEvent
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(EntityRegister.THROWN_BATH_WATER.get(), ThrownItemRenderer::new);

            ItemBlockRenderTypes.setRenderLayer(FluidsRegister.HOT_WATER_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidsRegister.HOT_WATER_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidsRegister.HONEY_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidsRegister.HONEY_BATH_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidsRegister.MILK_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidsRegister.MILK_BATH_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidsRegister.HERBAL_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidsRegister.HERBAL_BATH_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidsRegister.PEONY_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidsRegister.PEONY_BATH_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidsRegister.ROSE_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidsRegister.ROSE_BATH_FLOWING.get(), RenderType.translucent());
            
            // Dynamic custom fluid
            ItemBlockRenderTypes.setRenderLayer(com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry.DYNAMIC_FLUID_FLOWING.get(), RenderType.translucent());
        }

        @SubscribeEvent
        public static void registerParticles(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ParticleRegister.HOT_WATER_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.HONEY_WATER_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.MILK_WATER_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.HERBAL_WATER_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.PEONY_WATER_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.ROSE_WATER_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.HOT_WATER_EFFECT.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.HONEY_BATH_EFFECT.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.MILK_BATH_EFFECT.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.HERBAL_BATH_EFFECT.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.PEONY_BATH_EFFECT.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.ROSE_BATH_EFFECT.get(), net.minecraft.client.particle.SpellParticle.Provider::new);

            // Bubbles
            event.registerSpriteSet(ParticleRegister.HOT_WATER_BUBBLE.get(), HotBathBubbleParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.HONEY_BATH_BUBBLE.get(), HotBathBubbleParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.MILK_BATH_BUBBLE.get(), HotBathBubbleParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.HERBAL_BATH_BUBBLE.get(), HotBathBubbleParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.PEONY_BATH_BUBBLE.get(), HotBathBubbleParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.ROSE_BATH_BUBBLE.get(), HotBathBubbleParticle.Provider::new);

            // Dripping (Hanging)
            event.registerSpriteSet(ParticleRegister.DRIPPING_HOT_WATER.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, ParticleRegister.FALLING_HOT_WATER.get(), ParticleRegister.LANDING_HOT_WATER.get()));
            event.registerSpriteSet(ParticleRegister.DRIPPING_HONEY_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, ParticleRegister.FALLING_HONEY_BATH.get(), ParticleRegister.LANDING_HONEY_BATH.get()));
            event.registerSpriteSet(ParticleRegister.DRIPPING_MILK_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, ParticleRegister.FALLING_MILK_BATH.get(), ParticleRegister.LANDING_MILK_BATH.get()));
            event.registerSpriteSet(ParticleRegister.DRIPPING_HERBAL_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, ParticleRegister.FALLING_HERBAL_BATH.get(), ParticleRegister.LANDING_HERBAL_BATH.get()));
            event.registerSpriteSet(ParticleRegister.DRIPPING_PEONY_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, ParticleRegister.FALLING_PEONY_BATH.get(), ParticleRegister.LANDING_PEONY_BATH.get()));
            event.registerSpriteSet(ParticleRegister.DRIPPING_ROSE_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, ParticleRegister.FALLING_ROSE_BATH.get(), ParticleRegister.LANDING_ROSE_BATH.get()));

            // Falling
            event.registerSpriteSet(ParticleRegister.FALLING_HOT_WATER.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, ParticleRegister.LANDING_HOT_WATER.get()));
            event.registerSpriteSet(ParticleRegister.FALLING_HONEY_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, ParticleRegister.LANDING_HONEY_BATH.get()));
            event.registerSpriteSet(ParticleRegister.FALLING_MILK_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, ParticleRegister.LANDING_MILK_BATH.get()));
            event.registerSpriteSet(ParticleRegister.FALLING_HERBAL_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, ParticleRegister.LANDING_HERBAL_BATH.get()));
            event.registerSpriteSet(ParticleRegister.FALLING_PEONY_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, ParticleRegister.LANDING_PEONY_BATH.get()));
            event.registerSpriteSet(ParticleRegister.FALLING_ROSE_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, ParticleRegister.LANDING_ROSE_BATH.get()));

            // Landing
            event.registerSpriteSet(ParticleRegister.LANDING_HOT_WATER.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, null));
            event.registerSpriteSet(ParticleRegister.LANDING_HONEY_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, null));
            event.registerSpriteSet(ParticleRegister.LANDING_MILK_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, null));
            event.registerSpriteSet(ParticleRegister.LANDING_HERBAL_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, null));
            event.registerSpriteSet(ParticleRegister.LANDING_PEONY_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, null));
            event.registerSpriteSet(ParticleRegister.LANDING_ROSE_BATH.get(), 
                sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, null));

            // Fly particle for extremely dirty players
            event.registerSpriteSet(ParticleRegister.FLY.get(), FlyParticle.Factory::new);
        }
    }

    @SubscribeEvent
    public void onBrewingRecipeRegister(net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.HOT_WATER_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HOT_WATER_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.HONEY_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HONEY_BATH_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.MILK_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_MILK_BATH_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.HERBAL_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HERBAL_BATH_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.PEONY_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_PEONY_BATH_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.ROSE_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_ROSE_BATH_BOTTLE.get().getDefaultInstance());
        
        // Register dynamic brewing recipe for custom fluids from data packs
        // This allows: Custom Fluid Bottle + Gunpowder = Splash Custom Fluid Bottle
        event.getBuilder().addRecipe(new com.crabmod.hotbath.custom_fluid.CustomFluidBrewingRecipe());
    }
}
