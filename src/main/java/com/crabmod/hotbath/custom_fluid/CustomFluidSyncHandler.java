package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Handles events related to custom fluid synchronization between server and client.
 * Syncs custom fluid definitions to clients when they join the server or when
 * data packs are reloaded.
 */
@EventBusSubscriber(modid = HotBath.MOD_ID)
public class CustomFluidSyncHandler {
    
    /**
     * Sync custom fluid definitions to player when they log in.
     * This ensures clients see custom fluids in creative menu even when
     * the fluids are only defined in server-side data packs.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Delay sync slightly to ensure player is fully connected
            player.getServer().execute(() -> {
                CustomFluidNetworking.syncToClient(player);
            });
        }
    }
    
    /**
     * Sync custom fluid definitions when data packs are synced (player join or /reload).
     * This is the recommended way to sync data pack content in NeoForge.
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            // Sync to specific player (player joining)
            CustomFluidNetworking.syncToClient(event.getPlayer());
        } else {
            // Sync to all players (data pack reload via /reload)
            refreshLoadedBathLights(event.getPlayerList().getServer());
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.crabmod.hotbath.api.BathDefinitionsChangedEvent(event.getPlayerList().getServer()));
            CustomFluidNetworking.syncToAllClients(event.getPlayerList().getPlayers());
        }
    }
    private static void refreshLoadedBathLights(net.minecraft.server.MinecraftServer server) {
        for(var level:server.getAllLevels()) {
            var chunks=(com.crabmod.hotbath.mixin.ChunkMapAccessor)level.getChunkSource().chunkMap;
            for(var holder:chunks.hotbath$loadedChunks()) {
                var result=holder.getFullChunkFuture().getNow(null);
                var chunk=result==null?null:result.orElse(null);if(chunk==null)continue;
                for(var entry:chunk.getBlockEntities().entrySet()) {
                    if(entry.getValue() instanceof CustomFluidBlockEntity) {
                        level.getLightEngine().checkBlock(entry.getKey());
                        var state=level.getBlockState(entry.getKey());level.sendBlockUpdated(entry.getKey(),state,state,3);
                    }
                }
            }
        }
    }
}
