package com.crabmod.hotbath.registers;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.items.BathBucketItem;
import com.crabmod.hotbath.items.BathWaterBottleItem;
import com.crabmod.hotbath.items.BathWaterEffects;
import com.crabmod.hotbath.items.DescriptiveItem;
import com.crabmod.hotbath.items.SplashBathWaterBottleItem;
import com.crabmod.hotbath.fluid_details.FluidsColor;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegister {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HotBath.MOD_ID);

    public static final RegistryObject<Item> BATH_HERB = ITEMS.register("bath_herb",
            () -> new com.crabmod.hotbath.items.BathHerbItem(new Item.Properties()));

    public static final RegistryObject<Item> HERBAL_BATH_BUCKET = ITEMS.register("herbal_bath_bucket",
            () -> new BathBucketItem(FluidsRegister.HERBAL_BATH_FLUID, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HONEY_BATH_BUCKET = ITEMS.register("honey_bath_bucket",
            () -> new BathBucketItem(FluidsRegister.HONEY_BATH_FLUID, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HOT_WATER_BUCKET = ITEMS.register("hot_water_bucket",
            () -> new BathBucketItem(FluidsRegister.HOT_WATER_FLUID, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MILK_BATH_BUCKET = ITEMS.register("milk_bath_bucket",
            () -> new BathBucketItem(FluidsRegister.MILK_BATH_FLUID, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PEONY_BATH_BUCKET = ITEMS.register("peony_bath_bucket",
            () -> new BathBucketItem(FluidsRegister.PEONY_BATH_FLUID, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ROSE_BATH_BUCKET = ITEMS.register("rose_bath_bucket",
            () -> new BathBucketItem(FluidsRegister.ROSE_BATH_FLUID, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DESCRIPTIVE_ITEM = ITEMS.register("descriptive_item",
            () -> new DescriptiveItem(new Item.Properties()));

    public static final RegistryObject<Item> HOT_WATER_BOTTLE = ITEMS.register("hot_water_bottle",
            () -> new BathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::hotWaterEffect));

    public static final RegistryObject<Item> HONEY_BATH_BOTTLE = ITEMS.register("honey_bath_bottle",
            () -> new BathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::honeyBathEffect));

    public static final RegistryObject<Item> MILK_BATH_BOTTLE = ITEMS.register("milk_bath_bottle",
            () -> new BathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::milkBathEffect));

    public static final RegistryObject<Item> HERBAL_BATH_BOTTLE = ITEMS.register("herbal_bath_bottle",
            () -> new BathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::herbalBathEffect));

    public static final RegistryObject<Item> PEONY_BATH_BOTTLE = ITEMS.register("peony_bath_bottle",
            () -> new BathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::peonyBathEffect));

    public static final RegistryObject<Item> ROSE_BATH_BOTTLE = ITEMS.register("rose_bath_bottle",
            () -> new BathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::roseBathEffect));

    public static final RegistryObject<Item> SPLASH_HOT_WATER_BOTTLE = ITEMS.register("splash_hot_water_bottle",
            () -> new SplashBathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::hotWaterSplashEffect,
                    ParticleRegister.HOT_WATER_SPLASH, ParticleRegister.HOT_WATER_BUBBLE, ParticleRegister.HOT_WATER_EFFECT, FluidsColor.HOT_WATER_COLOR));

    public static final RegistryObject<Item> SPLASH_HONEY_BATH_BOTTLE = ITEMS.register("splash_honey_bath_bottle",
            () -> new SplashBathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::honeyBathSplashEffect,
                    ParticleRegister.HONEY_WATER_SPLASH, ParticleRegister.HONEY_BATH_BUBBLE, ParticleRegister.HONEY_BATH_EFFECT, FluidsColor.HONEY_BATH_COLOR));

    public static final RegistryObject<Item> SPLASH_MILK_BATH_BOTTLE = ITEMS.register("splash_milk_bath_bottle",
            () -> new SplashBathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::milkBathSplashEffect,
                    ParticleRegister.MILK_WATER_SPLASH, ParticleRegister.MILK_BATH_BUBBLE, ParticleRegister.MILK_BATH_EFFECT, FluidsColor.MILK_BATH_COLOR));

    public static final RegistryObject<Item> SPLASH_HERBAL_BATH_BOTTLE = ITEMS.register("splash_herbal_bath_bottle",
            () -> new SplashBathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::herbalBathSplashEffect,
                    ParticleRegister.HERBAL_WATER_SPLASH, ParticleRegister.HERBAL_BATH_BUBBLE, ParticleRegister.HERBAL_BATH_EFFECT, FluidsColor.HERBAL_BATH_COLOR));

    public static final RegistryObject<Item> SPLASH_PEONY_BATH_BOTTLE = ITEMS.register("splash_peony_bath_bottle",
            () -> new SplashBathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::peonyBathSplashEffect,
                    ParticleRegister.PEONY_WATER_SPLASH, ParticleRegister.PEONY_BATH_BUBBLE, ParticleRegister.PEONY_BATH_EFFECT, FluidsColor.PEONY_BATH_COLOR));

    public static final RegistryObject<Item> SPLASH_ROSE_BATH_BOTTLE = ITEMS.register("splash_rose_bath_bottle",
            () -> new SplashBathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::roseBathSplashEffect,
                    ParticleRegister.ROSE_WATER_SPLASH, ParticleRegister.ROSE_BATH_BUBBLE, ParticleRegister.ROSE_BATH_EFFECT, FluidsColor.ROSE_BATH_COLOR));

    // Icon item for advancement display only (not obtainable in survival)
    public static final RegistryObject<Item> SOMETHING_SMELLS_ICON = ITEMS.register("something_smells_icon",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
