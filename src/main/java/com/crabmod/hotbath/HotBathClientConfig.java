package com.crabmod.hotbath;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Local display preferences. These never change the player's server-side dirtiness. */
public final class HotBathClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue SHOW_DIRT = BUILDER
            .comment("Show Hot Bath dirt on player models.")
            .define("showDirt", true);
    public static final ModConfigSpec.BooleanValue SHOW_FLIES = BUILDER
            .comment("Show Hot Bath fly particles around dirty players.")
            .define("showFlies", true);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private HotBathClientConfig() {}
}
