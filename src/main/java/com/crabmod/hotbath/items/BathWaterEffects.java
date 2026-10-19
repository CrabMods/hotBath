package com.crabmod.hotbath.items;

import com.crabmod.hotbath.compat.*;
import net.minecraft.world.entity.LivingEntity;


/**
 * Drinking effects for bath water bottles
 */
public class BathWaterEffects {

    /**
     * Hot Water Bottle Effect: Speed I for 45 seconds
     */
    public static void hotWaterEffect(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.use("hot_water_fluid", entity, false);
        applyDrinkTemperatureEffects(entity);
    }

    public static void hotWaterSplashEffect(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.use("hot_water_fluid", entity, true);
        applySplashTemperatureEffects(entity);
    }

    /**
     * Honey Bath Water Effect: Absorption I for 45 seconds
     */
    public static void honeyBathEffect(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.use("honey_bath_fluid", entity, false);
        applyDrinkTemperatureEffects(entity);
    }

    public static void honeyBathSplashEffect(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.use("honey_bath_fluid", entity, true);
        applySplashTemperatureEffects(entity);
    }

    /**
     * Milk Bath Water Effect: Remove 1 random harmful effect (weakened from removing all)
     */
    public static void milkBathEffect(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.use("milk_bath_fluid", entity, false);
        applyDrinkTemperatureEffects(entity);
    }

    public static void milkBathSplashEffect(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.use("milk_bath_fluid", entity, true);
        applySplashTemperatureEffects(entity);
    }

    /**
     * Herbal Bath Water Effect: Instant heal 3 hearts + Resistance I for 45 seconds
     */
    public static void herbalBathEffect(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.use("herbal_bath_fluid", entity, false);
        applyDrinkTemperatureEffects(entity);
    }

    public static void herbalBathSplashEffect(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.use("herbal_bath_fluid", entity, true);
        applySplashTemperatureEffects(entity);
    }

    /**
     * Peony Bath Water Effect: Luck I for 45 seconds
     */
    public static void peonyBathEffect(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.use("peony_bath_fluid", entity, false);
        applyDrinkTemperatureEffects(entity);
    }

    public static void peonyBathSplashEffect(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.use("peony_bath_fluid", entity, true);
        applySplashTemperatureEffects(entity);
    }

    /**
     * Rose Bath Water Effect: Strength I for 45 seconds
     */
    public static void roseBathEffect(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.use("rose_bath_fluid", entity, false);
        applyDrinkTemperatureEffects(entity);
    }

    public static void roseBathSplashEffect(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.use("rose_bath_fluid", entity, true);
        applySplashTemperatureEffects(entity);
    }

    /**
     * Apply drink effects that are not supplied by item tags.
     * Tough As Nails handles built-in bottle warming, thirst, and hydration
     * through its consumed-item tags.
     */
    private static void applyDrinkTemperatureEffects(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }

        // Apply Cold Sweat temperature effect (20 seconds, 36掳C)
        if (ColdSweatIntegration.isColdSweatLoaded()) {
            BathWaterBottleColdSweatModifier.applyWarmEffect(player);
        }
        
        // Apply Legendary Survival Overhaul temperature effect (5 seconds, 20.0)
        if (LegendarySurvivalOverhaulIntegration.isLSOLoaded()) {
            BathWaterBottleLSOModifier.applyWarmEffect(player);
        }
    }

    private static void applySplashTemperatureEffects(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        
        // Apply ToughAsNails temperature effect (30 seconds, WARM)
        if (ToughAsNailsIntegration.isToughAsNailsLoaded()) {
            BathWaterBottleTANModifier.applySplashWarmEffect(player);
        }
        
        // Apply Cold Sweat temperature effect (10 seconds, 36掳C)
        if (ColdSweatIntegration.isColdSweatLoaded()) {
            BathWaterBottleColdSweatModifier.applySplashWarmEffect(player);
        }
        
        // Apply Legendary Survival Overhaul temperature effect (10 seconds, Cold Resistance II)
        if (LegendarySurvivalOverhaulIntegration.isLSOLoaded()) {
            BathWaterBottleLSOModifier.applySplashWarmEffect(player);
        }
    }

    /**
     * Apply only temperature effects (no potion effects).
     * Used when player drinks from bath water sources using ToughAsNails' hand drinking feature.
     */
    public static void applyTemperatureEffectsOnly(net.minecraft.world.entity.player.Player player) {
        // Apply ToughAsNails temperature effect (10 seconds, WARM)
        if (ToughAsNailsIntegration.isToughAsNailsLoaded()) {
            BathWaterBottleTANModifier.applyWarmEffect(player);
        }
        
        // Apply Cold Sweat temperature effect (20 seconds, 36掳C)
        if (ColdSweatIntegration.isColdSweatLoaded()) {
            BathWaterBottleColdSweatModifier.applyWarmEffect(player);
        }
        
        // Apply Legendary Survival Overhaul temperature effect (5 seconds, 20.0)
        if (LegendarySurvivalOverhaulIntegration.isLSOLoaded()) {
            BathWaterBottleLSOModifier.applyWarmEffect(player);
        }
    }

}
