package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.util.CustomFluidHandler;
import com.mojang.logging.LogUtils;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TempModifierEvent;
import com.momosoftworks.coldsweat.api.event.core.init.DefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.temperature.modifier.WaterTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Matcher;
import com.momosoftworks.coldsweat.api.util.placement.Placement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import org.slf4j.Logger;

/**
 * Event handler for Cold Sweat integration
 */
public class ColdSweatEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onTempModifierRegister(TempModifierRegisterEvent event) {
        CompatManager.safeEventCall("cold_sweat", "onTempModifierRegister", () -> {
            LOGGER.info("Registering Hot Bath temperature modifiers with Cold Sweat...");
            event.register(ResourceLocation.parse("hotbath:immersion"), HotBathImmersionModifier::new);
            event.register(ResourceLocation.parse("hotbath:bottle"), BathWaterBottleColdSweatModifier::new);
            LOGGER.info("Successfully registered Hot Bath temperature modifiers!");
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDefaultModifiers(DefaultTempModifiersEvent event) {
        CompatManager.safeEventCall("cold_sweat", "onDefaultModifiers", () -> {
            event.addModifier(
                    Temperature.Trait.WORLD,
                    new HotBathImmersionModifier(),
                    Placement.LAST.noDuplicates(Matcher.SAME_CLASS)
            );

            event.addModifier(
                    Temperature.Trait.WORLD,
                    new BathWaterBottleColdSweatModifier(),
                    Placement.LAST.noDuplicates(Matcher.SAME_CLASS)
            );
        });
    }

    /**
     * Neutralizes Cold Sweat's dynamically-added {@link WaterTempModifier}
     * while the player is soaking in a Hot Bath fluid.
     *
     * <p><b>Why this exists:</b> Hot Bath fluid blocks extend {@code LiquidBlock}
     * and are tagged into {@code minecraft:water}, so {@code player.isInWater()}
     * returns {@code true} in a bath. Cold Sweat's
     * {@code EntityTempManager.handleWaterFreezingFire} fires every 5 ticks and
     * appends a fresh {@link WaterTempModifier} to the WORLD trait chain via
     * {@code Placement.LAST}. That placement runs <em>after</em> our
     * {@link HotBathImmersionModifier}, so the water modifier's
     * {@code temp -> temp + newTemperature} re-adds the default water
     * temperature ({@code -0.2 MC} = -5&nbsp;&deg;C) on top of the bath's
     * forced value, producing the observed -5&nbsp;&deg;C offset.
     *
     * <p>Cancelling the {@code Calculate.Pre} event causes Cold Sweat's
     * {@code TempModifier.update()} to install our provided function (identity)
     * instead of calling the modifier's own {@code calculate()} &mdash; so the
     * water modifier stays in the chain (avoiding constant re-addition churn)
     * but contributes nothing while the player is in a bath.
     */
    @SubscribeEvent
    public static void onWaterTempModifierCalculate(TempModifierEvent.Calculate.Pre event) {
        CompatManager.safeEventCall("cold_sweat", "onWaterTempModifierCalculate", () -> {
            boolean modifierIsWaterTempModifier = event.getModifier() instanceof WaterTempModifier;
            boolean traitIsWorld = event.getTrait() == Temperature.Trait.WORLD;
            boolean entityIsPlayer = event.getEntity() instanceof Player;
            boolean playerIsInHotBathBlock =
                    entityIsPlayer && com.crabmod.hotbath.api.HotBathApi.allowsModIntegrations(event.getEntity())
                    && CustomFluidHandler.isPlayerInHotBathBlock((Player) event.getEntity());

            if (ColdSweatImmersionGate.shouldNeutralizeWaterTempModifier(
                    modifierIsWaterTempModifier,
                    traitIsWorld,
                    entityIsPlayer,
                    playerIsInHotBathBlock)) {
                event.setFunction(ColdSweatImmersionGate.IDENTITY);
                event.setCanceled(true);
            }
        });
    }
}
