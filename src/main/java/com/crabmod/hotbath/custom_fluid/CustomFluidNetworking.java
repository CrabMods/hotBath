package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Network handling for syncing custom fluid definitions from server to client.
 * This allows data pack defined fluids to be visible in the client's creative menu
 * when connected to a server.
 */
public class CustomFluidNetworking {
    
    private static final Gson GSON = new GsonBuilder().create();
    
    private static final String PROTOCOL_VERSION = "2";
    
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HotBath.MOD_ID, "custom_fluids"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int messageId = 0;
    
    /**
     * Register network packets.
     */
    public static void register() {
        CHANNEL.registerMessage(
                messageId++,
                SyncCustomFluidsPacket.class,
                SyncCustomFluidsPacket::encode,
                SyncCustomFluidsPacket::decode,
                SyncCustomFluidsPacket::handle
        );
    }
    
    /**
     * Packet for syncing all custom fluid definitions
     */
    public static class SyncCustomFluidsPacket {
        private final List<String> fluidJsons;
        
        public SyncCustomFluidsPacket(List<String> fluidJsons) {
            this.fluidJsons = fluidJsons;
        }
        
        public static void encode(SyncCustomFluidsPacket packet, FriendlyByteBuf buf) {
            buf.writeInt(packet.fluidJsons.size());
            for (String json : packet.fluidJsons) {
                buf.writeUtf(json);
            }
        }
        
        public static SyncCustomFluidsPacket decode(FriendlyByteBuf buf) {
            int size = buf.readInt();
            List<String> fluidJsons = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                fluidJsons.add(buf.readUtf(32767));
            }
            return new SyncCustomFluidsPacket(fluidJsons);
        }
        
        public static void handle(SyncCustomFluidsPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                HotBath.LOGGER.info("Received {} custom fluid definitions from server", packet.fluidJsons.size());
                
                if (!BathCatalogSync.apply(packet.fluidJsons)) return;
                
                // After registering all fluids, update light for all loaded custom fluid blocks
                // This fixes the issue where fluids with luminosity > 0 would lose their light
                // after quitting and rejoining the game
                CustomFluidClientEvents.refreshLoadedFluidAppearance();
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Sync all custom fluid definitions to a specific player.
     * Called when a player joins the server.
     */
    public static void syncToClient(ServerPlayer player) {
        List<String> fluidJsons = serializeAllFluids();
        
        if (fluidJsons.isEmpty()) {
            HotBath.LOGGER.debug("No custom fluids to sync to player {}", player.getName().getString());
            return;
        }
        
        SyncCustomFluidsPacket packet = new SyncCustomFluidsPacket(fluidJsons);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        HotBath.LOGGER.info("Synced {} custom fluids to player {}", 
                fluidJsons.size(), player.getName().getString());
    }
    
    /**
     * Sync all custom fluid definitions to all connected players.
     * Called when data packs are reloaded.
     */
    public static void syncToAllClients(Iterable<ServerPlayer> players) {
        List<String> fluidJsons = serializeAllFluids();
        
        if (fluidJsons.isEmpty()) {
            return;
        }
        
        SyncCustomFluidsPacket packet = new SyncCustomFluidsPacket(fluidJsons);
        for (ServerPlayer player : players) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
        HotBath.LOGGER.info("Synced {} custom fluids to all connected players", fluidJsons.size());
    }
    
    /**
     * Serializes all custom fluid definitions to JSON strings.
     * This is a shared method to avoid code duplication.
     */
    private static List<String> serializeAllFluids() {
        // The client can rebuild its runtime mirror while an integrated server syncs.
        Collection<CustomFluidDefinition> allFluids = CustomFluidManager.getInstance().getAllFluidDefinitions();
        
        
        List<String> fluidJsons = new ArrayList<>(BuiltinBathDefinitions.serverJsons());
        
        for (CustomFluidDefinition definition : allFluids) {
            try {
                JsonObject json = (JsonObject) CustomFluidDefinition.CODEC
                        .encodeStart(JsonOps.INSTANCE, definition)
                        .resultOrPartial(error -> HotBath.LOGGER.warn("Failed to encode fluid: {}", error))
                        .orElse(null);
                
                if (json != null) {
                    fluidJsons.add(GSON.toJson(json));
                }
            } catch (Exception e) {
                HotBath.LOGGER.error("Error serializing custom fluid {}: {}", 
                        definition.id(), e.getMessage());
            }
        }
        
        return fluidJsons;
    }
}
