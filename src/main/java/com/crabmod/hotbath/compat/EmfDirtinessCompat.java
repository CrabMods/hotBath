package com.crabmod.hotbath.compat;

import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.fml.ModList;
import traben.entity_model_features.EMFAnimationApi;

/** Keeps an animated player's dirt pass on the pose used by the skin pass. */
public final class EmfDirtinessCompat {
    private static AbstractClientPlayer overlayPlayer;
    private static boolean registered;

    private EmfDirtinessCompat() {}

    public static void initialize() {
        if (!registered && ModList.get().isLoaded("entity_model_features")) {
            EmfApi.register();
            registered = true;
        }
    }

    public static AbstractClientPlayer beginOverlay(AbstractClientPlayer player) {
        AbstractClientPlayer previous = overlayPlayer;
        overlayPlayer = player;
        return previous;
    }

    public static void endOverlay(AbstractClientPlayer previous) {
        overlayPlayer = previous;
    }

    // Loaded only when EMF is present. No global pause/resume state is changed.
    private static final class EmfApi {
        private static void register() {
            try {
                EMFAnimationApi.registerPauseCondition(entity -> entity == (Object) overlayPlayer);
            } catch (Exception error) {
                throw new IllegalStateException("Could not register the EMF dirt overlay animation scope", error);
            }
        }
    }
}
