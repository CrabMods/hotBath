package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HotBath.MOD_ID)
public final class CustomFluidCapabilities {
    public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(HotBath.MOD_ID, "custom_fluid_container");
    private CustomFluidCapabilities() {}

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        // Empty buckets use their intrinsic provider, installed by the narrow BucketItem mixin.
        if (stack.is(Items.GLASS_BOTTLE)) { event.addCapability(CAPABILITY_ID, createProvider(stack, false)); }
    }

    public static ICapabilityProvider createProvider(ItemStack stack, boolean bucket) {
        return bucket ? new CompatibleFluidBucketWrapper(stack) : new BottleProvider(stack);
    }

    private static final class BottleProvider implements ICapabilityProvider {
        private final LazyOptional<IFluidHandlerItem> handler;
        private BottleProvider(ItemStack stack) { handler = LazyOptional.of(() -> new CustomFluidBottleHandler(stack)); }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == ForgeCapabilities.FLUID_HANDLER_ITEM ? handler.cast() : LazyOptional.empty();
        }
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
