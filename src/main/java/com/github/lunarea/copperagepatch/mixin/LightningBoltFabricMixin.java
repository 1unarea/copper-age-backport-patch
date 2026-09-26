package com.github.lunarea.copperagepatch.mixin;

import com.github.lunarea.copperagepatch.lightning.CopperLightningRodPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting LightningBolt on Fabric (Intermediary mappings) to ensure Copper Age Backport
 * lightning rods emit redstone power signals and correctly handle de-oxidation and lightning cleaning.
 *
 * For NeoForge (Mojang mappings), see LightningBoltMixin.
 * The CopperAgePatchMixinPlugin filters out the inappropriate mixin at runtime.
 */
@Mixin(targets = "net.minecraft.class_1538", remap = false)
public abstract class LightningBoltFabricMixin {

    @Inject(method = "method_31499", at = @At("HEAD"), remap = false)
    private void copper_age_patch$onPowerLightningRod(CallbackInfo ci) {
        CopperLightningRodPatcher.onPowerLightningRod(this);
    }

    @Inject(method = "method_34707", at = @At("HEAD"), cancellable = true, remap = false)
    private static void copper_age_patch$onClearCopper(@Coerce Object level, @Coerce Object pos, CallbackInfo ci) {
        CopperLightningRodPatcher.onClearCopper(level, pos, ci);
    }
}
