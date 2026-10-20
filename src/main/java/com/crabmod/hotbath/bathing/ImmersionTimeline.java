package com.crabmod.hotbath.bathing;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Per-entity contact history. Identity is the actual liquid, never its container or block class. */
public final class ImmersionTimeline {
    public record Contact(boolean isFirstEnter, boolean shouldProcess, int stayedTicks, int totalEnterCount) {}
    private final Map<String,Integer> entries=new HashMap<>();
    private String liquid;
    private long lastTick=Long.MIN_VALUE;
    private int stayedTicks;

    public Contact contact(String identity,long tick,int exitGraceTicks) {
        Objects.requireNonNull(identity);
        if(liquid!=null&&lastTick==tick)return new Contact(false,false,stayedTicks,entries.getOrDefault(identity,0));
        boolean entered=!identity.equals(liquid)||tick<lastTick||tick-lastTick>exitGraceTicks;
        stayedTicks=entered?0:stayedTicks+1;
        if(entered)entries.merge(identity,1,Integer::sum);
        liquid=identity;lastTick=tick;
        return new Contact(entered,true,stayedTicks,entries.get(identity));
    }
    public int enterCount(String identity) {return entries.getOrDefault(identity,0);}
}
