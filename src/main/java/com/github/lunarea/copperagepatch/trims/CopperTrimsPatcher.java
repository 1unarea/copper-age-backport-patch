package com.github.lunarea.copperagepatch.trims;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Clean-room patcher that registers a built-in resource pack ({@code copper_trims}) at the
 * highest priority on both NeoForge and Fabric, ensuring that copper equipment item models
 * (armor + tool trim overrides) always win over any model provided by upstream mods
 * (e.g. {@code copperagebackport}).
 *
 * <p>The pack is unconditionally {@code ALWAYS_ENABLED} and placed at {@code Pack.Position.TOP}
 * so that all copper armor and tool models with trim predicate overrides are respected regardless
 * of alphabetical mod-ID ordering in the resource manager.</p>
 *
 * <p>Implemented mapping-agnostically via reflection — zero {@code net/minecraft/} class references
 * in method signatures or bytecode descriptors.</p>
 */
public final class CopperTrimsPatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopperTrimsPatcher.class);

    public static final String PACK_ID      = "copper_trims";
    public static final String PACK_SUBPATH = "resourcepacks/copper_trims";
    public static final String PACK_DISPLAY = "Copper Equipment Trim Models";

    private static volatile boolean fabricInitialized = false;

    private CopperTrimsPatcher() {}

    /** Resets internal state (for unit testing). */
    public static synchronized void resetForTesting() {
        fabricInitialized = false;
    }

    // -------------------------------------------------------------------------
    // NeoForge – called from AddPackFindersEvent listener
    // -------------------------------------------------------------------------

    /**
     * Handles {@code AddPackFindersEvent} on the NeoForge mod event bus.
     * Must be wired up during mod construction via the event bus.
     */
    public static void onNeoForgeAddPackFinders(Object event) {
        if (event == null) {
            return;
        }

        // Only handle CLIENT_RESOURCES pass
        try {
            Method getPackTypeMethod = event.getClass().getMethod("getPackType");
            Object eventPackType = getPackTypeMethod.invoke(event);
            if (eventPackType != null && !"CLIENT_RESOURCES".equals(eventPackType.toString())) {
                return;
            }
        } catch (Throwable ignored) {}

        try {
            LOGGER.info("[CopperAgeBackportPatch] Registering copper_trims built-in resource pack on NeoForge.");

            // 1. ResourceLocation for the pack
            Object packLocation = createResourceLocation("copper_age_patch", PACK_SUBPATH);
            if (packLocation == null) {
                LOGGER.warn("[CopperAgeBackportPatch] Could not create ResourceLocation for copper_trims pack (NeoForge).");
                return;
            }

            // 2. PackType.CLIENT_RESOURCES
            Class<?> packTypeClass = Class.forName("net.minecraft.server.packs.PackType");
            Object clientResources = Enum.valueOf((Class<Enum>) packTypeClass, "CLIENT_RESOURCES");

            // 3. Component.literal(...)
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method literalMethod = componentClass.getMethod("literal", String.class);
            Object displayName = literalMethod.invoke(null, PACK_DISPLAY);

            // 4. PackSource.BUILT_IN (fallback: DEFAULT)
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

            // 5. Pack.Position.TOP
            Class<?> packClass = Class.forName("net.minecraft.server.packs.repository.Pack");
            Class<?> positionClass = null;
            for (Class<?> declared : packClass.getDeclaredClasses()) {
                if ("Position".equals(declared.getSimpleName())) {
                    positionClass = declared;
                    break;
                }
            }
            Object positionTop = positionClass != null ? Enum.valueOf((Class<Enum>) positionClass, "TOP") : null;

            // 6. event.addPackFinders(...)
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
                LOGGER.info("[CopperAgeBackportPatch] Successfully registered copper_trims resource pack via AddPackFindersEvent.");
            } else {
                LOGGER.warn("[CopperAgeBackportPatch] Could not find addPackFinders method on AddPackFindersEvent.");
            }
        } catch (Throwable t) {
            LOGGER.error("[CopperAgeBackportPatch] Failed to register copper_trims resource pack on NeoForge: {}", t.getMessage(), t);
        }
    }

    // -------------------------------------------------------------------------
    // Fabric – called from ModInitializer
    // -------------------------------------------------------------------------

    /**
     * Registers the {@code copper_trims} built-in resource pack on Fabric via
     * {@code ResourceManagerHelper.registerBuiltinResourcePack}.
     */
    public static synchronized void initFabric() {
        if (fabricInitialized) {
            return;
        }
        fabricInitialized = true;

        try {
            LOGGER.info("[CopperAgeBackportPatch] Registering copper_trims built-in resource pack on Fabric.");

            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loaderInstance = loaderClass.getMethod("getInstance").invoke(null);
            Method getModContainerMethod = loaderClass.getMethod("getModContainer", String.class);
            Optional<?> containerOpt = (Optional<?>) getModContainerMethod.invoke(loaderInstance, "copper_age_patch");

            if (containerOpt.isEmpty()) {
                LOGGER.warn("[CopperAgeBackportPatch] Could not find mod container for 'copper_age_patch' on Fabric.");
                return;
            }
            Object container = containerOpt.get();

            // Identifier: "copper_age_patch:copper_trims"
            Object identifier = createResourceLocation("copper_age_patch", PACK_ID);
            if (identifier == null) {
                LOGGER.warn("[CopperAgeBackportPatch] Could not create Identifier for copper_trims pack (Fabric).");
                return;
            }

            // ResourcePackActivationType.ALWAYS_ENABLED
            Class<?> actTypeClass = Class.forName("net.fabricmc.fabric.api.resource.ResourcePackActivationType");
            Object alwaysEnabled = Enum.valueOf((Class<Enum>) actTypeClass, "ALWAYS_ENABLED");

            // ResourceManagerHelper.registerBuiltinResourcePack(id, container, activationType)
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
                    LOGGER.warn("[CopperAgeBackportPatch] registerBuiltinResourcePack returned false for copper_trims (pack not found?).");
                } else {
                    LOGGER.info("[CopperAgeBackportPatch] Successfully registered copper_trims built-in resource pack on Fabric.");
                }
            } else {
                LOGGER.warn("[CopperAgeBackportPatch] registerBuiltinResourcePack method not found on ResourceManagerHelper.");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("[CopperAgeBackportPatch] Fabric Resource Loader API not found on classpath, skipping copper_trims pack registration.");
        } catch (Throwable t) {
            LOGGER.error("[CopperAgeBackportPatch] Failed to register copper_trims resource pack on Fabric: {}", t.getMessage(), t);
        }
    }

    // -------------------------------------------------------------------------
    // Shared utility
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code ResourceLocation} / {@code Identifier} instance in a mapping-agnostic way,
     * trying Mojang/NeoForge naming, then Fabric Yarn naming, then Fabric Intermediary naming.
     */
    static Object createResourceLocation(String namespace, String path) {
        if (namespace == null || namespace.isBlank() || path == null || path.isBlank()) {
            return null;
        }
        // net.minecraft.resources.ResourceLocation (Mojang / NeoForge)
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

        // net.minecraft.util.Identifier (Fabric Yarn)
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

        // net.minecraft.class_2960 (Fabric Intermediary)
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
