package com.crabmod.hotbath.events;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles splash and exit water sound/particle synchronization for multiplayer.
 * This ensures all players can see and hear when entities enter or exit hot baths.
 */
@Mod.EventBusSubscriber(modid = "hotbath")
public class SplashSyncHandler {
    
    /**
     * Track entity bath states: entityId -> BathState
     */
    private static final Map<Entity, BathState> ENTITY_BATH_STATES = new java.util.WeakHashMap<>();
    private record Contact(double surfaceY,boolean splash,boolean bubbles) {}
    private record ReportedContact(long tick,Contact contact) {}
    private static final Map<Entity,ReportedContact> REPORTED=new java.util.WeakHashMap<>();
    public static void clearReportedContact(Entity entity) {REPORTED.remove(entity);}
    public static void reportContact(Entity entity,com.crabmod.hotbath.api.BathFluidProperties properties,double surfaceY) {
        REPORTED.put(entity,new ReportedContact(entity.level().getGameTime(),new Contact(surfaceY,properties.splash(),properties.bubbles())));
    }
    
    /**
     * Minimum ticks between splash effects for the same entity
     */
    private static final int SPLASH_COOLDOWN_TICKS = 10;
    
    /**
     * How many ticks after leaving water before we consider it a "real" exit
     * This prevents rapid enter/exit sounds when bobbing at water surface
     */
    private static final int EXIT_GRACE_TICKS = 5;
    
    private record BathState(boolean wasInBath, long lastEnterTime, long lastExitTime, int ticksOutOfBath,boolean showSplash) {}
    
    @SubscribeEvent
    public static void onEntityTick(LivingEvent.LivingTickEvent event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        
        // Only process on server side for LivingEntities
        if (level.isClientSide() || !(entity instanceof LivingEntity)) {
            return;
        }
        
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // Check if entity is currently in hot bath
        var contact=contactAt(entity);
        boolean isInBath=contact!=null;
        Entity entityId = entity;
        long currentTime = level.getGameTime();
        
        BathState prevState = ENTITY_BATH_STATES.get(entityId);
        
        if (prevState == null) {
            // First time seeing this entity
            if (isInBath) {
                // Entity spawned in bath or first detection
                broadcastEnterWaterEffects(serverLevel, entity,contact);
                ENTITY_BATH_STATES.put(entityId, new BathState(true, currentTime, 0, 0,contact.splash()));
            }
            return;
        }
        
        if (isInBath) {
            if (!prevState.wasInBath()) {
                // Entity just entered the bath
                if ((currentTime - prevState.lastEnterTime()) > SPLASH_COOLDOWN_TICKS) {
                    broadcastEnterWaterEffects(serverLevel, entity,contact);
                }
                ENTITY_BATH_STATES.put(entityId, new BathState(true, currentTime, prevState.lastExitTime(), 0,contact.splash()));
            } else {
                // Entity still in bath - reset out-of-bath counter
                if (prevState.ticksOutOfBath() > 0 || prevState.showSplash()!=contact.splash()) {
                    ENTITY_BATH_STATES.put(entityId, new BathState(true, prevState.lastEnterTime(), prevState.lastExitTime(), 0,contact.splash()));
                }
            }
        } else {
            if (prevState.wasInBath()) {
                // Entity may be leaving the bath - start grace period
                int newTicksOut = prevState.ticksOutOfBath() + 1;
                
                if (newTicksOut >= EXIT_GRACE_TICKS) {
                    // Entity has been out of bath long enough - trigger exit effects
                    if ((currentTime - prevState.lastExitTime()) > SPLASH_COOLDOWN_TICKS) {
                        broadcastExitWaterEffects(serverLevel, entity,prevState.showSplash());
                    }
                    ENTITY_BATH_STATES.put(entityId, new BathState(false, prevState.lastEnterTime(), currentTime, 0,prevState.showSplash()));
                } else {
                    // Still in grace period - update counter but keep wasInBath true
                    ENTITY_BATH_STATES.put(entityId, new BathState(true, prevState.lastEnterTime(), prevState.lastExitTime(), newTicksOut,prevState.showSplash()));
                }
            }
            // If already out of bath, do nothing
        }
        
        // Periodic cleanup of old entries (every ~5 minutes based on entity count)
        if (currentTime % 6000 == (entity.getId() % 6000)) {
            cleanupOldEntries(currentTime);
        }
    }
    
