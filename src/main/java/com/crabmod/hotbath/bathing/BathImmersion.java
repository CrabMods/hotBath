package com.crabmod.hotbath.bathing;

import net.minecraft.world.entity.Entity;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server-thread contact state. Stored values retain neither entities nor level instances. */
public final class BathImmersion {
    private static final Map<Entity,State> CONTACTS=new WeakHashMap<>();
    private static final Map<Entity,State> CLIENT_CONTACTS=new WeakHashMap<>();
    private static final class State {
        final Object dimension;
        final ImmersionTimeline timeline=new ImmersionTimeline();
        long hazardTick=Long.MIN_VALUE;
        int hazards;
        Report immersion;
        Report shower;
        State(Object dimension) {this.dimension=dimension;}
    }
    private record Report(com.crabmod.hotbath.api.BathContact contact,long tick,net.minecraft.world.phys.AABB bounds) {
        boolean active(Entity entity) {
            long age=entity.level().getGameTime()-tick;
            return age>=0&&age<=1&&bounds.intersects(entity.getBoundingBox());
        }
    }
    private BathImmersion() {}
    private static State state(Entity entity) {
        var contacts=entity.level().isClientSide()?CLIENT_CONTACTS:CONTACTS;
        var state=contacts.get(entity);var dimension=entity.level().dimension();
        if(state==null||!state.dimension.equals(dimension)) {
            state=new State(dimension);contacts.put(entity,state);
        }
        return state;
    }
    public static ImmersionTimeline.Contact contact(Entity entity,String liquid,int graceTicks) {
        return state(entity).timeline.contact(liquid,entity.level().getGameTime(),graceTicks);
    }
    public static boolean claimHazard(Entity entity,int channel) {
        var state=state(entity);long tick=entity.level().getGameTime();
        if(state.hazardTick!=tick) {state.hazardTick=tick;state.hazards=0;}
        if((state.hazards&channel)!=0)return false;
        state.hazards|=channel;return true;
    }
    public static int enterCount(Entity entity,String liquid) {
        var state=CONTACTS.get(entity);return state==null?0:state.timeline.enterCount(liquid);
    }
    public static void report(Entity entity,com.crabmod.hotbath.api.BathContact contact) {
        var state=state(entity);var report=new Report(contact,entity.level().getGameTime(),entity.getBoundingBox());
        if(contact.immersion())state.immersion=report;else state.shower=report;
    }
    public static java.util.Optional<com.crabmod.hotbath.api.BathContact> reported(Entity entity) {
        var contacts=entity.level().isClientSide()?CLIENT_CONTACTS:CONTACTS;var state=contacts.get(entity);
        if(state==null||!state.dimension.equals(entity.level().dimension()))return java.util.Optional.empty();
        var immersion=state.immersion!=null&&state.immersion.active(entity)?state.immersion.contact():null;
        var shower=state.shower!=null&&state.shower.active(entity)?state.shower.contact():null;
        if(immersion==null)return java.util.Optional.ofNullable(shower);
        if(shower==null)return java.util.Optional.of(immersion);
        // Active sources contribute cleaning together; thermal consumers use the hotter supplied source.
        // Any source opting out keeps optional-mod side effects disabled for this combined contact.
        return java.util.Optional.of(new com.crabmod.hotbath.api.BathContact(immersion.bath()||shower.bath(),immersion.hot()||shower.hot(),
                immersion.cleansing()||shower.cleansing(),Math.max(immersion.temperatureCelsius(),shower.temperatureCelsius()),
                immersion.fluidId(),immersion.surfaceY(),true,immersion.integrationsEnabled()&&shower.integrationsEnabled()));
    }
    public static java.util.Optional<com.crabmod.hotbath.api.BathContact> immersion(Entity entity) {
        var contacts=entity.level().isClientSide()?CLIENT_CONTACTS:CONTACTS;var state=contacts.get(entity);
        if(state==null||!state.dimension.equals(entity.level().dimension())||state.immersion==null||!state.immersion.active(entity))
            return java.util.Optional.empty();
        return java.util.Optional.of(state.immersion.contact());
    }
    public static void endImmersion(Entity entity) {
        var contacts=entity.level().isClientSide()?CLIENT_CONTACTS:CONTACTS;var state=contacts.get(entity);
        if(state!=null)state.immersion=null;
    }
    public static void endShower(UUID id) {
        for(var entry:CONTACTS.entrySet())if(entry.getKey().getUUID().equals(id))entry.getValue().shower=null;
    }
    public static void forget(Entity entity) {
        (entity.level().isClientSide()?CLIENT_CONTACTS:CONTACTS).remove(entity);
    }
    public static void forget(UUID id) {CONTACTS.keySet().removeIf(entity->entity.getUUID().equals(id));}
}
