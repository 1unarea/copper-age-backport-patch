package com.github.lunarea.copperagepatch.spawnegg;

import com.github.lunarea.copperagepatch.config.CopperAgeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Clean-room patcher for conditionally registering the modern Copper Golem spawn egg texture
 * as a built-in resource pack across NeoForge and Fabric loaders.
 *
 * Implemented completely mapping-agnostic via reflection with ZERO net/minecraft/ class
 * references in method signatures or bytecode descriptors.
 */
public final class CopperSpawnEggPatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopperSpawnEggPatcher.class);

    public static final String PACK_ID = "modern_copper_golem_spawn_egg";
    public static final String PACK_SUBPATH = "resourcepacks/modern_copper_golem_spawn_egg";
    public static final String PACK_DISPLAY_NAME = "Modern Copper Golem Spawn Egg";

    private static volatile boolean fabricInitialized = false;

    private CopperSpawnEggPatcher() {}

    /**
     * Resets internal registration state (for unit testing).
     */
    public static synchronized void resetForTesting() {
        fabricInitialized = false;
    }

    /**
     * Handles AddPackFindersEvent on NeoForge mod event bus.
     */
    public static void onNeoForgeAddPackFinders(Object event) {
        if (event == null) {
            return;
        }

        // NeoForge fires AddPackFindersEvent for CLIENT_RESOURCES and SERVER_DATA.
        // Modern spawn egg is a client resource pack; skip server data passes cleanly without logging.
        try {
            Method getPackTypeMethod = event.getClass().getMethod("getPackType");
            Object eventPackType = getPackTypeMethod.invoke(event);
            if (eventPackType != null && !"CLIENT_RESOURCES".equals(eventPackType.toString())) {
                return;
            }
        } catch (Throwable ignored) {}

        if (!CopperAgeConfig.shouldUseModernEgg()) {
            LOGGER.info("[CopperAgeBackportPatch] Copper Golem spawn egg using CLASSIC texture (modern texture not enabled).");
            return;
        }

        try {
            LOGGER.info("[CopperAgeBackportPatch] Registering modern Copper Golem spawn egg built-in resource pack on NeoForge.");

            // 1. Resolve ResourceLocation for pack
            Object packLocation = createResourceLocation("copper_age_patch", PACK_SUBPATH);
            if (packLocation == null) {
                LOGGER.warn("[CopperAgeBackportPatch] Could not create ResourceLocation for builtin resource pack on NeoForge.");
                return;
            }

            // 2. Resolve PackType.CLIENT_RESOURCES
            Class<?> packTypeClass = Class.forName("net.minecraft.server.packs.PackType");
            Object clientResources = Enum.valueOf((Class<Enum>) packTypeClass, "CLIENT_RESOURCES");

            // 3. Resolve Component.literal(...)
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method literalMethod = componentClass.getMethod("literal", String.class);
            Object displayName = literalMethod.invoke(null, PACK_DISPLAY_NAME);

            // 4. Resolve PackSource.BUILT_IN
            Class<?> packSourceClass = Class.forName("net.minecraft.server.packs.repository.PackSource");
            Object packSource = null;
            try {
                Field builtInField = packSourceClass.getField("BUILT_IN");
                packSource = builtInField.get(null);
            } catch (Throwable t) {
                try {
                    Field defaultField = packSourceClass.getField("DEFAULT");
                    packSource = defaultField.get(null);
                } catch (Throwable ignored) {}
            }

            // 5. Resolve Pack.Position.TOP
            Class<?> packClass = Class.forName("net.minecraft.server.packs.repository.Pack");
            Class<?> positionClass = null;
            for (Class<?> declared : packClass.getDeclaredClasses()) {
                if ("Position".equals(declared.getSimpleName())) {
                    positionClass = declared;
                    break;
                }
            }
            Object positionTop = positionClass != null ? Enum.valueOf((Class<Enum>) positionClass, "TOP") : null;

            // 6. Find and invoke addPackFinders on event
            Method addPackFindersMethod = null;
            for (Method m : event.getClass().getMethods()) {
                if ("addPackFinders".equals(m.getName()) && m.getParameterCount() >= 5) {
                    addPackFindersMethod = m;
                    break;
                }
            }

            if (addPackFindersMethod != null) {
                if (addPackFindersMethod.getParameterCount() == 6) {
                    addPackFindersMethod.invoke(event, packLocation, clientResources, displayName, packSource, true, positionTop);
                } else if (addPackFindersMethod.getParameterCount() == 5) {
                    addPackFindersMethod.invoke(event, packLocation, clientResources, displayName, packSource, true);
                }
                LOGGER.info("[CopperAgeBackportPatch] Successfully registered modern spawn egg resource pack via AddPackFindersEvent.");
            } else {
                LOGGER.warn("[CopperAgeBackportPatch] Could not find addPackFinders method on AddPackFindersEvent.");
            }
        } catch (Throwable t) {
            LOGGER.error("[CopperAgeBackportPatch] Failed to register modern spawn egg resource pack on NeoForge: {}", t.getMessage(), t);
        }
    }

    /**
     * Initializes modern spawn egg resource pack registration on Fabric via Fabric API.
     */
    public static synchronized void initFabric() {
        if (fabricInitialized) {
            return;
        }

        if (!CopperAgeConfig.shouldUseModernEgg()) {
            LOGGER.info("[CopperAgeBackportPatch] Copper Golem spawn egg using CLASSIC texture (modern texture not enabled).");
            return;
        }

        fabricInitialized = true;

        try {
            LOGGER.info("[CopperAgeBackportPatch] Registering modern Copper Golem spawn egg built-in resource pack on Fabric.");

            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loaderInstance = loaderClass.getMethod("getInstance").invoke(null);
            Method getModContainerMethod = loaderClass.getMethod("getModContainer", String.class);
            Optional<?> containerOpt = (Optional<?>) getModContainerMethod.invoke(loaderInstance, "copper_age_patch");

            if (containerOpt.isEmpty()) {
                LOGGER.warn("[CopperAgeBackportPatch] Could not find mod container for 'copper_age_patch' on Fabric.");
                return;
            }
            Object container = containerOpt.get();

            // Resolve Identifier (net.minecraft.class_2960 or net.minecraft.resources.ResourceLocation)
            Object identifier = createResourceLocation("copper_age_patch", PACK_ID);
            if (identifier == null) {
                LOGGER.warn("[CopperAgeBackportPatch] Could not create Identifier for builtin resource pack on Fabric.");
                return;
            }

            // Resolve ResourcePackActivationType.ALWAYS_ENABLED
            Class<?> actTypeClass = Class.forName("net.fabricmc.fabric.api.resource.ResourcePackActivationType");
            Object alwaysEnabled = Enum.valueOf((Class<Enum>) actTypeClass, "ALWAYS_ENABLED");

            // Resolve ResourceManagerHelper.registerBuiltinResourcePack
            Class<?> helperClass = Class.forName("net.fabricmc.fabric.api.resource.ResourceManagerHelper");
            Method registerMethod = null;
            for (Method m : helperClass.getMethods()) {
                if ("registerBuiltinResourcePack".equals(m.getName()) && m.getParameterCount() == 3) {
                    registerMethod = m;
                    break;
                }
            }

            if (registerMethod != null) {
                Object result = registerMethod.invoke(null, identifier, container, alwaysEnabled);
                if (Boolean.FALSE.equals(result)) {
                    LOGGER.warn("[CopperAgeBackportPatch] registerBuiltinResourcePack returned false (pack not found or not active).");
                } else {
                    LOGGER.info("[CopperAgeBackportPatch] Successfully registered modern spawn egg built-in resource pack on Fabric.");
                }
            } else {
                LOGGER.warn("[CopperAgeBackportPatch] registerBuiltinResourcePack method not found on ResourceManagerHelper.");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("[CopperAgeBackportPatch] Fabric Resource Loader API not found on classpath, skipping builtin pack registration.");
        } catch (Throwable t) {
            LOGGER.error("[CopperAgeBackportPatch] Failed to register modern spawn egg resource pack on Fabric: {}", t.getMessage(), t);
        }
    }

    /**
     * Creates a ResourceLocation / Identifier instance mapping-agnostically across NeoForge and Fabric.
     */
    public static Object createResourceLocation(String namespace, String path) {
        if (namespace == null || namespace.isBlank() || path == null || path.isBlank()) {
            return null;
        }
        // Try net.minecraft.resources.ResourceLocation (Mojang / NeoForge)
        try {
            Class<?> rlClass = Class.forName("net.minecraft.resources.ResourceLocation");
            try {
                Method fromNamespaceAndPath = rlClass.getMethod("fromNamespaceAndPath", String.class, String.class);
                return fromNamespaceAndPath.invoke(null, namespace, path);
            } catch (NoSuchMethodException e) {
                Constructor<?> c = rlClass.getDeclaredConstructor(String.class, String.class);
                c.setAccessible(true);
                return c.newInstance(namespace, path);
            }
        } catch (Throwable ignored) {}

        // Try net.minecraft.util.Identifier (Fabric Yarn Identifier)
        try {
            Class<?> idClass = Class.forName("net.minecraft.util.Identifier");
            try {
                Method ofMethod = idClass.getMethod("of", String.class, String.class);
                return ofMethod.invoke(null, namespace, path);
            } catch (NoSuchMethodException e) {
                Constructor<?> c = idClass.getDeclaredConstructor(String.class, String.class);
                c.setAccessible(true);
                return c.newInstance(namespace, path);
            }
        } catch (Throwable ignored) {}

        // Try net.minecraft.class_2960 (Fabric Intermediary Identifier)
        try {
            Class<?> idClass = Class.forName("net.minecraft.class_2960");
            try {
                Method ofMethod = idClass.getMethod("method_60655", String.class, String.class);
                return ofMethod.invoke(null, namespace, path);
            } catch (NoSuchMethodException ignored) {}
            try {
                Method ofMethod = idClass.getMethod("of", String.class, String.class);
                return ofMethod.invoke(null, namespace, path);
            } catch (NoSuchMethodException e) {
                Constructor<?> c = idClass.getDeclaredConstructor(String.class, String.class);
                c.setAccessible(true);
                return c.newInstance(namespace, path);
            }
        } catch (Throwable ignored) {}

        return null;
    }
}
