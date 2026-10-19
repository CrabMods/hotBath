package com.crabmod.hotbath.custom_fluid;

@net.neoforged.fml.common.EventBusSubscriber(modid="hotbath",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class BuiltinBathClientEvents {
    private BuiltinBathClientEvents() {}
    @net.neoforged.bus.api.SubscribeEvent
    public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        BuiltinBathDefinitions.resetClient();
    }
}
