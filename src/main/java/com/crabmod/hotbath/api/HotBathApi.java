package com.crabmod.hotbath.api;

import com.crabmod.hotbath.bathing.BathImmersion;
import com.crabmod.hotbath.compat.TwilightForestIntegration;
import com.crabmod.hotbath.custom_fluid.BuiltinBathDefinition;
import com.crabmod.hotbath.custom_fluid.BuiltinBathDefinitions;
import com.crabmod.hotbath.custom_fluid.BuiltinBathEffects;
import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.entity.FriendlyBathBenefits;
import com.crabmod.hotbath.events.SplashSyncHandler;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraftforge.fluids.FluidStack;
import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.HotBathConfig;
import com.crabmod.hotbath.dirtiness.DirtinessHandler;
import com.crabmod.hotbath.util.HotbathFluidHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;

/**
 * Stable public integration surface for external bath containers.
 */
public final class HotBathApi {

    public static final TagKey<Fluid> BATH_FLUIDS = fluidTag("bath_fluids");
    public static final TagKey<Fluid> CLEANSING_FLUIDS = fluidTag("cleansing_fluids");

    private HotBathApi() {
    }

    /**
     * Checks whether a fluid should be treated as a Hot Bath bath fluid.
     */
    public static boolean isBathFluid(Fluid fluid) {
        return isTaggedOrKnownHotBathFluid(fluid, BATH_FLUIDS);
    }

    /**
     * Checks whether a fluid may trigger Hot Bath dirtiness cleaning.
     */
    public static boolean isCleansingFluid(Fluid fluid) {
        var builtin = BuiltinBathDefinitions.forFluid(fluid);
        return builtin.map(BuiltinBathDefinition::cleansDirtiness)
                .orElseGet(() -> isTaggedOrKnownHotBathFluid(fluid, CLEANSING_FLUIDS));
    }

    /**
     * Applies one server tick of gradual dirtiness cleaning from a pre-validated bath.
     */
    public static boolean applyDirtinessCleaning(ServerPlayer player, boolean isMoving) {
        return DirtinessHandler.applyExternalBathing(player, isMoving);
    }

    /**
     * Applies one server tick of dirtiness cleaning if the supplied fluid is cleansing.
     */
    public static boolean applyDirtinessCleaning(ServerPlayer player, Fluid fluid, boolean isMoving) {
        return isCleansingFluid(fluid) && applyDirtinessCleaning(player, isMoving);
    }

    /**
     * Gets the current dirtiness value for a player.
     *
     * @return a value from 0.0 to 1.0, or 0.0 when no player is supplied
     */
    public static float getDirtiness(Player player) {
        return player == null ? 0.0f : DirtinessHandler.getDirtiness(player);
    }

    /**
     * Adds dirtiness from an external source while respecting Hot Bath's dirtiness toggle.
     *
     * @param player the player to dirty
     * @param percentage amount of maximum dirtiness to add, where 0.10 is 10%
     * @return true if the request was accepted
     */
    public static boolean addDirtiness(ServerPlayer player, float percentage) {
        if (!HotBathConfig.isDirtinessEnabled() || player == null || percentage <= 0.0f) {
            return false;
        }
        DirtinessHandler.addDirtToPlayer(player, percentage);
        return true;
    }

    /** Whether a living entity can receive immersion benefits from a bath. */
    public static boolean canReceiveBathBenefits(LivingEntity entity) {
        return entity != null && entity.isAlive() && (entity instanceof Player || FriendlyBathBenefits.isEligible(entity));
    }

    private static boolean isTaggedOrKnownHotBathFluid(Fluid fluid, TagKey<Fluid> tag) {
        return fluid != null && (fluid.defaultFluidState().is(tag) || HotbathFluidHelper.isHotbathFluid(fluid));
    }

    private static TagKey<Fluid> fluidTag(String name) {
        return TagKey.create(Registries.FLUID, new ResourceLocation(HotBath.MOD_ID, name));
    }

    /** Resolve liquid properties from its stack, including the exact data-pack liquid ID. */
    public static BathFluidProperties fluidProperties(FluidStack stack,boolean clientSide) {
        if(stack.isEmpty())return new BathFluidProperties(0,0,0,0,false,false,false,false,false);
        var custom=CustomFluidAPI.getCustomFluidDefinition(stack);
        if(custom.isPresent()) {
            var d=custom.get();return new BathFluidProperties(d.temperature(),d.density(),d.viscosity(),d.luminosity(),
                    d.isHot(),isCleansingFluid(stack.getFluid()),d.isHot()&&d.showSteam(),d.showBubbles(),d.showParticles());
        }
        var builtin=BuiltinBathDefinitions.forFluid(stack.getFluid(),clientSide);
        var type=stack.getFluid().getFluidType();
        float temperature=builtin.map(BuiltinBathDefinition::temperature).orElseGet(()->type.getTemperature(stack)-273f);
        boolean hot=temperature>=CustomFluidDefinition.HOT_TEMPERATURE_THRESHOLD;
        return new BathFluidProperties(temperature,type.getDensity(stack),type.getViscosity(stack),type.getLightLevel(stack),hot,
                builtin.map(BuiltinBathDefinition::cleansDirtiness).orElseGet(()->isCleansingFluid(stack.getFluid())),
                builtin.isPresent()&&hot,isBathFluid(stack.getFluid()),isBathFluid(stack.getFluid()));
    }

