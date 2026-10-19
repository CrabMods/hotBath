package com.crabmod.hotbath.events.enter_fluid_events;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.fluid_blocks.PeonyBathBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecation")
@EventBusSubscriber(modid = HotBath.MOD_ID)
public class PeonyBathEvents {
    
    // Memory cache for peony bath exit timers - avoids NBT writes every tick
    private static final Map<UUID, Integer> EXITED_TIMERS = new ConcurrentHashMap<>();
    
    // Threshold values for attribute removal

    // Method to reset invalid attributes
    private static void resetInvalidAttributes(ServerPlayer player) {
        // Get the attack speed attribute instance
        AttributeInstance attackSpeedAttribute = player.getAttribute(Attributes.ATTACK_SPEED);

        // Check if the attack speed is negative, if so, reset it to the default value
        if (attackSpeedAttribute != null) {
            double currentAttackSpeed = attackSpeedAttribute.getBaseValue();
            if (currentAttackSpeed < 0) {
                attackSpeedAttribute.setBaseValue(4.0);
            }
        }
    }

    // Handle player login event and reset invalid attributes
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resetInvalidAttributes(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (player.level().isClientSide()) return;

        UUID playerUUID = player.getUUID();
        Integer previous = EXITED_TIMERS.get(playerUUID);
        if (previous == null) return;
        int exitedTime = previous + 1;
        var bonuses = com.crabmod.hotbath.custom_fluid.BuiltinBathDefinitions.get("peony_bath_fluid").bath().peony();
        int attackExpiry = bonuses == null ? 0 : bonuses.attackSpeedLingerTicks();
        int knockbackExpiry = bonuses == null ? 0 : bonuses.knockbackLingerTicks();
        EXITED_TIMERS.put(playerUUID, exitedTime);

        // Honour shorter linger times after a data-pack reload as well as normal expiry.
        if (exitedTime >= attackExpiry) {
            PeonyBathBlock.applyAttributeModifier(
                serverPlayer,
                Attributes.ATTACK_SPEED,
                0,
                PeonyBathBlock.ATTACK_SPEED_MODIFIER_NAME,
                false,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
        if (exitedTime >= knockbackExpiry) {
            PeonyBathBlock.applyAttributeModifier(
                serverPlayer,
                Attributes.KNOCKBACK_RESISTANCE,
                0,
                PeonyBathBlock.KNOCKBACK_RESISTANCE_MODIFIER_NAME,
                false,
                AttributeModifier.Operation.ADDITION);
            
            if (exitedTime >= attackExpiry) EXITED_TIMERS.remove(playerUUID);
        }
    }
    
    /**
     * Reset the exit timer when player enters peony bath.
     * Called from PeonyBathBlock.entityInside()
     */
    public static void resetExitTimer(UUID playerUUID) {
        EXITED_TIMERS.put(playerUUID, 0);
    }
    
    /**
     * Clean up player data when they log out.
     */
    public static void cleanup(UUID playerUUID) {
        EXITED_TIMERS.remove(playerUUID);
    }
}










