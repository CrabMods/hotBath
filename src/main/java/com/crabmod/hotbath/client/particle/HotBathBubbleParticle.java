package com.crabmod.hotbath.client.particle;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;

/**
 * Bubble particle for hot bath bubble columns.
 * Physics replicate vanilla {@code BubbleColumnUpParticle} exactly:
 * negative gravity for upward buoyancy, long lifetime so bubbles
 * naturally rise above the water surface and are visible from above.
 */
public class HotBathBubbleParticle extends TextureSheetParticle {

    HotBathBubbleParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z);
        this.gravity = -0.125F;
        this.friction = 0.85F;
        this.setSize(0.02F, 0.02F);
        this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
        this.xd = xSpeed * 0.2F + (Math.random() * 2.0 - 1.0) * 0.02F;
        this.yd = ySpeed * 0.2F + (Math.random() * 2.0 - 1.0) * 0.02F;
        this.zd = zSpeed * 0.2F + (Math.random() * 2.0 - 1.0) * 0.02F;
        this.lifetime = (int)(40.0 / (Math.random() * 0.8 + 0.2));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
            if (!this.level.getFluidState(pos).is(FluidTags.WATER)
                    && !(this.level.getBlockState(pos).getBlock() instanceof AbstractHotbathBlock)
                    && !(this.level.getBlockEntity(pos) instanceof com.crabmod.hotbath.api.BathFluidContainer container
                        &&container.getCustomFluidDefinition().map(com.crabmod.hotbath.custom_fluid.CustomFluidDefinition::showBubbles).orElse(true)
                        &&container.touchesBathFluid(new net.minecraft.world.phys.AABB(this.x-.001,this.y-.001,this.z-.001,this.x+.001,this.y+.001,this.z+.001)))) {
                this.remove();
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            HotBathBubbleParticle particle = new HotBathBubbleParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprite);
            return particle;
        }
    }
}