    /** Apply liquid-owned contact effects only. The caller has already validated real contact. */
    public static void applyFluidContact(Entity entity,FluidStack stack) {
        if(entity.level().isClientSide()||!entity.isAlive()||stack.isEmpty())return;
        var custom=CustomFluidAPI.getCustomFluidDefinition(stack);
        if(custom.isPresent()) {
            applyCustomFluidContact(entity,custom.get());return;
        }
        BuiltinBathDefinitions.forFluid(stack.getFluid()).ifPresent(definition->
                BuiltinBathEffects.bathe(definition.id().getPath(),entity));
    }

    /** Shared world/container path for an already resolved Hot Bath custom liquid. */
    public static void applyCustomFluidContact(Entity entity,CustomFluidDefinition definition) {
        if(entity.level().isClientSide()||!entity.isAlive())return;
        var state=BathImmersion.contact(entity,definition.id().toString(),10);
        if(!state.shouldProcess())return;
        applyLiquidHazards(entity,definition.isHot());
        if(!(entity instanceof LivingEntity living)||!canReceiveBathBenefits(living))return;
        if(state.stayedTicks()>=definition.triggerTimeSeconds()*20)
            for(var effect:definition.createEffectInstances())living.addEffect(effect);
    }

    /** Liquid-owned hazards, independent of world-fluid movement and bubble columns. */
    public static void applyLiquidHazards(Entity entity,boolean hot) {
        if(entity.level().isClientSide()||!entity.isAlive())return;
        if(((entity instanceof AbstractFish&&!(entity instanceof TropicalFish))
                ||entity instanceof Squid)&&BathImmersion.claimHazard(entity,1))
            entity.hurt(entity.level().damageSources().magic(),1f);
        if(hot&&entity.tickCount%20==0&&allowsModIntegrations(entity)&&TwilightForestIntegration.isTwilightForestIceMob(entity)
                &&BathImmersion.claimHazard(entity,2))entity.hurt(entity.level().damageSources().magic(),1f);
    }

    /** The container supplies its measured surface; Hot Bath never discovers container geometry. */
    public static void reportFluidContact(Entity entity,FluidStack fluid,double surfaceY) {
        reportFluidContact(entity,fluid,surfaceY,true);
    }

    /** Optional-mod participation belongs to the reporting source; liquid effects stay enabled. */
    public static void reportFluidContact(Entity entity,FluidStack fluid,double surfaceY,boolean integrationsEnabled) {
        if(fluid.isEmpty())return;
        var properties=fluidProperties(fluid,entity.level().isClientSide());
        var custom=CustomFluidAPI.getCustomFluidId(fluid);
        var identity=custom!=null?custom:BuiltinBathDefinitions.forFluid(fluid.getFluid(),entity.level().isClientSide())
                .map(BuiltinBathDefinition::id).orElseGet(()->BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
        BathImmersion.report(entity,new BathContact(isBathFluid(fluid.getFluid()),properties.hot(),properties.cleansing(),
                properties.temperatureCelsius(),identity,surfaceY,true,integrationsEnabled));
        if(!entity.level().isClientSide()&&isBathFluid(fluid.getFluid()))
            SplashSyncHandler.reportContact(entity,properties,surfaceY);
    }
    /** Report an external shower's own temperature and cleaning policy without inventing a liquid. */
    public static void reportShowerContact(Entity entity,float temperatureCelsius,boolean cleansing) {
        reportShowerContact(entity,temperatureCelsius,cleansing,true);
    }
    public static void reportShowerContact(Entity entity,float temperatureCelsius,boolean cleansing,boolean integrationsEnabled) {
        BathImmersion.report(entity,new BathContact(true,temperatureCelsius>=CustomFluidDefinition.HOT_TEMPERATURE_THRESHOLD,cleansing,temperatureCelsius,null,Double.NaN,false,integrationsEnabled));
    }

    /** Immersion identity and heat, without contributions from an overlapping shower. */
    public static Optional<BathContact> currentFluidContact(Entity entity) {
        return BathImmersion.immersion(entity);
    }

    /** An active source may opt out of optional-mod side effects without losing core liquid behavior. */
    public static boolean allowsModIntegrations(Entity entity) {
        return currentBathContact(entity).map(BathContact::integrationsEnabled).orElse(true);
    }
    public static Optional<BathContact> currentBathContact(Entity entity) {
        return BathImmersion.reported(entity);
    }

    /** End the caller's measured immersion without removing independent shower contact or visit history. */
    public static void endFluidContact(Entity entity) {
        BathImmersion.endImmersion(entity);
        if(!entity.level().isClientSide())SplashSyncHandler.clearReportedContact(entity);
    }
    public static void endShowerContact(java.util.UUID entityId) {BathImmersion.endShower(entityId);}
}
