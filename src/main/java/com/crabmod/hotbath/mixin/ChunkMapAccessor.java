package com.crabmod.hotbath.mixin;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Enumerates already loaded chunks without requesting or waiting for chunk loads. */
@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
    @Invoker("getChunks")
    Iterable<ChunkHolder> hotbath$loadedChunks();
}
