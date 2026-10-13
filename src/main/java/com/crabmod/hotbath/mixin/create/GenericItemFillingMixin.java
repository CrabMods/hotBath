package com.crabmod.hotbath.mixin.create;

import com.crabmod.hotbath.custom_fluid.CompatibleFluidBucketWrapper;
import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps Create's bucket fast path compatible with data-driven fluid identities. */
@Mixin(value = GenericItemFilling.class, remap = false)
public abstract class GenericItemFillingMixin {
    @Inject(method = "getRequiredAmountForItem", at = @At("HEAD"), cancellable = true)
    private static void hotbath$customBucketAmount(Level level, ItemStack stack, FluidStack fluid,
                                                  CallbackInfoReturnable<Integer> cir) {
        // The shared dynamic fluid has no ordinary getBucket() identity. Ask its component-aware handler.
        if (!CustomFluidStackHelper.isDynamicCustomFluid(fluid)) {
            return;
        }
        // Create consumes one item at a time, even when the input holds several buckets.
        var handler = stack.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
        if (!(handler instanceof CompatibleFluidBucketWrapper)) {
            return;
        }
        int accepted = handler.fill(fluid.copyWithAmount(CustomFluidStackHelper.BUCKET_AMOUNT), FluidAction.SIMULATE);
        cir.setReturnValue(accepted > 0 ? accepted : -1);
    }
}
