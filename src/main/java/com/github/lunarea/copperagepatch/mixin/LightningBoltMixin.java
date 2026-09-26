package com.github.lunarea.copperagepatch.mixin;

import com.github.lunarea.copperagepatch.lightning.CopperLightningRodPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting LightningBolt to ensure Copper Age Backport lightning rods
 * emit redstone power signals and correctly handle de-oxidation and lightning cleaning.
 */
@Mixin(targets = "net.minecraft.world.entity.LightningBolt")
public abstract class LightningBoltMixin {

    @Inject(method = "powerLightningRod", at = @At("HEAD"))
    private void copper_age_patch$onPowerLightningRod(CallbackInfo ci) {
        CopperLightningRodPatcher.onPowerLightningRod(this);
    }

    @Inject(method = "clearCopperOnLightningStrike", at = @At("HEAD"), cancellable = true)
    private static void copper_age_patch$onClearCopper(@Coerce Object level, @Coerce Object pos, CallbackInfo ci) {
        CopperLightningRodPatcher.onClearCopper(level, pos, ci);
    }
}
