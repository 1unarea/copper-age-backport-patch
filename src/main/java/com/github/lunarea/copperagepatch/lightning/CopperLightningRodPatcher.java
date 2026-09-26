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
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private static final AtomicBoolean FABRIC_LIFECYCLE_REGISTERED = new AtomicBoolean(false);
    private static final AtomicBoolean NEOFORGE_LIFECYCLE_REGISTERED = new AtomicBoolean(false);

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
        patchPoiTypes();
        if (!weatheringPatched) {
            weatheringPatched = patchWeatheringAndWaxing();
        }
        registerFabricLifecycleEvents();
        registerNeoForgeLifecycleEvents();
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
            boolean needsReassign = false;

            for (String rodId : allCabRods) {
                Object block = getCabBlock(rodId);
                if (block == null || isAir(block)) {
                    continue;
                }

                Collection<?> states = getPossibleBlockStates(block);
                for (Object state : states) {
                    try {
                        typeByState.put(state, lightningRodHolder);
                    } catch (Throwable unsupported) {
                        if (!needsReassign) {
                            needsReassign = true;
                            typeByState = new HashMap<>(typeByState);
                            typeByState.put(state, lightningRodHolder);
                        }
                    }
                    additionalStates.add(state);
                    registeredStates++;
                }
            }

            if (needsReassign && UNSAFE != null) {
                try {
                    Object base = UNSAFE.staticFieldBase(typeByStateField);
                    long offset = UNSAFE.staticFieldOffset(typeByStateField);
                    UNSAFE.putObject(base, offset, typeByState);
                } catch (Throwable reassignEx) {
                    LOGGER.debug("[CopperAgeBackportPatch] Could not reassign TYPE_BY_STATE via Unsafe: {}", reassignEx.getMessage());
                }
            }

            if (registeredStates > 0) {
                poiPatched = true;
                // Also update PoiType.matchingStates record set if possible
                updatePoiTypeMatchingStates(lightningRodHolder, additionalStates);
                LOGGER.info("[CopperAgeBackportPatch] Registered {} CAB lightning rod states into PoiTypes.TYPE_BY_STATE.", registeredStates);
                return true;
            } else {
                LOGGER.warn("[CopperAgeBackportPatch] No CAB lightning rod states could be registered into PoiTypes.TYPE_BY_STATE.");
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
            Method valueMethod = findMethod(holder.getClass(), new String[]{"value", "comp_349", "method_40220", "get"}, 0);
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
        if (weatheringPatched) {
            return true;
        }
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
                    || waxedRod == null || waxedExposed == null || waxedWeathered == null || waxedOxidized == null
                    || isAir(rod) || isAir(exposed) || isAir(weathered) || isAir(oxidized)
                    || isAir(waxedRod) || isAir(waxedExposed) || isAir(waxedWeathered) || isAir(waxedOxidized)) {
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

            weatheringPatched = true;
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
            Object level = getEntityLevel(lightningBolt);
            if (level == null) return;

            Object strikePos = null;
            Method strikePosMethod = findMethod(lightningBolt.getClass(), new String[]{"getStrikePosition", "method_36607"}, 0);
            if (strikePosMethod != null) {
                strikePosMethod.setAccessible(true);
                strikePos = strikePosMethod.invoke(lightningBolt);
            }
            if (strikePos == null) {
                strikePos = getFallbackStrikePosition(lightningBolt);
            }
            if (strikePos == null) return;

            Object state = getBlockState(level, strikePos);
            Object block = state != null ? getBlock(state) : null;

            if (block == null || !isCabLightningRod(block)) {
                Object belowPos = getRelativePos(strikePos, getDirectionDown());
                if (belowPos != null) {
                    Object belowState = getBlockState(level, belowPos);
                    Object belowBlock = belowState != null ? getBlock(belowState) : null;
                    if (belowBlock != null && isCabLightningRod(belowBlock)) {
                        strikePos = belowPos;
                        state = belowState;
                        block = belowBlock;
                    }
                }
            }

            if (block == null || !isCabLightningRod(block)) return;

            Method onStrikeMethod = findMethod(block.getClass(), new String[]{"onLightningStrike", "method_31648", "method_31637"}, 3);
            if (onStrikeMethod != null) {
                onStrikeMethod.setAccessible(true);
                onStrikeMethod.invoke(block, state, level, strikePos);
                LOGGER.info("[CopperAgeBackportPatch] Powered CAB lightning rod at {}", strikePos);
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
            Object block = state != null ? getBlock(state) : null;

            if (block == null || !isCabLightningRod(block)) {
                Object belowPos = getRelativePos(pos, getDirectionDown());
                if (belowPos != null) {
                    Object belowState = getBlockState(level, belowPos);
                    Object belowBlock = belowState != null ? getBlock(belowState) : null;
                    if (belowBlock != null && isCabLightningRod(belowBlock)) {
                        pos = belowPos;
                        state = belowState;
                        block = belowBlock;
                    }
                }
            }

            if (block == null || !isCabLightningRod(block)) {
                return; // Let vanilla handle non-CAB blocks
            }

            Object facing = getFacingValue(state, block);

            // 1. If unwaxed WeatheringCopper, de-oxidize the rod itself
            if (isWeatheringCopper(block)) {
                Object prevState = getPreviousState(state, block);
                if (prevState != null) {
                    setBlockAndUpdate(level, pos, prevState);
                    Object prevBlock = getBlock(prevState);
                    if (prevBlock != null) {
                        Method onStrikeMethod = findMethod(prevBlock.getClass(), new String[]{"onLightningStrike", "method_31648", "method_31637"}, 3);
                        if (onStrikeMethod != null) {
                            onStrikeMethod.setAccessible(true);
                            onStrikeMethod.invoke(prevBlock, prevState, level, pos);
                        }
                    }
                    if (facing == null && prevBlock != null) {
                        facing = getFacingValue(prevState, prevBlock);
                    }
                    playLevelEvent(level, 3002, pos, getAxisOrdinal(facing));
                    LOGGER.info("[CopperAgeBackportPatch] De-oxidized CAB lightning rod at {}", pos);
                }
            }

            // 2. Redirect to attached block behind the rod (for both waxed and unwaxed rods)
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
        if (block == null || block instanceof Boolean) return false;
        String name = block.getClass().getName();
        if (name.contains("CopperLightningRodBlock")) {
            return true;
        }
        String s = block.toString().toLowerCase(Locale.ROOT);
        for (String id : CAB_WEATHERING_ROD_IDS) {
            if (s.contains(id)) return true;
        }
        for (String id : CAB_WAXED_ROD_IDS) {
            if (s.contains(id)) return true;
        }
        return false;
    }

    public static boolean isWeatheringCopper(Object block) {
        if (block == null || block instanceof Boolean) return false;
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
                    Method withProps = findMethod(prevBlock.getClass(), new String[]{"withPropertiesOf", "method_34725", "method_34734"}, 1);
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
            Field facingField = findField(block.getClass(), "FACING", "field_10927", "field_12525", "field_11177");
            if (facingField == null) {
                Class<?> bspClass = resolveClass("net.minecraft.world.level.block.state.properties.BlockStateProperties", "net.minecraft.class_2741");
                if (bspClass != null) {
                    facingField = findField(bspClass, "FACING", "field_12525");
                }
            }
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
                    Method mutableMethod = findMethod(targetPos.getClass(), new String[]{"mutable", "method_25503"}, 0);
                    Object mutable = mutableMethod != null ? mutableMethod.invoke(targetPos) : null;
                    if (mutable == null) {
                        try {
                            Class<?> mutableClass = resolveClass("net.minecraft.core.BlockPos$MutableBlockPos", "net.minecraft.class_2338$class_2339");
                            if (mutableClass != null) {
                                Constructor<?> ctor = mutableClass.getDeclaredConstructor();
                                ctor.setAccessible(true);
                                mutable = ctor.newInstance();
                            }
                        } catch (Throwable ignored) {}
                    }
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
            Method m = findMethod(level.getClass(), new String[]{"setBlockAndUpdate", "method_8501", "method_30092"}, 2);
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
            Method m = findMethod(level.getClass(), new String[]{"levelEvent", "method_20290", "method_8474", "method_8444"}, 3);
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
        // 0. Direct CAB ModBlocks supplier lookup for CAB blocks
        if ("minecraft".equals(namespace) || "copperagebackport".equals(namespace)) {
            String upper = path.toUpperCase(Locale.ROOT);
            try {
                Class<?> modBlocksClass = resolveClass("com.github.smallinger.copperagebackport.registry.ModBlocks");
                if (modBlocksClass != null) {
                    Field field = findField(modBlocksClass, upper);
                    if (field != null) {
                        field.setAccessible(true);
                        Object supplier = field.get(null);
                        if (supplier instanceof Supplier<?> s) {
                            Object block = s.get();
                            if (block != null && !isAir(block)) {
                                return block;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 1. Fallback for vanilla lightning_rod directly from Blocks
        if ("minecraft".equals(namespace) && "lightning_rod".equals(path)) {
            try {
                Class<?> blocksMojang = resolveClass("net.minecraft.world.level.block.Blocks");
                if (blocksMojang != null) {
                    Field f = findField(blocksMojang, "LIGHTNING_ROD");
                    if (f != null) {
                        f.setAccessible(true);
                        Object block = f.get(null);
                        if (block != null && !isAir(block)) return block;
                    }
                }
            } catch (Throwable ignored) {}
            try {
                Class<?> blocksIntermediary = resolveClass("net.minecraft.class_2246");
                if (blocksIntermediary != null) {
                    Field f = findField(blocksIntermediary, "field_27171", "LIGHTNING_ROD");
                    if (f != null) {
                        f.setAccessible(true);
                        Object block = f.get(null);
                        if (block != null && !isAir(block)) return block;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 2. Try Mojang BuiltInRegistries.BLOCK
        try {
            Class<?> regClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            Field blockRegField = regClass.getField("BLOCK");
            Object blockReg = blockRegField.get(null);
            Object loc = createIdentifier(namespace, path);
            if (loc != null && blockReg != null) {
                Method getMethod = blockReg.getClass().getMethod("get", loc.getClass());
                Object res = getMethod.invoke(blockReg, loc);
                if (res != null && !isAir(res)) return res;
            }
        } catch (Throwable ignored) {}

        // 3. Try Fabric Intermediary class_7923.field_41175
        try {
            Class<?> regClass = Class.forName("net.minecraft.class_7923");
            Field blockRegField = findField(regClass, "field_41175", "BLOCK");
            if (blockRegField != null) {
                blockRegField.setAccessible(true);
                Object blockReg = blockRegField.get(null);
                Object id = createIdentifier(namespace, path);
                if (id != null && blockReg != null) {
                    Method getMethod = findMethod(blockReg.getClass(), new String[]{"get", "method_10223", "getValue"}, 1);
                    if (getMethod != null) {
                        Object res = getMethod.invoke(blockReg, id);
                        if (res != null && !isAir(res)) return res;
                    }
                }
            }
        } catch (Throwable ignored) {}

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
        if (block instanceof Boolean) return true;
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
        String s = block.toString().toLowerCase(Locale.ROOT);
        return "air".equals(s) || "minecraft:air".equals(s) || s.contains("block{minecraft:air}");
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

    public static int getAxisOrdinal(Object direction) {
        if (direction == null) return 1; // default to Y axis
        try {
            Method getAxisMethod = findMethod(direction.getClass(), new String[]{"getAxis", "method_10166"}, 0);
            if (getAxisMethod != null) {
                Object axis = getAxisMethod.invoke(direction);
                if (axis instanceof Enum<?> e) {
                    return e.ordinal();
                }
            }
        } catch (Throwable ignored) {}
        String s = direction.toString().toLowerCase();
        if (s.contains("x") || s.contains("west") || s.contains("east")) return 0;
        if (s.contains("z") || s.contains("north") || s.contains("south")) return 2;
        return 1;
    }

    public static Object getDirectionDown() {
        try {
            Class<?> dirClass = resolveClass("net.minecraft.core.Direction", "net.minecraft.class_2350");
            if (dirClass != null) {
                Field f = findField(dirClass, "DOWN", "field_11036");
                if (f != null) {
                    f.setAccessible(true);
                    return f.get(null);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static Object createBlockPos(int x, int y, int z) {
        try {
            Class<?> bpClass = resolveClass("net.minecraft.core.BlockPos", "net.minecraft.class_2338");
            if (bpClass != null) {
                Constructor<?> ctor = bpClass.getConstructor(int.class, int.class, int.class);
                return ctor.newInstance(x, y, z);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object getFallbackStrikePosition(Object entity) {
        if (entity == null) return null;
        try {
            Method getX = findMethod(entity.getClass(), new String[]{"getX", "method_23317"}, 0);
            Method getY = findMethod(entity.getClass(), new String[]{"getY", "method_23318"}, 0);
            Method getZ = findMethod(entity.getClass(), new String[]{"getZ", "method_23321"}, 0);
            if (getX != null && getY != null && getZ != null) {
                double x = ((Number) getX.invoke(entity)).doubleValue();
                double y = ((Number) getY.invoke(entity)).doubleValue();
                double z = ((Number) getZ.invoke(entity)).doubleValue();
                return createBlockPos((int) Math.floor(x), (int) Math.floor(y - 1.0E-6), (int) Math.floor(z));
            }
        } catch (Throwable ignored) {}
        try {
            Method blockPosMethod = findMethod(entity.getClass(), new String[]{"blockPosition", "method_24515"}, 0);
            if (blockPosMethod != null) {
                return blockPosMethod.invoke(entity);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Registers Fabric lifecycle event listeners for ServerWorldEvents.LOAD and ServerLifecycleEvents.SERVER_STARTED.
     */
    public static void registerFabricLifecycleEvents() {
        if (FABRIC_LIFECYCLE_REGISTERED.getAndSet(true)) {
            return;
        }

        // 1. ServerWorldEvents.LOAD
        try {
            Class<?> worldEventsClass = Class.forName("net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents");
            Field loadField = worldEventsClass.getField("LOAD");
            Object loadEvent = loadField.get(null);
            Class<?> loadCallbackClass = Class.forName("net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents$Load");
            Object listener = Proxy.newProxyInstance(
                    CopperLightningRodPatcher.class.getClassLoader(),
                    new Class<?>[]{loadCallbackClass},
                    (proxy, method, args) -> {
                        String mName = method.getName();
                        if ("hashCode".equals(mName)) return System.identityHashCode(proxy);
                        if ("equals".equals(mName)) return proxy == (args != null && args.length > 0 ? args[0] : null);
                        if ("toString".equals(mName)) return "CopperLightningRodFabricWorldLoadListener@" + Integer.toHexString(System.identityHashCode(proxy));
                        patchPoiTypes();
                        if (!weatheringPatched) {
                            weatheringPatched = patchWeatheringAndWaxing();
                        }
                        return null;
                    }
            );
            for (Method m : loadEvent.getClass().getMethods()) {
                if ("register".equals(m.getName()) && m.getParameterCount() == 1) {
                    m.setAccessible(true);
                    m.invoke(loadEvent, listener);
                    LOGGER.info("[CopperAgeBackportPatch] Registered ServerWorldEvents.LOAD listener for CAB lightning rod POI registration on Fabric.");
                    break;
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("[CopperAgeBackportPatch] Could not register ServerWorldEvents.LOAD: {}", t.getMessage());
        }

        // 2. ServerLifecycleEvents.SERVER_STARTED
        try {
            Class<?> serverEventsClass = Class.forName("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents");
            Field startedField = serverEventsClass.getField("SERVER_STARTED");
            Object startedEvent = startedField.get(null);
            Class<?> startedCallbackClass = Class.forName("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$ServerStarted");
            Object listener = Proxy.newProxyInstance(
                    CopperLightningRodPatcher.class.getClassLoader(),
                    new Class<?>[]{startedCallbackClass},
                    (proxy, method, args) -> {
                        String mName = method.getName();
                        if ("hashCode".equals(mName)) return System.identityHashCode(proxy);
                        if ("equals".equals(mName)) return proxy == (args != null && args.length > 0 ? args[0] : null);
                        if ("toString".equals(mName)) return "CopperLightningRodFabricServerStartedListener@" + Integer.toHexString(System.identityHashCode(proxy));
                        patchPoiTypes();
                        if (!weatheringPatched) {
                            weatheringPatched = patchWeatheringAndWaxing();
                        }
                        return null;
                    }
            );
            for (Method m : startedEvent.getClass().getMethods()) {
                if ("register".equals(m.getName()) && m.getParameterCount() == 1) {
                    m.setAccessible(true);
                    m.invoke(startedEvent, listener);
                    LOGGER.info("[CopperAgeBackportPatch] Registered ServerLifecycleEvents.SERVER_STARTED listener for CAB lightning rod POI registration on Fabric.");
                    break;
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("[CopperAgeBackportPatch] Could not register ServerLifecycleEvents.SERVER_STARTED: {}", t.getMessage());
        }
    }

    /**
     * Registers NeoForge lifecycle event listeners for LevelEvent.Load and ServerStartedEvent on NeoForge.EVENT_BUS.
     */
    public static void registerNeoForgeLifecycleEvents() {
        if (NEOFORGE_LIFECYCLE_REGISTERED.getAndSet(true)) {
            return;
        }
        try {
            Class<?> neoForgeClass = Class.forName("net.neoforged.neoforge.common.NeoForge");
            Field eventBusField = neoForgeClass.getField("EVENT_BUS");
            Object gameEventBus = eventBusField.get(null);
            if (gameEventBus != null) {
                Method addListenerMethod = null;
                for (Method m : gameEventBus.getClass().getMethods()) {
                    if ("addListener".equals(m.getName()) && m.getParameterCount() == 2
                            && m.getParameterTypes()[0] == Class.class && m.getParameterTypes()[1] == Consumer.class) {
                        addListenerMethod = m;
                        break;
                    }
                }
                if (addListenerMethod == null) {
                    addListenerMethod = gameEventBus.getClass().getMethod("addListener", Class.class, Consumer.class);
                }

                Consumer<Object> poiConsumer = event -> {
                    patchPoiTypes();
                    if (!weatheringPatched) {
                        weatheringPatched = patchWeatheringAndWaxing();
                    }
                };

                try {
                    Class<?> levelLoadEventClass = Class.forName("net.neoforged.neoforge.event.level.LevelEvent$Load");
                    addListenerMethod.invoke(gameEventBus, levelLoadEventClass, poiConsumer);
                    LOGGER.info("[CopperAgeBackportPatch] Registered LevelEvent.Load listener for CAB lightning rod POI registration on NeoForge.");
                } catch (Throwable t) {
                    LOGGER.debug("[CopperAgeBackportPatch] Could not register LevelEvent.Load listener: {}", t.getMessage());
                }

                try {
                    Class<?> serverStartedEventClass = Class.forName("net.neoforged.neoforge.event.server.ServerStartedEvent");
                    addListenerMethod.invoke(gameEventBus, serverStartedEventClass, poiConsumer);
                    LOGGER.info("[CopperAgeBackportPatch] Registered ServerStartedEvent listener for CAB lightning rod POI registration on NeoForge.");
                } catch (Throwable t) {
                    LOGGER.debug("[CopperAgeBackportPatch] Could not register ServerStartedEvent listener: {}", t.getMessage());
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("[CopperAgeBackportPatch] Could not register NeoForge game event bus listeners: {}", t.getMessage());
        }
    }
}
