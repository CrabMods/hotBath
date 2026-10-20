package com.crabmod.hotbath.api.client;

import com.crabmod.hotbath.api.HotBathApi;
import com.crabmod.hotbath.registers.ParticleRegister;
import com.crabmod.hotbath.client.particle.HotBathBubbleParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.function.Predicate;

/** Client liquid visuals; the caller owns sampling positions and the contact predicate. */
public final class BathFluidVisuals {
    private BathFluidVisuals() {}
    public static void animateSurface(ClientLevel level,FluidStack fluid,RandomSource random,
                                      double x,double y,double z,Predicate<AABB> contains) {
        var properties=HotBathApi.fluidProperties(fluid,true);
        if(properties.steam()&&random.nextInt(10)==0)
            level.addParticle(ParticleRegister.STEAM_PARTICLE.get(),x,y+.02,z,0,.02,0);
        if(properties.bubbles()&&random.nextInt(10)==0)createBubble(level,fluid,x,y-.1,z,contains);
    }
    public static net.minecraft.client.particle.Particle createBubble(ClientLevel level,FluidStack fluid,
            double x,double y,double z,Predicate<AABB> contains) {
        if(!HotBathApi.fluidProperties(fluid,true).bubbles())return null;
        var particle=Minecraft.getInstance().particleEngine.createParticle(ParticleRegister.HOT_WATER_BUBBLE.get(),x,y,z,0,.04,0);
        if(particle instanceof HotBathBubbleParticle bubble)bubble.setContactBounds(contains);
        return particle;
    }
}
