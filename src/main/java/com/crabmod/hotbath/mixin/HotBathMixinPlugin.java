package com.crabmod.hotbath.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that checks for compatibility mode before loading mixins.
 * 
 * Config Priority:
 * 1. compatibilityMode = true -> advanced mixins disabled; core capability and fishing rules remain active
 * 
 * This reads the config file directly since Forge config is not available at mixin load time.
 */
public class HotBathMixinPlugin implements IMixinConfigPlugin {
    
    private static boolean compatibilityMode = false;
    private static boolean configChecked = false;
    
    // List of LSO-related mixin class names (without package)
    private static final Set<String> LSO_MIXINS = Set.of(
        "ThirstConsumableListenerMixin"
    );
    
    // Cached mod detection results
    private static Boolean lsoLoaded = null;
    
    static {
        checkConfig();
    }
    
    /**
     * Check config settings by reading the config file directly.
     * This is necessary because Forge config is not available at mixin load time.
     */
    private static void checkConfig() {
        if (configChecked) {
            return;
        }
        configChecked = true;
        
        try {
            // Try to find the config file in common locations
            Path configDir = Path.of("config");
            Path configFile = configDir.resolve("hotbath-common.toml");
            
            if (Files.exists(configFile)) {
                String content = Files.readString(configFile);
                // Parse the TOML file for settings
                for (String line : content.split("\\n")) {
                    line = line.trim();
                    if (line.startsWith("compatibilityMode")) {
                        if (line.contains("true")) {
                            compatibilityMode = true;
                            System.out.println("[HotBath] Compatibility mode enabled - disabling all advanced mixins");
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Config not found or not readable, use defaults
            System.out.println("[HotBath] Could not read config file, using default settings");
        }
    }
    
    /**
     * Check if Legendary Survival Overhaul mod is loaded.
     * Uses FMLLoader since it's available at mixin load time.
     */
    private static boolean isLSOLoaded() {
        if (lsoLoaded == null) {
            try {
                lsoLoaded = FMLLoader.getLoadingModList().getModFileById("legendarysurvivaloverhaul") != null;
                if (lsoLoaded) {
                    System.out.println("[HotBath] LSO detected via FMLLoader - enabling LSO integration mixins");
                }
            } catch (Exception e) {
                lsoLoaded = false;
            }
        }
        return lsoLoaded;
    }
    
    /**
     * Check if compatibility mode is enabled.
     * @return true if compatibility mode is enabled
     */
    public static boolean isCompatibilityModeEnabled() {
        return compatibilityMode;
    }
    
    @Override
    public void onLoad(String mixinPackage) {
        // No initialization needed
    }
    
    @Override
    public String getRefMapperConfig() {
        return null;
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Extract simple class name from full mixin class name
        String simpleName = mixinClassName;
        int lastDot = mixinClassName.lastIndexOf('.');
        if (lastDot >= 0) {
            simpleName = mixinClassName.substring(lastDot + 1);
        }
        
        // Core bucket capabilities and fishing rules remain active independently of advanced integrations
        if ((simpleName.equals("FishingHookMixin") || simpleName.equals("EmptyBucketCapabilityMixin"))) {
            return true;
        }
        
        // If compatibility mode is enabled, disable everything else
        if (compatibilityMode) {
            return false;
        }
        
        // LSO mixins should only be applied if LSO is loaded
        if (LSO_MIXINS.contains(simpleName) && !isLSOLoaded()) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // No special target handling needed
    }
    
    @Override
    public List<String> getMixins() {
        return null;
    }
    
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No pre-apply handling needed
    }
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No post-apply handling needed
    }
}
