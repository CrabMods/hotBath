package com.crabmod.hotbath.fluid_details;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class BaseFluidType extends FluidType {
    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;
    private final ResourceLocation overlayTexture;
    private final int tintColor;
    private final Vector3f fogColor;
    private final Supplier<? extends ParticleOptions> dripParticle;
    private final Supplier<? extends ParticleOptions> bubbleParticle;
    private final Supplier<? extends ParticleOptions> splashParticle;
    private final Supplier<? extends Fluid> fluidSupplier;

    public BaseFluidType(
            final ResourceLocation stillTexture,
            final ResourceLocation flowingTexture,
            final ResourceLocation overlayTexture,
            final int tintColor,
            final Vector3f fogColor,
            final Properties properties,
            final Supplier<? extends ParticleOptions> dripParticle,
            final Supplier<? extends ParticleOptions> bubbleParticle,
            final Supplier<? extends ParticleOptions> splashParticle,
            final Supplier<? extends Fluid> fluidSupplier) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.overlayTexture = overlayTexture;
        this.tintColor = tintColor;
        this.fogColor = fogColor;
        this.dripParticle = dripParticle;
        this.bubbleParticle = bubbleParticle;
        this.splashParticle = splashParticle;
        this.fluidSupplier = fluidSupplier;
    }

    public ParticleOptions getBubbleParticle() {
        return bubbleParticle != null ? bubbleParticle.get() : null;
    }

    public ParticleOptions getSplashParticle() {
        return splashParticle != null ? splashParticle.get() : null;
    }

    public ResourceLocation getStillTexture() {
        return stillTexture;
    }

    public ResourceLocation getFlowingTexture() {
        return flowingTexture;
    }

    public ResourceLocation getOverlayTexture() {
        return overlayTexture;
    }

    public int getTintColor() {
        return tintColor;
    }

    public Vector3f getFogColor() {
        return fogColor;
    }

    @Override
    public boolean canConvertToSource(net.minecraft.world.level.material.FluidState state,
                                      net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos) {
        return com.crabmod.hotbath.HotBathConfig.areInfiniteBathSourcesEnabled()
                && super.canConvertToSource(state, level, pos);
    }

    @Override
    public boolean canConvertToSource(net.minecraftforge.fluids.FluidStack stack) {
        return com.crabmod.hotbath.HotBathConfig.areInfiniteBathSourcesEnabled()
                && super.canConvertToSource(stack);
    }

    /**
     * Override to allow water-breathing entities (like tropical fish) to survive in hotBath fluids.
     * Entities that can breathe underwater won't drown in our fluid.
     */
    @Override
    public boolean canDrownIn(LivingEntity entity) {
        // If the entity can breathe underwater (like fish), it won't drown in our fluid
        if (entity.canBreatheUnderwater()) {
            return false;
        }
        // Otherwise, use the default behavior (canDrown property)
        return super.canDrownIn(entity);
    }

    public ParticleOptions getDripParticle() {
        return dripParticle != null ? dripParticle.get() : null;
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return stillTexture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowingTexture;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return overlayTexture;
            }

            @Override
            public int getTintColor() {
                return tintColor;
            }

            @Override
            public @NotNull Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                return fogColor;
            }
        });
    }
}










