package com.crabmod.hotbath.custom_fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

/** Native bucket semantics with component/NBT identity for data-pack bath fluids. */
public final class CompatibleFluidBucketWrapper extends FluidBucketWrapper {
    public CompatibleFluidBucketWrapper(ItemStack stack) {
        super(stack);
    }

    @Override
    public int fill(FluidStack fluid, FluidAction action) {
        // A malformed filled custom bucket must not be treated as an empty bucket.
        return container.is(Items.BUCKET) ? super.fill(fluid, action) : 0;
    }

    @Override
    public FluidStack getFluid() {
        if (!container.is(CustomFluidItems.CUSTOM_FLUID_BUCKET.get())) {
            return super.getFluid();
        }
        // Legacy lookup may migrate data, so read a copy even during SIMULATE.
        ResourceLocation id = CustomFluidStackHelper.getFluidId(container.copy());
        return id == null || CustomFluidRegistry.getDefinition(id).isEmpty()
                ? FluidStack.EMPTY : CustomFluidStackHelper.createStack(id, CustomFluidStackHelper.BUCKET_AMOUNT);
    }

    @Override
    public boolean canFillFluidType(FluidStack fluid) {
        return CustomFluidStackHelper.isDynamicCustomFluid(fluid)
                ? CustomFluidStackHelper.getDefinition(fluid).isPresent() : super.canFillFluidType(fluid);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack fluid) {
        return tank == 0 && canFillFluidType(fluid);
    }

    @Override
    protected void setFluid(FluidStack fluid) {
        if (CustomFluidStackHelper.isDynamicCustomFluid(fluid)) {
            container = CustomFluidAPI.createBucket(CustomFluidStackHelper.getFluidId(fluid));
        } else {
            super.setFluid(fluid);
        }
    }
}
