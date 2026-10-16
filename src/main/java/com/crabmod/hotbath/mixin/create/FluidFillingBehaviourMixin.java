package com.crabmod.hotbath.mixin.create;

import com.crabmod.hotbath.custom_fluid.CustomFluidStackContext;
import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidFillingBehaviour.class, remap = false)
public abstract class FluidFillingBehaviourMixin {
    @Redirect(method = "tryDeposit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            remap = true), remap = false, require = 2)
    private boolean hotbath$restorePlacedCustomFluidId(Level level, BlockPos pos, BlockState state, int flags) {
        boolean placed = level.setBlock(pos, state, flags);
        ResourceLocation fluidId = CustomFluidStackHelper.getFluidId(CustomFluidStackContext.getCreateDepositStack());
        if (placed && fluidId != null) {
            // Bind identity to the actual native placement, independent of the search area's size.
            CustomFluidStackHelper.setFluidIdAt(level, pos, fluidId);
        }
        return placed;
    }

    @Inject(method = "getAtPos", at = @At("RETURN"), cancellable = true, remap = false)
    private void hotbath$blockDifferentCustomFluid(Level level, BlockPos pos, Fluid fluid, CallbackInfoReturnable<Object> cir) {
        if (!CustomFluidStackHelper.isDynamicCustomFluid(fluid)) {
            return;
        }

        ResourceLocation fluidId = CustomFluidStackHelper.getFluidId(CustomFluidStackContext.getCreateDepositStack());
        if (fluidId == null || !CustomFluidStackHelper.hasDifferentFluidIdAt(level, pos, fluidId)) {
            return;
        }

        cir.setReturnValue(hotbath$blockingSpaceType());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object hotbath$blockingSpaceType() {
        try {
            Class<?> spaceType = Class.forName("com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour$SpaceType");
            return Enum.valueOf((Class<? extends Enum>) spaceType.asSubclass(Enum.class), "BLOCKING");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Create FluidFillingBehaviour.SpaceType is unavailable", e);
        }
    }
}
