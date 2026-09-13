package com.github.lunarea.copperagepatch.fabric;

import com.github.lunarea.copperagepatch.creative.CopperCombatTabPatcher;
import com.github.lunarea.copperagepatch.durability.CopperArmorDurabilityPatcher;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric ModInitializer entrypoint for Copper Age Backport Patch.
 */
public class CopperAgePatchFabric implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopperAgePatchFabric.class);

    @Override
    public void onInitialize() {
        LOGGER.info("[CopperAgeBackportPatch] Initializing Copper Age Backport Patch on Fabric.");
        CopperArmorDurabilityPatcher.applyPatch();
        CopperCombatTabPatcher.registerFabricCombatTab();
    }
}
