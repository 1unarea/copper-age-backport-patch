package com.github.lunarea.copperagepatch.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Lightweight client configuration management for Copper Age Backport Patch.
 * Manages copper_age_patch.json allowing users to customize behavior such as
 * the Copper Golem spawn egg texture mode (AUTO, MODERN, CLASSIC).
 *
 * Implemented completely mapping-agnostic across NeoForge and Fabric without
 * hard net/minecraft class references.
 */
public final class CopperAgeConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopperAgeConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String CONFIG_FILE_NAME = "copper_age_patch.json";
    public static final String KEY_COPPER_GOLEM_SPAWN_EGG = "copper_golem_spawn_egg";

    public enum SpawnEggTextureMode {
        AUTO,
        MODERN,
        CLASSIC;

        public static SpawnEggTextureMode fromString(String str) {
            if (str == null) {
                return AUTO;
            }
            try {
                return valueOf(str.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return AUTO;
            }
        }
    }

    private static volatile SpawnEggTextureMode spawnEggMode = SpawnEggTextureMode.AUTO;
    private static volatile boolean loaded = false;
    private static File activeConfigFile = null;

    private CopperAgeConfig() {}

    /**
     * Resolves the configuration file location across Fabric, NeoForge, or fallback paths.
     */
    public static File resolveConfigFile() {
        if (activeConfigFile != null) {
            return activeConfigFile;
        }

        File resolved = null;

        // 1. Check system property override (useful for testing or dedicated launchers)
        String overridePath = System.getProperty("copper_age_patch.config_file");
        if (overridePath != null && !overridePath.isBlank()) {
            resolved = new File(overridePath);
        }

        // 2. Try FabricLoader config directory
        if (resolved == null) {
            try {
                Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
                Object instance = fabricLoaderClass.getMethod("getInstance").invoke(null);
                Method getConfigDir = fabricLoaderClass.getMethod("getConfigDir");
                Path configDir = (Path) getConfigDir.invoke(instance);
                if (configDir != null) {
                    resolved = configDir.resolve(CONFIG_FILE_NAME).toFile();
                }
            } catch (Throwable ignored) {}
        }

        // 3. Try NeoForge FMLPaths.CONFIGDIR
        if (resolved == null) {
            try {
                Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                Object configDirObj = fmlPathsClass.getField("CONFIGDIR").get(null);
                Path p = null;
                if (configDirObj instanceof java.util.function.Supplier<?> s) {
                    Object pathObj = s.get();
                    if (pathObj instanceof Path) {
                        p = (Path) pathObj;
                    }
                } else if (configDirObj != null) {
                    try {
                        Method getMethod = configDirObj.getClass().getMethod("get");
                        Object pathObj = getMethod.invoke(configDirObj);
                        if (pathObj instanceof Path) {
                            p = (Path) pathObj;
                        }
                    } catch (Throwable ignored) {}
                }
                if (p != null) {
                    resolved = p.resolve(CONFIG_FILE_NAME).toFile();
                }
            } catch (Throwable ignored) {}
        }

        // 4. Default fallback: ./config/copper_age_patch.json
        if (resolved == null) {
            File configDir = new File("config");
            if (configDir.exists() && configDir.isDirectory()) {
                resolved = new File(configDir, CONFIG_FILE_NAME);
            } else {
                resolved = new File("config", CONFIG_FILE_NAME);
            }
        }

        activeConfigFile = resolved;
        return resolved;
    }

    /**
     * Loads or creates the configuration file.
     */
    public static synchronized void loadOrCreateConfig() {
        if (loaded && activeConfigFile != null) {
            return;
        }
        loadOrCreateConfig(resolveConfigFile());
    }

    /**
     * Loads or creates the configuration file at the specified location.
     */
    public static synchronized void loadOrCreateConfig(File file) {
        if (file == null) {
            file = resolveConfigFile();
        }
        activeConfigFile = file;
        if (!file.exists() || file.length() == 0) {
            // Write defaults
            spawnEggMode = SpawnEggTextureMode.AUTO;
            saveConfig(file);
            loaded = true;
            return;
        }

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has(KEY_COPPER_GOLEM_SPAWN_EGG)) {
                JsonElement elem = json.get(KEY_COPPER_GOLEM_SPAWN_EGG);
                if (elem != null && elem.isJsonPrimitive()) {
                    spawnEggMode = SpawnEggTextureMode.fromString(elem.getAsString());
                } else {
                    spawnEggMode = SpawnEggTextureMode.AUTO;
                }
            } else {
                spawnEggMode = SpawnEggTextureMode.AUTO;
            }
            loaded = true;
        } catch (Throwable t) {
            LOGGER.warn("[CopperAgeBackportPatch] Failed to parse {}: {}. Falling back to default AUTO.", file.getName(), t.getMessage());
            spawnEggMode = SpawnEggTextureMode.AUTO;
            loaded = true;
        }
    }

    /**
     * Persists the current configuration to disk.
     */
    public static synchronized void saveConfig() {
        saveConfig(resolveConfigFile());
    }

    /**
     * Persists configuration to the specified file.
     */
    public static synchronized void saveConfig(File file) {
        if (file == null) {
            return;
        }
        activeConfigFile = file;
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            JsonObject json = new JsonObject();
            json.addProperty(KEY_COPPER_GOLEM_SPAWN_EGG, spawnEggMode.name());

            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[CopperAgeBackportPatch] Could not write config file {}: {}", file.getAbsolutePath(), e.getMessage());
        }
    }

    public static SpawnEggTextureMode getSpawnEggMode() {
        if (!loaded) {
            loadOrCreateConfig();
        }
        return spawnEggMode;
    }

    public static synchronized void setSpawnEggMode(SpawnEggTextureMode mode) {
        spawnEggMode = mode != null ? mode : SpawnEggTextureMode.AUTO;
        loaded = true;
        saveConfig();
    }

    /**
     * Determines whether the modern Copper Golem spawn egg texture should be displayed.
     * - MODERN: always true
     * - CLASSIC: always false
     * - AUTO: true if 'vanillabackport' is loaded, false otherwise
     */
    public static boolean shouldUseModernEgg() {
        SpawnEggTextureMode mode = getSpawnEggMode();
        return switch (mode) {
            case MODERN -> true;
            case CLASSIC -> false;
            case AUTO -> isModLoaded("vanillabackport");
        };
    }

    /**
     * Robust, mapping-agnostic mod presence detection across Fabric and NeoForge loaders.
     */
    public static boolean isModLoaded(String modId) {
        if (modId == null || modId.isBlank()) {
            return false;
        }

        // Testing override property
        String testProp = System.getProperty("copper_age_patch.test.modloaded." + modId);
        if (testProp != null) {
            return Boolean.parseBoolean(testProp);
        }

        // 1. FabricLoader check
        try {
            Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object instance = fabricLoaderClass.getMethod("getInstance").invoke(null);
            Method isModLoaded = fabricLoaderClass.getMethod("isModLoaded", String.class);
            return (Boolean) isModLoaded.invoke(instance, modId);
        } catch (Throwable ignored) {}

        // 2. NeoForge ModList check
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object instance = modListClass.getMethod("get").invoke(null);
            Method isLoaded = modListClass.getMethod("isLoaded", String.class);
            return (Boolean) isLoaded.invoke(instance, modId);
        } catch (Throwable ignored) {}

        // 3. NeoForge LoadingModList check
        try {
            Class<?> loadingModListClass = Class.forName("net.neoforged.fml.loading.LoadingModList");
            Object instance = loadingModListClass.getMethod("get").invoke(null);
            Method getModFileById = loadingModListClass.getMethod("getModFileById", String.class);
            return getModFileById.invoke(instance, modId) != null;
        } catch (Throwable ignored) {}

        return false;
    }

    /**
     * Resets state (for unit testing).
     */
    public static synchronized void resetForTesting() {
        activeConfigFile = null;
        spawnEggMode = SpawnEggTextureMode.AUTO;
        loaded = false;
    }
}
