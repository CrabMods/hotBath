package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.crabmod.hotbath.registers.FluidsRegister;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import org.slf4j.Logger;

import java.util.List;

/**
 * Create mod integration for Hot Bath fluids.
 * Registers Open Pipe Effect Handlers and relies on Create filling recipes for spouts.
 * Each effect callback represents an actual delivery, independent of the world clock.
 */
public class CreateIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CREATE_MOD_ID = "create";
    
    // Effect durations (in ticks)
    private static final int PIPE_EFFECT_DURATION = 200; // 10 seconds
    
    /**
     * Check if Create mod is loaded.
     */
    public static boolean isCreateLoaded() {
        return net.neoforged.fml.ModList.get().isLoaded(CREATE_MOD_ID);
    }
    
    /**
     * Initialize all Create integrations.
     */
    public static void init() {
        registerOpenPipeEffects();
        registerSpoutBehaviours();
        LOGGER.info("Hot Bath Create integration registered.");
    }
    
    /**
     * Register Open Pipe Effect Handlers for all Hot Bath fluids.
     * These effects are applied when fluid flows out of an open pipe onto entities.
     */
    private static void registerOpenPipeEffects() {
        // Hot Water - Speed effect
        registerPipeEffect(
            FluidsRegister.HOT_WATER_FLUID.get(),
            FluidsRegister.HOT_WATER_FLOWING.get(),
            new HotWaterPipeEffect()
        );
        
        // Honey Bath - Absorption and Slowness
        registerPipeEffect(
            FluidsRegister.HONEY_BATH_FLUID.get(),
            FluidsRegister.HONEY_BATH_FLOWING.get(),
            new HoneyBathPipeEffect()
        );
        
        // Milk Bath - Remove negative effects
        registerPipeEffect(
            FluidsRegister.MILK_BATH_FLUID.get(),
            FluidsRegister.MILK_BATH_FLOWING.get(),
            new MilkBathPipeEffect()
        );
        
        // Herbal Bath - Resistance and Regeneration
        registerPipeEffect(
            FluidsRegister.HERBAL_BATH_FLUID.get(),
            FluidsRegister.HERBAL_BATH_FLOWING.get(),
            new HerbalBathPipeEffect()
        );
        
        // Peony Bath - Luck
        registerPipeEffect(
            FluidsRegister.PEONY_BATH_FLUID.get(),
            FluidsRegister.PEONY_BATH_FLOWING.get(),
            new PeonyBathPipeEffect()
        );
        
        // Rose Bath - Strength
        registerPipeEffect(
            FluidsRegister.ROSE_BATH_FLUID.get(),
            FluidsRegister.ROSE_BATH_FLOWING.get(),
            new RoseBathPipeEffect()
        );

        // Data-pack custom fluids share one registered fluid. The FluidStack
        // component carries the actual custom fluid ID.
        registerPipeEffect(
            DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get(),
            DynamicFluidRegistry.DYNAMIC_FLUID_FLOWING.get(),
            new DynamicCustomFluidPipeEffect()
        );
        
        LOGGER.debug("Registered Open Pipe Effect Handlers for all Hot Bath fluids.");
    }

    private static void registerPipeEffect(Fluid sourceFluid, Fluid flowingFluid, OpenPipeEffectHandler handler) {
        OpenPipeEffectHandler.REGISTRY.register(sourceFluid, handler);
        OpenPipeEffectHandler.REGISTRY.register(flowingFluid, handler);
    }
    
    /**
     * Create spouts fill items through create:filling recipes.
     * Hot Bath does not define dedicated cauldron blocks, so there is no custom
     * BlockSpoutingBehaviour to register here.
     */
    private static void registerSpoutBehaviours() {
        LOGGER.debug("Create spout item filling is provided by Hot Bath create:filling recipes.");
    }
    
    // ==================== Open Pipe Effect Handler Implementations ====================
    
    /**
     * Hot Water pipe effect - gives Speed I
     */
    private static class HotWaterPipeEffect implements OpenPipeEffectHandler {
        @Override
        public void apply(Level level, AABB area, FluidStack fluid) {
            List<Entity> entities = level.getEntities((Entity) null, area, e -> e instanceof LivingEntity);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED, 
                        PIPE_EFFECT_DURATION, 
                        0, // Level I
                        false, false, true
                    ));
                    // Extinguish fire
                    if (living.isOnFire()) {
                        living.clearFire();
                    }
                }
            }
        }
    }
    
    /**
     * Honey Bath pipe effect - gives Absorption I and Slowness I
     */
    private static class HoneyBathPipeEffect implements OpenPipeEffectHandler {
        @Override
        public void apply(Level level, AABB area, FluidStack fluid) {
            List<Entity> entities = level.getEntities((Entity) null, area, e -> e instanceof LivingEntity);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(
                        MobEffects.ABSORPTION, 
                        PIPE_EFFECT_DURATION, 
                        0, 
                        false, false, true
                    ));
                    living.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 
                        PIPE_EFFECT_DURATION / 2, 
                        0, 
                        false, false, true
                    ));
                    // Extinguish fire
                    if (living.isOnFire()) {
                        living.clearFire();
                    }
                }
            }
        }
    }
    
    /**
     * Milk Bath pipe effect - removes negative effects
     */
    private static class MilkBathPipeEffect implements OpenPipeEffectHandler {
        @Override
        public void apply(Level level, AABB area, FluidStack fluid) {
            List<Entity> entities = level.getEntities((Entity) null, area, e -> e instanceof LivingEntity);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    // Remove one random negative effect
                    living.getActiveEffects().stream()
                        .filter(effect -> !effect.getEffect().value().isBeneficial())
                        .findFirst()
                        .ifPresent(effect -> living.removeEffect(effect.getEffect()));
                    // Extinguish fire
                    if (living.isOnFire()) {
                        living.clearFire();
                    }
                }
            }
        }
    }
    
    /**
     * Herbal Bath pipe effect - gives Resistance I and Regeneration I
     */
    private static class HerbalBathPipeEffect implements OpenPipeEffectHandler {
        @Override
        public void apply(Level level, AABB area, FluidStack fluid) {
            List<Entity> entities = level.getEntities((Entity) null, area, e -> e instanceof LivingEntity);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE, 
                        PIPE_EFFECT_DURATION, 
                        0, 
                        false, false, true
                    ));
                    living.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION, 
                        PIPE_EFFECT_DURATION / 2, 
                        0, 
                        false, false, true
                    ));
                    // Extinguish fire
                    if (living.isOnFire()) {
                        living.clearFire();
                    }
                }
            }
        }
    }
    
    /**
     * Peony Bath pipe effect - gives Luck I
     */
    private static class PeonyBathPipeEffect implements OpenPipeEffectHandler {
        @Override
        public void apply(Level level, AABB area, FluidStack fluid) {
            List<Entity> entities = level.getEntities((Entity) null, area, e -> e instanceof LivingEntity);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(
                        MobEffects.LUCK, 
                        PIPE_EFFECT_DURATION, 
                        0, 
                        false, false, true
                    ));
                    // Extinguish fire
                    if (living.isOnFire()) {
                        living.clearFire();
                    }
                }
            }
        }
    }
    
    /**
     * Rose Bath pipe effect - gives Strength I
     */
    private static class RoseBathPipeEffect implements OpenPipeEffectHandler {
        @Override
        public void apply(Level level, AABB area, FluidStack fluid) {
            List<Entity> entities = level.getEntities((Entity) null, area, e -> e instanceof LivingEntity);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_BOOST, 
                        PIPE_EFFECT_DURATION, 
                        0, 
                        false, false, true
                    ));
                    // Extinguish fire
                    if (living.isOnFire()) {
                        living.clearFire();
                    }
                }
            }
        }
    }

    /**
     * Dynamic custom fluid pipe effect - applies the effects stored in the
     * custom fluid definition carried by the FluidStack.
     */
    private static class DynamicCustomFluidPipeEffect implements OpenPipeEffectHandler {
        @Override
        public void apply(Level level, AABB area, FluidStack fluid) {
            ResourceLocation fluidId = CustomFluidStackHelper.getFluidId(fluid);
            if (fluidId == null) {
                return;
            }

            CustomFluidDefinition definition = CustomFluidStackHelper.getDefinition(fluid).orElse(null);
            if (definition == null) {
                return;
            }

            List<Entity> entities = level.getEntities((Entity) null, area, e -> e instanceof LivingEntity);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    for (MobEffectInstance effect : definition.createEffectInstances()) {
                        living.addEffect(new MobEffectInstance(
                            effect.getEffect(),
                            effect.getDuration(),
                            effect.getAmplifier(),
                            effect.isAmbient(),
                            effect.isVisible(),
                            effect.showIcon()
                        ));
                    }
                    if (living.isOnFire()) {
                        living.clearFire();
                    }
                }
            }
        }
    }
}
