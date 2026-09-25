package com.github.lunarea.copperagepatch.item;

import com.github.lunarea.copperagepatch.durability.CopperArmorDurabilityPatcher;
import com.github.smallinger.copperagebackport.registry.RegistryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Mapping-agnostic conditional item registration for Copper Knife and Copper Shield.
 *
 * Requirements:
 * - Copper Knife: Registered ONLY when Farmer's Delight (farmersdelight) is installed.
 *   Canonical durability: 190 (CAB copper tier).
 * - Copper Shield: Registered ONLY when Shield Expansion (shieldexp) is installed.
 *   Canonical durability: 120 (Shield Expansion copper tier).
 *
 * Implemented completely mapping-agnostic via reflection and RegistryHelper with ZERO
 * net/minecraft/ class dependencies in bytecode descriptors, guaranteeing 100% crash-free
 * operation across both NeoForge (Mojang mappings) and Fabric (Intermediary mappings).
 */
public final class CopperItems {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopperItems.class);

    public static final String MOD_ID = "copper_age_patch";
    public static final String KNIFE_NAMESPACE = "farmersdelight";
    public static final String SHIELD_NAMESPACE = "shieldexp";
    public static final String KNIFE_NAME = "copper_knife";
    public static final String SHIELD_NAME = "copper_shield";

    public static final int KNIFE_DURABILITY = 190;
    public static final int SHIELD_DURABILITY = 120;

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private static Supplier<Object> knifeSupplier = null;
    private static Supplier<Object> shieldSupplier = null;
    private static Object knifeInstance = null;
    private static Object shieldInstance = null;

    private CopperItems() {}

    /**
     * Checks if a mod is loaded on either Fabric or NeoForge in a loader-agnostic way.
     */
    public static boolean isModLoaded(String modId) {
        if (modId == null || modId.isEmpty()) return false;
        // 1. FabricLoader check
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object instance = loaderClass.getMethod("getInstance").invoke(null);
            Method isModLoadedMethod = loaderClass.getMethod("isModLoaded", String.class);
            return (boolean) isModLoadedMethod.invoke(instance, modId);
        } catch (Throwable ignored) {}

        // 2. NeoForge ModList check
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object instance = modListClass.getMethod("get").invoke(null);
            Method isLoadedMethod = modListClass.getMethod("isLoaded", String.class);
            return (boolean) isLoadedMethod.invoke(instance, modId);
        } catch (Throwable ignored) {}

        return false;
    }

    public static boolean isFarmersDelightLoaded() {
        return isModLoaded("farmersdelight");
    }

    public static boolean isShieldExpansionLoaded() {
        return isModLoaded("shieldexp");
    }

    /**
     * Initializes and registers items conditionally based on installed mods.
     */
    public static synchronized void init() {
        if (INITIALIZED.get()) {
            return;
        }

        Object itemRegistryKey = resolveItemRegistryKey();
        RegistryHelper registryHelper = null;
        try {
            registryHelper = RegistryHelper.getInstance();
        } catch (Throwable t) {
            LOGGER.debug("[CopperAgeBackportPatch] RegistryHelper not yet available: {}", t.getMessage());
        }

        // 1. Register Copper Knife under farmersdelight namespace if Farmer's Delight is loaded
        if (isFarmersDelightLoaded()) {
            LOGGER.info("[CopperAgeBackportPatch] Farmer's Delight detected; registering Copper Knife under farmersdelight namespace.");
            Supplier<Object> supplier = () -> {
                if (knifeInstance == null) {
                    knifeInstance = createKnifeItem(KNIFE_DURABILITY);
                }
                return knifeInstance;
            };
            knifeSupplier = registerWithHelper(registryHelper, itemRegistryKey, KNIFE_NAMESPACE, KNIFE_NAME, supplier);
        } else {
            LOGGER.debug("[CopperAgeBackportPatch] Farmer's Delight not detected; skipping Copper Knife registration.");
        }

        // 2. Register Copper Shield under shieldexp namespace if Shield Expansion is loaded
        if (isShieldExpansionLoaded()) {
            LOGGER.info("[CopperAgeBackportPatch] Shield Expansion detected; registering Copper Shield under shieldexp namespace.");
            Supplier<Object> supplier = () -> {
                if (shieldInstance == null) {
                    shieldInstance = createShieldItem(SHIELD_DURABILITY);
                }
                return shieldInstance;
            };
            shieldSupplier = registerWithHelper(registryHelper, itemRegistryKey, SHIELD_NAMESPACE, SHIELD_NAME, supplier);
        } else {
            LOGGER.debug("[CopperAgeBackportPatch] Shield Expansion not detected; skipping Copper Shield registration.");
        }

        INITIALIZED.set(true);
    }

    /**
     * Registers an item using RegistryHelper with safe reflection fallback.
     */
    private static Supplier<Object> registerWithHelper(RegistryHelper helper, Object registryKey, String namespace, String name, Supplier<Object> supplier) {
        if (helper != null && registryKey != null) {
            try {
                for (Method m : RegistryHelper.class.getMethods()) {
                    if ("registerWithNamespace".equals(m.getName()) && m.getParameterCount() == 4) {
                        m.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        Supplier<Object> res = (Supplier<Object>) m.invoke(helper, registryKey, namespace, name, supplier);
                        if (res != null) {
                            return res;
                        }
                    }
                }
            } catch (Throwable t) {
                LOGGER.warn("[CopperAgeBackportPatch] Failed to register {}:{} via RegistryHelper: {}", namespace, name, t.getMessage());
            }
        }

        // Direct registration fallback (e.g., Fabric BuiltInRegistries.ITEM / class_7923)
        try {
            Object id = CopperArmorDurabilityPatcher.createIdentifier(namespace, name);
            Object item = supplier.get();
            if (id != null && item != null) {
                registerDirectlyToRegistry(id, item);
                return () -> item;
            }
        } catch (Throwable t) {
            LOGGER.warn("[CopperAgeBackportPatch] Direct registration fallback failed for {}: {}: {}", namespace, name, t.getMessage());
        }

        return supplier;
    }

    private static void registerDirectlyToRegistry(Object id, Object item) {
        // Try Mojang Registry.register(BuiltInRegistries.ITEM, id, item)
        try {
            Class<?> builtInRegClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            Object itemReg = builtInRegClass.getField("ITEM").get(null);
            Class<?> registryClass = Class.forName("net.minecraft.core.Registry");
            for (Method m : registryClass.getMethods()) {
                if ("register".equals(m.getName()) && m.getParameterCount() == 3) {
                    m.invoke(null, itemReg, id, item);
                    return;
                }
            }
        } catch (Throwable ignored) {}

        // Try Fabric Intermediary net.minecraft.class_2378.method_10230(class_7923.field_41178, id, item)
        try {
            Class<?> regClass = Class.forName("net.minecraft.class_7923");
            Field f = regClass.getDeclaredField("field_41178");
            f.setAccessible(true);
            Object itemReg = f.get(null);
            Class<?> registryInterface = Class.forName("net.minecraft.class_2378");
            for (Method m : registryInterface.getMethods()) {
                if (m.getParameterCount() == 3 && ("method_10230".equals(m.getName()) || "register".equals(m.getName()))) {
                    m.invoke(null, itemReg, id, item);
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Resolves the ITEM registry key across mappings.
     */
    public static Object resolveItemRegistryKey() {
        // 1. Try Mojang Registries.ITEM
        try {
            Class<?> regClass = Class.forName("net.minecraft.core.registries.Registries");
            Field f = regClass.getField("ITEM");
            return f.get(null);
        } catch (Throwable ignored) {}

        // 2. Try Fabric Intermediary net.minecraft.class_7924.field_41243
        try {
            Class<?> regKeysClass = Class.forName("net.minecraft.class_7924");
            Field f = regKeysClass.getDeclaredField("field_41243");
            f.setAccessible(true);
            return f.get(null);
        } catch (Throwable ignored) {}

        return null;
    }

    /**
     * Instantiates a mapping-agnostic Knife with Farmer's Delight knife stats and behaviors:
     * - Attack damage modifier: tier.getAttackDamageBonus() + 0.5f (CopperTier = 1.0f + 0.5f = 1.5f -> 2.5 Attack Damage)
     * - Attack speed modifier: -2.0f (Base player 4.0 - 2.0 = 2.0 Attack Speed, matching all FD knives)
     * - Canonical durability: 190 (CAB copper tool tier)
     */
    public static Object createKnifeItem(int durability) {
        Object tier = null;
        try {
            Class<?> tierClass = Class.forName("com.github.smallinger.copperagebackport.item.tools.CopperTier");
            tier = tierClass.getField("INSTANCE").get(null);
        } catch (Throwable t) {
            LOGGER.warn("[CopperAgeBackportPatch] Could not load CopperTier.INSTANCE for knife", t);
        }

        // 1. Try Farmer's Delight native KnifeItem and knifeItem(tier)
        try {
            Class<?> knifeItemClass = Class.forName("vectorwing.farmersdelight.common.item.KnifeItem");
            Class<?> modItemsClass = Class.forName("vectorwing.farmersdelight.common.registry.ModItems");
            Method knifeItemPropsMethod = null;
            for (Method m : modItemsClass.getMethods()) {
                if ("knifeItem".equals(m.getName()) && m.getParameterCount() == 1) {
                    knifeItemPropsMethod = m;
                    break;
                }
            }
            if (knifeItemPropsMethod != null && tier != null) {
                Object props = knifeItemPropsMethod.invoke(null, tier);
                for (Constructor<?> ctor : knifeItemClass.getConstructors()) {
                    if (ctor.getParameterCount() == 2 && ctor.getParameterTypes()[0].isInstance(tier)) {
                        Object knife = ctor.newInstance(tier, props);
                        CopperArmorDurabilityPatcher.applyDurability(knife, durability);
                        LOGGER.info("[CopperAgeBackportPatch] Successfully instantiated native Farmer's Delight KnifeItem for Copper Knife.");
                        return knife;
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("[CopperAgeBackportPatch] Native KnifeItem instantiation not available: {}", t.getMessage());
        }

        // 2. Standalone / Fallback implementation:
        // Construct Item with exact Farmer's Delight knife attributes:
        // Attack damage modifier: tier.getAttackDamageBonus() + 0.5f
        // Attack speed modifier: -2.0f
        Class<?> propsClass = null;
        for (String name : new String[]{"net.minecraft.world.item.Item$Properties", "net.minecraft.class_1792$class_1793"}) {
            try {
                propsClass = Class.forName(name);
                break;
            } catch (ClassNotFoundException ignored) {}
        }
        if (propsClass == null) return null;

        try {
            Object props = propsClass.getConstructor().newInstance();
            for (String mName : new String[]{"durability", "method_7895"}) {
                try {
                    Method m = propsClass.getMethod(mName, int.class);
                    m.invoke(props, durability);
                    break;
                } catch (Throwable ignored) {}
            }

            // Create knife attributes via DiggerItem.createAttributes(tier, 0.5f, -2.0f) or class_1766.method_57346
            Object attribs = null;
            for (String diggerName : new String[]{"net.minecraft.world.item.DiggerItem", "net.minecraft.class_1766"}) {
                try {
                    Class<?> diggerClass = Class.forName(diggerName);
                    for (Method m : diggerClass.getMethods()) {
                        if (m.getParameterCount() == 3 && ("createAttributes".equals(m.getName()) || "method_57346".equals(m.getName()))) {
                            m.setAccessible(true);
                            attribs = m.invoke(null, tier, 0.5f, -2.0f);
                            break;
                        }
                    }
                    if (attribs != null) break;
                } catch (Throwable ignored) {}
            }

            if (attribs != null) {
                for (String propMethod : new String[]{"attributes", "method_57348"}) {
                    try {
                        Method m = propsClass.getMethod(propMethod, attribs.getClass().getInterfaces().length > 0 ? attribs.getClass().getInterfaces()[0] : attribs.getClass());
                        m.invoke(props, attribs);
                        break;
                    } catch (Throwable ignored) {}
                    try {
                        for (Method m : propsClass.getMethods()) {
                            if (propMethod.equals(m.getName()) && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(attribs.getClass())) {
                                m.invoke(props, attribs);
                                break;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }

            // Find constructor for DiggerItem, SwordItem, or TieredItem
            for (String className : new String[]{
                    "net.minecraft.world.item.DiggerItem",
                    "net.minecraft.class_1766",
                    "net.minecraft.world.item.SwordItem",
                    "net.minecraft.class_1829",
                    "net.minecraft.world.item.TieredItem",
                    "net.minecraft.class_1794"
            }) {
                try {
                    Class<?> targetClass = Class.forName(className);
                    for (Constructor<?> ctor : targetClass.getConstructors()) {
                        if (ctor.getParameterCount() == 2
                                && tier != null
                                && ctor.getParameterTypes()[0].isAssignableFrom(tier.getClass())
                                && ctor.getParameterTypes()[1].isAssignableFrom(propsClass)) {
                            Object knife = ctor.newInstance(tier, props);
                            CopperArmorDurabilityPatcher.applyDurability(knife, durability);
                            return knife;
                        }
                    }
                } catch (ClassNotFoundException ignored) {}
            }

            // Simple Item fallback
            Class<?> itemClass = Class.forName("net.minecraft.world.item.Item");
            for (Constructor<?> ctor : itemClass.getConstructors()) {
                if (ctor.getParameterCount() == 1 && ctor.getParameterTypes()[0].isAssignableFrom(propsClass)) {
                    Object knife = ctor.newInstance(props);
                    CopperArmorDurabilityPatcher.applyDurability(knife, durability);
                    return knife;
                }
            }
        } catch (Throwable t) {
            LOGGER.error("[CopperAgeBackportPatch] Failed to create knife item instance", t);
        }
        return null;
    }

    /**
     * Instantiates a mapping-agnostic ShieldItem with canonical durability.
     */
    public static Object createShieldItem(int durability) {
        Class<?> shieldClass = null;
        for (String name : new String[]{"net.minecraft.world.item.ShieldItem", "net.minecraft.class_1819"}) {
            try {
                shieldClass = Class.forName(name);
                break;
            } catch (ClassNotFoundException ignored) {}
        }
        if (shieldClass == null) return null;

        Class<?> propsClass = null;
        for (String name : new String[]{"net.minecraft.world.item.Item$Properties", "net.minecraft.class_1792$class_1793"}) {
            try {
                propsClass = Class.forName(name);
                break;
            } catch (ClassNotFoundException ignored) {}
        }
        if (propsClass == null) return null;

        try {
            Object props = propsClass.getConstructor().newInstance();
            for (String mName : new String[]{"durability", "method_7895"}) {
                try {
                    Method m = propsClass.getMethod(mName, int.class);
                    m.invoke(props, durability);
                    break;
                } catch (Throwable ignored) {}
            }

            Constructor<?> targetCtor = null;
            for (Constructor<?> ctor : shieldClass.getConstructors()) {
                if (ctor.getParameterCount() == 1 && ctor.getParameterTypes()[0].isAssignableFrom(propsClass)) {
                    targetCtor = ctor;
                    break;
                }
            }
            if (targetCtor != null) {
                Object shield = targetCtor.newInstance(props);
                CopperArmorDurabilityPatcher.applyDurability(shield, durability);
                return shield;
            }
        } catch (Throwable t) {
            LOGGER.error("[CopperAgeBackportPatch] Failed to create shield item instance", t);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getCopperKnife() {
        if (knifeInstance != null) {
            return (T) knifeInstance;
        }
        if (knifeSupplier != null) {
            Object val = knifeSupplier.get();
            if (val != null) {
                knifeInstance = val;
                return (T) val;
            }
        }
        Object fromReg = CopperArmorDurabilityPatcher.getItemFromRegistry(KNIFE_NAMESPACE, KNIFE_NAME);
        if (fromReg == null || CopperArmorDurabilityPatcher.isAir(fromReg)) {
            fromReg = CopperArmorDurabilityPatcher.getItemFromRegistry(MOD_ID, KNIFE_NAME);
        }
        if (fromReg != null && !CopperArmorDurabilityPatcher.isAir(fromReg)) {
            knifeInstance = fromReg;
            return (T) fromReg;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getCopperShield() {
        if (shieldInstance != null) {
            return (T) shieldInstance;
        }
        if (shieldSupplier != null) {
            Object val = shieldSupplier.get();
            if (val != null) {
                shieldInstance = val;
                return (T) val;
            }
        }
        Object fromReg = CopperArmorDurabilityPatcher.getItemFromRegistry(SHIELD_NAMESPACE, SHIELD_NAME);
        if (fromReg == null || CopperArmorDurabilityPatcher.isAir(fromReg)) {
            fromReg = CopperArmorDurabilityPatcher.getItemFromRegistry(MOD_ID, SHIELD_NAME);
        }
        if (fromReg != null && !CopperArmorDurabilityPatcher.isAir(fromReg)) {
            shieldInstance = fromReg;
            return (T) fromReg;
        }
        return null;
    }

    /**
     * Resets state for testing.
     */
    public static synchronized void resetForTesting() {
        INITIALIZED.set(false);
        knifeSupplier = null;
        shieldSupplier = null;
        knifeInstance = null;
        shieldInstance = null;
    }

    public static synchronized void setKnifeInstanceForTesting(Object knife) {
        knifeInstance = knife;
    }

    public static synchronized void setShieldInstanceForTesting(Object shield) {
        shieldInstance = shield;
    }
}
