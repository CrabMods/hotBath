package com.crabmod.hotbath.mixin.create;

import com.crabmod.hotbath.custom_fluid.CustomFluidStackContext;
import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HosePulleyFluidHandler.class, remap = false)
public class HosePulleyFluidHandlerMixin {
    @Shadow private SmartFluidTank internalTank;

    @Inject(method = "fill", at = @At("HEAD"), remap = false)
    private void hotbath$captureCustomFluidStack(FluidStack resource, IFluidHandler.FluidAction action, CallbackInfoReturnable<Integer> cir) {
        if (CustomFluidStackHelper.getFluidId(resource) != null) {
            CustomFluidStackContext.setCreateDepositStack(resource);
        }
    }

    @Inject(method = "fill", at = @At("RETURN"), remap = false)
    private void hotbath$clearCustomFluidStack(FluidStack resource, IFluidHandler.FluidAction action, CallbackInfoReturnable<Integer> cir) {
        CustomFluidStackContext.clearCreateDepositStack();
    }

    @Redirect(method = "drainInternal", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;pullNext(Lnet/minecraft/core/BlockPos;Z)Z"),
            remap = false, require = 1)
    private boolean hotbath$checkBathBeforeExtraction(FluidDrainingBehaviour drainer, BlockPos pos, boolean simulate,
                                                    int maxDrain, FluidStack requested, IFluidHandler.FluidAction action) {
        FluidStack available = drainer.getDrainableFluid(pos);
        FluidStack buffered = internalTank.getFluid();
        // Create checks these stacks after pulling; reject a different bath before removing its source.
        if (CustomFluidStackHelper.isDynamicCustomFluid(available)
                && ((requested != null && !available.isFluidEqual(requested))
                || (!buffered.isEmpty() && !available.isFluidEqual(buffered)))) {
            return false;
        }
        return drainer.pullNext(pos, simulate);
    }
}
