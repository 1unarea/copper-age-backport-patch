package com.github.lunarea.copperagepatch;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

public class CopperArmorDurabilityTest {

    private static Unsafe unsafe;
    private static Field componentsField;

    @BeforeAll
    static void init() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        for (Field field : Item.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType().isInterface()) {
                if (DataComponentMap.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    componentsField = field;
                    break;
                }
            }
        }
        assertNotNull(componentsField, "Must find components field on Item.class");
    }

    @Test
    @DisplayName("Verify component patching sets max damage, stack size, damage and updates ItemStack")
    void testPatchItemDurability() {
        Item testItem = Items.IRON_HELMET;
        int originalMaxDamage = new ItemStack(testItem).getMaxDamage();
        assertEquals(165, originalMaxDamage); // Iron helmet has 165 durability in vanilla

        // Apply custom durability patch (e.g. 121)
        DataComponentMap.Builder builder = DataComponentMap.builder();
        builder.addAll(testItem.components());
        builder.set(DataComponents.MAX_DAMAGE, 121);
        builder.set(DataComponents.DAMAGE, 0);
        builder.set(DataComponents.MAX_STACK_SIZE, 1);
        DataComponentMap patched = builder.build();

        long offset = unsafe.objectFieldOffset(componentsField);
        unsafe.putObject(testItem, offset, patched);

        // Verify on Item
        assertEquals(121, testItem.components().get(DataComponents.MAX_DAMAGE));
        assertEquals(0, testItem.components().get(DataComponents.DAMAGE));
        assertEquals(1, testItem.components().get(DataComponents.MAX_STACK_SIZE));

        // Verify on fresh ItemStack
        ItemStack stackAfter = new ItemStack(testItem);
        assertEquals(121, stackAfter.getMaxDamage(), "After patch: max damage must be 121");
        assertTrue(stackAfter.isDamageableItem(), "After patch: isDamageableItem must be true");

        // Verify damage behavior
        assertFalse(stackAfter.isDamaged());
        stackAfter.setDamageValue(10);
        assertTrue(stackAfter.isDamaged());
        assertEquals(10, stackAfter.getDamageValue());
        assertTrue(testItem.isBarVisible(stackAfter));
        assertTrue(testItem.getBarWidth(stackAfter) > 0);

        // Restore original
        builder = DataComponentMap.builder();
        builder.addAll(testItem.components());
        builder.set(DataComponents.MAX_DAMAGE, originalMaxDamage);
        unsafe.putObject(testItem, offset, builder.build());
    }
}
