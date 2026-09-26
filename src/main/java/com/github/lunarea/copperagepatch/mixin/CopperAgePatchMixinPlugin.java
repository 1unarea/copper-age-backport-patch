package com.github.lunarea.copperagepatch.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * Mixin configuration plugin to dynamically filter targets based on runtime loader environment.
 *
 * LightningBoltMixin targets the Mojang class name (NeoForge).
 * LightningBoltFabricMixin targets the Intermediary class name (Fabric).
 *
 * On Fabric: LightningBoltMixin is skipped (Mojang class does not exist).
 * On NeoForge: LightningBoltFabricMixin is skipped (Intermediary class does not exist).
 */
public class CopperAgePatchMixinPlugin implements IMixinConfigPlugin {

    private static final String MOJANG_MIXIN = "com.github.lunarea.copperagepatch.mixin.LightningBoltMixin";
    private static final String FABRIC_MIXIN = "com.github.lunarea.copperagepatch.mixin.LightningBoltFabricMixin";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName == null) return true;
        String normalized = mixinClassName.replace('/', '.');
        if (MOJANG_MIXIN.equals(normalized)) {
            // Only apply on NeoForge (not Intermediary)
            return !isIntermediary();
        }
        if (FABRIC_MIXIN.equals(normalized)) {
            // Only apply on Fabric (Intermediary)
            return isIntermediary();
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    /**
     * Checks if current runtime is in Fabric Intermediary mapping environment.
     */
    public static boolean isIntermediary() {
        try {
            Class<?> flClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Method getInstance = flClass.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            if (instance != null) {
                Method getMappingResolver = flClass.getMethod("getMappingResolver");
                Object resolver = getMappingResolver.invoke(instance);
                if (resolver != null) {
                    Method getCurrentNamespace = resolver.getClass().getMethod("getCurrentRuntimeNamespace");
                    Object ns = getCurrentNamespace.invoke(resolver);
                    return "intermediary".equals(ns);
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Class.forName("net.minecraft.class_1538");
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }
}
