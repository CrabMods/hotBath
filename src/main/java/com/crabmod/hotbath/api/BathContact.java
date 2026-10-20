package com.crabmod.hotbath.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** A caller-reported contact, with no container reference or geometry discovery. */
public record BathContact(boolean bath,boolean hot,boolean cleansing,float temperatureCelsius,
                          @Nullable ResourceLocation fluidId,double surfaceY,boolean immersion,boolean integrationsEnabled) {}
