package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

/**
 * Handles registration of the custom fluid reload listener and 
 * syncing custom fluids to clients.
 */
@Mod.EventBusSubscriber(modid = HotBath.MOD_ID)
public class CustomFluidReloadListener {

    /**
     * Register the custom fluid manager as a reload listener.
     */
    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        CustomFluidManager manager = new CustomFluidManager();
        event.addListener(manager);
        HotBath.LOGGER.info("Registered custom fluid reload listener");
    }

    /**
     * Sync custom fluids when data packs are synced (player joins or /reload).
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            // Single player joined
            CustomFluidNetworking.syncToClient(player);
        } else {
            // Data packs reloaded - sync to all players
            if (event.getPlayerList() != null) {
                CustomFluidNetworking.syncToAllClients(event.getPlayerList().getPlayers());
            }
        }
    }

    /**
     * Also sync when a player logs in, as a backup.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            CustomFluidNetworking.syncToClient(serverPlayer);
        }
    }
}
