package com.github.lunarea.copperagepatch.creative;

import com.github.lunarea.copperagepatch.durability.CopperArmorDurabilityPatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Clean-room patcher placing the Copper Golem Spawn Egg into the Creative Mode
 * Spawn Eggs tab after the Cod Spawn Egg and before the Cow Spawn Egg, matching
 * the 1.21.9 canonical ordering on both NeoForge and Fabric loaders.
 *
 * Canonical ordering (alphabetical):
 *   ... -> Cod -> [Copper Golem] -> Cow -> ...
 *
 * Primary strategy:  insertAfter(cod_spawn_egg)  /  addAfter(cod_spawn_egg)
 * Secondary fallback: insertBefore(cow_spawn_egg) / addBefore(cow_spawn_egg)
 * Final fallback: accept / append
 *
 * Mapping-agnostic via reflection and dynamic proxies with ZERO net/minecraft/ class
 * references in method signatures or bytecode descriptors.
 */
public final class CopperSpawnEggTabPatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopperSpawnEggTabPatcher.class);
    private static final AtomicBoolean FABRIC_TAB_REGISTERED = new AtomicBoolean(false);

    /**
     * Creative Mode Spawn Eggs tab key across loaders (CreativeModeTabs.SPAWN_EGGS on NeoForge, ItemGroups.SPAWN_EGGS on Fabric).
     */
    public static final Object SPAWN_EGGS_TAB_KEY = resolveSpawnEggsTabKey();

    private CopperSpawnEggTabPatcher() {}

    /**
     * Resets internal registration state (for unit testing).
     */
    public static synchronized void resetForTesting() {
        FABRIC_TAB_REGISTERED.set(false);
    }

    public static Object resolveSpawnEggsTabKey() {
        // 1. Try Mojang CreativeModeTabs.SPAWN_EGGS
        try {
            Class<?> tabsClass = Class.forName("net.minecraft.world.item.CreativeModeTabs");
            Field f = tabsClass.getDeclaredField("SPAWN_EGGS");
            f.setAccessible(true);
            return f.get(null);
        } catch (Throwable t1) {
            // 2. Try Fabric Intermediary ItemGroups.SPAWN_EGGS (net.minecraft.class_7706.field_40209)
            try {
                Class<?> itemGroupsClass = Class.forName("net.minecraft.class_7706");
                Field f = itemGroupsClass.getDeclaredField("field_40209");
                f.setAccessible(true);
                return f.get(null);
            } catch (Throwable t2) {
                // 3. Fallback: try ResourceKey create with minecraft:spawn_eggs
                try {
                    Object id = CopperArmorDurabilityPatcher.createIdentifier("minecraft", "spawn_eggs");
                    if (id != null) {
                        try {
                            Class<?> regClass = Class.forName("net.minecraft.core.registries.Registries");
                            Object creativeTabReg = regClass.getField("CREATIVE_MODE_TAB").get(null);
                            Class<?> resKeyClass = Class.forName("net.minecraft.resources.ResourceKey");
                            Method createMethod = resKeyClass.getMethod("create", creativeTabReg.getClass(), id.getClass());
                            return createMethod.invoke(null, creativeTabReg, id);
                        } catch (Throwable ignored) {}
                        try {
                            Class<?> resKeyClass = Class.forName("net.minecraft.class_5321");
                            Class<?> regKeysClass = Class.forName("net.minecraft.class_7924");
                            Field tabRegKeyField = regKeysClass.getDeclaredField("field_44688");
                            tabRegKeyField.setAccessible(true);
                            Object tabRegKey = tabRegKeyField.get(null);
                            Method createMethod = resKeyClass.getMethod("method_29179", resKeyClass, id.getClass());
                            return createMethod.invoke(null, tabRegKey, id);
                        } catch (Throwable ignored) {}
                    }
                } catch (Throwable ignored) {}
                return null;
            }
        }
    }

    /**
     * Resolves Copper Golem Spawn Egg item instance across loaders.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCopperGolemSpawnEgg() {
        try {
            Class<?> modItemsClass = Class.forName("com.github.smallinger.copperagebackport.registry.ModItems");
            Field f = modItemsClass.getDeclaredField("COPPER_GOLEM_SPAWN_EGG");
            Object val = f.get(null);
            if (val instanceof Supplier<?> supplier) {
                val = supplier.get();
            }
            if (val != null && !isAir(val)) {
                return (T) val;
            }
        } catch (Throwable ignored) {}

        Object egg = getItemFromRegistry("minecraft", "copper_golem_spawn_egg");
        if (egg != null && !isAir(egg)) {
            return (T) egg;
        }
        egg = getItemFromRegistry("copperagebackport", "copper_golem_spawn_egg");
        if (egg != null && !isAir(egg)) {
            return (T) egg;
        }
        return null;
    }

    /**
     * Resolves Cod Spawn Egg item instance across loaders (primary anchor: insert after this).
     * Intermediary: net.minecraft.class_1802.field_17350
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCodSpawnEgg() {
        try {
            Class<?> itemsClass = Class.forName("net.minecraft.world.item.Items");
            Object egg = itemsClass.getField("COD_SPAWN_EGG").get(null);
            if (egg != null && !isAir(egg)) return (T) egg;
        } catch (Throwable ignored) {}

        try {
            Class<?> itemsClass = Class.forName("net.minecraft.class_1802");
            Field f = itemsClass.getDeclaredField("field_17350");
            f.setAccessible(true);
            Object egg = f.get(null);
            if (egg != null && !isAir(egg)) return (T) egg;
        } catch (Throwable ignored) {}

        return (T) getItemFromRegistry("minecraft", "cod_spawn_egg");
    }

    /**
     * Resolves Cow Spawn Egg item instance across loaders (secondary anchor: insert before this).
     * Intermediary: net.minecraft.class_1802.field_8250
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCowSpawnEgg() {
        try {
            Class<?> itemsClass = Class.forName("net.minecraft.world.item.Items");
            Object egg = itemsClass.getField("COW_SPAWN_EGG").get(null);
            if (egg != null && !isAir(egg)) return (T) egg;
        } catch (Throwable ignored) {}

        try {
            Class<?> itemsClass = Class.forName("net.minecraft.class_1802");
            Field f = itemsClass.getDeclaredField("field_8250");
            f.setAccessible(true);
            Object egg = f.get(null);
            if (egg != null && !isAir(egg)) return (T) egg;
        } catch (Throwable ignored) {}

        return (T) getItemFromRegistry("minecraft", "cow_spawn_egg");
    }

    /**
     * Obtains an ItemStack for an item across mappings.
     */
    public static Object getDefaultInstance(Object item) {
        if (item == null) return null;
        for (String mName : new String[]{"getDefaultInstance", "method_7854"}) {
            try {
                Method m = item.getClass().getMethod(mName);
                m.setAccessible(true);
                return m.invoke(item);
            } catch (Throwable ignored) {}
        }
        for (String className : new String[]{"net.minecraft.world.item.ItemStack", "net.minecraft.class_1799"}) {
            try {
                Class<?> stackClass = Class.forName(className);
                for (Constructor<?> ctor : stackClass.getConstructors()) {
                    if (ctor.getParameterCount() == 1 && ctor.getParameterTypes()[0].isAssignableFrom(item.getClass())) {
                        ctor.setAccessible(true);
                        return ctor.newInstance(item);
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    /**
     * NeoForge hook: handles BuildCreativeModeTabContentsEvent for CreativeModeTabs.SPAWN_EGGS.
     */
    public static boolean applyNeoForgeSpawnEggTabPlacement(Object event) {
        if (event == null) {
            return false;
        }
        try {
            Method getTabKeyMethod = event.getClass().getMethod("getTabKey");
            getTabKeyMethod.setAccessible(true);
            Object tabKey = getTabKeyMethod.invoke(event);
            if (!isSpawnEggsTab(tabKey)) {
                return false;
            }

            Object copperEgg = getCopperGolemSpawnEgg();
            if (copperEgg == null) {
                LOGGER.warn("[CopperAgeBackportPatch] Copper Golem spawn egg not found for NeoForge tab placement.");
                return false;
            }

            // Avoid duplicate insertion
            try {
                for (String methodName : new String[]{"getParentEntries", "getSearchEntries"}) {
                    try {
                        Method entriesMethod = event.getClass().getMethod(methodName);
                        Object entriesSet = entriesMethod.invoke(event);
                        if (entriesSet instanceof java.util.Collection<?> col) {
                            for (Object itemObj : col) {
                                if (isSameItem(itemObj, copperEgg)) {
                                    LOGGER.debug("[CopperAgeBackportPatch] Copper Golem spawn egg already present in NeoForge Spawn Eggs tab via {}.", methodName);
                                    return true;
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            Object copperStack = getDefaultInstance(copperEgg);
            if (copperStack == null) {
                return false;
            }

            Object parentAndSearch = resolveTabVisibility("PARENT_AND_SEARCH_TABS");

            // Primary strategy: insertAfter Cod spawn egg
            Object codEgg = getCodSpawnEgg();
            Object codStack = codEgg != null ? getDefaultInstance(codEgg) : null;

            if (codStack != null) {
                if (tryNeoForgeInsert(event, "insertAfter", codStack, copperStack, parentAndSearch)) {
                    LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Golem spawn egg after Cod spawn egg in NeoForge Spawn Eggs tab.");
                    return true;
                }
            }

            // Fallback 1: insertBefore Cow spawn egg
            Object cowEgg = getCowSpawnEgg();
            Object cowStack = cowEgg != null ? getDefaultInstance(cowEgg) : null;
            if (cowStack != null) {
                if (tryNeoForgeInsert(event, "insertBefore", cowStack, copperStack, parentAndSearch)) {
                    LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Golem spawn egg before Cow spawn egg in NeoForge Spawn Eggs tab (fallback).");
                    return true;
                }
            }

            // Fallback 2: accept / append
            for (Method m : event.getClass().getMethods()) {
                if ("accept".equals(m.getName())) {
                    try {
                        if (m.getParameterCount() == 2 && parentAndSearch != null
                                && m.getParameterTypes()[1].isAssignableFrom(parentAndSearch.getClass())) {
                            m.invoke(event, copperStack, parentAndSearch);
                            LOGGER.info("[CopperAgeBackportPatch] Appended Copper Golem spawn egg to NeoForge Spawn Eggs tab (fallback, visibility).");
                            return true;
                        } else if (m.getParameterCount() == 1) {
                            m.invoke(event, copperStack);
                            LOGGER.info("[CopperAgeBackportPatch] Appended Copper Golem spawn egg to NeoForge Spawn Eggs tab (fallback, no-visibility).");
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            LOGGER.error("[CopperAgeBackportPatch] Failed to process NeoForge Spawn Eggs tab placement", t);
        }
        return false;
    }

    /**
     * Attempts a 3-param (anchor, stack, visibility) insert method, falling back through all
     * enum constants if parentAndSearch is null, and finally trying a 2-param variant.
     */
    private static boolean tryNeoForgeInsert(Object event, String methodName, Object anchorStack, Object copperStack, Object parentAndSearch) {
        Method method = findMethod(event.getClass(), methodName, 3);
        if (method == null) return false;
        method.setAccessible(true);

        // 1. Try with resolved parentAndSearch (may be null)
        if (parentAndSearch != null) {
            try {
                method.invoke(event, anchorStack, copperStack, parentAndSearch);
                return true;
            } catch (Throwable t) {
                Throwable cause = (t instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null) ? ite.getCause() : t;
                if (cause instanceof IllegalArgumentException && cause.getMessage() != null && cause.getMessage().contains("already exists")) {
                    return true;
                }
            }
        }

        // 2. Enumerate all TabVisibility enum constants and try each
        Class<?> visibilityParamType = method.getParameterTypes()[2];
        if (visibilityParamType.isEnum()) {
            for (Object constant : visibilityParamType.getEnumConstants()) {
                try {
                    method.invoke(event, anchorStack, copperStack, constant);
                    return true;
                } catch (Throwable t) {
                    Throwable cause = (t instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null) ? ite.getCause() : t;
                    if (cause instanceof IllegalArgumentException && cause.getMessage() != null && cause.getMessage().contains("already exists")) {
                        return true;
                    }
                }
            }
        }

        // 3. Try 2-param variant
        Method method2 = findMethod(event.getClass(), methodName, 2);
        if (method2 != null) {
            method2.setAccessible(true);
            try {
                method2.invoke(event, anchorStack, copperStack);
                return true;
            } catch (Throwable ignored) {}
        }

        return false;
    }




    /**
     * Fabric hook: registers with Fabric API ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).
     */
    public static void registerFabricSpawnEggTab() {
        if (FABRIC_TAB_REGISTERED.get()) {
            return;
        }
        try {
            Class<?> itemGroupEventsClass = Class.forName("net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents");

            Method modifyEventMethod = null;
            for (Method m : itemGroupEventsClass.getMethods()) {
                if ("modifyEntriesEvent".equals(m.getName()) && m.getParameterCount() == 1) {
                    modifyEventMethod = m;
                    break;
                }
            }
            if (modifyEventMethod == null) {
                LOGGER.warn("[CopperAgeBackportPatch] Could not find ItemGroupEvents.modifyEntriesEvent method.");
                return;
            }

            Object tabKey = SPAWN_EGGS_TAB_KEY != null ? SPAWN_EGGS_TAB_KEY : resolveSpawnEggsTabKey();
            if (tabKey == null) {
                LOGGER.warn("[CopperAgeBackportPatch] Spawn Eggs tab key could not be resolved for Fabric.");
                return;
            }

            modifyEventMethod.setAccessible(true);
            Object event = modifyEventMethod.invoke(null, tabKey);
            Class<?> modifyEntriesInterface = Class.forName("net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents$ModifyEntries");

            Object listener = Proxy.newProxyInstance(
                    CopperSpawnEggTabPatcher.class.getClassLoader(),
                    new Class<?>[]{modifyEntriesInterface},
                    (proxy, method, args) -> {
                        if ("modifyEntries".equals(method.getName()) && args != null && args.length == 1) {
                            applyFabricSpawnEggTabPlacement(args[0]);
                        }
                        return null;
                    }
            );

            Method registerMethod = null;
            for (Method m : event.getClass().getMethods()) {
                if ("register".equals(m.getName()) && m.getParameterCount() == 1) {
                    registerMethod = m;
                    break;
                }
            }
            if (registerMethod != null) {
                registerMethod.setAccessible(true);
                registerMethod.invoke(event, listener);
                FABRIC_TAB_REGISTERED.set(true);
                LOGGER.info("[CopperAgeBackportPatch] Successfully registered Copper Golem spawn egg in Fabric Spawn Eggs tab via ItemGroupEvents.");
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            LOGGER.debug("[CopperAgeBackportPatch] Fabric ItemGroupEvents not found on classpath.");
        } catch (Throwable t) {
            LOGGER.warn("[CopperAgeBackportPatch] Could not register Fabric Spawn Eggs tab via ItemGroupEvents: {}", t.getMessage());
        }
    }

    /**
     * Inserts Copper Golem Spawn Egg before Iron Golem Spawn Egg on FabricItemGroupEntries.
     */
    public static boolean applyFabricSpawnEggTabPlacement(Object entries) {
        if (entries == null) {
            return false;
        }
        Object copperEgg = getCopperGolemSpawnEgg();
        if (copperEgg == null) {
            LOGGER.warn("[CopperAgeBackportPatch] Copper Golem spawn egg not found for Fabric tab placement.");
            return false;
        }

        Object copperStack = getDefaultInstance(copperEgg);

        // Check if already in displayStacks to avoid duplicate insertion
        try {
            Method getDisplayStacksMethod = entries.getClass().getMethod("getDisplayStacks");
            Object list = getDisplayStacksMethod.invoke(entries);
            if (list instanceof java.util.Collection<?> stackList) {
                for (Object itemObj : stackList) {
                    if (isSameItem(itemObj, copperEgg)) {
                        LOGGER.debug("[CopperAgeBackportPatch] Copper Golem spawn egg already present in Fabric Spawn Eggs tab.");
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Primary strategy: addAfter Cod spawn egg
        Object codEgg = getCodSpawnEgg();
        if (codEgg != null && tryFabricAddAfter(entries, codEgg, copperEgg, copperStack)) {
            LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Golem spawn egg after Cod spawn egg in FabricItemGroupEntries.");
            return true;
        }

        // Fallback 1: addBefore Cow spawn egg
        Object cowEgg = getCowSpawnEgg();
        if (cowEgg != null && tryFabricAddBefore(entries, cowEgg, copperEgg, copperStack)) {
            LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Golem spawn egg before Cow spawn egg in FabricItemGroupEntries (fallback).");
            return true;
        }

        // Fallback 2: prepend
        try {
            for (Method m : entries.getClass().getMethods()) {
                if ("prepend".equals(m.getName()) && m.getParameterCount() == 1) {
                    Class<?> pType = m.getParameterTypes()[0];
                    if (copperStack != null && pType.isAssignableFrom(copperStack.getClass())) {
                        m.invoke(entries, copperStack);
                        LOGGER.info("[CopperAgeBackportPatch] Prepended Copper Golem spawn egg to Fabric tab (fallback stack).");
                        return true;
                    } else if (pType.isAssignableFrom(copperEgg.getClass())) {
                        m.invoke(entries, copperEgg);
                        LOGGER.info("[CopperAgeBackportPatch] Prepended Copper Golem spawn egg to Fabric tab (fallback item).");
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Fallback 3: displayStacks.add
        try {
            Method getDisplayStacksMethod = entries.getClass().getMethod("getDisplayStacks");
            Object list = getDisplayStacksMethod.invoke(entries);
            if (list instanceof java.util.List stackList) {
                stackList.add(copperStack != null ? copperStack : copperEgg);
                LOGGER.info("[CopperAgeBackportPatch] Appended Copper Golem spawn egg to Fabric displayStacks (fallback).");
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }


    private static boolean tryFabricAddBefore(Object entries, Object targetItem, Object copperEgg, Object copperStack) {
        Object targetStack = getDefaultInstance(targetItem);

        // 1. Array overloads of addBefore
        for (Method m : entries.getClass().getMethods()) {
            if ("addBefore".equals(m.getName()) && m.getParameterCount() == 2 && m.getParameterTypes()[1].isArray()) {
                Class<?> firstParam = m.getParameterTypes()[0];
                Class<?> compType = m.getParameterTypes()[1].getComponentType();
                m.setAccessible(true);

                if (firstParam.isAssignableFrom(targetItem.getClass()) && compType.isAssignableFrom(copperEgg.getClass())) {
                    Object itemArray = Array.newInstance(compType, 1);
                    Array.set(itemArray, 0, copperEgg);
                    try {
                        m.invoke(entries, targetItem, itemArray);
                        return true;
                    } catch (Throwable ignored) {}
                }
                if (targetStack != null && copperStack != null && firstParam.isAssignableFrom(targetStack.getClass()) && compType.isAssignableFrom(copperStack.getClass())) {
                    Object stackArray = Array.newInstance(compType, 1);
                    Array.set(stackArray, 0, copperStack);
                    try {
                        m.invoke(entries, targetStack, stackArray);
                        return true;
                    } catch (Throwable ignored) {}
                }
                if (copperStack != null && firstParam.isAssignableFrom(targetItem.getClass()) && compType.isAssignableFrom(copperStack.getClass())) {
                    Object stackArray = Array.newInstance(compType, 1);
                    Array.set(stackArray, 0, copperStack);
                    try {
                        m.invoke(entries, targetItem, stackArray);
                        return true;
                    } catch (Throwable ignored) {}
                }
                if (targetStack != null && firstParam.isAssignableFrom(targetStack.getClass()) && compType.isAssignableFrom(copperEgg.getClass())) {
                    Object itemArray = Array.newInstance(compType, 1);
                    Array.set(itemArray, 0, copperEgg);
                    try {
                        m.invoke(entries, targetStack, itemArray);
                        return true;
                    } catch (Throwable ignored) {}
                }
            }
        }

        // 2. Collection overloads of addBefore
        for (Method m : entries.getClass().getMethods()) {
            if ("addBefore".equals(m.getName()) && m.getParameterCount() == 2 && java.util.Collection.class.isAssignableFrom(m.getParameterTypes()[1])) {
                Class<?> firstParam = m.getParameterTypes()[0];
                m.setAccessible(true);
                Object toInsert = copperStack != null ? copperStack : copperEgg;
                if (firstParam.isAssignableFrom(targetItem.getClass())) {
                    try {
                        m.invoke(entries, targetItem, Collections.singletonList(toInsert));
                        return true;
                    } catch (Throwable ignored) {}
                } else if (targetStack != null && firstParam.isAssignableFrom(targetStack.getClass())) {
                    try {
                        m.invoke(entries, targetStack, Collections.singletonList(toInsert));
                        return true;
                    } catch (Throwable ignored) {}
                }
            }
        }
        return false;
    }

    private static boolean tryFabricAddAfter(Object entries, Object targetItem, Object copperEgg, Object copperStack) {
        Object targetStack = getDefaultInstance(targetItem);

        // 1. Array overloads of addAfter
        for (Method m : entries.getClass().getMethods()) {
            if ("addAfter".equals(m.getName()) && m.getParameterCount() == 2 && m.getParameterTypes()[1].isArray()) {
                Class<?> firstParam = m.getParameterTypes()[0];
                Class<?> compType = m.getParameterTypes()[1].getComponentType();
                m.setAccessible(true);

                if (firstParam.isAssignableFrom(targetItem.getClass()) && compType.isAssignableFrom(copperEgg.getClass())) {
                    Object itemArray = Array.newInstance(compType, 1);
                    Array.set(itemArray, 0, copperEgg);
                    try {
                        m.invoke(entries, targetItem, itemArray);
                        return true;
                    } catch (Throwable ignored) {}
                }
                if (targetStack != null && copperStack != null && firstParam.isAssignableFrom(targetStack.getClass()) && compType.isAssignableFrom(copperStack.getClass())) {
                    Object stackArray = Array.newInstance(compType, 1);
                    Array.set(stackArray, 0, copperStack);
                    try {
                        m.invoke(entries, targetStack, stackArray);
                        return true;
                    } catch (Throwable ignored) {}
                }
                if (copperStack != null && firstParam.isAssignableFrom(targetItem.getClass()) && compType.isAssignableFrom(copperStack.getClass())) {
                    Object stackArray = Array.newInstance(compType, 1);
                    Array.set(stackArray, 0, copperStack);
                    try {
                        m.invoke(entries, targetItem, stackArray);
                        return true;
                    } catch (Throwable ignored) {}
                }
                if (targetStack != null && firstParam.isAssignableFrom(targetStack.getClass()) && compType.isAssignableFrom(copperEgg.getClass())) {
                    Object itemArray = Array.newInstance(compType, 1);
                    Array.set(itemArray, 0, copperEgg);
                    try {
                        m.invoke(entries, targetStack, itemArray);
                        return true;
                    } catch (Throwable ignored) {}
                }
            }
        }

        // 2. Collection overloads of addAfter
        for (Method m : entries.getClass().getMethods()) {
            if ("addAfter".equals(m.getName()) && m.getParameterCount() == 2 && java.util.Collection.class.isAssignableFrom(m.getParameterTypes()[1])) {
                Class<?> firstParam = m.getParameterTypes()[0];
                m.setAccessible(true);
                Object toInsert = copperStack != null ? copperStack : copperEgg;
                if (firstParam.isAssignableFrom(targetItem.getClass())) {
                    try {
                        m.invoke(entries, targetItem, Collections.singletonList(toInsert));
                        return true;
                    } catch (Throwable ignored) {}
                } else if (targetStack != null && firstParam.isAssignableFrom(targetStack.getClass())) {
                    try {
                        m.invoke(entries, targetStack, Collections.singletonList(toInsert));
                        return true;
                    } catch (Throwable ignored) {}
                }
            }
        }
        return false;
    }

    private static Method findMethod(Class<?> clazz, String name, int paramCount) {
        for (Method m : clazz.getMethods()) {
            if (name.equals(m.getName()) && m.getParameterCount() == paramCount) {
                return m;
            }
        }
        return null;
    }

    public static boolean isSameItem(Object stackObj, Object itemObj) {
        if (stackObj == null || itemObj == null) return false;
        if (stackObj == itemObj) return true;
        for (String mName : new String[]{"getItem", "method_7909"}) {
            try {
                Method m = stackObj.getClass().getMethod(mName);
                Object item = m.invoke(stackObj);
                if (item == itemObj) return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private static boolean isSpawnEggsTab(Object tabKey) {
        if (tabKey == null) return false;
        if (SPAWN_EGGS_TAB_KEY != null && SPAWN_EGGS_TAB_KEY.equals(tabKey)) return true;
        String s = tabKey.toString().toLowerCase();
        return s.contains("spawn_eggs");
    }

    private static Object resolveTabVisibility(String name) {
        for (String className : new String[]{
                "net.minecraft.world.item.CreativeModeTab$TabVisibility",
                "net.minecraft.class_1761$class_7705"
        }) {
            try {
                Class<?> cls = Class.forName(className);
                if (cls.isEnum()) {
                    for (Object constant : cls.getEnumConstants()) {
                        if (((Enum<?>) constant).name().equals(name)) {
                            return constant;
                        }
                    }
                    return cls.getEnumConstants()[0];
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static Object getItemFromRegistry(String namespace, String path) {
        return CopperArmorDurabilityPatcher.getItemFromRegistry(namespace, path);
    }

    public static boolean isAir(Object item) {
        return CopperArmorDurabilityPatcher.isAir(item);
    }
}
