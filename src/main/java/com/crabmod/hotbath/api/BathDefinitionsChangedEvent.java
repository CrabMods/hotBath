package com.crabmod.hotbath.api;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import net.neoforged.bus.api.Event;

/** Emitted after successful definition replacement; consumers refresh their own loaded state. */
public final class BathDefinitionsChangedEvent extends Event {
    private final @Nullable MinecraftServer server;
    public BathDefinitionsChangedEvent(@Nullable MinecraftServer server) {this.server=server;}
    public @Nullable MinecraftServer server() {return server;}
    public boolean clientSide() {return server==null;}
}
