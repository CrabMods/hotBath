package com.crabmod.hotbath.api;

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
        var builtin = com.crabmod.hotbath.custom_fluid.BuiltinBathDefinitions.forFluid(fluid);
        return builtin.map(com.crabmod.hotbath.custom_fluid.BuiltinBathDefinition::cleansDirtiness)
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
    public static boolean canReceiveBathBenefits(net.minecraft.world.entity.LivingEntity entity) {
        return entity != null && entity.isAlive() && (entity instanceof Player || com.crabmod.hotbath.entity.FriendlyBathBenefits.isEligible(entity));
    }

    private static boolean isTaggedOrKnownHotBathFluid(Fluid fluid, TagKey<Fluid> tag) {
        return fluid != null && (fluid.defaultFluidState().is(tag) || HotbathFluidHelper.isHotbathFluid(fluid));
    }

    private static TagKey<Fluid> fluidTag(String name) {
        return TagKey.create(Registries.FLUID, new ResourceLocation(HotBath.MOD_ID, name));
    }
}
