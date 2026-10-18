package com.crabmod.hotbath.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;

import net.neoforged.neoforge.common.NeoForge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Central manager for all mod compatibility integrations.
 * Provides safe initialization with automatic fallback on errors.
 * 
 * Features:
 * - Safe initialization with try-catch wrapper
 * - Automatic disabling of failed compat modules
 * - Player notification system (once per session)
 * - Version tracking for debugging
 * - GitHub issue reporting link
 */
public class CompatManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String GITHUB_ISSUES_URL = "https://github.com/CrabMods/hotBath/issues";
    
    // Track which compats have been disabled due to errors
    private static final Map<String, CompatError> DISABLED_COMPATS = new ConcurrentHashMap<>();
    
    // Track which players have been notified (UUID -> set of compat names)
    private static final Map<UUID, Set<String>> NOTIFIED_PLAYERS = new ConcurrentHashMap<>();
    
    // Track all registered compats and their status
    private static final Map<String, CompatInfo> REGISTERED_COMPATS = new ConcurrentHashMap<>();
    
    // Flag to track if we've logged the startup summary
    private static volatile boolean startupSummaryLogged = false;
    
    /**
     * Information about a registered compat module
     */
    public static class CompatInfo {
        public final String modId;
        public final String displayName;
        public final BooleanSupplier isLoaded;
        public final Runnable initializer;
        public final String[] requiredApiClasses;
        public volatile boolean initialized = false;
        public volatile boolean enabled = true;
        public volatile String modVersion = "unknown";
        
        public CompatInfo(String modId, String displayName, BooleanSupplier isLoaded, Runnable initializer, String... requiredApiClasses) {
            this.modId = modId;
            this.displayName = displayName;
            this.isLoaded = isLoaded;
            this.initializer = initializer;
            this.requiredApiClasses = requiredApiClasses;
        }
    }
    
    /**
     * Information about a compat error
     */
    public static class CompatError {
        public final String compatName;
        public final String modId;
        public final String modVersion;
        public final String errorMessage;
        public final String stackTrace;
        public final long timestamp;
        
        public CompatError(String compatName, String modId, String modVersion, Throwable error) {
            this.compatName = compatName;
            this.modId = modId;
            this.modVersion = modVersion;
            this.errorMessage = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
            this.stackTrace = getStackTraceString(error);
            this.timestamp = System.currentTimeMillis();
        }
        
        private static String getStackTraceString(Throwable error) {
            StringBuilder sb = new StringBuilder();
            sb.append(error.getClass().getName()).append(": ").append(error.getMessage()).append("\n");
            for (StackTraceElement element : error.getStackTrace()) {
                if (element.getClassName().startsWith("com.crabmod.hotbath")) {
                    sb.append("  at ").append(element).append("\n");
                }
            }
            if (error.getCause() != null) {
                sb.append("Caused by: ").append(error.getCause().getClass().getName())
                  .append(": ").append(error.getCause().getMessage());
            }
            return sb.toString();
        }
    }
    
    /**
     * Register a compat module for safe initialization.
     */
    public static void registerCompat(String modId, String displayName, BooleanSupplier isLoaded, Runnable initializer) {
        REGISTERED_COMPATS.put(modId, new CompatInfo(modId, displayName, isLoaded, initializer));
    }
    
    /**
     * Register a compat module with required API class verification.
     * The requiredApiClasses will be checked via Class.forName() BEFORE the initializer runs.
     * If any class is missing, the compat will be disabled with a detailed diagnostic.
     * 
     * @param modId The mod ID
     * @param displayName Display name for logging
     * @param isLoaded Check if the mod is loaded
     * @param initializer The initialization logic
     * @param requiredApiClasses Fully qualified class names to verify before init (e.g. "com.example.api.SomeClass")
     */
    public static void registerCompat(String modId, String displayName, BooleanSupplier isLoaded, Runnable initializer, String... requiredApiClasses) {
        REGISTERED_COMPATS.put(modId, new CompatInfo(modId, displayName, isLoaded, initializer, requiredApiClasses));
    }
    
    /**
     * Safely register event handler classes on the NeoForge EVENT_BUS for a compat module.
     * This method should be called from within a compat's init() method instead of
     * calling NeoForge.EVENT_BUS.register() directly.
     * 
     * It verifies each handler class can be loaded (catches NoClassDefFoundError from
     * missing mod API dependencies in static initializers or field declarations)
     * and provides detailed diagnostics on failure.
     * 
     * @param modId The mod ID this registration belongs to
     * @param handlerClasses The event handler classes to register
     */
    public static void registerEventHandlers(String modId, Class<?>... handlerClasses) {
        for (Class<?> handlerClass : handlerClasses) {
            try {
                // Trigger class loading to verify all dependencies are available
                // This catches NoClassDefFoundError if the handler references missing mod API classes
                Class.forName(handlerClass.getName(), true, handlerClass.getClassLoader());
                NeoForge.EVENT_BUS.register(handlerClass);
                LOGGER.debug("  Registered event handler: {}", handlerClass.getSimpleName());
            } catch (Throwable e) {
                LOGGER.error("Failed to register event handler {} for compat module '{}'", 
                    handlerClass.getSimpleName(), modId);
                LOGGER.error("  Error: {} - {}", e.getClass().getName(), e.getMessage());
                throw new RuntimeException("Event handler registration failed: " + handlerClass.getSimpleName(), e);
            }
        }
    }
    
    /**
     * Verify that required API classes from an external mod are available.
     * This should be called early (e.g., during init) to fail fast with a clear message
     * rather than crashing later at runtime with a confusing NoClassDefFoundError.
     * 
     * @param modId The mod ID whose API is being verified
     * @param classNames Fully qualified class names to check
     * @throws RuntimeException if any class is not found, with detailed diagnostic info
     */
    public static void verifyApiClasses(String modId, String... classNames) {
        List<String> missing = new ArrayList<>();
        for (String className : classNames) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                missing.add(className);
            }
        }
        if (!missing.isEmpty()) {
            String modVersion = getModVersion(modId);
            StringBuilder sb = new StringBuilder();
            sb.append("API verification failed for mod '").append(modId)
              .append("' (version: ").append(modVersion).append("). Missing classes:\n");
            for (String cls : missing) {
                sb.append("  - ").append(cls).append("\n");
            }
            sb.append("The installed version of this mod may have changed its API.");
            LOGGER.error(sb.toString());
            throw new RuntimeException(sb.toString());
        }
        LOGGER.debug("API verification passed for '{}': all {} classes found.", modId, classNames.length);
    }
    
    /**
     * Execute an event handler callback safely within a compat context.
     * Use this in @SubscribeEvent methods to wrap the entire method body.
     * If an error occurs, it will be caught, logged with full diagnostics,
     * and the compat module will be disabled. Subsequent calls will be no-ops.
     * 
     * Usage example:
     * <pre>
     * {@code @SubscribeEvent}
     * public static void onPlayerTick(PlayerTickEvent.Post event) {
     *     CompatManager.safeEventCall("alexsmobs", "onPlayerTick", () -> {
     *         // actual event handling logic here
     *     });
     * }
     * </pre>
     * 
     * @param modId The mod ID for the compat module
     * @param eventName Name of the event/method for logging
     * @param action The actual event handling logic
     */
    public static void safeEventCall(String modId, String eventName, Runnable action) {
        if (!isCompatEnabled(modId)) {
            return; // Already disabled, skip silently
        }
        try {
            action.run();
        } catch (Throwable e) {
            handleRuntimeError(modId, eventName, e);
        }
    }
    
    /**
     * Execute an event handler callback safely with a return value.
     * 
     * @param modId The mod ID for the compat module
     * @param eventName Name of the event/method for logging
     * @param action The actual event handling logic
     * @param defaultValue Value to return if compat is disabled or error occurs
     * @return The result of the action, or defaultValue on failure
     */
    public static <T> T safeEventCall(String modId, String eventName, Supplier<T> action, T defaultValue) {
        if (!isCompatEnabled(modId)) {
            return defaultValue;
        }
        try {
            return action.get();
        } catch (Throwable e) {
            handleRuntimeError(modId, eventName, e);
            return defaultValue;
        }
    }
    
    /**
     * Initialize all registered compat modules safely
     */
    public static void initializeAll() {
        LOGGER.info("=== Hot Bath Mod Compatibility System ===");
        LOGGER.info("Initializing mod integrations...");
        
        for (CompatInfo compat : REGISTERED_COMPATS.values()) {
            initializeCompat(compat);
        }
        
        logStartupSummary();
        
        // Update Patchouli flags after all compats are initialized
        PatchouliCompat.updateCompatFlags();
    }
    
    /**
     * Safely initialize a single compat module
     */
    private static void initializeCompat(CompatInfo compat) {
        if (!safeCheckLoaded(compat)) {
            LOGGER.debug("{} not detected, skipping integration.", compat.displayName);
            return;
        }
        
        // Get mod version
        compat.modVersion = getModVersion(compat.modId);
        
        LOGGER.info("{} detected (version: {}). Initializing integration...", 
            compat.displayName, compat.modVersion);
        
        try {
            // Verify required API classes before running the initializer
            if (compat.requiredApiClasses != null && compat.requiredApiClasses.length > 0) {
                LOGGER.debug("Verifying {} API classes for {}...", compat.requiredApiClasses.length, compat.displayName);
                verifyApiClasses(compat.modId, compat.requiredApiClasses);
            }
            
            compat.initializer.run();
            compat.initialized = true;
            LOGGER.info("{} integration initialized successfully.", compat.displayName);
        } catch (Throwable e) {
            handleCompatError(compat, e);
        }
    }
    
    /**
     * Handle a compat initialization error
     */
    private static void handleCompatError(CompatInfo compat, Throwable error) {
        compat.enabled = false;
        
        CompatError compatError = new CompatError(
            compat.displayName, 
            compat.modId, 
            compat.modVersion, 
            error
        );
        DISABLED_COMPATS.put(compat.modId, compatError);
        
        LOGGER.error("========================================");
        LOGGER.error("HOT BATH COMPAT ERROR: {} integration DISABLED", compat.displayName);
        LOGGER.error("Mod: {} (version: {})", compat.modId, compat.modVersion);
        LOGGER.error("Hot Bath version: {}", getHotBathVersion());
        LOGGER.error("Minecraft version: {}", getMinecraftVersion());
        LOGGER.error("Error type: {}", error.getClass().getName());
        LOGGER.error("Error message: {}", error.getMessage());
        
        // Classify the error for easier debugging
        if (error instanceof NoClassDefFoundError || error instanceof ClassNotFoundException) {
            LOGGER.error("DIAGNOSIS: A required class from {} was not found.", compat.displayName);
            LOGGER.error("  This usually means the mod's API has changed in this version.");
            LOGGER.error("  Missing class: {}", error.getMessage());
        } else if (error instanceof NoSuchMethodError || error instanceof NoSuchFieldError) {
            LOGGER.error("DIAGNOSIS: A method/field in {}'s API was removed or renamed.", compat.displayName);
            LOGGER.error("  The mod's API is incompatible with this version of Hot Bath.");
            LOGGER.error("  Changed API element: {}", error.getMessage());
        } else if (error instanceof AbstractMethodError) {
            LOGGER.error("DIAGNOSIS: {} added new abstract methods that Hot Bath hasn't implemented.", compat.displayName);
            LOGGER.error("  Method: {}", error.getMessage());
        } else {
            LOGGER.error("DIAGNOSIS: Unexpected error during initialization.");
        }
        
        LOGGER.error("Please report this issue at: {}", GITHUB_ISSUES_URL);
        LOGGER.error("Full stack trace:", error);
        if (error.getCause() != null) {
            LOGGER.error("Root cause: {} - {}", error.getCause().getClass().getName(), error.getCause().getMessage());
        }
        LOGGER.error("========================================");
    }
    
    /**
     * Check if a compat module is enabled (not disabled due to error)
     */
    public static boolean isCompatEnabled(String modId) {
        if (DISABLED_COMPATS.containsKey(modId)) {
            return false;
        }
        CompatInfo info = REGISTERED_COMPATS.get(modId);
        return info != null && info.enabled && info.initialized;
    }
    
    /**
     * Safely execute a compat operation with automatic fallback
     * @param modId The mod ID for the compat
     * @param operation The operation to execute
     * @param operationName Name of the operation for logging
     * @return true if operation succeeded, false if it failed or compat is disabled
     */
    public static boolean safeExecute(String modId, Runnable operation, String operationName) {
        if (!isCompatEnabled(modId)) {
            return false;
        }
        
        try {
            operation.run();
            return true;
        } catch (Throwable e) {
            handleRuntimeError(modId, operationName, e);
            return false;
        }
    }
    
    /**
     * Safely execute a compat operation that returns a value
     * @param modId The mod ID for the compat
     * @param operation The operation to execute
     * @param defaultValue Default value to return on failure
     * @param operationName Name of the operation for logging
     * @return The operation result or default value on failure
     */
    public static <T> T safeExecute(String modId, Supplier<T> operation, T defaultValue, String operationName) {
        if (!isCompatEnabled(modId)) {
            return defaultValue;
        }
        
        try {
            return operation.get();
        } catch (Throwable e) {
            handleRuntimeError(modId, operationName, e);
            return defaultValue;
        }
    }
    
    /**
     * Handle a runtime error in a compat module
     */
    private static void handleRuntimeError(String modId, String operationName, Throwable error) {
        CompatInfo compat = REGISTERED_COMPATS.get(modId);
        if (compat == null) {
            LOGGER.error("HOT BATH RUNTIME ERROR: Unknown compat module '{}' reported error in operation '{}'", modId, operationName);
            LOGGER.error("Error: {} - {}", error.getClass().getName(), error.getMessage());
            LOGGER.error("Stack trace:", error);
            return;
        }
        
        // Disable the compat
        compat.enabled = false;
        
        CompatError compatError = new CompatError(
            compat.displayName,
            modId,
            compat.modVersion,
            error
        );
        DISABLED_COMPATS.put(modId, compatError);
        
        LOGGER.error("========================================");
        LOGGER.error("HOT BATH RUNTIME ERROR: {} integration DISABLED", compat.displayName);
        LOGGER.error("Operation: {}", operationName);
        LOGGER.error("Mod: {} (version: {})", modId, compat.modVersion);
        LOGGER.error("Hot Bath version: {}", getHotBathVersion());
        LOGGER.error("Minecraft version: {}", getMinecraftVersion());
        LOGGER.error("Error type: {}", error.getClass().getName());
        LOGGER.error("Error message: {}", error.getMessage());
        
        // Classify the error for easier debugging
        if (error instanceof NoClassDefFoundError || error instanceof ClassNotFoundException) {
            LOGGER.error("DIAGNOSIS: A required class from {} was not found at runtime.", compat.displayName);
            LOGGER.error("  Missing class: {}", error.getMessage());
            LOGGER.error("  This may indicate the mod version has changed its internal class structure.");
        } else if (error instanceof NoSuchMethodError || error instanceof NoSuchFieldError) {
            LOGGER.error("DIAGNOSIS: An API method/field in {} was removed or its signature changed.", compat.displayName);
            LOGGER.error("  Changed API element: {}", error.getMessage());
        } else if (error instanceof AbstractMethodError) {
            LOGGER.error("DIAGNOSIS: {} requires new methods that Hot Bath hasn't implemented yet.", compat.displayName);
            LOGGER.error("  Method: {}", error.getMessage());
        } else if (error instanceof NullPointerException) {
            LOGGER.error("DIAGNOSIS: NullPointerException during {} operation.", operationName);
            LOGGER.error("  This may indicate the mod's data/state was not available when expected.");
        } else {
            LOGGER.error("DIAGNOSIS: Unexpected runtime error during {} operation.", operationName);
        }
        
        LOGGER.error("The integration has been disabled to prevent further crashes.");
        LOGGER.error("Please report this issue at: {}", GITHUB_ISSUES_URL);
        LOGGER.error("Full stack trace:", error);
        if (error.getCause() != null) {
            LOGGER.error("Root cause: {} - {}", error.getCause().getClass().getName(), error.getCause().getMessage());
        }
        LOGGER.error("========================================");
        
        // Update Patchouli flags to reflect the disabled compat
        PatchouliCompat.updateCompatFlags();
    }
    
    /**
     * Report a runtime error from an integration module.
     * Called by integration classes that handle their own try-catch for performance.
     * This is the preferred method for hot-path code.
     * 
     * @param modId The mod ID
     * @param operationName Name of the operation that failed
     * @param error The error that occurred
     */
    public static void reportRuntimeError(String modId, String operationName, Throwable error) {
        handleRuntimeError(modId, operationName, error);
    }
    
    /**
     * Notify a player about disabled compats (only once per session per compat)
     */
    public static void notifyPlayer(ServerPlayer player) {
        if (DISABLED_COMPATS.isEmpty()) return;
        
        UUID playerId = player.getUUID();
        Set<String> notifiedFor = NOTIFIED_PLAYERS.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        
        for (Map.Entry<String, CompatError> entry : DISABLED_COMPATS.entrySet()) {
            String modId = entry.getKey();
            if (notifiedFor.contains(modId)) continue;
            
            notifiedFor.add(modId);
            sendPlayerNotification(player, entry.getValue());
        }
    }
    
    /**
     * Send a notification to a player about a disabled compat
     */
    private static void sendPlayerNotification(ServerPlayer player, CompatError error) {
        // Header
        MutableComponent header = Component.literal("[Hot Bath] ")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        
        MutableComponent warning = Component.translatable("hotbath.compat.warning.title")
            .withStyle(ChatFormatting.YELLOW);
        
        player.sendSystemMessage(header.append(warning));
        
        // Error info
        MutableComponent errorInfo = Component.literal("  ")
            .append(Component.literal(error.compatName)
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" ")
                .withStyle(ChatFormatting.GRAY))
            .append(Component.translatable("hotbath.compat.warning.disabled")
                .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(errorInfo);
        
        // Version info
        MutableComponent versionInfo = Component.translatable("hotbath.compat.warning.version", error.modVersion)
            .withStyle(ChatFormatting.GRAY);
        player.sendSystemMessage(versionInfo);
        
        // Error type and message for debugging
        MutableComponent errorDetail = Component.literal("  ")
            .append(Component.literal("Error: ")
                .withStyle(ChatFormatting.RED))
            .append(Component.literal(error.errorMessage)
                .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(errorDetail);
        
        // Possible cause
        MutableComponent cause = Component.translatable("hotbath.compat.warning.api_change", error.modId)
            .withStyle(ChatFormatting.GRAY);
        player.sendSystemMessage(cause);
        
        // Report link
        MutableComponent reportLink = Component.literal("  ")
            .append(Component.translatable("hotbath.compat.warning.report")
                .withStyle(style -> style
                    .withColor(ChatFormatting.GREEN)
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, GITHUB_ISSUES_URL))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        Component.translatable("hotbath.compat.warning.report.hover")))));
        player.sendSystemMessage(reportLink);
        
        // Separator
        player.sendSystemMessage(Component.literal(""));
    }
    
    /**
     * Get the version of a mod
     */
    public static String getModVersion(String modId) {
        return ModList.get().getModContainerById(modId)
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse("unknown");
    }
    
    /**
     * Get Hot Bath mod version
     */
    public static String getHotBathVersion() {
        return getModVersion("hotbath");
    }
    
    /**
     * Get Minecraft version
     */
    public static String getMinecraftVersion() {
        return FMLLoader.versionInfo().mcVersion();
    }
    
    /**
     * Log a startup summary of all compat modules
     */
    private static void logStartupSummary() {
        if (startupSummaryLogged) return;
        startupSummaryLogged = true;
        
        LOGGER.info("=== Hot Bath Compatibility Summary ===");
        LOGGER.info("Hot Bath version: {}", getHotBathVersion());
        LOGGER.info("Minecraft version: {}", getMinecraftVersion());
        LOGGER.info("");
        
        int enabledCount = 0;
        int disabledCount = 0;
        int notLoadedCount = 0;
        
        for (CompatInfo compat : REGISTERED_COMPATS.values()) {
            if (!safeCheckLoaded(compat)) {
                LOGGER.info("  [NOT LOADED] {} - mod not present", compat.displayName);
                notLoadedCount++;
            } else if (!compat.enabled) {
                LOGGER.info("  [DISABLED]   {} (v{}) - initialization failed", 
                    compat.displayName, compat.modVersion);
                disabledCount++;
            } else if (compat.initialized) {
                LOGGER.info("  [ENABLED]    {} (v{})", compat.displayName, compat.modVersion);
                enabledCount++;
            }
        }
        
        LOGGER.info("");
        LOGGER.info("Summary: {} enabled, {} disabled, {} not loaded", 
            enabledCount, disabledCount, notLoadedCount);
        
        if (disabledCount > 0) {
            LOGGER.warn("Some integrations failed! Check logs above for details.");
            LOGGER.warn("Report issues at: {}", GITHUB_ISSUES_URL);
        }
        
        LOGGER.info("======================================");
    }
    
    /**
     * Get a formatted error report for GitHub issue
     */
    public static String generateErrorReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Hot Bath Compatibility Error Report\n\n");
        sb.append("## Environment\n");
        sb.append("- Hot Bath version: ").append(getHotBathVersion()).append("\n");
        sb.append("- Minecraft version: ").append(getMinecraftVersion()).append("\n\n");
        
        sb.append("## Loaded Mods\n");
        for (CompatInfo compat : REGISTERED_COMPATS.values()) {
            if (safeCheckLoaded(compat)) {
                sb.append("- ").append(compat.displayName)
                  .append(" (").append(compat.modId).append("): ")
                  .append(compat.modVersion).append("\n");
            }
        }
        
        sb.append("\n## Errors\n");
        for (CompatError error : DISABLED_COMPATS.values()) {
            sb.append("### ").append(error.compatName).append("\n");
            sb.append("- Mod: ").append(error.modId).append(" v").append(error.modVersion).append("\n");
            sb.append("- Error: ").append(error.errorMessage).append("\n");
            sb.append("```\n").append(error.stackTrace).append("```\n\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Clear player notification tracking (call on player disconnect)
     */
    public static void clearPlayerNotifications(UUID playerId) {
        NOTIFIED_PLAYERS.remove(playerId);
    }
    
    /**
     * Check if there are any disabled compats
     */
    public static boolean hasDisabledCompats() {
        return !DISABLED_COMPATS.isEmpty();
    }
    
    /**
     * Get all disabled compat names
     */
    public static Set<String> getDisabledCompatNames() {
        Set<String> names = new HashSet<>();
        for (CompatError error : DISABLED_COMPATS.values()) {
            names.add(error.compatName);
        }
        return names;
    }
    
    /**
     * Safely check if a compat module's target mod is loaded.
     * Catches any exceptions thrown by the isLoaded supplier.
     * 
     * @param compat The compat info to check
     * @return true if the mod is loaded, false otherwise or on error
     */
    private static boolean safeCheckLoaded(CompatInfo compat) {
        try {
            return compat.isLoaded.getAsBoolean();
        } catch (Throwable e) {
            LOGGER.error("Error checking if {} is loaded: {}", compat.displayName, e.getMessage());
            return false;
        }
    }
}
