package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 * Network handling for syncing custom fluid definitions from server to client.
 * This allows data pack defined fluids to be visible in the client's creative menu
 * when connected to a server.
 */
@EventBusSubscriber(modid = HotBath.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CustomFluidNetworking {
    
    private static final Gson GSON = new GsonBuilder().create();
    
    public static final ResourceLocation SYNC_CUSTOM_FLUIDS_ID = 
            ResourceLocation.fromNamespaceAndPath(HotBath.MOD_ID, "sync_custom_fluids");
    
    /**
     * Packet payload for syncing all custom fluid definitions
     */
    public record SyncCustomFluidsPayload(List<String> fluidJsons) implements CustomPacketPayload {
        
        public static final Type<SyncCustomFluidsPayload> TYPE = 
                new Type<>(SYNC_CUSTOM_FLUIDS_ID);
        
        public static final StreamCodec<FriendlyByteBuf, SyncCustomFluidsPayload> STREAM_CODEC = 
                StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    SyncCustomFluidsPayload::fluidJsons,
                    SyncCustomFluidsPayload::new
                );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(HotBath.MOD_ID);
        registrar.playToClient(
            SyncCustomFluidsPayload.TYPE,
            SyncCustomFluidsPayload.STREAM_CODEC,
            CustomFluidNetworking::handleSyncOnClient
        );
    }
    
    /**
     * Handle the sync packet on client - deserialize and register all fluid definitions
     */
    private static void handleSyncOnClient(SyncCustomFluidsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            HotBath.LOGGER.info("Received {} custom fluid definitions from server", payload.fluidJsons().size());
            
            // Clear existing client-side definitions (server is authoritative)
            CustomFluidRegistry.clearClientSide();
            
            int successCount = 0;
            
            for (String json : payload.fluidJsons()) {
                try {
                    JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
                    CustomFluidDefinition definition = CustomFluidDefinition.CODEC
                            .parse(JsonOps.INSTANCE, jsonObject)
                            .resultOrPartial(error -> HotBath.LOGGER.warn("Failed to parse synced fluid: {}", error))
                            .orElse(null);
                    
                    if (definition != null) {
                        CustomFluidRegistry.registerClientSide(definition);
                        definition.registerTranslations();
                        successCount++;
                    }
                } catch (Exception e) {
                    HotBath.LOGGER.error("Error deserializing custom fluid from server: {}", e.getMessage());
                }
            }
            
            HotBath.LOGGER.info("Client registered {} custom fluids from server", successCount);
        });
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
        
        SyncCustomFluidsPayload payload = new SyncCustomFluidsPayload(fluidJsons);
        PacketDistributor.sendToPlayer(player, payload);
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
        
        SyncCustomFluidsPayload payload = new SyncCustomFluidsPayload(fluidJsons);
        for (ServerPlayer player : players) {
            PacketDistributor.sendToPlayer(player, payload);
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
        
        if (allFluids.isEmpty()) {
            return List.of();
        }
        
        List<String> fluidJsons = new ArrayList<>(allFluids.size());
        
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
