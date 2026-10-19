package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Manages loading and access to custom fluid definitions from data packs.
 * Loads JSON files from data/&lt;namespace&gt;/hotbath/custom_fluids/
 * 
 * <p>This class implements the SimpleJsonResourceReloadListener to automatically
 * reload fluid definitions when data packs are reloaded.</p>
 * 
 * <p>Usage example:
 * <pre>
 * // Get a specific fluid definition
 * Optional&lt;CustomFluidDefinition&gt; fluid = CustomFluidManager.getInstance()
 *     .getFluidDefinition(new ResourceLocation("mymod", "golden_bath"));
 * 
 * // Get all loaded fluids
 * Collection&lt;CustomFluidDefinition&gt; allFluids = CustomFluidManager.getInstance().getAllFluidDefinitions();
 * </pre>
 */
public class CustomFluidManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "hotbath/custom_fluids";
    
    private static volatile CustomFluidManager INSTANCE;
    
    private Map<ResourceLocation, CustomFluidDefinition> fluidDefinitions = ImmutableMap.of();

    public CustomFluidManager() {
        super(GSON, DIRECTORY);
    }

    /**
     * Gets the singleton instance of the CustomFluidManager.
     * Note: The instance is set when the reload listener is registered.
     */
    public static CustomFluidManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CustomFluidManager();
        }
        return INSTANCE;
    }

    /**
     * Sets the singleton instance. Called during registration.
     */
    public static void setInstance(CustomFluidManager manager) {
        INSTANCE = manager;
    }

    @Override
    protected void apply(
            @NotNull Map<ResourceLocation, JsonElement> resources,
            @NotNull ResourceManager resourceManager,
            @NotNull ProfilerFiller profiler) {
        
        
        ImmutableMap.Builder<ResourceLocation, CustomFluidDefinition> builder = ImmutableMap.builder();
        
        Map<ResourceLocation, BuiltinBathDefinition> builtins = new java.util.HashMap<>();
        java.util.List<String> builtinJsons = new java.util.ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            JsonElement json = entry.getValue();
            if (BuiltinBathDefinitions.isBuiltin(location)) {
                BuiltinBathDefinition definition;
                try { definition = BuiltinBathDefinition.parse(json); }
                catch (RuntimeException error) {
                    throw new IllegalArgumentException("Invalid built-in bath " + location + ": " + error.getMessage(), error);
                }
                if (!location.equals(definition.id()))
                    throw new IllegalArgumentException("Built-in bath id must match its resource path: " + location);
                builtins.put(location, definition);
                builtinJsons.add(json.toString());
                continue;
            }
            if (json.isJsonObject() && json.getAsJsonObject().has("id") &&
                    BuiltinBathDefinitions.isBuiltin(new ResourceLocation(json.getAsJsonObject().get("id").getAsString())))
                throw new IllegalArgumentException("Override a built-in bath at its own resource path: " + location);

            
            try {
                CustomFluidDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error -> 
                                HotBath.LOGGER.warn("Failed to parse custom fluid {}: {}", location, error))
                        .ifPresent(definition -> {
                            builder.put(definition.id(), definition);
                            HotBath.LOGGER.debug("Loaded custom fluid: {}", definition.id());
                        });
            } catch (Exception e) {
                HotBath.LOGGER.error("Exception while parsing custom fluid {}: {}", location, e.getMessage());
            }
        }
        
        var nextCustom = builder.build();
        var nextBuiltin = BuiltinBathDefinitions.catalog(builtins, builtinJsons);
        CustomFluidTranslationManager.clearAll();
        nextCustom.values().forEach(CustomFluidDefinition::registerTranslations);
        this.fluidDefinitions = nextCustom;
        BuiltinBathDefinitions.replace(nextBuiltin);
        HotBath.LOGGER.info("Loaded {} custom fluid definitions with {} translations", 
                fluidDefinitions.size(), CustomFluidTranslationManager.getTranslationCount());
        
        // Notify the registry to rebuild dynamic fluids
        CustomFluidRegistry.onFluidsReloaded(fluidDefinitions.values());
        setInstance(this);
    }

    /**
     * Gets a fluid definition by its ID.
     * 
     * @param id The resource location ID of the fluid
     * @return Optional containing the fluid definition if found
     */
    public Optional<CustomFluidDefinition> getFluidDefinition(ResourceLocation id) {
        return Optional.ofNullable(fluidDefinitions.get(id));
    }

    /**
     * Gets all loaded fluid definitions.
     * 
     * @return Collection of all fluid definitions
     */
    public Collection<CustomFluidDefinition> getAllFluidDefinitions() {
        return fluidDefinitions.values();
    }

    /**
     * Checks if a fluid definition exists.
     * 
     * @param id The resource location ID to check
     * @return true if the fluid exists
     */
    public boolean hasFluidDefinition(ResourceLocation id) {
        return fluidDefinitions.containsKey(id);
    }

    /**
     * Gets the total number of loaded fluid definitions.
     * 
     * @return The count of loaded fluids
     */
    public int getFluidCount() {
        return fluidDefinitions.size();
    }
}
