package com.github.lunarea.copperagepatch;

import com.github.lunarea.copperagepatch.durability.CopperArmorDurabilityPatcher;
import com.github.smallinger.copperagebackport.item.tools.CopperTier;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AnvilRepairVerificationTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Verify CopperTier repair ingredient accepts copper ingots and rejects invalid items")
    void testCopperTierRepairIngredient() {
        Tier copperTier = CopperTier.INSTANCE;
        assertNotNull(copperTier, "CopperTier.INSTANCE must not be null");

        Ingredient repairIngredient = copperTier.getRepairIngredient();
        assertNotNull(repairIngredient, "Repair ingredient must not be null");

        ItemStack copperIngot = new ItemStack(Items.COPPER_INGOT);
        ItemStack ironIngot = new ItemStack(Items.IRON_INGOT);
        ItemStack diamond = new ItemStack(Items.DIAMOND);

        assertTrue(repairIngredient.test(copperIngot), "CopperTier repair ingredient must accept copper ingots");
        assertFalse(repairIngredient.test(ironIngot), "CopperTier repair ingredient must reject iron ingots");
        assertFalse(repairIngredient.test(diamond), "CopperTier repair ingredient must reject diamonds");
    }

    private static void enableIntrusiveHolders(boolean enable) {
        try {
            java.lang.reflect.Field holdersField = net.minecraft.core.MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
            holdersField.setAccessible(true);
            java.lang.reflect.Field frozenField = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
            frozenField.setAccessible(true);
            if (enable) {
                holdersField.set(BuiltInRegistries.ITEM, new java.util.IdentityHashMap<>());
                frozenField.set(BuiltInRegistries.ITEM, false);
                frozenField.set(BuiltInRegistries.ARMOR_MATERIAL, false);
            } else {
                holdersField.set(BuiltInRegistries.ITEM, null);
                frozenField.set(BuiltInRegistries.ITEM, true);
                frozenField.set(BuiltInRegistries.ARMOR_MATERIAL, true);
            }
        } catch (Throwable ignored) {}
    }

    @Test
    @DisplayName("Verify copper tools accept copper ingots for anvil repair via isValidRepairItem")
    void testCopperToolAnvilRepair() {
        enableIntrusiveHolders(true);
        try {
            SwordItem copperSword = new SwordItem(CopperTier.INSTANCE, new Item.Properties());
            ItemStack swordStack = new ItemStack(copperSword);

            ItemStack copperIngot = new ItemStack(Items.COPPER_INGOT);
            ItemStack ironIngot = new ItemStack(Items.IRON_INGOT);

            assertTrue(copperSword.isValidRepairItem(swordStack, copperIngot),
                    "Copper sword must accept COPPER_INGOT for anvil repair");
            assertFalse(copperSword.isValidRepairItem(swordStack, ironIngot),
                    "Copper sword must reject IRON_INGOT for anvil repair");
        } finally {
            enableIntrusiveHolders(false);
        }
    }

    @Test
    @DisplayName("Verify copper armor accepts copper ingots for anvil repair and is damageable")
    void testCopperArmorAnvilRepair() throws Exception {
        // 1. Verify upstream CAB's CopperArmorMaterial specifies COPPER_INGOT as repair ingredient
        java.lang.reflect.Method repairIngredientMethod = Class.forName("com.github.smallinger.copperagebackport.item.armor.CopperArmorMaterial")
                .getDeclaredMethod("lambda$createCopper$1");
        repairIngredientMethod.setAccessible(true);
        Ingredient cabRepairIngredient = (Ingredient) repairIngredientMethod.invoke(null);

        assertNotNull(cabRepairIngredient, "CAB CopperArmorMaterial repair ingredient must not be null");
        assertTrue(cabRepairIngredient.test(new ItemStack(Items.COPPER_INGOT)),
                "CAB CopperArmorMaterial must accept COPPER_INGOT for repair");
        assertFalse(cabRepairIngredient.test(new ItemStack(Items.IRON_INGOT)),
                "CAB CopperArmorMaterial must reject IRON_INGOT for repair");

        // 2. Construct ArmorMaterial holder with CAB's exact repair ingredient supplier
        Holder<ArmorMaterial> copperArmorHolder = Holder.direct(new ArmorMaterial(
                java.util.Map.of(
                        ArmorItem.Type.BOOTS, 1,
                        ArmorItem.Type.LEGGINGS, 3,
                        ArmorItem.Type.CHESTPLATE, 4,
                        ArmorItem.Type.HELMET, 2,
                        ArmorItem.Type.BODY, 4
                ),
                8,
                SoundEvents.ARMOR_EQUIP_IRON,
                () -> cabRepairIngredient,
                java.util.List.of(new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace("copper"))),
                0.0F,
                0.0F
        ));

        enableIntrusiveHolders(true);
        try {
            ArmorItem copperHelmet = new ArmorItem(copperArmorHolder, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1));

            // 3. Before durability patch: verify undamageable (upstream bug in CAB prevented anvil repair)
            assertFalse(CopperArmorDurabilityPatcher.isDamageable(copperHelmet), "Unpatched armor must lack durability");
            assertFalse(new ItemStack(copperHelmet).isDamageableItem(), "Unpatched armor stack must not be damageable in Anvil");

            // 4. Apply durability patch
            assertTrue(CopperArmorDurabilityPatcher.applyDurability(copperHelmet, CopperArmorDurabilityPatcher.HELMET_DURABILITY),
                    "Must successfully apply durability to copper helmet");

            // 5. After patch: verify damageable
            assertTrue(CopperArmorDurabilityPatcher.isDamageable(copperHelmet), "Patched armor must be damageable");
            assertEquals(121, new ItemStack(copperHelmet).getMaxDamage(), "Helmet must have 121 max durability");

            // 6. Verify isValidRepairItem accepts COPPER_INGOT
            ItemStack helmetStack = new ItemStack(copperHelmet);
            ItemStack copperIngot = new ItemStack(Items.COPPER_INGOT);
            ItemStack ironIngot = new ItemStack(Items.IRON_INGOT);

            assertTrue(copperHelmet.isValidRepairItem(helmetStack, copperIngot),
                    "Copper helmet must accept COPPER_INGOT for anvil repair");
            assertFalse(copperHelmet.isValidRepairItem(helmetStack, ironIngot),
                    "Copper helmet must reject IRON_INGOT for anvil repair");
        } finally {
            enableIntrusiveHolders(false);
        }
    }
}
