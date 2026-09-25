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
 * Clean-room patcher restoring Copper Axe to the Creative Mode Combat tab
 * immediately after the Stone Axe on both NeoForge and Fabric loaders.
 *
 * In vanilla Minecraft, all axes appear in both the Tools & Utilities and Combat tabs.
 * Upstream Copper Age Backport only added Copper Axe to Tools & Utilities, leaving it
 * absent from Combat.
 *
 * This patcher places Copper Axe directly after Stone Axe, maintaining natural tier
 * progression: Wooden -> Stone -> Copper -> Iron.
 *
 * Implemented completely mapping-agnostic via reflection and proxies with ZERO net/minecraft/
 * class dependencies in bytecode, guaranteeing 100% crash-free operation across both
 * NeoForge (Mojang mappings) and Fabric (Intermediary mappings).
 */
public final class CopperCombatTabPatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopperCombatTabPatcher.class);
    private static final AtomicBoolean FABRIC_TAB_REGISTERED = new AtomicBoolean(false);

    /**
     * Creative Mode Combat tab key across loaders (CreativeModeTabs.COMBAT on NeoForge, ItemGroups.COMBAT on Fabric).
     */
    public static final Object COMBAT_TAB_KEY = resolveCombatTabKey();

    /**
     * Creative Mode Tools & Utilities tab key across loaders.
     */
    public static final Object TOOLS_TAB_KEY = resolveToolsTabKey();

    private CopperCombatTabPatcher() {}

    public static Object resolveTabKey(String namespace, String path) {
        try {
            Object id = CopperArmorDurabilityPatcher.createIdentifier(namespace, path);
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

    public static Object resolveCombatTabKey() {
        // 1. Try Mojang CreativeModeTabs.COMBAT
        try {
            Class<?> tabsClass = Class.forName("net.minecraft.world.item.CreativeModeTabs");
            return tabsClass.getField("COMBAT").get(null);
        } catch (Throwable t1) {
            // 2. Try Fabric Intermediary ItemGroups.COMBAT (net.minecraft.class_7706.field_40202)
            try {
                Class<?> itemGroupsClass = Class.forName("net.minecraft.class_7706");
                Field f = itemGroupsClass.getDeclaredField("field_40202");
                f.setAccessible(true);
                return f.get(null);
            } catch (Throwable t2) {
                return resolveTabKey("minecraft", "combat");
            }
        }
    }

    public static Object resolveToolsTabKey() {
        try {
            Class<?> tabsClass = Class.forName("net.minecraft.world.item.CreativeModeTabs");
            return tabsClass.getField("TOOLS_AND_UTILITIES").get(null);
        } catch (Throwable t1) {
            try {
                Class<?> itemGroupsClass = Class.forName("net.minecraft.class_7706");
                for (String fName : new String[]{"field_40201", "field_40200"}) {
                    try {
                        Field f = itemGroupsClass.getDeclaredField(fName);
                        f.setAccessible(true);
                        return f.get(null);
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            return resolveTabKey("minecraft", "tools_and_utilities");
        }
    }

    /**
     * Resolves the Copper Axe item instance from ModItems or BuiltInRegistries.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCopperAxe() {
        try {
            Class<?> modItemsClass = Class.forName("com.github.smallinger.copperagebackport.registry.ModItems");
            Field f = modItemsClass.getDeclaredField("COPPER_AXE");
            Object val = f.get(null);
            if (val instanceof Supplier<?> supplier) {
                val = supplier.get();
            }
            if (val != null && !isAir(val)) {
                return (T) val;
            }
        } catch (Throwable ignored) {}

        Object axe = getItemFromRegistry("minecraft", "copper_axe");
        if (axe != null && !isAir(axe)) {
            return (T) axe;
        }
        axe = getItemFromRegistry("copperagebackport", "copper_axe");
        if (axe != null && !isAir(axe)) {
            return (T) axe;
        }
        return null;
    }

    /**
     * Resolves the Stone Axe item instance across mappings.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getStoneAxe() {
        try {
            Class<?> itemsClass = Class.forName("net.minecraft.world.item.Items");
            Object axe = itemsClass.getField("STONE_AXE").get(null);
            if (axe != null && !isAir(axe)) return (T) axe;
        } catch (Throwable ignored) {}

        try {
            Class<?> itemsClass = Class.forName("net.minecraft.class_1802");
            Field f = itemsClass.getDeclaredField("field_8062");
            f.setAccessible(true);
            Object axe = f.get(null);
            if (axe != null && !isAir(axe)) return (T) axe;
        } catch (Throwable ignored) {}

        return (T) getItemFromRegistry("minecraft", "stone_axe");
    }

    /**
     * Resolves the Iron Axe item instance across mappings.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getIronAxe() {
        try {
            Class<?> itemsClass = Class.forName("net.minecraft.world.item.Items");
            Object axe = itemsClass.getField("IRON_AXE").get(null);
            if (axe != null && !isAir(axe)) return (T) axe;
        } catch (Throwable ignored) {}

        try {
            Class<?> itemsClass = Class.forName("net.minecraft.class_1802");
            Field f = itemsClass.getDeclaredField("field_8475");
            f.setAccessible(true);
            Object axe = f.get(null);
            if (axe != null && !isAir(axe)) return (T) axe;
        } catch (Throwable ignored) {}

        return (T) getItemFromRegistry("minecraft", "iron_axe");
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
     * NeoForge hook: handles BuildCreativeModeTabContentsEvent for CreativeModeTabs.COMBAT.
     */
    public static boolean applyNeoForgeCombatTabPlacement(Object event) {
        if (event == null) {
            return false;
        }
        try {
            Method getTabKeyMethod = event.getClass().getMethod("getTabKey");
            getTabKeyMethod.setAccessible(true);
            Object tabKey = getTabKeyMethod.invoke(event);
            if (isCombatTab(tabKey)) {
                Object copperAxe = getCopperAxe();
                Object stoneAxe = getStoneAxe();
                if (copperAxe != null) {
                    if (!insertItemIntoNeoForgeTab(event, stoneAxe, copperAxe, true)) {
                        Object ironAxe = getIronAxe();
                        if (!insertItemIntoNeoForgeTab(event, ironAxe, copperAxe, false)) {
                            insertItemIntoNeoForgeTab(event, null, copperAxe, true);
                        }
                    }
                }

                // Copper Shield (when shieldexp is present)
                Object copperShield = com.github.lunarea.copperagepatch.item.CopperItems.getCopperShield();
                if (copperShield != null) {
                    Object woodenShield = getItemFromRegistry("shieldexp", "wooden_shield");
                    boolean placed = false;
                    if (woodenShield != null) {
                        placed = insertItemIntoNeoForgeTab(event, woodenShield, copperShield, true);
                    }
                    if (!placed) {
                        Object ironShield = getItemFromRegistry("shieldexp", "iron_shield");
                        if (ironShield != null) {
                            placed = insertItemIntoNeoForgeTab(event, ironShield, copperShield, false);
                        }
                    }
                    if (!placed) {
                        Object vanillaShield = getItemFromRegistry("minecraft", "shield");
                        if (vanillaShield != null) {
                            placed = insertItemIntoNeoForgeTab(event, vanillaShield, copperShield, true);
                        }
                    }
                    if (!placed) {
                        insertItemIntoNeoForgeTab(event, null, copperShield, true);
                    }
                }

                return true;
            } else {
                String s = tabKey.toString().toLowerCase();
                if (s.contains("farmersdelight")) {
                    Object copperKnife = com.github.lunarea.copperagepatch.item.CopperItems.getCopperKnife();
                    if (copperKnife != null) {
                        Object flintKnife = getItemFromRegistry("farmersdelight", "flint_knife");
                        if (flintKnife != null && insertItemIntoNeoForgeTab(event, flintKnife, copperKnife, true)) {
                            return true;
                        }
                        Object ironKnife = getItemFromRegistry("farmersdelight", "iron_knife");
                        if (ironKnife != null && insertItemIntoNeoForgeTab(event, ironKnife, copperKnife, false)) {
                            return true;
                        }
                        insertItemIntoNeoForgeTab(event, null, copperKnife, true);
                        return true;
                    }
                }
                if (s.contains("shieldexp")) {
                    Object copperShield = com.github.lunarea.copperagepatch.item.CopperItems.getCopperShield();
                    if (copperShield != null) {
                        insertItemIntoNeoForgeTab(event, null, copperShield, true);
                        return true;
                    }
                }
            }
            return false;
        } catch (Throwable t) {
            LOGGER.error("[CopperAgeBackportPatch] Failed to process NeoForge tab placement", t);
        }
        return false;
    }

    /**
     * Fabric hook: registers with Fabric API ItemGroupEvents.modifyEntriesEvent.
     * Uses dynamic reflection and proxy to ensure zero classloader or mapping errors.
     */
    public static void registerFabricCombatTab() {
        if (FABRIC_TAB_REGISTERED.getAndSet(true)) {
            return;
        }
        registerFabricTabListener(COMBAT_TAB_KEY != null ? COMBAT_TAB_KEY : resolveCombatTabKey(), CopperCombatTabPatcher::applyFabricCombatTabPlacement);

        // Farmer's Delight tab: place Copper Knife after Flint Knife and before Iron Knife
        Object fdTabKey = resolveTabKey("farmersdelight", "farmersdelight");
        if (fdTabKey != null) {
            registerFabricTabListener(fdTabKey, entries -> {
                Object copperKnife = com.github.lunarea.copperagepatch.item.CopperItems.getCopperKnife();
                if (copperKnife != null) {
                    Object flintKnife = getItemFromRegistry("farmersdelight", "flint_knife");
                    if (flintKnife != null && insertItemIntoFabricTab(entries, flintKnife, copperKnife, true)) {
                        return;
                    }
                    Object ironKnife = getItemFromRegistry("farmersdelight", "iron_knife");
                    if (ironKnife != null && insertItemIntoFabricTab(entries, ironKnife, copperKnife, false)) {
                        return;
                    }
                    insertItemIntoFabricTab(entries, null, copperKnife, true);
                }
            });
        }

        // Optional Shield Expansion custom tab
        for (String path : new String[]{"shield_tab", "shields", "tab"}) {
            Object shieldTabKey = resolveTabKey("shieldexp", path);
            if (shieldTabKey != null) {
                registerFabricTabListener(shieldTabKey, entries -> {
                    Object copperShield = com.github.lunarea.copperagepatch.item.CopperItems.getCopperShield();
                    if (copperShield != null) {
                        insertItemIntoFabricTab(entries, null, copperShield, true);
                    }
                });
                break;
            }
        }
    }

    private static void registerFabricTabListener(Object tabKey, java.util.function.Consumer<Object> consumer) {
        if (tabKey == null || consumer == null) {
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

            modifyEventMethod.setAccessible(true);
            Object event = modifyEventMethod.invoke(null, tabKey);
            Class<?> modifyEntriesInterface = Class.forName("net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents$ModifyEntries");

            Object listener = Proxy.newProxyInstance(
                    CopperCombatTabPatcher.class.getClassLoader(),
                    new Class<?>[]{modifyEntriesInterface},
                    (proxy, method, args) -> {
                        if ("modifyEntries".equals(method.getName()) && args != null && args.length == 1) {
                            consumer.accept(args[0]);
                        }
                        return null;
                    }
            );

            boolean registeredPhased = false;
            try {
                Object afterPhase = CopperArmorDurabilityPatcher.createIdentifier("copper_age_patch", "after");
                Field defaultPhaseField = null;
                for (Field f : event.getClass().getFields()) {
                    if ("DEFAULT_PHASE".equals(f.getName())) {
                        defaultPhaseField = f;
                        break;
                    }
                }
                if (defaultPhaseField == null && event.getClass().getSuperclass() != null) {
                    for (Field f : event.getClass().getSuperclass().getFields()) {
                        if ("DEFAULT_PHASE".equals(f.getName())) {
                            defaultPhaseField = f;
                            break;
                        }
                    }
                }
                if (defaultPhaseField != null && afterPhase != null) {
                    Object defaultPhase = defaultPhaseField.get(null);
                    Method addPhaseOrderingMethod = null;
                    for (Method m : event.getClass().getMethods()) {
                        if ("addPhaseOrdering".equals(m.getName()) && m.getParameterCount() == 2) {
                            addPhaseOrderingMethod = m;
                            break;
                        }
                    }
                    if (addPhaseOrderingMethod != null) {
                        addPhaseOrderingMethod.invoke(event, defaultPhase, afterPhase);
                    }
                    for (Method m : event.getClass().getMethods()) {
                        if ("register".equals(m.getName()) && m.getParameterCount() == 2
                                && m.getParameterTypes()[0].isAssignableFrom(afterPhase.getClass())) {
                            m.invoke(event, afterPhase, listener);
                            registeredPhased = true;
                            LOGGER.info("[CopperAgeBackportPatch] Successfully registered phased tab listener for tab: {}", tabKey);
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                LOGGER.debug("[CopperAgeBackportPatch] Phased registration failed, falling back: {}", t.getMessage());
            }

            if (!registeredPhased) {
                for (Method m : event.getClass().getMethods()) {
                    if ("register".equals(m.getName()) && m.getParameterCount() == 1) {
                        m.setAccessible(true);
                        m.invoke(event, listener);
                        LOGGER.info("[CopperAgeBackportPatch] Successfully registered tab listener for tab: {}", tabKey);
                        break;
                    }
                }
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            LOGGER.debug("[CopperAgeBackportPatch] Fabric ItemGroupEvents not found on classpath (running on NeoForge or vanilla Fabric).");
        } catch (Throwable t) {
            LOGGER.warn("[CopperAgeBackportPatch] Could not register Fabric tab listener for {}: {}", tabKey, t.getMessage());
        }
    }

    /**
     * Inserts Copper Axe and Copper Shield into Fabric Combat tab.
     */
    public static boolean applyFabricCombatTabPlacement(Object entries) {
        if (entries == null) {
            return false;
        }
        boolean anyPlaced = false;

        // 1. Copper Axe after Stone Axe (or before Iron Axe)
        Object copperAxe = getCopperAxe();
        Object stoneAxe = getStoneAxe();
        if (copperAxe != null) {
            if (insertItemIntoFabricTab(entries, stoneAxe, copperAxe, true)) {
                anyPlaced = true;
            } else {
                Object ironAxe = getIronAxe();
                if (insertItemIntoFabricTab(entries, ironAxe, copperAxe, false)) {
                    anyPlaced = true;
                }
            }
        }

        // 2. Copper Shield after Wooden Shield and before Iron Shield (when shieldexp is present)
        Object copperShield = com.github.lunarea.copperagepatch.item.CopperItems.getCopperShield();
        if (copperShield != null) {
            Object woodenShield = getItemFromRegistry("shieldexp", "wooden_shield");
            boolean placed = false;
            if (woodenShield != null) {
                placed = insertItemIntoFabricTab(entries, woodenShield, copperShield, true);
            }
            if (!placed) {
                Object ironShield = getItemFromRegistry("shieldexp", "iron_shield");
                if (ironShield != null) {
                    placed = insertItemIntoFabricTab(entries, ironShield, copperShield, false);
                }
            }
            if (!placed) {
                Object vanillaShield = getItemFromRegistry("minecraft", "shield");
                if (vanillaShield != null) {
                    placed = insertItemIntoFabricTab(entries, vanillaShield, copperShield, true);
                }
            }
            if (placed) {
                anyPlaced = true;
            }
        }

        return anyPlaced;
    }

    /**
     * Tools & Utilities tab placement (no-op since knife is exclusive to Farmer's Delight tab).
     */
    public static boolean applyFabricToolsTabPlacement(Object entries) {
        return false;
    }

    private static boolean isSameItem(Object stackObj, Object itemObj) {
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

    private static boolean isCombatTab(Object tabKey) {
        if (tabKey == null) return false;
        if (COMBAT_TAB_KEY != null && COMBAT_TAB_KEY.equals(tabKey)) return true;
        String s = tabKey.toString().toLowerCase();
        return s.contains("combat");
    }

    private static boolean isToolsTab(Object tabKey) {
        if (tabKey == null) return false;
        if (TOOLS_TAB_KEY != null && TOOLS_TAB_KEY.equals(tabKey)) return true;
        String s = tabKey.toString().toLowerCase();
        return s.contains("tools") || s.contains("utilities");
    }

    public static boolean insertItemIntoNeoForgeTab(Object event, Object anchorItem, Object itemToInsert, boolean after) {
        if (event == null || itemToInsert == null) return false;
        try {
            for (String methodName : new String[]{"getParentEntries", "getSearchEntries"}) {
                try {
                    Method entriesMethod = event.getClass().getMethod(methodName);
                    Object entriesSet = entriesMethod.invoke(event);
                    if (entriesSet instanceof java.util.Collection<?> col) {
                        for (Object itemObj : col) {
                            if (isSameItem(itemObj, itemToInsert)) {
                                return true;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            Object anchorStack = anchorItem != null ? getDefaultInstance(anchorItem) : null;
            Object insertStack = getDefaultInstance(itemToInsert);
            if (insertStack == null) return false;
            Object parentAndSearch = resolveTabVisibility("PARENT_AND_SEARCH_TABS");

            if (anchorStack != null) {
                String methodName = after ? "insertAfter" : "insertBefore";
                for (Method m : event.getClass().getMethods()) {
                    if (methodName.equals(m.getName()) && m.getParameterCount() == 3) {
                        try {
                            m.setAccessible(true);
                            m.invoke(event, anchorStack, insertStack, parentAndSearch);
                            return true;
                        } catch (Throwable t) {
                            Throwable cause = (t instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null) ? ite.getCause() : t;
                            if (cause instanceof IllegalArgumentException && cause.getMessage() != null && cause.getMessage().contains("already exists")) {
                                return true;
                            }
                            return false;
                        }
                    }
                }
                return false;
            }

            for (Method m : event.getClass().getMethods()) {
                if ("accept".equals(m.getName())) {
                    if (m.getParameterCount() == 2 && parentAndSearch != null
                            && m.getParameterTypes()[1].isAssignableFrom(parentAndSearch.getClass())) {
                        m.invoke(event, insertStack, parentAndSearch);
                        return true;
                    } else if (m.getParameterCount() == 1) {
                        m.invoke(event, insertStack);
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean insertItemIntoFabricTab(Object entries, Object anchorItem, Object itemToInsert, boolean after) {
        if (entries == null || itemToInsert == null) return false;
        try {
            // Check if item is already in displayStacks to avoid duplicate insertion
            try {
                Method getDisplayStacksMethod = entries.getClass().getMethod("getDisplayStacks");
                Object list = getDisplayStacksMethod.invoke(entries);
                if (list instanceof java.util.Collection<?> stackList) {
                    for (Object itemObj : stackList) {
                        if (isSameItem(itemObj, itemToInsert)) {
                            return true;
                        }
                    }
                }
            } catch (Throwable ignored) {}

            Object anchorStack = anchorItem != null ? getDefaultInstance(anchorItem) : null;
            Object insertStack = getDefaultInstance(itemToInsert);
            if (insertStack == null) return false;
            String methodName = after ? "addAfter" : "addBefore";

            if (anchorItem != null) {
                // 1. Array overloads
                for (Method m : entries.getClass().getMethods()) {
                    if (methodName.equals(m.getName()) && m.getParameterCount() == 2 && m.getParameterTypes()[1].isArray()) {
                        Class<?> firstParam = m.getParameterTypes()[0];
                        Class<?> compType = m.getParameterTypes()[1].getComponentType();
                        m.setAccessible(true);

                        // 1a. ItemLike -> ItemLike...
                        if (firstParam.isAssignableFrom(anchorItem.getClass()) && compType.isAssignableFrom(itemToInsert.getClass())) {
                            Object itemArray = Array.newInstance(compType, 1);
                            Array.set(itemArray, 0, itemToInsert);
                            try {
                                m.invoke(entries, anchorItem, itemArray);
                                return true;
                            } catch (Throwable ignored) {}
                        }
                        // 1b. ItemStack -> ItemStack...
                        if (anchorStack != null && firstParam.isAssignableFrom(anchorStack.getClass()) && compType.isAssignableFrom(insertStack.getClass())) {
                            Object stackArray = Array.newInstance(compType, 1);
                            Array.set(stackArray, 0, insertStack);
                            try {
                                m.invoke(entries, anchorStack, stackArray);
                                return true;
                            } catch (Throwable ignored) {}
                        }
                        // 1c. ItemLike -> ItemStack...
                        if (firstParam.isAssignableFrom(anchorItem.getClass()) && compType.isAssignableFrom(insertStack.getClass())) {
                            Object stackArray = Array.newInstance(compType, 1);
                            Array.set(stackArray, 0, insertStack);
                            try {
                                m.invoke(entries, anchorItem, stackArray);
                                return true;
                            } catch (Throwable ignored) {}
                        }
                        // 1d. ItemStack -> ItemLike...
                        if (anchorStack != null && firstParam.isAssignableFrom(anchorStack.getClass()) && compType.isAssignableFrom(itemToInsert.getClass())) {
                            Object itemArray = Array.newInstance(compType, 1);
                            Array.set(itemArray, 0, itemToInsert);
                            try {
                                m.invoke(entries, anchorStack, itemArray);
                                return true;
                            } catch (Throwable ignored) {}
                        }
                    }
                }

                // 2. Collection overloads
                for (Method m : entries.getClass().getMethods()) {
                    if (methodName.equals(m.getName()) && m.getParameterCount() == 2 && java.util.Collection.class.isAssignableFrom(m.getParameterTypes()[1])) {
                        Class<?> firstParam = m.getParameterTypes()[0];
                        m.setAccessible(true);
                        Object toInsert = insertStack != null ? insertStack : itemToInsert;
                        if (firstParam.isAssignableFrom(anchorItem.getClass())) {
                            try {
                                m.invoke(entries, anchorItem, Collections.singletonList(toInsert));
                                return true;
                            } catch (Throwable ignored) {}
                        } else if (anchorStack != null && firstParam.isAssignableFrom(anchorStack.getClass())) {
                            try {
                                m.invoke(entries, anchorStack, Collections.singletonList(toInsert));
                                return true;
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }

            // Fallback 1: add(ItemStack) or add(ItemLike)
            for (Method m : entries.getClass().getMethods()) {
                if ("add".equals(m.getName()) && m.getParameterCount() == 1) {
                    Class<?> pType = m.getParameterTypes()[0];
                    if (pType.isAssignableFrom(insertStack.getClass())) {
                        try {
                            m.invoke(entries, insertStack);
                            return true;
                        } catch (Throwable ignored) {}
                    } else if (pType.isAssignableFrom(itemToInsert.getClass())) {
                        try {
                            m.invoke(entries, itemToInsert);
                            return true;
                        } catch (Throwable ignored) {}
                    }
                }
            }

            // Fallback 2: prepend
            for (Method m : entries.getClass().getMethods()) {
                if ("prepend".equals(m.getName()) && m.getParameterCount() == 1) {
                    Class<?> pType = m.getParameterTypes()[0];
                    if (pType.isAssignableFrom(insertStack.getClass())) {
                        try {
                            m.invoke(entries, insertStack);
                            return true;
                        } catch (Throwable ignored) {}
                    } else if (pType.isAssignableFrom(itemToInsert.getClass())) {
                        try {
                            m.invoke(entries, itemToInsert);
                            return true;
                        } catch (Throwable ignored) {}
                    }
                }
            }

            // Fallback 3: displayStacks.add
            Method getDisplayStacksMethod = entries.getClass().getMethod("getDisplayStacks");
            Object list = getDisplayStacksMethod.invoke(entries);
            if (list instanceof java.util.List stackList) {
                stackList.add(insertStack != null ? insertStack : itemToInsert);
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
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
