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
    private static final Map<Integer, BathState> ENTITY_BATH_STATES = new ConcurrentHashMap<>();
    
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
        boolean isInBath = isEntityInHotBath(entity);
        int entityId = entity.getId();
        long currentTime = level.getGameTime();
        
        BathState prevState = ENTITY_BATH_STATES.get(entityId);
        
        if (prevState == null) {
            // First time seeing this entity
            if (isInBath) {
                // Entity spawned in bath or first detection
                broadcastEnterWaterEffects(serverLevel, entity);
                ENTITY_BATH_STATES.put(entityId, new BathState(true, currentTime, 0, 0,showParticles(entity)));
            }
            return;
        }
        
        if (isInBath) {
            if (!prevState.wasInBath()) {
                // Entity just entered the bath
                if ((currentTime - prevState.lastEnterTime()) > SPLASH_COOLDOWN_TICKS) {
                    broadcastEnterWaterEffects(serverLevel, entity);
                }
                ENTITY_BATH_STATES.put(entityId, new BathState(true, currentTime, prevState.lastExitTime(), 0,showParticles(entity)));
            } else {
                // Entity still in bath - reset out-of-bath counter
                if (prevState.ticksOutOfBath() > 0 || prevState.showSplash()!=showParticles(entity)) {
                    ENTITY_BATH_STATES.put(entityId, new BathState(true, prevState.lastEnterTime(), prevState.lastExitTime(), 0,showParticles(entity)));
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
        if (currentTime % 6000 == (entityId % 6000)) {
            cleanupOldEntries(currentTime);
        }
    }
    
    /**
     * Check if entity is currently in a hot bath block
     */
    private static com.crabmod.hotbath.api.BathFluidContainer containerAt(Entity entity) {
        var bounds=entity.getBoundingBox();var pos=new BlockPos.MutableBlockPos();
        for(int x=Mth.floor(bounds.minX);x<=Mth.floor(bounds.maxX-1e-7);x++)
            for(int y=Mth.floor(bounds.minY);y<=Mth.floor(bounds.maxY-1e-7);y++)
                for(int z=Mth.floor(bounds.minZ);z<=Mth.floor(bounds.maxZ-1e-7);z++)
                    if(entity.level().getBlockEntity(pos.set(x,y,z)) instanceof com.crabmod.hotbath.api.BathFluidContainer container&&container.touchesBathFluid(bounds))return container;
        return null;
    }
    private static java.util.Optional<com.crabmod.hotbath.custom_fluid.CustomFluidDefinition> definition(Entity entity) {
        var container=containerAt(entity);if(container!=null)return container.getCustomFluidDefinition();
        for(var pos:java.util.List.of(entity.blockPosition(),entity.blockPosition().below())) {
            if(entity.level().getBlockEntity(pos) instanceof com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity fluid)return fluid.getFluidDefinition();
            if(entity.level().getBlockState(pos).getBlock() instanceof com.crabmod.hotbath.custom_fluid.CustomFluidBlock fluid)return java.util.Optional.of(fluid.getDefinition());
        }
        return java.util.Optional.empty();
    }
    private static boolean showParticles(Entity entity){return definition(entity).map(com.crabmod.hotbath.custom_fluid.CustomFluidDefinition::showParticles).orElse(true);}
    private static boolean showBubbles(Entity entity){return definition(entity).map(com.crabmod.hotbath.custom_fluid.CustomFluidDefinition::showBubbles).orElse(true);}

    private static boolean isEntityInHotBath(Entity entity) {
        BlockPos pos = entity.blockPosition();
        Level level = entity.level();
        
        // Check current position and one below (in case entity is floating at surface)
        return containerAt(entity)!=null||level.getBlockState(pos).getBlock() instanceof AbstractHotbathBlock ||
               level.getBlockState(pos.below()).getBlock() instanceof AbstractHotbathBlock;
    }
    
    /**
     * Broadcast enter water effects (splash sound + particles) to all clients
     */
    private static void broadcastEnterWaterEffects(ServerLevel serverLevel, Entity entity) {
        var container=containerAt(entity);
        float surfaceY=container==null?(float)Mth.floor(entity.getY())+1:(float)container.bathSurfaceY();
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
        if(showBubbles(entity))serverLevel.sendParticles(
            ParticleTypes.BUBBLE,
            entity.getX(), surfaceY, entity.getZ(),
            particleCount,
            entityWidth * 0.5, 0.1, entityWidth * 0.5,
            0.1
        );
        
        if(showParticles(entity))serverLevel.sendParticles(
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
        ENTITY_BATH_STATES.remove(entityId);
    }
}
