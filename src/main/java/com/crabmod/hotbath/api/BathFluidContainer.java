package com.crabmod.hotbath.api;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

/** A container exposes identity and occupied fluid bounds; Hot Bath owns its behavior. */
public interface BathFluidContainer {
    FluidStack fluid();
    @Nullable ResourceLocation getCustomFluidId();
    boolean touchesBathFluid(AABB bounds);
    double bathSurfaceY();
    /** Refresh container-owned cached state after the shared definitions change. */
    void refreshBathDefinition();

    default Optional<CustomFluidDefinition> getCustomFluidDefinition() {
        var id=getCustomFluidId();
        return fluid().isEmpty()||id==null?Optional.empty():CustomFluidAPI.getFluidDefinition(id);
    }
}
