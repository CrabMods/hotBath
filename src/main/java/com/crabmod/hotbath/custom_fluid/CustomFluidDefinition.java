package com.crabmod.hotbath.custom_fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-driven custom fluid definition that can be loaded from data packs.
 * Users can create JSON files in data/&lt;namespace&gt;/hotbath/custom_fluids/ to define custom bath liquids.
 * 
 * <p>Example JSON:
 * <pre>
 * {
 *   "id": "example:golden_bath",
 *   "color": 16766720,
 *   "temperature": 40.0,
 *   "viscosity": 1000,
 *   "density": 1000,
 *   "luminosity": 4,
 *   "show_particles": true,
 *   "show_bubbles": true,
 *   "show_steam": true,
 *   "effects": [
 *     {
 *       "effect": "minecraft:regeneration",
 *       "duration": 200,
 *       "amplifier": 1,
 *       "ambient": true,
 *       "show_particles": false,
 *       "show_icon": true
 *     }
 *   ],
 *   "trigger_time_seconds": 15,
 *   "texture_still": "hotbath:block/still_custom_fluid_grayscale",
 *   "texture_flowing": "hotbath:block/flowing_custom_fluid_grayscale"
 * }
 * </pre>
 */
public record CustomFluidDefinition(
        ResourceLocation id,
        int color,
        float temperature,
        int viscosity,
        int density,
        int luminosity,
        float opacity,
        int nutrition,
        int thirst,
        boolean showParticles,
        boolean showBubbles,
        boolean showSteam,
        List<EffectEntry> effects,
        int triggerTimeSeconds,
        ResourceLocation textureStill,
        ResourceLocation textureFlowing,
        String nameKey,
        Map<String, String> translations
) {
    public static final float HOT_TEMPERATURE_THRESHOLD=35.0f;

    
    /** Default saturation modifier for nutrition calculation (same as normal food) */
    private static final float DEFAULT_SATURATION_MODIFIER = 0.6f;
    
    /**
     * Calculates the saturation value based on nutrition.
     * Formula: nutrition * saturationModifier * 2 (same as Minecraft food)
     * @return the saturation value to restore
     */
    public float getSaturation() {
        return nutrition * DEFAULT_SATURATION_MODIFIER * 2.0f;
    }
    
    // Helper record for first group of fields (up to 16)
    private record BaseFields(
            ResourceLocation id, int color, float temperature, int viscosity, int density,
            int luminosity, float opacity, int nutrition, int thirst,
            boolean showParticles, boolean showBubbles, boolean showSteam,
            List<EffectEntry> effects, int triggerTimeSeconds,
            ResourceLocation textureStill, ResourceLocation textureFlowing
    ) {}
    
    // Helper record for extra fields
    private record ExtraFields(String nameKey, Map<String, String> translations) {}
    
    private static final Codec<BaseFields> BASE_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("id").forGetter(BaseFields::id),
                    Codec.INT.optionalFieldOf("color", 0x45E1E9).forGetter(BaseFields::color),
                    Codec.FLOAT.optionalFieldOf("temperature", 40.0f).forGetter(BaseFields::temperature),
                    Codec.INT.optionalFieldOf("viscosity", 1000).forGetter(BaseFields::viscosity),
                    Codec.INT.optionalFieldOf("density", 1000).forGetter(BaseFields::density),
                    Codec.INT.optionalFieldOf("luminosity", 2).forGetter(BaseFields::luminosity),
                    Codec.FLOAT.optionalFieldOf("opacity", 1.0f).forGetter(BaseFields::opacity),
                    Codec.INT.optionalFieldOf("nutrition", 0).forGetter(BaseFields::nutrition),
                    Codec.INT.optionalFieldOf("thirst", 4).forGetter(BaseFields::thirst),
                    Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(BaseFields::showParticles),
                    Codec.BOOL.optionalFieldOf("show_bubbles", true).forGetter(BaseFields::showBubbles),
                    Codec.BOOL.optionalFieldOf("show_steam", true).forGetter(BaseFields::showSteam),
                    EffectEntry.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(BaseFields::effects),
                    Codec.INT.optionalFieldOf("trigger_time_seconds", 15).forGetter(BaseFields::triggerTimeSeconds),
                    ResourceLocation.CODEC.optionalFieldOf("texture_still", 
                            new ResourceLocation("hotbath", "block/still_custom_fluid_grayscale"))
                            .forGetter(BaseFields::textureStill),
                    ResourceLocation.CODEC.optionalFieldOf("texture_flowing",
                            new ResourceLocation("hotbath", "block/flowing_custom_fluid_grayscale"))
                            .forGetter(BaseFields::textureFlowing)
            ).apply(instance, BaseFields::new)
    );
    
    private static final Codec<ExtraFields> EXTRA_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.STRING.optionalFieldOf("name_key", "").forGetter(ExtraFields::nameKey),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("translations", Map.of())
                            .forGetter(ExtraFields::translations)
            ).apply(instance, ExtraFields::new)
    );
    
    public static final Codec<CustomFluidDefinition> CODEC = Codec.pair(
            BASE_CODEC, EXTRA_CODEC
    ).xmap(
            pair -> new CustomFluidDefinition(
                    pair.getFirst().id, pair.getFirst().color, pair.getFirst().temperature,
                    pair.getFirst().viscosity, pair.getFirst().density, pair.getFirst().luminosity,
                    pair.getFirst().opacity, pair.getFirst().nutrition, pair.getFirst().thirst,
                    pair.getFirst().showParticles, pair.getFirst().showBubbles, pair.getFirst().showSteam,
                    pair.getFirst().effects, pair.getFirst().triggerTimeSeconds,
                    pair.getFirst().textureStill, pair.getFirst().textureFlowing,
                    pair.getSecond().nameKey, pair.getSecond().translations
            ),
            def -> com.mojang.datafixers.util.Pair.of(
                    new BaseFields(def.id, def.color, def.temperature, def.viscosity, def.density,
                            def.luminosity, def.opacity, def.nutrition, def.thirst,
                            def.showParticles, def.showBubbles, def.showSteam,
                            def.effects, def.triggerTimeSeconds, def.textureStill, def.textureFlowing),
                    new ExtraFields(def.nameKey, def.translations)
            )
    );

    /**
     * Gets the translation key for this fluid's name.
     * If a custom name_key is provided in the data pack, it is used directly.
     * Otherwise, generates a key based on the fluid ID: "fluid.{namespace}.{path}"
     * 
     * @return The translation key for this fluid
     */
    public String getTranslationKey() {
        if (nameKey != null && !nameKey.isEmpty()) {
            return nameKey;
        }
        return "fluid." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    /**
     * Gets the translated name of this fluid using the translation manager.
     * Falls back to the translation key if no translation is found.
     */
    public String getTranslatedName() {
        return CustomFluidTranslationManager.getTranslation(getTranslationKey());
    }

    /**
     * Registers all translations from this definition to the translation manager.
     */
    public void registerTranslations() {
        if (!translations.isEmpty()) {
            CustomFluidTranslationManager.registerTranslations(getTranslationKey(), translations);
        }
    }

    /**
     * Creates MobEffectInstance list from the effect entries.
     */
    public List<MobEffectInstance> createEffectInstances() {
        return effects.stream()
                .map(EffectEntry::toMobEffectInstance)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Whether this fluid should be considered "hot" (temperature >= 35°C).
     * Hot fluids cause damage to ice mobs and GummyBear from Alex's Caves.
     */
    public boolean isHot() {
        return temperature >= HOT_TEMPERATURE_THRESHOLD;
    }

    /**
     * Represents a single effect entry in the fluid definition.
     */
    public record EffectEntry(
            ResourceLocation effect,
            int duration,
            int amplifier,
            boolean ambient,
            boolean showParticles,
            boolean showIcon
    ) {
        public static final Codec<EffectEntry> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("effect").forGetter(EffectEntry::effect),
                        Codec.INT.optionalFieldOf("duration", 200).forGetter(EffectEntry::duration),
                        Codec.INT.optionalFieldOf("amplifier", 0).forGetter(EffectEntry::amplifier),
                        Codec.BOOL.optionalFieldOf("ambient", false).forGetter(EffectEntry::ambient),
                        Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(EffectEntry::showParticles),
                        Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(EffectEntry::showIcon)
                ).apply(instance, EffectEntry::new)
        );

        /**
         * Converts this entry to a MobEffectInstance.
         * Returns null if the effect is not found in the registry.
         */
        public MobEffectInstance toMobEffectInstance() {
            MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(effect);
            if (mobEffect == null) {
                return null;
            }
            return new MobEffectInstance(mobEffect, duration, amplifier, ambient, showParticles, showIcon);
        }
    }

    /**
     * Builder for programmatic creation of CustomFluidDefinition
     */
    public static class Builder {
        private ResourceLocation id;
        private int color = 0x45E1E9;
        private float temperature = 40.0f;
        private int viscosity = 1000;
        private int density = 1000;
        private int luminosity = 2;
        private float opacity = 1.0f;
        private int nutrition = 0;
        private int thirst = 0;
        private boolean showParticles = true;
        private boolean showBubbles = true;
        private boolean showSteam = true;
        private List<EffectEntry> effects = List.of();
        private int triggerTimeSeconds = 15;
        private ResourceLocation textureStill = new ResourceLocation("hotbath", "block/still_custom_fluid_grayscale");
        private ResourceLocation textureFlowing = new ResourceLocation("hotbath", "block/flowing_custom_fluid_grayscale");
        private String nameKey = "";
        private Map<String, String> translations = new HashMap<>();

        public Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder viscosity(int viscosity) {
            this.viscosity = viscosity;
            return this;
        }

        public Builder density(int density) {
            this.density = density;
            return this;
        }

        public Builder luminosity(int luminosity) {
            this.luminosity = luminosity;
            return this;
        }
        
        public Builder opacity(float opacity) {
            this.opacity = opacity;
            return this;
        }
        
        public Builder nutrition(int nutrition) {
            this.nutrition = nutrition;
            return this;
        }
        
        public Builder thirst(int thirst) {
            this.thirst = thirst;
            return this;
        }

        public Builder showParticles(boolean showParticles) {
            this.showParticles = showParticles;
            return this;
        }

        public Builder showBubbles(boolean showBubbles) {
            this.showBubbles = showBubbles;
            return this;
        }

        public Builder showSteam(boolean showSteam) {
            this.showSteam = showSteam;
            return this;
        }

        public Builder effects(List<EffectEntry> effects) {
            this.effects = effects;
            return this;
        }
        
        public Builder nameKey(String nameKey) {
            this.nameKey = nameKey;
            return this;
        }
        
        public Builder translation(String languageCode, String translatedName) {
            this.translations.put(languageCode, translatedName);
            return this;
        }
        
        public Builder translations(Map<String, String> translations) {
            this.translations.putAll(translations);
            return this;
        }

        public Builder triggerTimeSeconds(int seconds) {
            this.triggerTimeSeconds = seconds;
            return this;
        }

        public Builder textureStill(ResourceLocation texture) {
            this.textureStill = texture;
            return this;
        }

        public Builder textureFlowing(ResourceLocation texture) {
            this.textureFlowing = texture;
            return this;
        }

        public CustomFluidDefinition build() {
            return new CustomFluidDefinition(
                    id, color, temperature, viscosity, density, luminosity,
                    opacity, nutrition, thirst,
                    showParticles, showBubbles, showSteam,
                    effects, triggerTimeSeconds,
                    textureStill, textureFlowing, nameKey, translations
            );
        }
    }
}
