package com.github.lunarea.copperagepatch.spawnegg;

import com.github.lunarea.copperagepatch.config.CopperAgeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

    /**
     * NeoForge hook: handles RegisterColorHandlersEvent.Item to register untinted ItemColor.
     */
    public static void onNeoForgeRegisterColorHandlers(Object event) {
        if (event == null) {
            return;
        }
        try {
            Object copperEgg = com.github.lunarea.copperagepatch.creative.CopperSpawnEggTabPatcher.getCopperGolemSpawnEgg();
            if (copperEgg == null) {
                LOGGER.warn("[CopperAgeBackportPatch] Copper Golem spawn egg not found for NeoForge color registration.");
                return;
            }

            Method registerMethod = null;
            for (Method m : event.getClass().getMethods()) {
                if ("register".equals(m.getName()) && m.getParameterCount() == 2) {
                    registerMethod = m;
                    break;
                }
            }
            if (registerMethod == null) {
                LOGGER.warn("[CopperAgeBackportPatch] register method not found on RegisterColorHandlersEvent.Item.");
                return;
            }

            Class<?> itemColorInterface = registerMethod.getParameterTypes()[0];
            Class<?> itemLikeArrayClass = registerMethod.getParameterTypes()[1];
            Class<?> itemLikeClass = itemLikeArrayClass.getComponentType();

            Object colorProxy = createItemColorProxy(itemColorInterface);
            Object itemArray = Array.newInstance(itemLikeClass, 1);
            Array.set(itemArray, 0, copperEgg);

            registerMethod.setAccessible(true);
            registerMethod.invoke(event, colorProxy, itemArray);
            LOGGER.info("[CopperAgeBackportPatch] Registered untinted ItemColor provider for Copper Golem spawn egg on NeoForge.");
        } catch (Throwable t) {
            LOGGER.error("[CopperAgeBackportPatch] Failed to register ItemColor on NeoForge: {}", t.getMessage(), t);
        }
    }

    /**
     * Fabric client hook: defers untinted ItemColor registration until after all mods have
     * finished their client init. Uses ClientLifecycleEvents.CLIENT_STARTED so our provider
     * always runs after the CAB mod's DeferredSpawnEggItem color registration.
     */
    public static void initFabricClient() {
        // Try to register a CLIENT_STARTED listener that will run after full client init.
        // This ensures we override any color handler the CAB mod registered during its own init.
        boolean deferred = false;
        try {
            Class<?> lifecycleClass = Class.forName("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents");
            Field startedField = lifecycleClass.getField("CLIENT_STARTED");
            Object startedEvent = startedField.get(null);

            // Build a Consumer<MinecraftClient> proxy
            Class<?> callbackInterface = null;
            for (Class<?> inner : lifecycleClass.getDeclaredClasses()) {
                if (inner.getSimpleName().equals("ClientStarted")) {
                    callbackInterface = inner;
                    break;
                }
            }
            if (callbackInterface == null) {
                // Try as a direct interface
                callbackInterface = Class.forName("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents$ClientStarted");
            }

            Object callback = Proxy.newProxyInstance(
                    CopperSpawnEggPatcher.class.getClassLoader(),
                    new Class<?>[]{callbackInterface},
                    (proxy, method, args) -> {
                        if (!"equals".equals(method.getName()) && !"hashCode".equals(method.getName()) && !"toString".equals(method.getName())) {
                            registerColorNow();
                        }
                        return null;
                    }
            );

            Method registerMethod = null;
            for (Method m : startedEvent.getClass().getMethods()) {
                if ("register".equals(m.getName()) && m.getParameterCount() == 1) {
                    registerMethod = m;
                    break;
                }
            }
            if (registerMethod != null) {
                registerMethod.setAccessible(true);
                registerMethod.invoke(startedEvent, callback);
                LOGGER.info("[CopperAgeBackportPatch] Registered CLIENT_STARTED listener for Fabric spawn egg color override.");
                deferred = true;
            }
        } catch (Throwable t) {
            LOGGER.debug("[CopperAgeBackportPatch] Could not register CLIENT_STARTED listener: {}", t.getMessage());
        }

        if (!deferred) {
            // Fallback: register immediately via ColorProviderRegistry (may or may not win over CAB mod)
            registerColorNow();
        }
    }

    /**
     * Performs the actual ItemColor registration. Called either immediately or from CLIENT_STARTED.
     * Tries ColorProviderRegistry first, then direct ItemColors injection.
     */
    private static void registerColorNow() {
        try {
            Object copperEgg = com.github.lunarea.copperagepatch.creative.CopperSpawnEggTabPatcher.getCopperGolemSpawnEgg();
            if (copperEgg == null) {
                LOGGER.warn("[CopperAgeBackportPatch] Copper Golem spawn egg not found for Fabric client color registration.");
                return;
            }

            // Try ColorProviderRegistry.ITEM.register(provider, items...)
            Class<?> regClass = null;
            for (String className : new String[]{
                    "net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry",
                    "net.fabricmc.fabric.api.client.render.ColorProviderRegistry"
            }) {
                try {
                    regClass = Class.forName(className);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }

            if (regClass != null) {
                Field itemField = regClass.getField("ITEM");
                Object itemRegistry = itemField.get(null);
                if (itemRegistry != null) {
                    Method registerMethod = null;
                    for (Method m : itemRegistry.getClass().getMethods()) {
                        if ("register".equals(m.getName()) && m.getParameterCount() == 2) {
                            registerMethod = m;
                            break;
                        }
                    }
                    if (registerMethod != null) {
                        Class<?> providerInterface = registerMethod.getParameterTypes()[0];
                        Class<?> targetParam = registerMethod.getParameterTypes()[1];
                        Object colorProxy = createItemColorProxy(providerInterface);

                        if (targetParam.isArray()) {
                            Class<?> compType = targetParam.getComponentType();
                            Object itemArray = Array.newInstance(compType, 1);
                            Array.set(itemArray, 0, copperEgg);
                            registerMethod.invoke(itemRegistry, colorProxy, itemArray);
                        } else {
                            registerMethod.invoke(itemRegistry, colorProxy, copperEgg);
                        }
                        LOGGER.info("[CopperAgeBackportPatch] Registered ItemColor provider for Copper Golem spawn egg on Fabric via ColorProviderRegistry.");
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[CopperAgeBackportPatch] Could not register ItemColor via Fabric ColorProviderRegistry: {}", t.getMessage());
        }

        // Always also try direct ItemColors injection — this wins even if ColorProviderRegistry
        // kept the earlier CAB mod registration, because we overwrite the backing map entry.
        try {
            registerDirectItemColorFallback();
        } catch (Throwable ignored) {}
    }


    private static void registerDirectItemColorFallback() {
        try {
            Object copperEgg = com.github.lunarea.copperagepatch.creative.CopperSpawnEggTabPatcher.getCopperGolemSpawnEgg();
            if (copperEgg == null) return;

            Object mcInstance = null;
            for (String mcName : new String[]{"net.minecraft.client.Minecraft", "net.minecraft.client.MinecraftClient", "net.minecraft.class_310"}) {
                try {
                    Class<?> mcClass = Class.forName(mcName);
                    for (String mName : new String[]{"getInstance", "method_1551"}) {
                        try {
                            Method m = mcClass.getMethod(mName);
                            mcInstance = m.invoke(null);
                            if (mcInstance != null) break;
                        } catch (Throwable ignored) {}
                    }
                } catch (Throwable ignored) {}
                if (mcInstance != null) break;
            }
            if (mcInstance == null) return;

            Object itemColors = null;
            for (String mName : new String[]{"getItemColors", "method_1508"}) {
                try {
                    Method m = mcInstance.getClass().getMethod(mName);
                    itemColors = m.invoke(mcInstance);
                    if (itemColors != null) break;
                } catch (Throwable ignored) {}
            }
            if (itemColors == null) return;

            for (Method m : itemColors.getClass().getMethods()) {
                if (("register".equals(m.getName()) || "method_1708".equals(m.getName())) && m.getParameterCount() == 2) {
                    Class<?> providerType = m.getParameterTypes()[0];
                    Class<?> itemParam = m.getParameterTypes()[1];
                    Object colorProxy = createItemColorProxy(providerType);
                    if (itemParam.isArray()) {
                        Object itemArr = Array.newInstance(itemParam.getComponentType(), 1);
                        Array.set(itemArr, 0, copperEgg);
                        m.invoke(itemColors, colorProxy, itemArr);
                    } else {
                        m.invoke(itemColors, colorProxy, copperEgg);
                    }
                    LOGGER.info("[CopperAgeBackportPatch] Registered ItemColor provider for Copper Golem spawn egg directly into ItemColors.");
                    break;
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Creates a dynamic proxy implementing ItemColor / class_326 returning -1 (0xFFFFFFFF)
     * when modern egg is enabled to cancel Minecraft's spawn egg tint filter.
     */
    public static Object createItemColorProxy(Class<?> interfaceClass) {
        return Proxy.newProxyInstance(
                CopperSpawnEggPatcher.class.getClassLoader(),
                new Class<?>[]{interfaceClass},
                (proxy, method, args) -> {
                    if (args != null && args.length == 2 && args[1] instanceof Integer) {
                        if (CopperAgeConfig.shouldUseModernEgg()) {
                            // Modern spawn egg is a complete 16x16 icon texture.
                            // Return -1 (0xFFFFFFFF) to prevent color tint multiplication.
                            return -1;
                        }
                        int tintIndex = (int) args[1];
                        return tintIndex == 0 ? 12088115 : (tintIndex == 1 ? 4772300 : -1);
                    }
                    if ("toString".equals(method.getName())) {
                        return "CopperSpawnEggColorProxy";
                    }
                    return 0;
                }
        );
    }
}

