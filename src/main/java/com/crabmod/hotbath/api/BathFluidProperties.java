package com.crabmod.hotbath.api;

/** Snapshot of liquid data; contains no world, entity, geometry or container reference. */
public record BathFluidProperties(float temperatureCelsius,int density,int viscosity,int luminosity,
                                  boolean hot,boolean cleansing,boolean steam,boolean bubbles,boolean splash) {}
