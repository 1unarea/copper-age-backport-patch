package com.github.lunarea.copperagepatch;

import com.github.lunarea.copperagepatch.creative.CopperCombatTabPatcher;
import com.github.lunarea.copperagepatch.durability.CopperArmorDurabilityPatcher;
import com.github.lunarea.copperagepatch.item.CopperItems;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated verification test for Copper Knife (Farmer's Delight) and Copper Shield (Shield Expansion).
 * Verifies clean-room creation, stats, durability, recipes, data-driven shield attributes,
 * tags, textures, models, and localization.
 */
public class CopperItemsVerificationTest {

    private static final Gson GSON = new Gson();

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        enableIntrusiveHolders(true);
    }

    @org.junit.jupiter.api.AfterAll
    static void cleanup() {
        enableIntrusiveHolders(false);
    }

    private static void enableIntrusiveHolders(boolean enable) {
        try {
            java.lang.reflect.Field holdersField = net.minecraft.core.MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
            holdersField.setAccessible(true);
            java.lang.reflect.Field frozenField = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
            frozenField.setAccessible(true);
            if (enable) {
                holdersField.set(net.minecraft.core.registries.BuiltInRegistries.ITEM, new java.util.IdentityHashMap<>());
                frozenField.set(net.minecraft.core.registries.BuiltInRegistries.ITEM, false);
            } else {
                holdersField.set(net.minecraft.core.registries.BuiltInRegistries.ITEM, null);
                frozenField.set(net.minecraft.core.registries.BuiltInRegistries.ITEM, true);
            }
        } catch (Throwable ignored) {}
    }

    @Test
    @DisplayName("Verify Copper Knife creation and canonical stats")
    void testCopperKnifeCreation() {
        Object knife = CopperItems.createKnifeItem(CopperItems.KNIFE_DURABILITY);
        assertNotNull(knife, "Copper knife item must instantiate successfully");
        assertTrue(knife instanceof Item, "Knife must instantiate as an Item");

        Item knifeItem = (Item) knife;
        ItemStack stack = new ItemStack(knifeItem);
        assertTrue(stack.isDamageableItem(), "Knife stack must be damageable");
        assertEquals(190, stack.getMaxDamage(), "Knife max damage must be canonical 190");
    }

    @Test
    @DisplayName("Verify Copper Shield creation and canonical stats")
    void testCopperShieldCreation() {
        Object shield = CopperItems.createShieldItem(CopperItems.SHIELD_DURABILITY);
        assertNotNull(shield, "Copper shield item must instantiate successfully");
        assertTrue(shield instanceof ShieldItem, "Shield must instantiate as ShieldItem");

        ShieldItem shieldItem = (ShieldItem) shield;
        ItemStack stack = new ItemStack(shieldItem);
        assertTrue(stack.isDamageableItem(), "Shield stack must be damageable");
        assertEquals(120, stack.getMaxDamage(), "Shield max damage must be canonical 120");
    }

    @Test
    @DisplayName("Verify Shield Expansion JSON attributes file")
    void testShieldExpansionAttributesJson() throws Exception {
        File file = new File("src/main/resources/data/shieldexp/shields/copper_shield.json");
        assertTrue(file.exists(), "Shield Expansion attributes JSON file must exist");

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            assertNotNull(json);
            assertEquals(25, json.get("cooldownTicks").getAsInt(), "cooldownTicks must match Shield Expansion balance");
            assertEquals(0.70, json.get("speedFactor").getAsDouble(), 0.001, "speedFactor must be 0.70");
            assertEquals(0.10, json.get("parryDamage").getAsDouble(), 0.001, "parryDamage must be 0.10");
            assertEquals(5, json.get("parryTicks").getAsInt(), "parryTicks must be 5");
            assertEquals(2, json.get("stamina").getAsInt(), "stamina must be 2");
            assertEquals(0.10, json.get("blastResistance").getAsDouble(), 0.001, "blastResistance must be 0.10");
            assertEquals(1, json.get("flatDamage").getAsInt(), "flatDamage must be 1");
        }
    }

    @Test
    @DisplayName("Verify Copper Knife crafting recipe")
    void testCopperKnifeRecipe() throws Exception {
        File file = new File("src/main/resources/data/farmersdelight/recipe/copper_knife.json");
        assertTrue(file.exists(), "Copper knife recipe must exist");

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            assertEquals("minecraft:crafting_shaped", json.get("type").getAsString());
            JsonObject result = json.getAsJsonObject("result");
            assertEquals("farmersdelight:copper_knife", result.get("id").getAsString());
        }
    }

    @Test
    @DisplayName("Verify Copper Shield crafting recipe")
    void testCopperShieldRecipe() throws Exception {
        File file = new File("src/main/resources/data/shieldexp/recipe/copper_shield.json");
        assertTrue(file.exists(), "Copper shield recipe must exist");

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            assertEquals("minecraft:crafting_shaped", json.get("type").getAsString());
            JsonObject result = json.getAsJsonObject("result");
            assertEquals("shieldexp:copper_shield", result.get("id").getAsString());
        }
    }

    @Test
    @DisplayName("Verify Conventional and Mod tags for Knife and Shield")
    void testTagsExist() throws Exception {
        String[] knifeTagPaths = {
                "src/main/resources/data/c/tags/item/tools/knife.json",
                "src/main/resources/data/c/tags/item/tools/knives.json",
                "src/main/resources/data/c/tags/item/knives.json",
                "src/main/resources/data/farmersdelight/tags/item/knives.json",
                "src/main/resources/data/farmersdelight/tags/item/enchantable/knife.json"
        };
        for (String path : knifeTagPaths) {
            File f = new File(path);
            assertTrue(f.exists(), "Tag file must exist: " + path);
            try (FileReader reader = new FileReader(f, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                assertTrue(json.getAsJsonArray("values").toString().contains("farmersdelight:copper_knife"));
            }
        }

        String[] shieldTagPaths = {
                "src/main/resources/data/c/tags/item/shields.json",
                "src/main/resources/data/c/tags/item/tools/shield.json",
                "src/main/resources/data/forge/tags/item/shields.json",
                "src/main/resources/data/shieldexp/tags/item/shields.json"
        };
        for (String path : shieldTagPaths) {
            File f = new File(path);
            assertTrue(f.exists(), "Tag file must exist: " + path);
            try (FileReader reader = new FileReader(f, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                assertTrue(json.getAsJsonArray("values").toString().contains("shieldexp:copper_shield"));
            }
        }
    }

    @Test
    @DisplayName("Verify textures and models exist and copper shield uses custom elements without builtin/entity")
    void testAssetsExist() throws Exception {
        assertTrue(new File("src/main/resources/assets/farmersdelight/textures/item/copper_knife.png").exists());
        assertTrue(new File("src/main/resources/assets/farmersdelight/models/item/copper_knife.json").exists());
        assertTrue(new File("src/main/resources/assets/shieldexp/textures/item/copper_shield.png").exists());
        assertTrue(new File("src/main/resources/assets/shieldexp/textures/item/copper.png").exists());

        File shieldModel = new File("src/main/resources/assets/shieldexp/models/item/copper_shield.json");
        assertTrue(shieldModel.exists());
        try (FileReader reader = new FileReader(shieldModel, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            assertFalse(json.has("parent"), "Shield model must not have parent builtin/entity");
            assertTrue(json.has("elements"), "Shield model must define 3D block elements");
            assertTrue(json.has("overrides"), "Shield model must define blocking overrides");
        }

        File shieldBlockingModel = new File("src/main/resources/assets/shieldexp/models/item/copper_shield_blocking.json");
        assertTrue(shieldBlockingModel.exists());
        try (FileReader reader = new FileReader(shieldBlockingModel, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            assertFalse(json.has("parent"), "Shield blocking model must not have parent builtin/entity");
            assertTrue(json.has("elements"), "Shield blocking model must define 3D block elements");
        }
    }

    @Test
    @DisplayName("Verify localization keys in en_us and tr_tr")
    void testLocalization() throws Exception {
        File fdEn = new File("src/main/resources/assets/farmersdelight/lang/en_us.json");
        try (FileReader reader = new FileReader(fdEn, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            assertEquals("Copper Knife", json.get("item.farmersdelight.copper_knife").getAsString());
        }

        File fdTr = new File("src/main/resources/assets/farmersdelight/lang/tr_tr.json");
        try (FileReader reader = new FileReader(fdTr, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            assertEquals("Bakır Bıçak", json.get("item.farmersdelight.copper_knife").getAsString());
        }

        File seEn = new File("src/main/resources/assets/shieldexp/lang/en_us.json");
        try (FileReader reader = new FileReader(seEn, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            assertEquals("Copper Shield", json.get("item.shieldexp.copper_shield").getAsString());
        }

        File seTr = new File("src/main/resources/assets/shieldexp/lang/tr_tr.json");
        try (FileReader reader = new FileReader(seTr, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            assertEquals("Bakır Kalkan", json.get("item.shieldexp.copper_shield").getAsString());
        }
    }

    @Test
    @DisplayName("Verify Fabric tab insertion helper fallback")
    void testFabricTabInsertion() {
        MockFabricEntries entries = new MockFabricEntries();
        Item knife = (Item) CopperItems.createKnifeItem(190);
        assertNotNull(knife);

        boolean inserted = CopperCombatTabPatcher.insertItemIntoFabricTab(entries, null, knife, true);
        assertTrue(inserted, "insertItemIntoFabricTab should succeed with fallback");
        assertFalse(entries.displayStacks.isEmpty(), "Stack should have been added to displayStacks");
    }

    public static class MockFabricEntries {
        public final List<Object> displayStacks = new ArrayList<>();

        public List<Object> getDisplayStacks() {
            return displayStacks;
        }

        public void add(ItemStack stack) {
            displayStacks.add(stack);
        }
    }
}
