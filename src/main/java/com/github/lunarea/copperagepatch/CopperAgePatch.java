package com.github.lunarea.copperagepatch;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge 1.21.1 entrypoint for Copper Age Patch.
 * Addresses duplicate ResourceKey[minecraft:armor_material / minecraft:copper]
 * registration crash in Copper Age Backport on NeoForge 21.1.250+.
 */
@Mod(CopperAgePatch.MOD_ID)
public class CopperAgePatch {
    public static final String MOD_ID = "copper_age_patch";
    public static final Logger LOGGER = LoggerFactory.getLogger(CopperAgePatch.class);

    public CopperAgePatch() {
        LOGGER.info("[CopperAgePatch] Initializing patch mod for Copper Age Backport.");
    }
}
