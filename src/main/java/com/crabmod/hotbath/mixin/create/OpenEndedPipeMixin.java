package com.crabmod.hotbath.mixin.create;

import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = OpenEndedPipe.class, remap = false)
public abstract class OpenEndedPipeMixin {
    @Unique private ResourceLocation hotbath$drainedCustomFluidId;

    @Shadow public abstract Level getWorld();
    @Shadow public abstract BlockPos getOutputPos();

    @Inject(method = "provideFluidToSpace", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void hotbath$preserveDifferentCustomFluid(FluidStack stack, boolean simulate,
                                                    CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation fluidId = CustomFluidStackHelper.getFluidId(stack);
        Level level = getWorld();
        BlockPos pos = getOutputPos();
        if (fluidId != null && level != null && level.isLoaded(pos)
                && CustomFluidStackHelper.hasDifferentFluidIdAt(level, pos, fluidId)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "provideFluidToSpace", at = @At("RETURN"), remap = false, require = 1)
    private void hotbath$restorePlacedCustomFluidId(FluidStack stack, boolean simulate,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (simulate || !cir.getReturnValueZ()) {
            return;
        }
        ResourceLocation fluidId = CustomFluidStackHelper.getFluidId(stack);
        if (fluidId != null) {
            CustomFluidStackHelper.setFluidIdAt(getWorld(), getOutputPos(), fluidId);
        }
    }

    @Inject(method = "removeFluidFromSpace", at = @At("HEAD"), remap = false, require = 1)
    private void hotbath$captureDrainedCustomFluidId(boolean simulate, CallbackInfoReturnable<FluidStack> cir) {
        Level level = getWorld();
        BlockPos pos = getOutputPos();
        hotbath$drainedCustomFluidId = level != null && level.isLoaded(pos)
                ? CustomFluidStackHelper.getFluidIdAt(level, pos) : null;
    }

    @Inject(method = "removeFluidFromSpace", at = @At("RETURN"), remap = false, require = 1)
    private void hotbath$restoreDrainedCustomFluidId(boolean simulate, CallbackInfoReturnable<FluidStack> cir) {
        ResourceLocation fluidId = hotbath$drainedCustomFluidId;
        hotbath$drainedCustomFluidId = null;
        FluidStack stack = cir.getReturnValue();
        if (fluidId != null && CustomFluidStackHelper.isDynamicCustomFluid(stack)
                && CustomFluidStackHelper.getFluidId(stack) == null) {
            CustomFluidStackHelper.setFluidId(stack, fluidId);
        }
    }
}
