package com.github.lunarea.copperagepatch;

import com.github.lunarea.copperagepatch.creative.CopperCombatTabPatcher;
import com.github.lunarea.copperagepatch.durability.CopperArmorDurabilityPatcher;
import net.minecraft.core.registries.Registries;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * NeoForge 1.21.1 entrypoint for Copper Age Backport Patch.
 * Addresses duplicate ResourceKey[minecraft:armor_material / minecraft:copper] registration crash,
 * restores Copper Armor canonical durability (multiplier 11), and adds Copper Axe to the Combat creative tab.
 */
@Mod(CopperAgePatch.MOD_ID)
public class CopperAgePatch {
    public static final String MOD_ID = "copper_age_patch";
    public static final Logger LOGGER = LoggerFactory.getLogger(CopperAgePatch.class);

    public CopperAgePatch() {
        LOGGER.info("[CopperAgeBackportPatch] Initializing Copper Age Backport Patch on NeoForge.");
        CopperArmorDurabilityPatcher.applyPatch();
        registerNeoForgeEvents();
    }

    private void registerNeoForgeEvents() {
        try {
            ModContainer container = null;
            try {
                container = net.neoforged.fml.ModList.get().getModContainerById(MOD_ID).orElse(null);
            } catch (Throwable ignored) {}
            if (container == null) {
                try {
                    container = ModLoadingContext.get().getActiveContainer();
                } catch (Throwable ignored) {}
            }
            if (container == null) {
                LOGGER.warn("[CopperAgeBackportPatch] No active ModContainer found during NeoForge init.");
                return;
            }
            Method getEventBusMethod = container.getClass().getMethod("getEventBus");
            Object eventBus = getEventBusMethod.invoke(container);
            if (eventBus == null) {
                LOGGER.warn("[CopperAgeBackportPatch] No event bus found on ModContainer.");
                return;
            }

            Method addListenerMethod = null;
            for (Method m : eventBus.getClass().getMethods()) {
                if ("addListener".equals(m.getName()) && m.getParameterCount() == 2
                        && m.getParameterTypes()[0] == Class.class && m.getParameterTypes()[1] == Consumer.class) {
                    addListenerMethod = m;
                    break;
                }
            }
            if (addListenerMethod == null) {
                addListenerMethod = eventBus.getClass().getMethod("addListener", Class.class, Consumer.class);
            }

            // 1. BuildCreativeModeTabContentsEvent -> CopperCombatTabPatcher & Durability
            try {
                Class<?> tabEventClass = Class.forName("net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent");
                Consumer<Object> tabConsumer = event -> {
                    CopperArmorDurabilityPatcher.applyPatch();
                    CopperCombatTabPatcher.applyNeoForgeCombatTabPlacement(event);
                };
                addListenerMethod.invoke(eventBus, tabEventClass, tabConsumer);
                LOGGER.info("[CopperAgeBackportPatch] Registered BuildCreativeModeTabContentsEvent listener on NeoForge.");
            } catch (Throwable t) {
                LOGGER.warn("[CopperAgeBackportPatch] Could not register BuildCreativeModeTabContentsEvent listener: {}", t.getMessage());
            }

            // 2. RegisterEvent -> Durability patch on item registry
            try {
                Class<?> registerEventClass = Class.forName("net.neoforged.neoforge.registries.RegisterEvent");
                Consumer<Object> registerConsumer = event -> {
                    try {
                        Method getRegistryKeyMethod = event.getClass().getMethod("getRegistryKey");
                        Object key = getRegistryKeyMethod.invoke(event);
                        if (Registries.ITEM.equals(key)) {
                            CopperArmorDurabilityPatcher.applyPatch();
                        }
                    } catch (Throwable ignored) {}
                };
                addListenerMethod.invoke(eventBus, registerEventClass, registerConsumer);
                LOGGER.info("[CopperAgeBackportPatch] Registered RegisterEvent listener on NeoForge.");
            } catch (Throwable t) {
                LOGGER.warn("[CopperAgeBackportPatch] Could not register RegisterEvent listener: {}", t.getMessage());
            }

            // 3. FMLCommonSetupEvent -> Enqueue durability patch
            try {
                Class<?> commonSetupClass = Class.forName("net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent");
                Consumer<Object> setupConsumer = event -> {
                    try {
                        Method enqueueWorkMethod = event.getClass().getMethod("enqueueWork", Runnable.class);
                        enqueueWorkMethod.invoke(event, (Runnable) CopperArmorDurabilityPatcher::applyPatch);
                    } catch (Throwable ignored) {}
                };
                addListenerMethod.invoke(eventBus, commonSetupClass, setupConsumer);
                LOGGER.info("[CopperAgeBackportPatch] Registered FMLCommonSetupEvent listener on NeoForge.");
            } catch (Throwable t) {
                LOGGER.warn("[CopperAgeBackportPatch] Could not register FMLCommonSetupEvent listener: {}", t.getMessage());
            }

            // 4. AddPackFindersEvent -> Conditional modern spawn egg resource pack
            try {
                Class<?> packFindersClass = Class.forName("net.neoforged.neoforge.event.AddPackFindersEvent");
                Consumer<Object> packConsumer = com.github.lunarea.copperagepatch.spawnegg.CopperSpawnEggPatcher::onNeoForgeAddPackFinders;
                addListenerMethod.invoke(eventBus, packFindersClass, packConsumer);
                LOGGER.info("[CopperAgeBackportPatch] Registered AddPackFindersEvent listener on NeoForge.");
            } catch (Throwable t) {
                LOGGER.warn("[CopperAgeBackportPatch] Could not register AddPackFindersEvent listener: {}", t.getMessage());
            }
        } catch (Throwable t) {
            LOGGER.warn("[CopperAgeBackportPatch] Failed to register NeoForge event bus listeners: {}", t.getMessage());
        }
    }
}
