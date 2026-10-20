package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import com.crabmod.hotbath.registers.EntityRegister;

/**
 * Client-side event handlers for custom fluid rendering.
 * Handles item color tinting and entity rendering registration.
 */
@EventBusSubscriber(modid = HotBath.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CustomFluidClientEvents {
    public static void refreshLoadedFluidAppearance() {
        var minecraft=net.minecraft.client.Minecraft.getInstance();var level=minecraft.level;
        if(level==null||minecraft.player==null)return;
        var center=minecraft.player.chunkPosition();int radius=minecraft.options.renderDistance().get();
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++) {
            var chunk=level.getChunkSource().getChunkNow(center.x+x,center.z+z);
            if(chunk==null)continue;
            for(var entry:chunk.getBlockEntities().entrySet()) {
                if(entry.getValue() instanceof CustomFluidBlockEntity||entry.getValue() instanceof com.crabmod.hotbath.api.BathFluidContainer) {
                    var pos=entry.getKey();level.getLightEngine().checkBlock(pos);
                    var state=level.getBlockState(pos);level.sendBlockUpdated(pos,state,state,3);
                }
            }
        }

    }




    /**
     * Registers item color handlers for custom fluid items.
     * This allows the items to display with the color of the fluid they contain.
     * Also handles smoke overlay visibility - only shown for hot fluids.
     *
     * Layer structure (matching vanilla potion patterns):
     * - Bucket: layer0=bucket, layer1=fluid overlay (tinted), layer2=smoke
     * - Bottle: layer0=potion overlay (tinted), layer1=potion bottle, layer2=smoke
     * - Splash: layer0=splash overlay (tinted), layer1=splash bottle, layer2=smoke
     */
    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // Custom fluid bucket - tint layer 1 (the fluid inside), layer 2 (smoke overlay)
        event.register((stack, tintIndex) -> {
            if (tintIndex == 1) {
                // Fluid layer - apply fluid color
                int color = CustomFluidDataComponents.getFluidColor(stack);
                if (color != -1) {
                    return color;
                }
                return 0xFF4FC3F7; // Default light blue
            } else if (tintIndex == 2) {
                // Smoke layer - only visible for hot fluids with showSteam enabled
                CustomFluidDefinition definition = CustomFluidDataComponents.getFluidDefinition(stack);
                if (definition != null && definition.isHot() && definition.showSteam()) {
                    return 0xFFFFFFFF; // Full white = visible
                }
                return 0x00FFFFFF; // Fully transparent = hidden
            }
            return 0xFFFFFFFF;
        }, CustomFluidItems.CUSTOM_FLUID_BUCKET.get());

        // Custom fluid bottle - tint layer 0 (the fluid overlay like vanilla potion), layer 2 (smoke overlay)
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                // Fluid overlay layer - apply fluid color (like vanilla potion)
                int color = CustomFluidDataComponents.getFluidColor(stack);
                if (color != -1) {
                    return color;
                }
                return 0xFF4FC3F7; // Default light blue
            } else if (tintIndex == 2) {
                // Smoke layer - only visible for hot fluids with showSteam enabled
                CustomFluidDefinition definition = CustomFluidDataComponents.getFluidDefinition(stack);
                if (definition != null && definition.isHot() && definition.showSteam()) {
                    return 0xFFFFFFFF; // Full white = visible
                }
                return 0x00FFFFFF; // Fully transparent = hidden
            }
            return 0xFFFFFFFF;
        }, CustomFluidItems.CUSTOM_FLUID_BOTTLE.get());

        // Splash custom fluid bottle - tint layer 0 (the fluid overlay like vanilla splash potion), layer 2 (smoke overlay)
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                // Fluid overlay layer - apply fluid color (like vanilla splash potion)
                int color = CustomFluidDataComponents.getFluidColor(stack);
                if (color != -1) {
                    return color;
                }
                return 0xFF4FC3F7; // Default light blue
            } else if (tintIndex == 2) {
                // Smoke layer - only visible for hot fluids with showSteam enabled
                CustomFluidDefinition definition = CustomFluidDataComponents.getFluidDefinition(stack);
                if (definition != null && definition.isHot() && definition.showSteam()) {
                    return 0xFFFFFFFF; // Full white = visible
                }
                return 0x00FFFFFF; // Fully transparent = hidden
            }
            return 0xFFFFFFFF;
        }, CustomFluidItems.CUSTOM_FLUID_SPLASH_BOTTLE.get());
    }

    /**
     * Registers entity renderers for custom fluid projectiles.
     */
    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegister.THROWN_CUSTOM_FLUID_BOTTLE.get(), ThrownItemRenderer::new);
    }
}
