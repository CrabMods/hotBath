package com.crabmod.hotbath.custom_fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public final class CustomFluidCapabilities {
    private CustomFluidCapabilities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new CompatibleFluidBucketWrapper(
                        stack, new CustomFluidContainerHandler(stack, ContainerKind.BUCKET)),
                Items.BUCKET,
                CustomFluidItems.CUSTOM_FLUID_BUCKET.get());
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new CustomFluidContainerHandler(stack, ContainerKind.BOTTLE),
                Items.GLASS_BOTTLE,
                CustomFluidItems.CUSTOM_FLUID_BOTTLE.get());
    }

    private enum ContainerKind {
        BUCKET(CustomFluidStackHelper.BUCKET_AMOUNT),
        BOTTLE(CustomFluidStackHelper.BOTTLE_AMOUNT);

        final int amount;

        ContainerKind(int amount) {
            this.amount = amount;
        }
    }

    private static final class CustomFluidContainerHandler implements IFluidHandlerItem {
        private ItemStack container;
        private final ContainerKind kind;

        private CustomFluidContainerHandler(ItemStack container, ContainerKind kind) {
            this.container = container;
            this.kind = kind;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? getContainedFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? kind.amount : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && CustomFluidStackHelper.getFluidId(stack) != null;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            ResourceLocation fluidId = CustomFluidStackHelper.getFluidId(resource);
            if (fluidId == null || resource.getAmount() < kind.amount || !isEmptyContainer()) {
                return 0;
            }

            if (action.execute()) {
                container = switch (kind) {
                    case BUCKET -> CustomFluidAPI.createBucket(fluidId);
                    case BOTTLE -> CustomFluidAPI.createBottle(fluidId);
                };
            }
            return kind.amount;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack contained = getContainedFluid();
            if (contained.isEmpty()
                    || resource.getAmount() < kind.amount
                    || !CustomFluidStackHelper.hasSameFluidId(contained, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(kind.amount, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack contained = getContainedFluid();
            if (contained.isEmpty() || maxDrain < kind.amount) {
                return FluidStack.EMPTY;
            }

            if (action.execute()) {
                container = switch (kind) {
                    case BUCKET -> new ItemStack(Items.BUCKET);
                    case BOTTLE -> new ItemStack(Items.GLASS_BOTTLE);
                };
            }
            return contained;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        private FluidStack getContainedFluid() {
            ResourceLocation fluidId = CustomFluidStackHelper.getFluidId(container);
            if (fluidId == null || !isFilledContainer()) {
                return FluidStack.EMPTY;
            }
            return CustomFluidStackHelper.createStack(fluidId, kind.amount);
        }

        private boolean isEmptyContainer() {
            return switch (kind) {
                case BUCKET -> container.is(Items.BUCKET);
                case BOTTLE -> container.is(Items.GLASS_BOTTLE);
            };
        }

        private boolean isFilledContainer() {
            return switch (kind) {
                case BUCKET -> container.is(CustomFluidItems.CUSTOM_FLUID_BUCKET.get());
                case BOTTLE -> container.is(CustomFluidItems.CUSTOM_FLUID_BOTTLE.get());
            };
        }
    }
}
