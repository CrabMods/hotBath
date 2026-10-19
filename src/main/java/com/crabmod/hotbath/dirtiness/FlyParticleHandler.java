package com.crabmod.hotbath.dirtiness;

import com.crabmod.hotbath.HotBathClientConfig;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.HotBathConfig;
import com.crabmod.hotbath.registers.ParticleRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Client-side handler for spawning fly particles around extremely dirty players.
 * Flies appear when a player has been at 100% dirtiness for 2+ game days.
 * Now supports rendering flies around ALL nearby dirty players, not just the local player.
 */
@Mod.EventBusSubscriber(modid = HotBath.MOD_ID, value = Dist.CLIENT)
public class FlyParticleHandler {
    
    // Spawn flies every 40 ticks (2 seconds)
    private static final int SPAWN_INTERVAL = 40;
    
    private static final Random random = new Random();
    
    private static int tickCounter = 0;
    
    @SubscribeEvent
    public static void onClientLevelTick(TickEvent.LevelTickEvent event) {
        // Check if dirtiness system is enabled
        if (!HotBathConfig.isDirtinessEnabled() || !HotBathClientConfig.SHOW_FLIES.get()) return;
        
        if (event.phase != TickEvent.Phase.END) return;
        
        Level level = event.level;
        if (!level.isClientSide || !(level instanceof ClientLevel clientLevel)) return;
        
        // Only spawn flies at intervals
        tickCounter++;
        if (tickCounter % SPAWN_INTERVAL != 0) return;
        
        // Check all players in the client world
        for (Player player : clientLevel.players()) {
            if (!(player instanceof AbstractClientPlayer clientPlayer)) continue;
            
            // Check if this player should have flies
            boolean hasFlies = DirtinessClientData.hasFlies(clientPlayer.getUUID());
            
            if (!hasFlies) continue;
            
            // Spawn flies around this player
            spawnFliesAroundPlayer(clientPlayer);
        }
    }
    
    /**
     * Spawn fly particles around a dirty player
     */
    private static void spawnFliesAroundPlayer(AbstractClientPlayer player) {
        Level level = player.level();
        
        // Number of flies depends on how dirty (more dramatic effect)
        int flyCount = 1 + random.nextInt(3);
        
        double playerX = player.getX();
        double playerY = player.getY() + 1.5; // Head height
        double playerZ = player.getZ();
        
        for (int i = 0; i < flyCount; i++) {
            // Random spawn position near player
            double offsetX = (random.nextDouble() - 0.5) * 2.0;
            double offsetY = (random.nextDouble() - 0.5) * 1.0;
            double offsetZ = (random.nextDouble() - 0.5) * 2.0;
            
            double spawnX = playerX + offsetX;
            double spawnY = playerY + offsetY;
            double spawnZ = playerZ + offsetZ;
            
            // Spawn particle - use player position as orbit center (passed via speed params)
            level.addParticle(
                ParticleRegister.FLY.get(),
                spawnX, spawnY, spawnZ,
                playerX, playerY, playerZ // Orbit center
            );
        }
    }
}
