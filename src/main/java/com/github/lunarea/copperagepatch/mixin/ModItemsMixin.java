package com.github.lunarea.copperagepatch.mixin;

import com.github.lunarea.copperagepatch.creative.CopperCombatTabPatcher;
import com.github.lunarea.copperagepatch.durability.CopperArmorDurabilityPatcher;
import com.github.smallinger.copperagebackport.registry.ModItems;
import com.github.smallinger.copperagebackport.registry.RegistryHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting ModItems to hook item registration completion across both loaders.
 *
 * Targets mod class with remap = false and has zero net/minecraft references in bytecode
 * descriptors, ensuring 100% mapping-agnostic safety across NeoForge and Fabric Intermediary.
 */
@Mixin(value = ModItems.class, remap = false)
public abstract class ModItemsMixin {

    @Inject(method = "register", at = @At("RETURN"), remap = false)
    private static void copper_age_patch$onRegisterItems(CallbackInfo ci) {
        // 1. Hook into RegistryHelper registration completion callbacks
        try {
            RegistryHelper.getInstance().onRegisterComplete(CopperArmorDurabilityPatcher::applyPatch);
        } catch (Throwable ignored) {}

        // 2. Register Fabric Creative Combat tab placement for Copper Axe
        try {
            CopperCombatTabPatcher.registerFabricCombatTab();
        } catch (Throwable ignored) {}

        // 3. Eager durability patch attempt (e.g. Fabric where items register synchronously in registerAuto)
        try {
            CopperArmorDurabilityPatcher.applyPatch();
        } catch (Throwable ignored) {}
    }
}
