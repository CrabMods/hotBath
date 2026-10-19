package com.crabmod.hotbath;

import net.minecraftforge.common.ForgeConfigSpec;

/** Local display preferences. These never change the player's server-side dirtiness. */
public final class HotBathClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue SHOW_DIRT = BUILDER
            .comment("Show Hot Bath dirt on player models.")
            .define("showDirt", true);
    public static final ForgeConfigSpec.BooleanValue SHOW_FLIES = BUILDER
            .comment("Show Hot Bath fly particles around dirty players.")
            .define("showFlies", true);
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private HotBathClientConfig() {}
}
