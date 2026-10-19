package com.crabmod.hotbath.dirtiness;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.HotBathConfig;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.*;

/** Optional, server-owned dirtiness ranges. Empty by default. */
public final class DirtinessEffectRules extends SimpleJsonResourceReloadListener {
    private static volatile List<Rule> rules = List.of();
    private record Effect(ResourceLocation id, int amplifier, int duration, int refreshInterval,
                          boolean particles, boolean icon) {}
    private record Rule(float min, float max, List<Effect> effects) {
        boolean includes(float dirt) { return dirt >= min && (dirt < max || max == 1 && dirt == 1); }
    }

    public DirtinessEffectRules() { super(new Gson(), "hotbath/dirtiness_effects"); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        var next = new ArrayList<Rule>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(file -> {
            try { next.addAll(parse(file.getValue())); }
            catch (RuntimeException error) {
                HotBath.LOGGER.warn("Ignoring invalid dirtiness effects {}: {}", file.getKey(), error.getMessage());
            }
        });
        rules = List.copyOf(next);
    }

    private static List<Rule> parse(JsonElement json) {
        var parsed = new ArrayList<Rule>();
        for (var element : json.getAsJsonObject().getAsJsonArray("rules")) {
            var rule = element.getAsJsonObject();
            float min = rule.get("min").getAsFloat(), max = rule.get("max").getAsFloat();
            if (!Float.isFinite(min) || !Float.isFinite(max) || min < 0 || max > 1 || min >= max)
                throw new JsonParseException("Each range requires 0 <= min < max <= 1");
            var effects = new ArrayList<Effect>();
            for (var entry : rule.getAsJsonArray("effects")) {
                var value = entry.getAsJsonObject();var id = ResourceLocation.tryParse(value.get("effect").getAsString());
                if (id == null || !BuiltInRegistries.MOB_EFFECT.containsKey(id)) throw new JsonParseException("Unknown effect: " + id);
                int duration = number(value, "duration", 60, 20, 72000);
                effects.add(new Effect(id, number(value, "amplifier", 0, 0, 255), duration,
                        number(value, "refresh_interval", 40, 20, duration),
                        flag(value, "show_particles", false), flag(value, "show_icon", true)));
            }
            parsed.add(new Rule(min, max, List.copyOf(effects)));
        }
        return parsed;
    }

    private static boolean flag(JsonObject object, String name, boolean fallback) {
        if (!object.has(name)) return fallback;
        var value = object.get(name);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
            throw new JsonParseException(name + " must be true or false");
        return value.getAsBoolean();
    }

    private static int number(JsonObject object, String name, int fallback, int min, int max) {
        if (!object.has(name)) return Math.min(fallback, max);
        int value;
        try { value = object.get(name).getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException | NumberFormatException error) { throw new JsonParseException(name + " must be an integer"); }
        if (value < min || value > max) throw new JsonParseException(name + " must be between " + min + " and " + max);
        return value;
    }

    public static void tick(ServerPlayer player, float dirtiness) {
        if (!HotBathConfig.isDirtinessEnabled() || player.tickCount % 20 != 0 || !player.isAlive() || rules.isEmpty()) return;
        var selected = new LinkedHashMap<ResourceLocation, Effect>();
        for (var rule : rules) if (rule.includes(dirtiness)) for (var effect : rule.effects())
            selected.merge(effect.id(), effect, (a,b) -> b.amplifier() > a.amplifier()
                    || b.amplifier() == a.amplifier() && b.duration() > a.duration() ? b : a);
        for (var effect : selected.values()) {
            var type = BuiltInRegistries.MOB_EFFECT.getHolder(effect.id()).orElseThrow();
            var existing = player.getEffect(type);
            if (existing == null || existing.getAmplifier() < effect.amplifier()
                    || existing.getAmplifier() == effect.amplifier() && existing.getDuration() <= effect.duration() - effect.refreshInterval())
                player.addEffect(new MobEffectInstance(type, effect.duration(), effect.amplifier(), false, effect.particles(), effect.icon()));
        }
    }
}
