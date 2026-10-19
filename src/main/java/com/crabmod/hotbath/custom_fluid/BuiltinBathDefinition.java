package com.crabmod.hotbath.custom_fluid;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.*;

/** Reloadable behaviour of an already registered bath, bottle and splash bottle. */
public record BuiltinBathDefinition(ResourceLocation id, float temperature, boolean cleansDirtiness, float splashDirtinessReduction,
                                    Profile bath, Profile drink, Profile splash, Profile pipe) {
    public record Effect(ResourceLocation id, int duration, int amplifier, boolean ambient,
                         boolean particles, boolean icon, int afterTicks, int minimumEntries) {
        public MobEffectInstance create() {
            return new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.get(id), duration, amplifier, ambient, particles, icon);
        }
    }
    public record Regeneration(float amount, int intervalTicks) {}
    public record Cleansing(int afterTicks, int count, boolean nonBeneficial, boolean random,
                            List<ResourceLocation> exclude, List<ResourceLocation> include) {}
    public record Advancement(ResourceLocation id, int entries) {}
    public record PeonyBonuses(int afterTicks, double attackSpeed, double knockbackResistance,
                              int attackSpeedLingerTicks, int knockbackLingerTicks) {}
    public record UndeadDamage(float amount, int intervalTicks) {}
    public record Profile(List<Effect> effects, Regeneration healing, Regeneration hunger,
                          float instantHealing, Cleansing cleansing, Advancement advancement,
                          PeonyBonuses peony, UndeadDamage undeadDamage, boolean extinguish) {}

    public static BuiltinBathDefinition parse(JsonElement input) {
        JsonObject o=input.getAsJsonObject();
        ResourceLocation id=location(o,"id");
        BuiltinBathDefinition result = new BuiltinBathDefinition(id, (float)number(o,"temperature",-273.15,10000),
                bool(o,"cleans_dirtiness"), (float)number(o,"splash_dirtiness_reduction",0,1), profile(o.getAsJsonObject("bath")),
                profile(o.getAsJsonObject("drink")), profile(o.getAsJsonObject("splash")), profile(o.getAsJsonObject("pipe")));
        validateSingleUse(result.drink());validateSingleUse(result.splash());validateSingleUse(result.pipe());
        if(result.bath().peony()!=null&&!id.getPath().equals("peony_bath_fluid"))
            throw new IllegalArgumentException("Peony attribute bonuses belong to peony_bath_fluid");
        return result;
    }

    private static void validateSingleUse(Profile p) {
        if(p.healing()!=null||p.hunger()!=null||p.advancement()!=null||p.peony()!=null||p.undeadDamage()!=null)
            throw new IllegalArgumentException("Continuous bath mechanics cannot be used in a single-delivery profile");
        if(p.cleansing()!=null&&p.cleansing().afterTicks()!=0)
            throw new IllegalArgumentException("Single-delivery cleansing must have after_ticks=0");
        if(p.effects().stream().anyMatch(e->e.afterTicks()!=0||e.minimumEntries()!=0))
            throw new IllegalArgumentException("Single-delivery effects must have after_ticks=0 and minimum_entries=0");
    }

    public boolean isHot() { return temperature >= 35.0f; }

    private static Profile profile(JsonObject o) {
        Objects.requireNonNull(o,"Missing bath/drink/splash profile");
        List<Effect> effects=new ArrayList<>();
        for(JsonElement value:o.getAsJsonArray("effects")) {
            JsonObject e=value.getAsJsonObject();ResourceLocation id=location(e,"effect");
            requireEffect(id);
            effects.add(new Effect(id,integer(e,"duration",1,1200000),integer(e,"amplifier",0,255),
                    bool(e,"ambient"),bool(e,"show_particles"),bool(e,"show_icon"),
                    integer(e,"after_ticks",0,1200000),integer(e,"minimum_entries",0,Integer.MAX_VALUE)));
        }
        Regeneration healing=regeneration(o,"healing"),hunger=regeneration(o,"hunger");
        if(hunger!=null && hunger.amount()!=(int)hunger.amount())throw new IllegalArgumentException("Hunger amount must be an integer");
        Cleansing cleansing=null;
        if(o.has("cleansing")) {
            JsonObject c=o.getAsJsonObject("cleansing");String category=c.get("category").getAsString();
            if(!Set.of("harmful","non_beneficial").contains(category))throw new IllegalArgumentException("Unknown cleansing category: "+category);
            String selection=c.get("selection").getAsString();
            if(!Set.of("first","random").contains(selection))throw new IllegalArgumentException("Unknown cleansing selection: "+selection);
            cleansing=new Cleansing(integer(c,"after_ticks",0,1200000),integer(c,"count",1,Integer.MAX_VALUE),
                    category.equals("non_beneficial"),selection.equals("random"),effectIds(c,"exclude"),effectIds(c,"include"));
        }
        Advancement advancement=null;
        if(o.has("advancement")) {
            JsonObject a=o.getAsJsonObject("advancement");advancement=new Advancement(location(a,"id"),integer(a,"entries",1,Integer.MAX_VALUE));
        }
        PeonyBonuses peony=null;
        if(o.has("peony_bonuses")) {
            JsonObject a=o.getAsJsonObject("peony_bonuses");
            peony=new PeonyBonuses(integer(a,"after_ticks",0,1200000),number(a,"attack_speed",-0.99,100),
                    number(a,"knockback_resistance",0,1),integer(a,"attack_speed_linger_ticks",1,1200000),
                    integer(a,"knockback_linger_ticks",1,1200000));
        }
        UndeadDamage damage=null;
        if(o.has("undead_damage")) {
            JsonObject a=o.getAsJsonObject("undead_damage");damage=new UndeadDamage((float)number(a,"amount",0,1000000),integer(a,"interval_ticks",1,1200000));
        }
        float instant=o.has("instant_healing")?(float)number(o,"instant_healing",0,1000000):0;
        return new Profile(List.copyOf(effects),healing,hunger,instant,cleansing,advancement,peony,damage,o.has("extinguish")&&bool(o,"extinguish"));
    }

    private static Regeneration regeneration(JsonObject o,String key) {
        if(!o.has(key))return null;
        JsonObject r=o.getAsJsonObject(key);
        return new Regeneration((float)number(r,"amount",0,1000000),integer(r,"interval_ticks",1,1200000));
    }
    private static List<ResourceLocation> effectIds(JsonObject o,String key) {
        List<ResourceLocation> result=new ArrayList<>();
        for(JsonElement e:o.getAsJsonArray(key)) {
            ResourceLocation id=new ResourceLocation(e.getAsString());requireEffect(id);result.add(id);
        }
        return List.copyOf(result);
    }
    private static void requireEffect(ResourceLocation id) {
        if(!BuiltInRegistries.MOB_EFFECT.containsKey(id))throw new IllegalArgumentException("Unknown effect: "+id);
    }
    private static ResourceLocation location(JsonObject o,String key) {
        return new ResourceLocation(o.get(key).getAsString());
    }
    private static int integer(JsonObject o,String key,int min,int max) {
        double value=number(o,key,min,max);
        if(value!=Math.rint(value))throw new IllegalArgumentException(key+" must be an integer");
        return (int)value;
    }
    private static boolean bool(JsonObject o,String key) {
        JsonElement e=Objects.requireNonNull(o.get(key),"Missing "+key);
        if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException(key+" must be boolean");
        return e.getAsBoolean();
    }
    private static double number(JsonObject o,String key,double min,double max) {
        JsonElement element=Objects.requireNonNull(o.get(key),"Missing "+key);
        if(!element.isJsonPrimitive()||!element.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key+" must be numeric");
        double value=element.getAsDouble();
        if(!Double.isFinite(value)||value<min||value>max)throw new IllegalArgumentException(key+" outside ["+min+", "+max+"]");
        return value;
    }
}
