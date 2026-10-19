package com.crabmod.hotbath.custom_fluid;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="hotbath",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class BuiltinBathClientEvents {
    private BuiltinBathClientEvents() {}
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        BuiltinBathDefinitions.resetClient();
    }
}
