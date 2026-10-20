package com.crabmod.hotbath.fluid_blocks;

import net.minecraft.server.level.ServerPlayer;
import com.crabmod.hotbath.bathing.BathImmersion;
import java.util.UUID;

/** Legacy area-tracker adapter. All timing is implemented by the shared contact timeline. */
public interface IInsideAreaTracker {
    record InsideAreaResult(boolean isFirstEnter,boolean shouldProcess,int stayedTicks,int totalEnterCount) {}
    default int reenterThresholdTicks() {return 10;}
    default InsideAreaResult trackInside(ServerPlayer player) {return trackContact(player);}
    default InsideAreaResult trackInside(net.minecraft.world.entity.LivingEntity entity) {return trackContact(entity);}
    private InsideAreaResult trackContact(net.minecraft.world.entity.Entity entity) {
        var frame=BathImmersion.contact(entity,getAreaKey(),reenterThresholdTicks());
        return new InsideAreaResult(frame.isFirstEnter(),frame.shouldProcess(),frame.stayedTicks(),frame.totalEnterCount());
    }
    default String getAreaKey() {return getClass().getSimpleName();}
    static void cleanupPlayer(UUID id) {BathImmersion.forget(id);}
    default int getEnterCount(ServerPlayer player) {return BathImmersion.enterCount(player,getAreaKey());}
}
