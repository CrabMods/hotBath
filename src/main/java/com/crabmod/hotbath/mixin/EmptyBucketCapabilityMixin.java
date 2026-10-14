package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.custom_fluid.CustomFluidCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class EmptyBucketCapabilityMixin {
    @Inject(method="initCapabilities", at=@At("RETURN"), cancellable=true, remap=false)
    private void hotbath$emptyBucket(ItemStack stack, CompoundTag tag, CallbackInfoReturnable<ICapabilityProvider> cir) {
        if (stack.is(Items.BUCKET)) cir.setReturnValue(CustomFluidCapabilities.createProvider(stack, true));
    }
}
