package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Event handler for registering the CustomFluidManager as a reload listener.
 * This ensures custom fluid definitions are loaded from data packs.
 */
@EventBusSubscriber(modid = HotBath.MOD_ID)
public class CustomFluidReloadListener {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        CustomFluidManager manager = new CustomFluidManager();
        event.addListener(manager);
        HotBath.LOGGER.info("Registered CustomFluidManager reload listener");
    }
}
