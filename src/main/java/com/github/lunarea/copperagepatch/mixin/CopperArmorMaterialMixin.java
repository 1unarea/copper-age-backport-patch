package com.github.lunarea.copperagepatch.mixin;

import com.github.lunarea.copperagepatch.util.MemoizedSupplier;
import com.github.smallinger.copperagebackport.item.armor.CopperArmorMaterial;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

/**
 * Mixin targeting CopperArmorMaterial to prevent duplicate registration of
 * ResourceKey[minecraft:armor_material / minecraft:copper] in BuiltInRegistries.ARMOR_MATERIAL.
 *
 * Upstream bug in Copper Age Backport:
 * CopperArmorMaterial.init() assigns COPPER = () -> createCopper().
 * Every time an armor item is registered (helmet, chestplate, leggings, boots, horse armor),
 * COPPER.get() is called, which invokes createCopper().
 * In NeoForge 21.1.237+, duplicate keys are strictly rejected by the registry, crashing the game.
 *
 * This mixin:
 * 1. Wraps COPPER in a thread-safe MemoizedSupplier at the end of CopperArmorMaterial.init().
 * 2. Directly guards createCopper() so that subsequent calls immediately return the cached Holder.
 */
@Mixin(value = CopperArmorMaterial.class, remap = false)
public abstract class CopperArmorMaterialMixin {

    @Shadow
    public static Supplier<Holder<ArmorMaterial>> COPPER;

    @Unique
    private static volatile Holder<ArmorMaterial> copper_age_patch$cachedHolder;

    /**
     * Intercepts CopperArmorMaterial.init() to wrap COPPER with a memoizing supplier.
     */
    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private static void copper_age_patch$memoizeCopperSupplier(CallbackInfo ci) {
        if (COPPER != null && !(COPPER instanceof MemoizedSupplier)) {
            COPPER = new MemoizedSupplier<>(COPPER);
        }
    }

    /**
     * Intercepts createCopper() at HEAD. If the material was already registered and cached,
     * immediately returns the cached Holder without executing the registration body again.
     */
    @Inject(method = "createCopper", at = @At("HEAD"), cancellable = true, remap = false)
    private static void copper_age_patch$onPreCreateCopper(CallbackInfoReturnable<Holder<ArmorMaterial>> cir) {
        if (copper_age_patch$cachedHolder != null) {
            cir.setReturnValue(copper_age_patch$cachedHolder);
        }
    }

    /**
     * Intercepts createCopper() at RETURN to record the created Holder in the static cache.
     */
    @Inject(method = "createCopper", at = @At("RETURN"), remap = false)
    private static void copper_age_patch$onPostCreateCopper(CallbackInfoReturnable<Holder<ArmorMaterial>> cir) {
        if (copper_age_patch$cachedHolder == null && cir.getReturnValue() != null) {
            copper_age_patch$cachedHolder = cir.getReturnValue();
        }
    }
}
