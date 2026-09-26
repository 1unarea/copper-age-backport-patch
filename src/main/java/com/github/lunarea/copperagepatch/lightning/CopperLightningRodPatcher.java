package com.github.lunarea.copperagepatch.lightning;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Clean-room patcher for Copper Age Backport lightning rods.
 *
 * Implements full parity with vanilla and modern Minecraft lightning rods:
 * 1. POI Registration: Registers all 7 CAB lightning rod block states into
 *    PoiTypes.TYPE_BY_STATE, enabling natural lightning redirection via PoiManager.
 * 2. Weathering & Waxing: Integrates CAB rods into WeatheringCopper and HoneycombItem
 *    lookup maps for axe-scraping, honeycomb-waxing, and de-oxidation chains.
 * 3. Lightning Strike Handling:
 *    - onPowerLightningRod: powers CAB lightning rods on strike, emitting 8-tick redstone pulse and particles.
 *    - onClearCopper: de-oxidizes unwaxed weathered rods and redirects lightning cleaning to attached copper blocks.
 *
 * Implemented completely mapping-agnostic with zero net/minecraft references in bytecode signatures.
 */
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class CopperLightningRodPatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopperLightningRodPatcher.class);

    private static final Unsafe UNSAFE;
    private static volatile boolean poiPatched = false;
    private static volatile boolean weatheringPatched = false;

    private static final String[] CAB_WEATHERING_ROD_IDS = {
            "exposed_lightning_rod",
            "weathered_lightning_rod",
            "oxidized_lightning_rod"
    };

    private static final String[] CAB_WAXED_ROD_IDS = {
            "waxed_lightning_rod",
            "waxed_exposed_lightning_rod",
            "waxed_weathered_lightning_rod",
            "waxed_oxidized_lightning_rod"
    };

    static {
        Unsafe u = null;
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            u = (Unsafe) f.get(null);
        } catch (Throwable t) {
            LOGGER.error("[CopperAgeBackportPatch] Failed to obtain sun.misc.Unsafe", t);
        }
        UNSAFE = u;
    }

    private CopperLightningRodPatcher() {}

    /**
     * Initializes all lightning rod patches.
     */
    public static synchronized void init() {
        if (!poiPatched) {
            poiPatched = patchPoiTypes();
        }
        if (!weatheringPatched) {
            weatheringPatched = patchWeatheringAndWaxing();
        }
    }

    /**
     * Registers all block states of the 7 CAB lightning rods into PoiTypes.TYPE_BY_STATE.
     *
     * @return true if successfully patched or already complete, false otherwise
     */
    public static boolean patchPoiTypes() {
        try {
            Class<?> poiTypesClass = resolveClass(
                    "net.minecraft.world.entity.ai.village.poi.PoiTypes",
                    "net.minecraft.class_7477"
            );
            if (poiTypesClass == null) {
                return false;
            }

            Field typeByStateField = findField(poiTypesClass, "TYPE_BY_STATE", "field_39301");
            if (typeByStateField == null) {
                LOGGER.warn("[CopperAgeBackportPatch] Could not find PoiTypes.TYPE_BY_STATE field.");
                return false;
            }
            typeByStateField.setAccessible(true);
            Map<Object, Object> typeByState = (Map<Object, Object>) typeByStateField.get(null);
            if (typeByState == null) {
                return false;
            }

            Object vanillaRodBlock = getBlockFromRegistry("minecraft", "lightning_rod");
            if (vanillaRodBlock == null || isAir(vanillaRodBlock)) {
                return false;
            }

            // Resolve Holder<PoiType> for LIGHTNING_ROD
            Object lightningRodHolder = null;
            Method defaultStateMethod = findMethod(vanillaRodBlock.getClass(), new String[]{"defaultBlockState", "method_9564"}, 0);
            if (defaultStateMethod != null) {
                Object defaultState = defaultStateMethod.invoke(vanillaRodBlock);
                if (defaultState != null) {
                    lightningRodHolder = typeByState.get(defaultState);
                    if (lightningRodHolder == null) {
                        Method forStateMethod = findMethod(poiTypesClass, new String[]{"forState", "method_43989"}, 1);
                        if (forStateMethod != null) {
                            Object opt = forStateMethod.invoke(null, defaultState);
                            if (opt instanceof Optional<?> o && o.isPresent()) {
                                lightningRodHolder = o.get();
                            }
                        }
                    }
                }
            }

            if (lightningRodHolder == null) {
                Object poiRegistry = getPoiRegistry();
                Field lrKeyField = findField(poiTypesClass, "LIGHTNING_ROD", "field_39297");
                if (poiRegistry != null && lrKeyField != null) {
                    lrKeyField.setAccessible(true);
                    Object lrKey = lrKeyField.get(null);
                    if (lrKey != null) {
                        Method getHolderMethod = findMethod(poiRegistry.getClass(), new String[]{"getHolder", "method_40264", "getHolderOrThrow", "method_40290"}, 1);
                        if (getHolderMethod != null) {
                            Object res = getHolderMethod.invoke(poiRegistry, lrKey);
                            if (res instanceof Optional<?> o) {
                                lightningRodHolder = o.orElse(null);
                            } else {
                                lightningRodHolder = res;
                            }
                        }
                    }
                }
            }

            if (lightningRodHolder == null) {
                LOGGER.warn("[CopperAgeBackportPatch] Could not resolve Holder for PoiTypes.LIGHTNING_ROD.");
                return false;
            }

            List<String> allCabRods = new ArrayList<>();
            Collections.addAll(allCabRods, CAB_WEATHERING_ROD_IDS);
            Collections.addAll(allCabRods, CAB_WAXED_ROD_IDS);

            int registeredStates = 0;
            Set<Object> additionalStates = new HashSet<>();

            for (String rodId : allCabRods) {
                Object block = getBlockFromRegistry("minecraft", rodId);
                if (block == null || isAir(block)) {
                    block = getBlockFromRegistry("copperagebackport", rodId);
                }
                if (block == null || isAir(block)) {
                    continue;
                }

                Collection<?> states = getPossibleBlockStates(block);
                for (Object state : states) {
                    typeByState.put(state, lightningRodHolder);
                    additionalStates.add(state);
                    registeredStates++;
                }
            }

            if (registeredStates > 0) {
                // Also update PoiType.matchingStates record set if possible
                updatePoiTypeMatchingStates(lightningRodHolder, additionalStates);
                LOGGER.info("[CopperAgeBackportPatch] Registered {} CAB lightning rod states into PoiTypes.TYPE_BY_STATE.", registeredStates);
                return true;
            }
        } catch (Throwable t) {
            LOGGER.warn("[CopperAgeBackportPatch] Failed to patch PoiTypes: {}", t.getMessage());
        }
        return false;
    }

    private static void updatePoiTypeMatchingStates(Object holder, Set<Object> extraStates) {
        if (holder == null || extraStates == null || extraStates.isEmpty() || UNSAFE == null) {
            return;
        }
        try {
            Method valueMethod = findMethod(holder.getClass(), new String[]{"value", "method_40220", "get"}, 0);
            Object poiType = valueMethod != null ? valueMethod.invoke(holder) : null;
            if (poiType == null) {
                return;
            }

            Field matchingStatesField = null;
            for (Field f : poiType.getClass().getDeclaredFields()) {
                if (Set.class.isAssignableFrom(f.getType())) {
                    matchingStatesField = f;
                    break;
                }
            }
            if (matchingStatesField != null) {
                matchingStatesField.setAccessible(true);
                Set<Object> currentSet = (Set<Object>) matchingStatesField.get(poiType);
                Set<Object> merged = new HashSet<>();
                if (currentSet != null) {
                    merged.addAll(currentSet);
                }
                merged.addAll(extraStates);
                Set<Object> unmodifiable = Collections.unmodifiableSet(merged);

                long offset = UNSAFE.objectFieldOffset(matchingStatesField);
                UNSAFE.putObject(poiType, offset, unmodifiable);
                LOGGER.debug("[CopperAgeBackportPatch] Updated PoiType matchingStates with {} total states.", unmodifiable.size());
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Wraps WeatheringCopper and HoneycombItem static BiMap suppliers with CAB rods.
     *
     * @return true if successfully patched, false otherwise
     */
    public static boolean patchWeatheringAndWaxing() {
        try {
            Object rod = getBlockFromRegistry("minecraft", "lightning_rod");
            Object exposed = getCabBlock("exposed_lightning_rod");
            Object weathered = getCabBlock("weathered_lightning_rod");
            Object oxidized = getCabBlock("oxidized_lightning_rod");

            Object waxedRod = getCabBlock("waxed_lightning_rod");
            Object waxedExposed = getCabBlock("waxed_exposed_lightning_rod");
            Object waxedWeathered = getCabBlock("waxed_weathered_lightning_rod");
            Object waxedOxidized = getCabBlock("waxed_oxidized_lightning_rod");

            if (rod == null || exposed == null || weathered == null || oxidized == null
                    || waxedRod == null || waxedExposed == null || waxedWeathered == null || waxedOxidized == null) {
                return false;
            }

            Class<?> weatheringClass = resolveClass(
                    "net.minecraft.world.level.block.WeatheringCopper",
                    "net.minecraft.class_5955"
            );
            if (weatheringClass != null) {
                // NEXT_BY_BLOCK
                patchSupplierBiMap(weatheringClass, new String[]{"NEXT_BY_BLOCK", "field_29564"}, map -> {
                    map.forcePut(rod, exposed);
                    map.forcePut(exposed, weathered);
                    map.forcePut(weathered, oxidized);
                });

                // PREVIOUS_BY_BLOCK
                patchSupplierBiMap(weatheringClass, new String[]{"PREVIOUS_BY_BLOCK", "field_29565"}, map -> {
                    map.forcePut(oxidized, weathered);
                    map.forcePut(weathered, exposed);
                    map.forcePut(exposed, rod);
                });
            }

            Class<?> honeycombClass = resolveClass(
                    "net.minecraft.world.item.HoneycombItem",
                    "net.minecraft.class_5953"
            );
            if (honeycombClass != null) {
                // WAXABLES
                patchSupplierBiMap(honeycombClass, new String[]{"WAXABLES", "field_29560"}, map -> {
                    map.forcePut(rod, waxedRod);
                    map.forcePut(exposed, waxedExposed);
                    map.forcePut(weathered, waxedWeathered);
                    map.forcePut(oxidized, waxedOxidized);
                });

                // WAX_OFF_BY_BLOCK
                patchSupplierBiMap(honeycombClass, new String[]{"WAX_OFF_BY_BLOCK", "field_29561"}, map -> {
                    map.forcePut(waxedRod, rod);
                    map.forcePut(waxedExposed, exposed);
                    map.forcePut(waxedWeathered, weathered);
                    map.forcePut(waxedOxidized, oxidized);
                });
            }

            LOGGER.info("[CopperAgeBackportPatch] Patched WeatheringCopper and HoneycombItem BiMap suppliers for CAB lightning rods.");
            return true;
        } catch (Throwable t) {
            LOGGER.warn("[CopperAgeBackportPatch] Failed to patch weathering and waxing maps: {}", t.getMessage());
            return false;
        }
    }

    private static Object getCabBlock(String path) {
        Object b = getBlockFromRegistry("minecraft", path);
        if (b == null || isAir(b)) {
            b = getBlockFromRegistry("copperagebackport", path);
        }
        return (b != null && !isAir(b)) ? b : null;
    }

    private static void patchSupplierBiMap(Class<?> holderClass, String[] fieldNames, Consumer<BiMap<Object, Object>> mapUpdater) {
        if (holderClass == null || UNSAFE == null) return;
        Field f = findField(holderClass, fieldNames);
        if (f == null) return;
        try {
            f.setAccessible(true);
            Supplier<?> originalSupplier = (Supplier<?>) f.get(null);
            if (originalSupplier == null) return;

            Supplier<Object> wrappedSupplier = () -> {
                Object base = originalSupplier.get();
                if (base instanceof BiMap<?, ?> baseBiMap) {
                    BiMap<Object, Object> copy = HashBiMap.create((Map<?, ?>) baseBiMap);
                    mapUpdater.accept(copy);
                    return Maps.unmodifiableBiMap(copy);
                }
                return base;
            };

            Object base = UNSAFE.staticFieldBase(f);
            long offset = UNSAFE.staticFieldOffset(f);
            UNSAFE.putObject(base, offset, wrappedSupplier);
        } catch (Throwable t) {
            LOGGER.debug("[CopperAgeBackportPatch] Could not patch supplier BiMap field: {}", f.getName(), t);
        }
    }

    /**
     * Intercepts LightningBolt.powerLightningRod to power CAB lightning rods on strike.
     */
    public static void onPowerLightningRod(Object lightningBolt) {
        if (lightningBolt == null) return;
        try {
            Method strikePosMethod = findMethod(lightningBolt.getClass(), new String[]{"getStrikePosition", "method_36607"}, 0);
            if (strikePosMethod == null) return;
            strikePosMethod.setAccessible(true);
            Object strikePos = strikePosMethod.invoke(lightningBolt);
            if (strikePos == null) return;

            Object level = getEntityLevel(lightningBolt);
            if (level == null) return;

            Object state = getBlockState(level, strikePos);
            if (state == null) return;

            Object block = getBlock(state);
            if (block == null || !isCabLightningRod(block)) return;

            Method onStrikeMethod = findMethod(block.getClass(), new String[]{"onLightningStrike", "method_31637"}, 3);
            if (onStrikeMethod != null) {
                onStrikeMethod.setAccessible(true);
                onStrikeMethod.invoke(block, state, level, strikePos);
                LOGGER.debug("[CopperAgeBackportPatch] Powered CAB lightning rod at {}", strikePos);
            }
        } catch (Throwable t) {
            LOGGER.warn("[CopperAgeBackportPatch] Failed to power CAB lightning rod on strike: {}", t.getMessage());
        }
    }

    /**
     * Intercepts LightningBolt.clearCopperOnLightningStrike to de-oxidize unwaxed weathered rods
     * and redirect de-oxidation to attached copper blocks.
     */
    public static void onClearCopper(Object level, Object pos, Object ci) {
        if (level == null || pos == null) return;
        try {
            Object state = getBlockState(level, pos);
            if (state == null) return;

            Object block = getBlock(state);
            if (block == null || !isCabLightningRod(block)) {
                return; // Let vanilla handle non-CAB blocks
            }

            // 1. If unwaxed WeatheringCopper, de-oxidize the rod itself
            if (isWeatheringCopper(block)) {
                Object prevState = getPreviousState(state, block);
                if (prevState != null) {
                    setBlockAndUpdate(level, pos, prevState);
                    playLevelEvent(level, 3002, pos, -1);
                    LOGGER.debug("[CopperAgeBackportPatch] De-oxidized CAB lightning rod at {}", pos);
                }
            }

            // 2. Redirect to attached block behind the rod (for both waxed and unwaxed rods)
            Object facing = getFacingValue(state, block);
            if (facing != null) {
                Object opposite = getOppositeDirection(facing);
                if (opposite != null) {
                    Object targetPos = getRelativePos(pos, opposite);
                    if (targetPos != null) {
                        Object targetState = getBlockState(level, targetPos);
                        Object targetBlock = targetState != null ? getBlock(targetState) : null;
                        if (targetBlock != null && isWeatheringCopper(targetBlock)) {
                            Object firstState = getFirstState(targetState, targetBlock);
                            if (firstState != null) {
                                setBlockAndUpdate(level, targetPos, firstState);
                                performRandomWalkCleaning(level, targetPos);
                            }
                        }
                    }
                }
            }

            // 3. Cancel vanilla clearCopperOnLightningStrike so it doesn't return early without cleaning attached blocks
            cancelCallbackInfo(ci);
        } catch (Throwable t) {
            LOGGER.warn("[CopperAgeBackportPatch] Error in onClearCopper: {}", t.getMessage());
        }
    }

    public static boolean isCabLightningRod(Object block) {
        if (block == null) return false;
        String name = block.getClass().getName();
        if (name.contains("CopperLightningRodBlock")) {
            return true;
        }
        String s = block.toString().toLowerCase();
        for (String id : CAB_WEATHERING_ROD_IDS) {
            if (s.contains(id)) return true;
        }
        for (String id : CAB_WAXED_ROD_IDS) {
            if (s.contains(id)) return true;
        }
        return false;
    }

    public static boolean isWeatheringCopper(Object block) {
        if (block == null) return false;
        for (Class<?> iface : block.getClass().getInterfaces()) {
            String name = iface.getName();
            if (name.endsWith("WeatheringCopper") || "net.minecraft.class_5955".equals(name)) {
                return true;
            }
        }
        Class<?> sc = block.getClass().getSuperclass();
        while (sc != null && sc != Object.class) {
            for (Class<?> iface : sc.getInterfaces()) {
                String name = iface.getName();
                if (name.endsWith("WeatheringCopper") || "net.minecraft.class_5955".equals(name)) {
                    return true;
                }
            }
            sc = sc.getSuperclass();
        }
        return false;
    }

    private static Object getPreviousState(Object state, Object block) {
        try {
            // Priority 1: WeatheringCopper.getPrevious(BlockState)
            Class<?> wcClass = resolveClass("net.minecraft.world.level.block.WeatheringCopper", "net.minecraft.class_5955");
            if (wcClass != null) {
                Method getPrevMethod = findMethod(wcClass, new String[]{"getPrevious", "method_34735"}, 1);
                if (getPrevMethod != null) {
                    Object opt = getPrevMethod.invoke(null, state);
                    if (opt instanceof Optional<?> o && o.isPresent()) {
                        return o.get();
                    }
                }
            }

            // Priority 2: block.getPreviousBlock()
            Method prevBlockMethod = findMethod(block.getClass(), new String[]{"getPreviousBlock"}, 0);
            if (prevBlockMethod != null) {
                Object opt = prevBlockMethod.invoke(block);
                if (opt instanceof Optional<?> o && o.isPresent()) {
                    Object prevBlock = o.get();
                    Method withProps = findMethod(prevBlock.getClass(), new String[]{"withPropertiesOf", "method_34734", "method_9564"}, 1);
                    if (withProps != null) {
                        return withProps.invoke(prevBlock, state);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object getFirstState(Object state, Object block) {
        try {
            Class<?> wcClass = resolveClass("net.minecraft.world.level.block.WeatheringCopper", "net.minecraft.class_5955");
            if (wcClass != null) {
                Method getFirstMethod = findMethod(wcClass, new String[]{"getFirst", "method_34738"}, 1);
                if (getFirstMethod != null) {
                    return getFirstMethod.invoke(null, state);
                }
            }
        } catch (Throwable ignored) {}
        return state;
    }

    private static Object getFacingValue(Object state, Object block) {
        try {
            Field facingField = findField(block.getClass(), "FACING", "field_11177");
            if (facingField != null) {
                facingField.setAccessible(true);
                Object prop = facingField.get(null);
                if (prop != null) {
                    Method getValueMethod = findMethod(state.getClass(), new String[]{"getValue", "method_11654"}, 1);
                    if (getValueMethod != null) {
                        return getValueMethod.invoke(state, prop);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object getOppositeDirection(Object direction) {
        if (direction == null) return null;
        try {
            Method getOpp = findMethod(direction.getClass(), new String[]{"getOpposite", "method_10153"}, 0);
            if (getOpp != null) {
                return getOpp.invoke(direction);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object getRelativePos(Object pos, Object direction) {
        if (pos == null || direction == null) return null;
        try {
            Method rel = findMethod(pos.getClass(), new String[]{"relative", "method_10093", "offset"}, 1);
            if (rel != null) {
                return rel.invoke(pos, direction);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void performRandomWalkCleaning(Object level, Object targetPos) {
        try {
            Class<?> lbClass = resolveClass("net.minecraft.world.entity.LightningBolt", "net.minecraft.class_1538");
            if (lbClass != null) {
                Method rwMethod = findMethod(lbClass, new String[]{"randomWalkCleaningCopper", "method_34709"}, 4);
                if (rwMethod != null) {
                    rwMethod.setAccessible(true);
                    Method mutableMethod = findMethod(targetPos.getClass(), new String[]{"mutable", "method_10069"}, 0);
                    Object mutable = mutableMethod != null ? mutableMethod.invoke(targetPos) : null;
                    if (mutable != null) {
                        Random rand = new Random();
                        int count = rand.nextInt(3) + 3;
                        for (int i = 0; i < count; i++) {
                            int steps = rand.nextInt(8) + 1;
                            rwMethod.invoke(null, level, targetPos, mutable, steps);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void cancelCallbackInfo(Object ci) {
        if (ci == null) return;
        try {
            Method cancelMethod = ci.getClass().getMethod("cancel");
            cancelMethod.invoke(ci);
        } catch (Throwable ignored) {}
    }

    public static Object getEntityLevel(Object entity) {
        if (entity == null) return null;
        try {
            Method levelMethod = findMethod(entity.getClass(), new String[]{"level", "method_37908", "method_37222", "getWorld"}, 0);
            if (levelMethod != null) {
                levelMethod.setAccessible(true);
                return levelMethod.invoke(entity);
            }
        } catch (Throwable ignored) {}
        try {
            Field levelField = findField(entity.getClass(), "level", "field_6002");
            if (levelField != null) {
                levelField.setAccessible(true);
                return levelField.get(entity);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static Object getBlockState(Object level, Object pos) {
        if (level == null || pos == null) return null;
        try {
            Method getBlockStateMethod = findMethod(level.getClass(), new String[]{"getBlockState", "method_8320"}, 1);
            if (getBlockStateMethod != null) {
                return getBlockStateMethod.invoke(level, pos);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static Object getBlock(Object blockState) {
        if (blockState == null) return null;
        try {
            Method getBlockMethod = findMethod(blockState.getClass(), new String[]{"getBlock", "method_26204"}, 0);
            if (getBlockMethod != null) {
                return getBlockMethod.invoke(blockState);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean setBlockAndUpdate(Object level, Object pos, Object state) {
        if (level == null || pos == null || state == null) return false;
        try {
            Method m = findMethod(level.getClass(), new String[]{"setBlockAndUpdate", "method_30092"}, 2);
            if (m != null) {
                return Boolean.TRUE.equals(m.invoke(level, pos, state));
            }
        } catch (Throwable ignored) {}
        try {
            Method m = findMethod(level.getClass(), new String[]{"setBlock", "method_8652"}, 3);
            if (m != null) {
                return Boolean.TRUE.equals(m.invoke(level, pos, state, 3));
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void playLevelEvent(Object level, int eventId, Object pos, int data) {
        if (level == null || pos == null) return;
        try {
            Method m = findMethod(level.getClass(), new String[]{"levelEvent", "method_8444"}, 3);
            if (m != null) {
                m.invoke(level, eventId, pos, data);
            }
        } catch (Throwable ignored) {}
    }

    public static Collection<?> getPossibleBlockStates(Object block) {
        if (block == null) return Collections.emptyList();
        try {
            Method stateDefMethod = findMethod(block.getClass(), new String[]{"getStateDefinition", "getStateManager", "method_9595"}, 0);
            if (stateDefMethod != null) {
                Object stateDef = stateDefMethod.invoke(block);
                if (stateDef != null) {
                    Method getPossible = findMethod(stateDef.getClass(), new String[]{"getPossibleStates", "getStates", "method_11662"}, 0);
                    if (getPossible != null) {
                        Object states = getPossible.invoke(stateDef);
                        if (states instanceof Collection<?> col) {
                            return col;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }

    public static Object getBlockFromRegistry(String namespace, String path) {
        // 1. Try Mojang BuiltInRegistries.BLOCK
        try {
            Class<?> regClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            Field blockRegField = regClass.getField("BLOCK");
            Object blockReg = blockRegField.get(null);
            Object loc = createIdentifier(namespace, path);
            if (loc != null && blockReg != null) {
                Method getMethod = blockReg.getClass().getMethod("get", loc.getClass());
                return getMethod.invoke(blockReg, loc);
            }
        } catch (Throwable t1) {
            // 2. Try Fabric Intermediary class_7923.field_41175
            try {
                Class<?> regClass = Class.forName("net.minecraft.class_7923");
                Field blockRegField = findField(regClass, "field_41175", "BLOCK");
                if (blockRegField != null) {
                    blockRegField.setAccessible(true);
                    Object blockReg = blockRegField.get(null);
                    Object id = createIdentifier(namespace, path);
                    if (id != null && blockReg != null) {
                        for (Method m : blockReg.getClass().getMethods()) {
                            if (m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(id.getClass())) {
                                Object res = m.invoke(blockReg, id);
                                if (res != null) return res;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static Object getPoiRegistry() {
        try {
            Class<?> regClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            Field poiRegField = regClass.getField("POINT_OF_INTEREST_TYPE");
            return poiRegField.get(null);
        } catch (Throwable ignored) {}
        try {
            Class<?> regClass = Class.forName("net.minecraft.class_7923");
            Field poiRegField = findField(regClass, "field_41128", "POINT_OF_INTEREST_TYPE");
            if (poiRegField != null) {
                poiRegField.setAccessible(true);
                return poiRegField.get(null);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean isAir(Object block) {
        if (block == null) return true;
        try {
            Class<?> blocksClass = Class.forName("net.minecraft.world.level.block.Blocks");
            Field airField = blocksClass.getField("AIR");
            if (block == airField.get(null)) return true;
        } catch (Throwable ignored) {}
        try {
            Class<?> blocksClass = Class.forName("net.minecraft.class_2246");
            Field airField = findField(blocksClass, "field_10124", "AIR");
            if (airField != null && block == airField.get(null)) return true;
        } catch (Throwable ignored) {}
        String s = block.toString().toLowerCase();
        return "air".equals(s) || "minecraft:air".equals(s);
    }

    public static Object createIdentifier(String namespace, String path) {
        // 1. Try Mojang ResourceLocation
        try {
            Class<?> locClass = Class.forName("net.minecraft.resources.ResourceLocation");
            try {
                Method m = locClass.getMethod("fromNamespaceAndPath", String.class, String.class);
                return m.invoke(null, namespace, path);
            } catch (NoSuchMethodException ignored) {}
            try {
                Method m = locClass.getMethod("tryBuild", String.class, String.class);
                return m.invoke(null, namespace, path);
            } catch (NoSuchMethodException ignored) {}
            try {
                Constructor<?> ctor = locClass.getDeclaredConstructor(String.class, String.class);
                ctor.setAccessible(true);
                return ctor.newInstance(namespace, path);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}

        // 2. Try Fabric Intermediary class_2960
        try {
            Class<?> idClass = Class.forName("net.minecraft.class_2960");
            for (String mName : new String[]{"method_60655", "method_43902", "of"}) {
                try {
                    Method m = idClass.getMethod(mName, String.class, String.class);
                    m.setAccessible(true);
                    return m.invoke(null, namespace, path);
                } catch (Throwable ignored) {}
            }
            for (String mName : new String[]{"method_60654", "method_12829", "of"}) {
                try {
                    Method m = idClass.getMethod(mName, String.class);
                    m.setAccessible(true);
                    return m.invoke(null, namespace + ":" + path);
                } catch (Throwable ignored) {}
            }
            try {
                Constructor<?> ctor = idClass.getDeclaredConstructor(String.class, String.class);
                ctor.setAccessible(true);
                return ctor.newInstance(namespace, path);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}

        return null;
    }

    public static Class<?> resolveClass(String... names) {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    public static Field findField(Class<?> clazz, String... names) {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                for (String name : names) {
                    if (name.equals(f.getName())) {
                        f.setAccessible(true);
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public static Method findMethod(Class<?> clazz, String[] names, int paramCount) {
        if (clazz == null) return null;
        for (Method m : clazz.getMethods()) {
            if (m.getParameterCount() == paramCount) {
                for (String name : names) {
                    if (name.equals(m.getName())) {
                        m.setAccessible(true);
                        return m;
                    }
                }
            }
        }
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() == paramCount) {
                    for (String name : names) {
                        if (name.equals(m.getName())) {
                            m.setAccessible(true);
                            return m;
                        }
                    }
                }
            }
            for (Class<?> iface : c.getInterfaces()) {
                for (Method m : iface.getDeclaredMethods()) {
                    if (m.getParameterCount() == paramCount) {
                        for (String name : names) {
                            if (name.equals(m.getName())) {
                                m.setAccessible(true);
                                return m;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
