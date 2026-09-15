package com.github.lunarea.copperagepatch.fabric;

import com.github.lunarea.copperagepatch.spawnegg.CopperSpawnEggPatcher;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric ClientModInitializer entrypoint for Copper Age Backport Patch.
 * Handles client-side color handler registration to remove the spawn egg color tint filter.
 */
public class CopperAgePatchFabricClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopperAgePatchFabricClient.class);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[CopperAgeBackportPatch] Initializing Copper Age Backport Patch client on Fabric.");
        CopperSpawnEggPatcher.initFabricClient();
    }
}
