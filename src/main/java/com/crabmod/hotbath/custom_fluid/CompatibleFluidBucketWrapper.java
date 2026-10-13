package com.crabmod.hotbath.custom_fluid;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

/**
 * Keeps ordinary bucket operations on NeoForge's native implementation while
 * delegating data-pack fluids to Hot Bath's component-aware container handler.
 * Extending FluidBucketWrapper also preserves Create's stacked-bucket handling.
 */
public final class CompatibleFluidBucketWrapper extends FluidBucketWrapper {
    private final IFluidHandlerItem custom;

    public CompatibleFluidBucketWrapper(ItemStack stack, IFluidHandlerItem custom) {
        super(stack);
        this.custom = custom;
    }

    private boolean isCustomBucket() {
        return container.is(CustomFluidItems.CUSTOM_FLUID_BUCKET.get());
    }

    @Override
    public FluidStack getFluid() {
        return isCustomBucket() ? custom.getFluidInTank(0) : super.getFluid();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack fluid) {
        if (tank != 0) {
            return false;
        }
        return CustomFluidStackHelper.isDynamicCustomFluid(fluid)
                ? custom.isFluidValid(tank, fluid) : super.canFillFluidType(fluid);
    }

    @Override
    public int fill(FluidStack fluid, FluidAction action) {
        // Do not let ordinary fluids overwrite an existing custom bath, or let
        // malformed dynamic fluids fall through and lose their custom identity.
        if (!CustomFluidStackHelper.isDynamicCustomFluid(fluid)) {
            return isCustomBucket() ? 0 : super.fill(fluid, action);
        }
        if (!container.is(Items.BUCKET) || container.getCount() != 1) {
            return 0;
        }
        int result = custom.fill(fluid, action);
        // Both delegates replace their container on execution; simulations must
        // leave the exposed item stack unchanged.
        if (action.execute() && result > 0) {
            container = custom.getContainer();
        }
        return result;
    }

    @Override
    public FluidStack drain(int amount, FluidAction action) {
        if (!isCustomBucket()) {
            return super.drain(amount, action);
        }
        if (container.getCount() != 1) {
            return FluidStack.EMPTY;
        }
        FluidStack result = custom.drain(amount, action);
        if (action.execute() && !result.isEmpty()) {
            container = custom.getContainer();
        }
        return result;
    }

    @Override
    public FluidStack drain(FluidStack fluid, FluidAction action) {
        if (!isCustomBucket()) {
            return super.drain(fluid, action);
        }
        if (container.getCount() != 1) {
            return FluidStack.EMPTY;
        }
        FluidStack result = custom.drain(fluid, action);
        if (action.execute() && !result.isEmpty()) {
            container = custom.getContainer();
        }
        return result;
    }
}
