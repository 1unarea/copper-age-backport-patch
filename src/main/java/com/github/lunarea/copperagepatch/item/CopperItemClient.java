package com.github.lunarea.copperagepatch.item;

import com.github.lunarea.copperagepatch.durability.CopperArmorDurabilityPatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Client-side property registration for Copper Shield (blocking predicate).
 * Allows the shield to properly rotate into blocking position in first/third person when holding right-click.
 *
 * Implemented completely mapping-agnostic via dynamic proxies with ZERO net/minecraft/ class
 * references in bytecode descriptors.
 */
public final class CopperItemClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopperItemClient.class);

    private CopperItemClient() {}

    public static void initClient() {
        registerShieldBlockingProperty();
    }

    private static void registerShieldBlockingProperty() {
        Object shield = CopperItems.getCopperShield();
        if (shield == null) {
            return;
        }

        try {
            Class<?> itemPropertiesClass = null;
            for (String name : new String[]{"net.minecraft.client.renderer.item.ItemProperties", "net.minecraft.class_630"}) {
                try {
                    itemPropertiesClass = Class.forName(name);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }
            if (itemPropertiesClass == null) return;

            Class<?> propertyInterface = null;
            for (String name : new String[]{"net.minecraft.client.renderer.item.ClampedItemPropertyFunction", "net.minecraft.class_631"}) {
                try {
                    propertyInterface = Class.forName(name);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }
            if (propertyInterface == null) return;

            Object blockingId = CopperArmorDurabilityPatcher.createIdentifier("minecraft", "blocking");
            if (blockingId == null) return;

            Object propertyFunction = Proxy.newProxyInstance(
                    CopperItemClient.class.getClassLoader(),
                    new Class<?>[]{propertyInterface},
                    (proxy, method, args) -> {
                        if (args != null && args.length >= 3) {
                            Object stack = args[0];
                            Object entity = args[2];
                            if (entity != null) {
                                try {
                                    boolean isUsing = false;
                                    for (String mName : new String[]{"isUsingItem", "method_6115"}) {
                                        try {
                                            Method m = entity.getClass().getMethod(mName);
                                            isUsing = (boolean) m.invoke(entity);
                                            break;
                                        } catch (Throwable ignored) {}
                                    }
                                    if (isUsing) {
                                        Object useItem = null;
                                        for (String mName : new String[]{"getUseItem", "method_6017"}) {
                                            try {
                                                Method m = entity.getClass().getMethod(mName);
                                                useItem = m.invoke(entity);
                                                break;
                                            } catch (Throwable ignored) {}
                                        }
                                        if (useItem != null && (useItem == stack || isSameItem(useItem, stack))) {
                                            return 1.0F;
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                        return 0.0F;
                    }
            );

            for (Method m : itemPropertiesClass.getMethods()) {
                if (m.getParameterCount() == 3 && ("register".equals(m.getName()) || "method_2789".equals(m.getName()))) {
                    m.setAccessible(true);
                    m.invoke(null, shield, blockingId, propertyFunction);
                    LOGGER.info("[CopperAgeBackportPatch] Registered blocking item property for Copper Shield on client.");
                    return;
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[CopperAgeBackportPatch] Could not register blocking property for Copper Shield: {}", t.getMessage());
        }
    }

    private static boolean isSameItem(Object stackA, Object stackB) {
        if (stackA == null || stackB == null) return false;
        if (stackA == stackB) return true;
        try {
            for (String mName : new String[]{"getItem", "method_7909"}) {
                try {
                    Method getItemMethod = stackA.getClass().getMethod(mName);
                    Object itemA = getItemMethod.invoke(stackA);
                    Object itemB = getItemMethod.invoke(stackB);
                    return itemA == itemB;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
