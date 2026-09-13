package com.github.lunarea.copperagepatch;

import com.github.lunarea.copperagepatch.durability.CopperArmorDurabilityPatcher;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated verification unit tests for copper armor durability fix.
 * Verifies canonical durability values (multiplier 11), ItemStack behavior,
 * null safety, idempotency, and component mapping.
 */
@SuppressWarnings({"deprecation", "removal"})
public class CopperArmorDurabilityVerificationTest {

    private static DataComponentMap originalHelmetComponents;
    private static DataComponentMap originalBootsComponents;
    private static Field componentsField;
    private static Unsafe unsafe;

    @BeforeAll
    static void init() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        originalHelmetComponents = Items.CHAINMAIL_HELMET.components();
        originalBootsComponents = Items.CHAINMAIL_BOOTS.components();

        componentsField = CopperArmorDurabilityPatcher.findComponentsField();
        assertNotNull(componentsField, "Must locate components field");

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);
    }

    @AfterAll
    static void restore() {
        if (unsafe != null && componentsField != null) {
            long offset = unsafe.objectFieldOffset(componentsField);
            if (originalHelmetComponents != null) {
                unsafe.putObject(Items.CHAINMAIL_HELMET, offset, originalHelmetComponents);
            }
            if (originalBootsComponents != null) {
                unsafe.putObject(Items.CHAINMAIL_BOOTS, offset, originalBootsComponents);
            }
        }
    }

    @Test
    @DisplayName("Verify canonical multiplier 11 durability values for all armor pieces")
    void testCanonicalDurabilityValues() {
        // Multiplier 11 derivation:
        // Helmet:     11 * 11 = 121
        // Chestplate: 16 * 11 = 176
        // Leggings:   15 * 11 = 165
        // Boots:      13 * 11 = 143
        assertEquals(121, CopperArmorDurabilityPatcher.HELMET_DURABILITY);
        assertEquals(176, CopperArmorDurabilityPatcher.CHESTPLATE_DURABILITY);
        assertEquals(165, CopperArmorDurabilityPatcher.LEGGINGS_DURABILITY);
        assertEquals(143, CopperArmorDurabilityPatcher.BOOTS_DURABILITY);

        assertEquals(121, CopperArmorDurabilityPatcher.getCanonicalDurability("copper_helmet"));
        assertEquals(176, CopperArmorDurabilityPatcher.getCanonicalDurability("copper_chestplate"));
        assertEquals(165, CopperArmorDurabilityPatcher.getCanonicalDurability("copper_leggings"));
        assertEquals(143, CopperArmorDurabilityPatcher.getCanonicalDurability("copper_boots"));
        assertEquals(-1, CopperArmorDurabilityPatcher.getCanonicalDurability("unknown_item"));

        // Test namespace stripping and case insensitivity
        assertEquals(121, CopperArmorDurabilityPatcher.getCanonicalDurability("minecraft:copper_helmet"));
        assertEquals(176, CopperArmorDurabilityPatcher.getCanonicalDurability("copperagebackport:copper_chestplate"));
        assertEquals(165, CopperArmorDurabilityPatcher.getCanonicalDurability("COPPER_LEGGINGS"));
        assertEquals(143, CopperArmorDurabilityPatcher.getCanonicalDurability("minecraft:COPPER_BOOTS"));
        assertEquals(-1, CopperArmorDurabilityPatcher.getCanonicalDurability(null));

        Map<String, Integer> map = CopperArmorDurabilityPatcher.getCanonicalDurabilities();
        assertEquals(4, map.size());
        assertEquals(121, map.get("copper_helmet"));
        assertEquals(176, map.get("copper_chestplate"));
        assertEquals(165, map.get("copper_leggings"));
        assertEquals(143, map.get("copper_boots"));
    }

    @Test
    @DisplayName("Verify findComponentsField locates DataComponentMap on Item class")
    void testFindComponentsField() {
        Field field = CopperArmorDurabilityPatcher.findComponentsField();
        assertNotNull(field, "findComponentsField must return non-null Field");
        assertTrue(field.canAccess(Items.IRON_HELMET), "Field must be accessible");
    }

    @Test
    @DisplayName("Verify durability patching on Item and subsequent ItemStack behavior")
    void testApplyDurabilityAndItemStack() {
        Item testArmor = Items.CHAINMAIL_HELMET;

        // Apply canonical helmet durability 121
        boolean applied = CopperArmorDurabilityPatcher.applyDurability(testArmor, 121);
        assertTrue(applied, "applyDurability must return true");

        // Verify component values on Item
        assertEquals(121, testArmor.components().get(DataComponents.MAX_DAMAGE));
        assertEquals(0, testArmor.components().get(DataComponents.DAMAGE));
        assertEquals(1, testArmor.components().get(DataComponents.MAX_STACK_SIZE));

        // Verify fresh ItemStack reflects patched values
        ItemStack stackAfter = new ItemStack(testArmor);
        assertEquals(121, stackAfter.getMaxDamage(), "After patch: max damage must be 121");
        assertTrue(stackAfter.isDamageableItem(), "After patch: isDamageableItem must be true");
        assertTrue(CopperArmorDurabilityPatcher.isDamageable(stackAfter));
        assertTrue(CopperArmorDurabilityPatcher.isDamageable(testArmor));

        // Verify damage progression
        assertFalse(stackAfter.isDamaged());
        stackAfter.setDamageValue(21);
        assertTrue(stackAfter.isDamaged());
        assertEquals(21, stackAfter.getDamageValue());
        assertEquals(100, stackAfter.getMaxDamage() - stackAfter.getDamageValue());

        // Verify item durability bar
        assertTrue(testArmor.isBarVisible(stackAfter));
        assertTrue(testArmor.getBarWidth(stackAfter) > 0);
    }

    private static void enableIntrusiveHolders(boolean enable) {
        try {
            Field holdersField = net.minecraft.core.MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
            holdersField.setAccessible(true);
            Field frozenField = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
            frozenField.setAccessible(true);
            if (enable) {
                holdersField.set(BuiltInRegistries.ITEM, new java.util.IdentityHashMap<>());
                frozenField.set(BuiltInRegistries.ITEM, false);
            } else {
                holdersField.set(BuiltInRegistries.ITEM, null);
                frozenField.set(BuiltInRegistries.ITEM, true);
            }
        } catch (Throwable ignored) {}
    }

    @Test
    @DisplayName("Verify full simulation of upstream Copper Age Backport armor items and applyPatch()")
    void testFullUpstreamArmorItemsPatchSimulation() {
        enableIntrusiveHolders(true);
        try {
            // Upstream registration in ModItems: new ArmorItem(..., Type, new Item.Properties().stacksTo(1))
            // Omitting durability, meaning MAX_DAMAGE is null, max damage is 0, and isDamageableItem() is false.
            net.minecraft.world.item.ArmorItem unpatchedHelmet = new net.minecraft.world.item.ArmorItem(
                    net.minecraft.world.item.ArmorMaterials.IRON,
                    net.minecraft.world.item.ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1)
            );
            net.minecraft.world.item.ArmorItem unpatchedChestplate = new net.minecraft.world.item.ArmorItem(
                    net.minecraft.world.item.ArmorMaterials.IRON,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().stacksTo(1)
            );
            net.minecraft.world.item.ArmorItem unpatchedLeggings = new net.minecraft.world.item.ArmorItem(
                    net.minecraft.world.item.ArmorMaterials.IRON,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
                    new Item.Properties().stacksTo(1)
            );
            net.minecraft.world.item.ArmorItem unpatchedBoots = new net.minecraft.world.item.ArmorItem(
                    net.minecraft.world.item.ArmorMaterials.IRON,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS,
                    new Item.Properties().stacksTo(1)
            );

        // Verify pre-patch state
        assertEquals(0, new ItemStack(unpatchedHelmet).getMaxDamage());
        assertFalse(new ItemStack(unpatchedHelmet).isDamageableItem());
        assertFalse(CopperArmorDurabilityPatcher.isDamageable(unpatchedHelmet));

        assertEquals(0, new ItemStack(unpatchedChestplate).getMaxDamage());
        assertFalse(new ItemStack(unpatchedChestplate).isDamageableItem());
        assertFalse(CopperArmorDurabilityPatcher.isDamageable(unpatchedChestplate));

        assertEquals(0, new ItemStack(unpatchedLeggings).getMaxDamage());
        assertFalse(new ItemStack(unpatchedLeggings).isDamageableItem());
        assertFalse(CopperArmorDurabilityPatcher.isDamageable(unpatchedLeggings));

        assertEquals(0, new ItemStack(unpatchedBoots).getMaxDamage());
        assertFalse(new ItemStack(unpatchedBoots).isDamageableItem());
        assertFalse(CopperArmorDurabilityPatcher.isDamageable(unpatchedBoots));

        // Inject into ModItems supplier fields
        com.github.smallinger.copperagebackport.registry.ModItems.COPPER_HELMET = () -> unpatchedHelmet;
        com.github.smallinger.copperagebackport.registry.ModItems.COPPER_CHESTPLATE = () -> unpatchedChestplate;
        com.github.smallinger.copperagebackport.registry.ModItems.COPPER_LEGGINGS = () -> unpatchedLeggings;
        com.github.smallinger.copperagebackport.registry.ModItems.COPPER_BOOTS = () -> unpatchedBoots;

        // Execute patcher
        CopperArmorDurabilityPatcher.applyPatch();

        // Acceptance Criteria Verification:
        // 1. Copper Helmet reports max damage of 121 and isDamageable() is true
        ItemStack helmetStack = new ItemStack(unpatchedHelmet);
        assertEquals(121, helmetStack.getMaxDamage(), "Copper Helmet must report 121 max durability");
        assertTrue(helmetStack.isDamageableItem(), "Copper Helmet isDamageableItem must be true");
        assertTrue(CopperArmorDurabilityPatcher.isDamageable(helmetStack));
        assertTrue(CopperArmorDurabilityPatcher.isDamageable(unpatchedHelmet));

        // 2. Copper Chestplate reports max damage of 176 and isDamageable() is true
        ItemStack chestplateStack = new ItemStack(unpatchedChestplate);
        assertEquals(176, chestplateStack.getMaxDamage(), "Copper Chestplate must report 176 max durability");
        assertTrue(chestplateStack.isDamageableItem(), "Copper Chestplate isDamageableItem must be true");
        assertTrue(CopperArmorDurabilityPatcher.isDamageable(chestplateStack));
        assertTrue(CopperArmorDurabilityPatcher.isDamageable(unpatchedChestplate));

        // 3. Copper Leggings reports max damage of 165 and isDamageable() is true
        ItemStack leggingsStack = new ItemStack(unpatchedLeggings);
        assertEquals(165, leggingsStack.getMaxDamage(), "Copper Leggings must report 165 max durability");
        assertTrue(chestplateStack.isDamageableItem(), "Copper Leggings isDamageableItem must be true");
        assertTrue(CopperArmorDurabilityPatcher.isDamageable(leggingsStack));
        assertTrue(CopperArmorDurabilityPatcher.isDamageable(unpatchedLeggings));

        // 4. Copper Boots reports max damage of 143 and isDamageable() is true
        ItemStack bootsStack = new ItemStack(unpatchedBoots);
        assertEquals(143, bootsStack.getMaxDamage(), "Copper Boots must report 143 max durability");
        assertTrue(bootsStack.isDamageableItem(), "Copper Boots isDamageableItem must be true");
        assertTrue(CopperArmorDurabilityPatcher.isDamageable(bootsStack));
        assertTrue(CopperArmorDurabilityPatcher.isDamageable(unpatchedBoots));

        // Verify wear & tear and durability bar
        helmetStack.setDamageValue(50);
        assertTrue(helmetStack.isDamaged());
        assertEquals(71, helmetStack.getMaxDamage() - helmetStack.getDamageValue());
        assertTrue(unpatchedHelmet.isBarVisible(helmetStack));
        assertTrue(unpatchedHelmet.getBarWidth(helmetStack) > 0);
        } finally {
            enableIntrusiveHolders(false);
        }
    }

    @Test
    @DisplayName("Verify idempotency: re-applying same durability is a successful no-op")
    void testIdempotence() {
        Item testArmor = Items.CHAINMAIL_BOOTS;
        assertTrue(CopperArmorDurabilityPatcher.applyDurability(testArmor, 143));
        assertEquals(143, testArmor.components().get(DataComponents.MAX_DAMAGE));

        // Second call should return true without failure
        assertTrue(CopperArmorDurabilityPatcher.applyDurability(testArmor, 143));
        assertEquals(143, testArmor.components().get(DataComponents.MAX_DAMAGE));
    }

    @Test
    @DisplayName("Verify null-safety and invalid durability rejection of applyDurability and isDamageable")
    void testNullAndInvalidInputs() {
        assertFalse(CopperArmorDurabilityPatcher.applyDurability(null, 121));
        assertFalse(CopperArmorDurabilityPatcher.applyDurability(Items.CHAINMAIL_HELMET, 0));
        assertFalse(CopperArmorDurabilityPatcher.applyDurability(Items.CHAINMAIL_HELMET, -10));
        assertFalse(CopperArmorDurabilityPatcher.isDamageable((ItemStack) null));
        assertFalse(CopperArmorDurabilityPatcher.isDamageable((Item) null));
    }

    @Test
    @DisplayName("Verify applyDurability patches missing DAMAGE component even if MAX_DAMAGE is already set")
    void testPartialDurabilityFixRepairsDamageComponent() {
        enableIntrusiveHolders(true);
        try {
            net.minecraft.world.item.ArmorItem partialItem = new net.minecraft.world.item.ArmorItem(
                    net.minecraft.world.item.ArmorMaterials.IRON,
                    net.minecraft.world.item.ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1)
            );

            // Artificially put MAX_DAMAGE without DAMAGE (simulating defective partial patch)
            long offset = unsafe.objectFieldOffset(componentsField);
            DataComponentMap.Builder builder = DataComponentMap.builder();
            builder.addAll(partialItem.components());
            builder.set(DataComponents.MAX_DAMAGE, 121);
            // Notice: DataComponents.DAMAGE is intentionally omitted
            unsafe.putObject(partialItem, offset, builder.build());

            // Prior to fix: DAMAGE component is missing, so ItemStack is NOT damageable
            assertFalse(new ItemStack(partialItem).isDamageableItem(), "Without DAMAGE component, isDamageableItem must be false");
            assertFalse(CopperArmorDurabilityPatcher.isDamageable(partialItem));

            // Run applyDurability: should detect incomplete configuration and apply DAMAGE component
            assertTrue(CopperArmorDurabilityPatcher.applyDurability(partialItem, 121));

            // After fix: DAMAGE is present and item is fully damageable
            assertTrue(new ItemStack(partialItem).isDamageableItem(), "After repair, isDamageableItem must be true");
            assertTrue(CopperArmorDurabilityPatcher.isDamageable(partialItem));
            assertEquals(121, new ItemStack(partialItem).getMaxDamage());
            assertEquals(0, new ItemStack(partialItem).getDamageValue());
        } finally {
            enableIntrusiveHolders(false);
        }
    }

    @Test
    @DisplayName("Verify findComponentsField never returns static field and identifies non-static instance field")
    void testFindComponentsFieldNonStaticIntegrity() {
        Field f = CopperArmorDurabilityPatcher.findComponentsField();
        assertNotNull(f);
        assertFalse(java.lang.reflect.Modifier.isStatic(f.getModifiers()), "components field must be non-static instance field");
        assertTrue(DataComponentMap.class.isAssignableFrom(f.getType()), "components field type must be DataComponentMap");
    }

    @Test
    @DisplayName("Verify isAir correctly identifies null, AIR item, and non-air items")
    void testIsAirIntegrity() {
        assertTrue(CopperArmorDurabilityPatcher.isAir(null), "null must be treated as air");
        assertTrue(CopperArmorDurabilityPatcher.isAir(Items.AIR), "Items.AIR must be air");
        assertFalse(CopperArmorDurabilityPatcher.isAir(Items.IRON_HELMET), "Items.IRON_HELMET must not be air");
        assertFalse(CopperArmorDurabilityPatcher.isAir(Items.CHAINMAIL_CHESTPLATE), "Chainmail chestplate must not be air");
    }

    interface SuperInterface {
        default String customMethod() { return "super"; }
    }

    interface SubInterface extends SuperInterface {}

    static class ImplementingClass implements SubInterface {}

    @Test
    @DisplayName("Verify findMethod resolves methods declared on inherited super-interfaces")
    void testFindMethodInterfaceHierarchy() throws Exception {
        java.lang.reflect.Method findMethod = CopperArmorDurabilityPatcher.class.getDeclaredMethod("findMethod", Class.class, String[].class, int.class);
        findMethod.setAccessible(true);

        java.lang.reflect.Method resolved = (java.lang.reflect.Method) findMethod.invoke(null, ImplementingClass.class, new String[]{"customMethod"}, 0);
        assertNotNull(resolved, "Must find method declared on super-interface");
        assertEquals("super", resolved.invoke(new ImplementingClass()));
    }
}
