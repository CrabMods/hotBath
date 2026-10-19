package com.crabmod.hotbath.client.particle;

import com.crabmod.hotbath.HotBathClientConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Fly particle that orbits around a dirty player.
 * Spawns when player maintains 100% dirtiness for 2+ game days.
 */
@OnlyIn(Dist.CLIENT)
public class FlyParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;
    private double orbitX;
    private double orbitY;
    private double orbitZ;
    private final boolean reverseOrbit;
    private final float orbitSpeed;
    private final float orbitRadius;
    private final float verticalOffset;
    private float orbitAngle;
    
    // Maximum distance to follow player
    private static final double MAX_FOLLOW_DISTANCE = 8.0;
    // How quickly fly catches up to player (0-1, higher = faster)
    private static final double FOLLOW_SPEED = 0.08;

    protected FlyParticle(ClientLevel world, double x, double y, double z, 
                          double orbitCenterX, double orbitCenterY, double orbitCenterZ, 
                          SpriteSet spriteSet) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        
        // Particle size
        this.quadSize = 0.12F + world.random.nextFloat() * 0.04F;
        
        // Orbit center (player position)
        this.orbitX = orbitCenterX;
        this.orbitY = orbitCenterY;
        this.orbitZ = orbitCenterZ;
        
        // Physics
        this.hasPhysics = true;
        this.friction = 0.92F;
        
        // Lifetime: 2-4 seconds
        this.lifetime = 40 + world.random.nextInt(40);
        
        // Random orbit parameters
        this.reverseOrbit = world.random.nextBoolean();
        this.orbitSpeed = 3.0F + world.random.nextFloat() * 3.0F;
        this.orbitRadius = 0.6F + world.random.nextFloat() * 0.8F;
        this.verticalOffset = (world.random.nextFloat() - 0.5F) * 0.6F;
        
        // Random starting angle
        this.orbitAngle = world.random.nextFloat() * 360F;
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        if (!HotBathClientConfig.SHOW_FLIES.get()) return 0.0F;
        // Smooth fade in/out
        float lifeProgress = ((float) this.age + scaleFactor) / (float) this.lifetime;
        float fadeIn = Mth.clamp(lifeProgress * 6.0F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((1.0F - lifeProgress) * 4.0F, 0.0F, 1.0F);
        return this.quadSize * fadeIn * fadeOut;
    }

    @Override
    public void tick() {
        if (!HotBathClientConfig.SHOW_FLIES.get()) {
            remove();
            return;
        }
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        // Wing flapping animation (2 frames)
        int spriteFrame = this.age % 4 >= 2 ? 1 : 0;
        this.setSprite(spriteSet.get(spriteFrame, 1));
        
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        
        // Update orbit center to follow nearest player
        updateOrbitCenter();
        
        // Update orbit angle
        float direction = reverseOrbit ? -1F : 1F;
        this.orbitAngle += orbitSpeed * direction;
        
        // Calculate target orbit position
        Vec3 targetPos = getOrbitPosition();
        
        // Smooth movement towards target
        double moveSpeed = 0.15;
        this.xd = (targetPos.x - this.x) * moveSpeed + random.nextGaussian() * 0.01;
        this.yd = (targetPos.y - this.y) * moveSpeed + random.nextGaussian() * 0.008;
        this.zd = (targetPos.z - this.z) * moveSpeed + random.nextGaussian() * 0.01;
        
        // Bounce up if hitting ground
        if (this.onGround) {
            this.yd = Math.abs(this.yd) + 0.15;
        }
        
        // Apply movement
        this.move(this.xd, this.yd, this.zd);
        
        // Apply friction
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
    }
    
    /**
     * Update orbit center to smoothly follow the nearest player
     */
    private void updateOrbitCenter() {
        Player nearestPlayer = findNearestPlayer();
        if (nearestPlayer != null) {
            double targetX = nearestPlayer.getX();
            double targetY = nearestPlayer.getY() + 1.5; // Head height
            double targetZ = nearestPlayer.getZ();
            
            // Smoothly interpolate orbit center towards player
            this.orbitX += (targetX - this.orbitX) * FOLLOW_SPEED;
            this.orbitY += (targetY - this.orbitY) * FOLLOW_SPEED;
            this.orbitZ += (targetZ - this.orbitZ) * FOLLOW_SPEED;
        }
    }
    
    /**
     * Find the nearest player within range
     */
    private Player findNearestPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        
        Player nearest = null;
        double nearestDistSq = MAX_FOLLOW_DISTANCE * MAX_FOLLOW_DISTANCE;
        
        for (Player player : mc.level.players()) {
            double distSq = player.distanceToSqr(this.orbitX, this.orbitY - 1.5, this.orbitZ);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = player;
            }
        }
        
        return nearest;
    }

    /**
     * Calculate the target position on the orbit
     */
    private Vec3 getOrbitPosition() {
        double radians = Math.toRadians(orbitAngle);
        
        // Circular orbit around player
        double offsetX = Math.cos(radians) * orbitRadius;
        double offsetZ = Math.sin(radians) * orbitRadius;
        
        // Vertical bobbing motion
        double bobbing = Math.sin(this.age * 0.2) * 0.15;
        
        return new Vec3(
            orbitX + offsetX,
            orbitY + verticalOffset + bobbing,
            orbitZ + offsetZ
        );
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    /**
     * Factory for creating FlyParticle instances
     */
    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world, 
                                       double x, double y, double z, 
                                       double xSpeed, double ySpeed, double zSpeed) {
            // x, y, z = spawn position
            // xSpeed, ySpeed, zSpeed = orbit center (player position)
            FlyParticle particle = new FlyParticle(world, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
            particle.setSprite(spriteSet.get(0, 1));
            return particle;
        }
    }
}
