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

    private CopperCombatTabPatcher() {}

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
                // 3. Fallback: try ResourceKey / RegistryKey create
                try {
                    Object id = CopperArmorDurabilityPatcher.createIdentifier("minecraft", "combat");
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
            if (!isCombatTab(tabKey)) {
                return false;
            }

            Object copperAxe = getCopperAxe();
            Object stoneAxe = getStoneAxe();
            if (copperAxe == null) {
                LOGGER.warn("[CopperAgeBackportPatch] Copper Axe not found for NeoForge tab placement.");
                return false;
            }

            // Check if copperAxe is already in parentEntries or searchEntries to avoid duplicate insertion error
            try {
                for (String methodName : new String[]{"getParentEntries", "getSearchEntries"}) {
                    try {
                        Method entriesMethod = event.getClass().getMethod(methodName);
                        Object entriesSet = entriesMethod.invoke(event);
                        if (entriesSet instanceof java.util.Collection<?> col) {
                            for (Object itemObj : col) {
                                if (isSameItem(itemObj, copperAxe)) {
                                    LOGGER.debug("[CopperAgeBackportPatch] Copper Axe already present in NeoForge Combat tab via {}.", methodName);
                                    return true;
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            Object stoneStack = getDefaultInstance(stoneAxe);
            Object copperStack = getDefaultInstance(copperAxe);
            if (stoneStack == null || copperStack == null) {
                return false;
            }

            Object parentAndSearch = resolveTabVisibility("PARENT_AND_SEARCH_TABS");

            Method insertAfterMethod = null;
            for (Method m : event.getClass().getMethods()) {
                if ("insertAfter".equals(m.getName()) && m.getParameterCount() == 3) {
                    insertAfterMethod = m;
                    break;
                }
            }

            if (insertAfterMethod != null) {
                try {
                    insertAfterMethod.setAccessible(true);
                    insertAfterMethod.invoke(event, stoneStack, copperStack, parentAndSearch);
                    LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe after Stone Axe in NeoForge Combat tab.");
                    return true;
                } catch (Throwable t) {
                    Throwable cause = (t instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null) ? ite.getCause() : t;
                    if (cause instanceof IllegalArgumentException && cause.getMessage() != null && cause.getMessage().contains("already exists")) {
                        return true;
                    }
                    // Fallback 1: try insertBefore(ironStack) to maintain natural tier order if stone axe is missing
                    try {
                        Object ironAxe = getIronAxe();
                        Object ironStack = getDefaultInstance(ironAxe);
                        if (ironStack != null) {
                            Method insertBeforeMethod = null;
                            for (Method m : event.getClass().getMethods()) {
                                if ("insertBefore".equals(m.getName()) && m.getParameterCount() == 3) {
                                    insertBeforeMethod = m;
                                    break;
                                }
                            }
                            if (insertBeforeMethod != null) {
                                insertBeforeMethod.setAccessible(true);
                                insertBeforeMethod.invoke(event, ironStack, copperStack, parentAndSearch);
                                LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe before Iron Axe in NeoForge Combat tab (fallback).");
                                return true;
                            }
                        }
                    } catch (Throwable ignored) {}

                    // Fallback 2: append Copper Axe to tab if both insertAfter and insertBefore fail
                    try {
                        Method acceptMethod = null;
                        for (Method m : event.getClass().getMethods()) {
                            if ("accept".equals(m.getName())) {
                                if (m.getParameterCount() == 2 && parentAndSearch != null
                                        && m.getParameterTypes()[1].isAssignableFrom(parentAndSearch.getClass())) {
                                    acceptMethod = m;
                                    break;
                                } else if (m.getParameterCount() == 1) {
                                    if (acceptMethod == null) {
                                        acceptMethod = m;
                                    }
                                }
                            }
                        }
                        if (acceptMethod != null) {
                            if (acceptMethod.getParameterCount() == 2 && parentAndSearch != null) {
                                acceptMethod.invoke(event, copperStack, parentAndSearch);
                            } else {
                                acceptMethod.invoke(event, copperStack);
                            }
                            LOGGER.info("[CopperAgeBackportPatch] Appended Copper Axe to NeoForge Combat tab (fallback).");
                            return true;
                        }
                    } catch (Throwable fallbackErr) {
                        LOGGER.error("[CopperAgeBackportPatch] Failed to insert or append Copper Axe in NeoForge Combat tab", t);
                        return false;
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("[CopperAgeBackportPatch] Failed to process NeoForge Combat tab placement", t);
        }
        return false;
    }

    /**
     * Fabric hook: registers with Fabric API ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).
     * Uses dynamic reflection and proxy to ensure zero classloader or mapping errors.
     */
    public static void registerFabricCombatTab() {
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

            Object tabKey = COMBAT_TAB_KEY != null ? COMBAT_TAB_KEY : resolveCombatTabKey();
            if (tabKey == null) {
                LOGGER.warn("[CopperAgeBackportPatch] Combat tab key could not be resolved for Fabric.");
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
                            applyFabricCombatTabPlacement(args[0]);
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
                LOGGER.info("[CopperAgeBackportPatch] Successfully registered Copper Axe placement in Fabric Combat tab via ItemGroupEvents.");
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            LOGGER.debug("[CopperAgeBackportPatch] Fabric ItemGroupEvents not found on classpath (running on NeoForge or vanilla Fabric).");
        } catch (Throwable t) {
            LOGGER.warn("[CopperAgeBackportPatch] Could not register Fabric Combat tab via ItemGroupEvents: {}", t.getMessage());
        }
    }

    /**
     * Inserts Copper Axe after Stone Axe on a FabricItemGroupEntries instance.
     */
    public static boolean applyFabricCombatTabPlacement(Object entries) {
        if (entries == null) {
            return false;
        }
        Object copperAxe = getCopperAxe();
        Object stoneAxe = getStoneAxe();
        if (copperAxe == null || stoneAxe == null) {
            LOGGER.warn("[CopperAgeBackportPatch] Stone Axe or Copper Axe not found for Fabric tab placement.");
            return false;
        }

        Object stoneStack = getDefaultInstance(stoneAxe);
        Object copperStack = getDefaultInstance(copperAxe);

        // Check if copperAxe is already in displayStacks to avoid duplicate insertion
        try {
            Method getDisplayStacksMethod = entries.getClass().getMethod("getDisplayStacks");
            Object list = getDisplayStacksMethod.invoke(entries);
            if (list instanceof java.util.Collection<?> stackList) {
                for (Object itemObj : stackList) {
                    if (isSameItem(itemObj, copperAxe)) {
                        LOGGER.debug("[CopperAgeBackportPatch] Copper Axe already present in Fabric Combat tab.");
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 1. Try array overloads of addAfter
        for (Method m : entries.getClass().getMethods()) {
            if ("addAfter".equals(m.getName()) && m.getParameterCount() == 2 && m.getParameterTypes()[1].isArray()) {
                Class<?> firstParam = m.getParameterTypes()[0];
                Class<?> compType = m.getParameterTypes()[1].getComponentType();
                m.setAccessible(true);

                // 1a. ItemLike -> ItemLike...
                if (firstParam.isAssignableFrom(stoneAxe.getClass()) && compType.isAssignableFrom(copperAxe.getClass())) {
                    Object itemArray = Array.newInstance(compType, 1);
                    Array.set(itemArray, 0, copperAxe);
                    try {
                        m.invoke(entries, stoneAxe, itemArray);
                        LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe after Stone Axe in FabricItemGroupEntries (ItemLike -> ItemLike).");
                        return true;
                    } catch (Throwable ignored) {}
                }

                // 1b. ItemStack -> ItemStack...
                if (stoneStack != null && copperStack != null && firstParam.isAssignableFrom(stoneStack.getClass()) && compType.isAssignableFrom(copperStack.getClass())) {
                    Object stackArray = Array.newInstance(compType, 1);
                    Array.set(stackArray, 0, copperStack);
                    try {
                        m.invoke(entries, stoneStack, stackArray);
                        LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe after Stone Axe in FabricItemGroupEntries (ItemStack -> ItemStack).");
                        return true;
                    } catch (Throwable ignored) {}
                }

                // 1c. ItemLike -> ItemStack...
                if (copperStack != null && firstParam.isAssignableFrom(stoneAxe.getClass()) && compType.isAssignableFrom(copperStack.getClass())) {
                    Object stackArray = Array.newInstance(compType, 1);
                    Array.set(stackArray, 0, copperStack);
                    try {
                        m.invoke(entries, stoneAxe, stackArray);
                        LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe after Stone Axe in FabricItemGroupEntries (ItemLike -> ItemStack).");
                        return true;
                    } catch (Throwable ignored) {}
                }

                // 1d. ItemStack -> ItemLike...
                if (stoneStack != null && firstParam.isAssignableFrom(stoneStack.getClass()) && compType.isAssignableFrom(copperAxe.getClass())) {
                    Object itemArray = Array.newInstance(compType, 1);
                    Array.set(itemArray, 0, copperAxe);
                    try {
                        m.invoke(entries, stoneStack, itemArray);
                        LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe after Stone Axe in FabricItemGroupEntries (ItemStack -> ItemLike).");
                        return true;
                    } catch (Throwable ignored) {}
                }
            }
        }

        // 2. Try Collection overloads of addAfter
        for (Method m : entries.getClass().getMethods()) {
            if ("addAfter".equals(m.getName()) && m.getParameterCount() == 2 && java.util.Collection.class.isAssignableFrom(m.getParameterTypes()[1])) {
                Class<?> firstParam = m.getParameterTypes()[0];
                m.setAccessible(true);
                Object toInsert = copperStack != null ? copperStack : copperAxe;
                if (firstParam.isAssignableFrom(stoneAxe.getClass())) {
                    try {
                        m.invoke(entries, stoneAxe, Collections.singletonList(toInsert));
                        LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe after Stone Axe in FabricItemGroupEntries (Collection).");
                        return true;
                    } catch (Throwable ignored) {}
                } else if (stoneStack != null && firstParam.isAssignableFrom(stoneStack.getClass())) {
                    try {
                        m.invoke(entries, stoneStack, Collections.singletonList(toInsert));
                        LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe after Stone Axe in FabricItemGroupEntries (Collection).");
                        return true;
                    } catch (Throwable ignored) {}
                }
            }
        }

        // 3. Fallback: try addBefore(ironAxe) to maintain natural tier order if Stone Axe is missing
        Object ironAxe = getIronAxe();
        Object ironStack = getDefaultInstance(ironAxe);
        if (ironAxe != null) {
            // 3a. Try array overloads of addBefore
            for (Method m : entries.getClass().getMethods()) {
                if ("addBefore".equals(m.getName()) && m.getParameterCount() == 2 && m.getParameterTypes()[1].isArray()) {
                    Class<?> firstParam = m.getParameterTypes()[0];
                    Class<?> compType = m.getParameterTypes()[1].getComponentType();
                    m.setAccessible(true);

                    if (firstParam.isAssignableFrom(ironAxe.getClass()) && compType.isAssignableFrom(copperAxe.getClass())) {
                        Object itemArray = Array.newInstance(compType, 1);
                        Array.set(itemArray, 0, copperAxe);
                        try {
                            m.invoke(entries, ironAxe, itemArray);
                            LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe before Iron Axe in FabricItemGroupEntries (ItemLike -> ItemLike fallback).");
                            return true;
                        } catch (Throwable ignored) {}
                    }
                    if (ironStack != null && copperStack != null && firstParam.isAssignableFrom(ironStack.getClass()) && compType.isAssignableFrom(copperStack.getClass())) {
                        Object stackArray = Array.newInstance(compType, 1);
                        Array.set(stackArray, 0, copperStack);
                        try {
                            m.invoke(entries, ironStack, stackArray);
                            LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe before Iron Axe in FabricItemGroupEntries (ItemStack -> ItemStack fallback).");
                            return true;
                        } catch (Throwable ignored) {}
                    }
                    if (copperStack != null && firstParam.isAssignableFrom(ironAxe.getClass()) && compType.isAssignableFrom(copperStack.getClass())) {
                        Object stackArray = Array.newInstance(compType, 1);
                        Array.set(stackArray, 0, copperStack);
                        try {
                            m.invoke(entries, ironAxe, stackArray);
                            LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe before Iron Axe in FabricItemGroupEntries (ItemLike -> ItemStack fallback).");
                            return true;
                        } catch (Throwable ignored) {}
                    }
                    if (ironStack != null && firstParam.isAssignableFrom(ironStack.getClass()) && compType.isAssignableFrom(copperAxe.getClass())) {
                        Object itemArray = Array.newInstance(compType, 1);
                        Array.set(itemArray, 0, copperAxe);
                        try {
                            m.invoke(entries, ironStack, itemArray);
                            LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe before Iron Axe in FabricItemGroupEntries (ItemStack -> ItemLike fallback).");
                            return true;
                        } catch (Throwable ignored) {}
                    }
                }
            }

            // 3b. Try Collection overloads of addBefore
            for (Method m : entries.getClass().getMethods()) {
                if ("addBefore".equals(m.getName()) && m.getParameterCount() == 2 && java.util.Collection.class.isAssignableFrom(m.getParameterTypes()[1])) {
                    Class<?> firstParam = m.getParameterTypes()[0];
                    m.setAccessible(true);
                    Object toInsert = copperStack != null ? copperStack : copperAxe;
                    if (firstParam.isAssignableFrom(ironAxe.getClass())) {
                        try {
                            m.invoke(entries, ironAxe, Collections.singletonList(toInsert));
                            LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe before Iron Axe in FabricItemGroupEntries (Collection fallback).");
                            return true;
                        } catch (Throwable ignored) {}
                    } else if (ironStack != null && firstParam.isAssignableFrom(ironStack.getClass())) {
                        try {
                            m.invoke(entries, ironStack, Collections.singletonList(toInsert));
                            LOGGER.info("[CopperAgeBackportPatch] Inserted Copper Axe before Iron Axe in FabricItemGroupEntries (Collection fallback).");
                            return true;
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }

        // 4. Fallback: prepend
        try {
            for (Method m : entries.getClass().getMethods()) {
                if ("prepend".equals(m.getName()) && m.getParameterCount() == 1) {
                    Class<?> pType = m.getParameterTypes()[0];
                    if (copperStack != null && pType.isAssignableFrom(copperStack.getClass())) {
                        m.invoke(entries, copperStack);
                        LOGGER.info("[CopperAgeBackportPatch] Prepended Copper Axe to Fabric tab (fallback stack).");
                        return true;
                    } else if (pType.isAssignableFrom(copperAxe.getClass())) {
                        m.invoke(entries, copperAxe);
                        LOGGER.info("[CopperAgeBackportPatch] Prepended Copper Axe to Fabric tab (fallback item).");
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 5. Fallback: displayStacks.add
        try {
            Method getDisplayStacksMethod = entries.getClass().getMethod("getDisplayStacks");
            Object list = getDisplayStacksMethod.invoke(entries);
            if (list instanceof java.util.List stackList) {
                stackList.add(copperStack != null ? copperStack : copperAxe);
                LOGGER.info("[CopperAgeBackportPatch] Appended Copper Axe to Fabric displayStacks (fallback).");
                return true;
            }
        } catch (Throwable ignored) {}

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