    /**
     * Check if entity is currently in a hot bath block
     */
    private static Contact contactAt(Entity entity) {
        var report=REPORTED.get(entity);
        if(report!=null&&entity.level().getGameTime()-report.tick()<=1)return report.contact();
        REPORTED.remove(entity);
        for(var pos:java.util.List.of(entity.blockPosition(),entity.blockPosition().below())) {
            var block=entity.level().getBlockState(pos).getBlock();
            if(!(block instanceof AbstractHotbathBlock))continue;
            java.util.Optional<com.crabmod.hotbath.custom_fluid.CustomFluidDefinition> definition=java.util.Optional.empty();
            if(entity.level().getBlockEntity(pos) instanceof com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity custom)definition=custom.getFluidDefinition();
            else if(block instanceof com.crabmod.hotbath.custom_fluid.CustomFluidBlock custom)definition=java.util.Optional.of(custom.getDefinition());
            return new Contact(pos.getY()+1,definition.map(com.crabmod.hotbath.custom_fluid.CustomFluidDefinition::showParticles).orElse(true),
                    definition.map(com.crabmod.hotbath.custom_fluid.CustomFluidDefinition::showBubbles).orElse(true));
        }
        return null;
    }

    /**
     * Broadcast enter water effects (splash sound + particles) to all clients
     */
    private static void broadcastEnterWaterEffects(ServerLevel serverLevel, Entity entity,Contact contact) {
        float surfaceY=(float)contact.surfaceY();
        double entityWidth = entity.getBoundingBox().getXsize();
        
        // Calculate volume based on entity size and fall distance
        float baseVolume = Math.min(1.0F, (float) entityWidth * 0.5F);
        float fallFactor = Math.min(1.0F, entity.fallDistance / 3.0F);
        float volume = Math.max(0.3F, baseVolume * (0.5F + fallFactor * 0.5F));
        
        // Play splash sound - null broadcasts to all players
        serverLevel.playSound(
            null,
            entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.PLAYER_SPLASH,
            SoundSource.PLAYERS,
            volume,
            0.8F + serverLevel.random.nextFloat() * 0.4F
        );
        
        // Calculate particle count based on entity size and fall distance
        int baseCount = (int) (1.0F + entityWidth * 15.0F);
        int particleCount = Math.min((int) (baseCount * (0.5F + fallFactor * 0.5F)), 40);
        
        // Send particles in batches for network efficiency
        if(contact.bubbles())serverLevel.sendParticles(
            ParticleTypes.BUBBLE,
            entity.getX(), surfaceY, entity.getZ(),
            particleCount,
            entityWidth * 0.5, 0.1, entityWidth * 0.5,
            0.1
        );
        
        if(contact.splash())serverLevel.sendParticles(
            ParticleTypes.SPLASH,
            entity.getX(), surfaceY, entity.getZ(),
            particleCount,
            entityWidth * 0.5, 0.0, entityWidth * 0.5,
            0.0
        );
    }
    
    /**
     * Broadcast exit water effects (exit sound) to all clients
     */
    private static void broadcastExitWaterEffects(ServerLevel serverLevel, Entity entity,boolean showSplash) {
        double entityWidth = entity.getBoundingBox().getXsize();
        float volume = Math.min(0.8F, (float) entityWidth * 0.4F);
        
        // Play exit water sound - null broadcasts to all players
        serverLevel.playSound(
            null,
            entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.AMBIENT_UNDERWATER_EXIT,
            SoundSource.PLAYERS,
            volume,
            0.9F + serverLevel.random.nextFloat() * 0.2F
        );
        
        // Small splash particles when exiting
        int particleCount = Math.min((int) (entityWidth * 8.0F), 15);
        if(showSplash)serverLevel.sendParticles(
            ParticleTypes.SPLASH,
            entity.getX(), entity.getY() + 0.5, entity.getZ(),
            particleCount,
            entityWidth * 0.3, 0.1, entityWidth * 0.3,
            0.05
        );
    }
    
    /**
     * Remove entries for entities that haven't been updated in a while
     */
    private static void cleanupOldEntries(long currentTime) {
        // Remove entries older than 5 minutes (6000 ticks)
        ENTITY_BATH_STATES.entrySet().removeIf(entry -> {
            BathState state = entry.getValue();
            long lastActivity = Math.max(state.lastEnterTime(), state.lastExitTime());
            return (currentTime - lastActivity) > 6000;
        });
    }
    
    /**
     * Called when an entity is removed from the world (death, dimension change, etc.)
     * This prevents memory leaks.
     */
    public static void onEntityRemoved(int entityId) {
        ENTITY_BATH_STATES.keySet().removeIf(entity->entity.getId()==entityId);
        REPORTED.keySet().removeIf(entity->entity.getId()==entityId);
    }
    @SubscribeEvent public static void leaving(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        if(event.getLevel().isClientSide())return;
        var entity=event.getEntity();ENTITY_BATH_STATES.remove(entity);REPORTED.remove(entity);
        com.crabmod.hotbath.bathing.BathImmersion.forget(entity);
    }
}
