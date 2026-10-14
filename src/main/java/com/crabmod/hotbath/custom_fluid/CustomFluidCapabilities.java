package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.registers.ItemRegister;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

public final class CustomFluidCapabilities {
    private CustomFluidCapabilities() {}

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> new CompatibleFluidBucketWrapper(stack),
                Items.BUCKET, CustomFluidItems.CUSTOM_FLUID_BUCKET.get());
        // NeoForge's automatic provider excludes our BucketItem subclasses.
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> new FluidBucketWrapper(stack),
                ItemRegister.HOT_WATER_BUCKET.get(), ItemRegister.HONEY_BATH_BUCKET.get(),
                ItemRegister.MILK_BATH_BUCKET.get(), ItemRegister.HERBAL_BATH_BUCKET.get(),
                ItemRegister.PEONY_BATH_BUCKET.get(), ItemRegister.ROSE_BATH_BUCKET.get());
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> new CustomFluidBottleHandler(stack),
                Items.GLASS_BOTTLE, CustomFluidItems.CUSTOM_FLUID_BOTTLE.get());
    }

    private static final class CustomFluidBottleHandler implements IFluidHandlerItem {
        private ItemStack container;

        private CustomFluidBottleHandler(ItemStack stack) {
            container = stack;
        }

        @Override
        public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? getContainedFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? CustomFluidStackHelper.BOTTLE_AMOUNT : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack fluid) {
            return tank == 0 && CustomFluidStackHelper.getDefinition(fluid).isPresent();
        }

        @Override
        public int fill(FluidStack fluid, FluidAction action) {
            ResourceLocation id = CustomFluidStackHelper.getFluidId(fluid);
            if (container.getCount() != 1 || !container.is(Items.GLASS_BOTTLE) || id == null
                    || CustomFluidRegistry.getDefinition(id).isEmpty()
                    || fluid.getAmount() < CustomFluidStackHelper.BOTTLE_AMOUNT) {
                return 0;
            }
            if (action.execute()) { container = CustomFluidAPI.createBottle(id); }
            return CustomFluidStackHelper.BOTTLE_AMOUNT;
        }

        @Override
        public FluidStack drain(FluidStack fluid, FluidAction action) {
            FluidStack contained = getContainedFluid();
            if (fluid.getAmount() < CustomFluidStackHelper.BOTTLE_AMOUNT
                    || !CustomFluidStackHelper.hasSameFluidId(contained, fluid)) {
                return FluidStack.EMPTY;
            }
            return drain(CustomFluidStackHelper.BOTTLE_AMOUNT, action);
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            FluidStack contained = getContainedFluid();
            if (container.getCount() != 1 || amount < CustomFluidStackHelper.BOTTLE_AMOUNT || contained.isEmpty()) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) { container = new ItemStack(Items.GLASS_BOTTLE); }
            return contained;
        }

        @Override
        public ItemStack getContainer() { return container; }

        private FluidStack getContainedFluid() {
            if (!container.is(CustomFluidItems.CUSTOM_FLUID_BOTTLE.get())) { return FluidStack.EMPTY; }
            ResourceLocation id = CustomFluidStackHelper.getFluidId(container.copy());
            return id == null || CustomFluidRegistry.getDefinition(id).isEmpty()
                    ? FluidStack.EMPTY : CustomFluidStackHelper.createStack(id, CustomFluidStackHelper.BOTTLE_AMOUNT);
        }
    }
}
