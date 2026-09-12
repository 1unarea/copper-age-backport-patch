package com.github.lunarea.copperagepatch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DataJsonVerificationTest {

    private JsonObject parseResourceJson(String resourcePath) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Resource must exist at: " + resourcePath);
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                assertTrue(element.isJsonObject(), "Resource must be a valid JSON object: " + resourcePath);
                return element.getAsJsonObject();
            }
        }
    }

    @Test
    @DisplayName("Verify Better Combat attribute configurations exist and have valid parent references")
    void testBetterCombatAttributes() throws Exception {
        // Items with a defined parent in Better Combat (sword, axe, pickaxe use parent inheritance)
        Map<String, String> withParent = Map.of(
                "copper_sword", "bettercombat:sword",
                "copper_axe", "bettercombat:axe",
                "copper_pickaxe", "bettercombat:pickaxe"
        );
        // Items without an upstream parent — defined inline with 'attributes' block
        List<String> withInlineAttribs = List.of("copper_shovel", "copper_hoe");

        for (Map.Entry<String, String> entry : withParent.entrySet()) {
            String item = entry.getKey();
            String expectedParent = entry.getValue();

            // Check copperagebackport namespace
            String path = "data/copperagebackport/weapon_attributes/" + item + ".json";
            JsonObject json = parseResourceJson(path);
            assertTrue(json.has("parent"), path + " must define 'parent'");
            assertEquals(expectedParent, json.get("parent").getAsString(),
                    path + " parent must match " + expectedParent);

            // Check minecraft namespace
            String mcPath = "data/minecraft/weapon_attributes/" + item + ".json";
            JsonObject mcJson = parseResourceJson(mcPath);
            assertTrue(mcJson.has("parent"), mcPath + " must define 'parent'");
            assertEquals(expectedParent, mcJson.get("parent").getAsString(),
                    mcPath + " parent must match " + expectedParent);
        }

        // shovel & hoe: no upstream parent exists in Better Combat, so we use inline 'attributes'
        for (String item : withInlineAttribs) {
            String path = "data/copperagebackport/weapon_attributes/" + item + ".json";
            JsonObject json = parseResourceJson(path);
            assertTrue(json.has("attributes"),
                    path + " must define inline 'attributes' (no upstream bettercombat parent exists for this item type)");
            assertTrue(json.getAsJsonObject("attributes").has("attacks"),
                    path + " 'attributes' must contain 'attacks' array");

            String mcPath = "data/minecraft/weapon_attributes/" + item + ".json";
            JsonObject mcJson = parseResourceJson(mcPath);
            assertTrue(mcJson.has("attributes"),
                    mcPath + " must define inline 'attributes'");
            assertTrue(mcJson.getAsJsonObject("attributes").has("attacks"),
                    mcPath + " 'attributes' must contain 'attacks' array");
        }
    }

    @Test
    @DisplayName("Verify Create crushing recipes exist for all armor pieces and tools")
    void testCreateCrushingRecipes() throws Exception {
        List<String> items = List.of(
                "copper_helmet",
                "copper_chestplate",
                "copper_leggings",
                "copper_boots",
                "copper_sword",
                "copper_axe",
                "copper_pickaxe",
                "copper_shovel",
                "copper_hoe",
                "copper_horse_armor"
        );

        for (String item : items) {
            String path = "data/copper_age_patch/recipe/crushing/" + item + ".json";
            JsonObject json = parseResourceJson(path);

            assertTrue(json.has("type"), path + " must define recipe 'type'");
            assertEquals("create:crushing", json.get("type").getAsString(),
                    path + " type must be 'create:crushing'");

            assertTrue(json.has("ingredients"), path + " must define 'ingredients'");
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            assertFalse(ingredients.isEmpty(), path + " ingredients must not be empty");

            JsonObject firstIngredient = ingredients.get(0).getAsJsonObject();
            assertTrue(firstIngredient.has("item"), path + " ingredient must specify 'item'");
            assertEquals("minecraft:" + item, firstIngredient.get("item").getAsString());

            assertTrue(json.has("results"), path + " must define 'results'");
            JsonArray results = json.getAsJsonArray("results");
            assertFalse(results.isEmpty(), path + " results must not be empty");

            // Must output copper ingots or copper nuggets
            boolean hasCopperOutput = false;
            for (JsonElement res : results) {
                JsonObject resObj = res.getAsJsonObject();
                assertTrue(resObj.has("item"), "Result entry must have 'item'");
                String resItem = resObj.get("item").getAsString();
                if ("minecraft:copper_ingot".equals(resItem) || "minecraft:copper_nugget".equals(resItem)) {
                    hasCopperOutput = true;
                }
            }
            assertTrue(json.has("neoforge:conditions"), path + " must define 'neoforge:conditions'");
            JsonArray conditions = json.getAsJsonArray("neoforge:conditions");
            assertFalse(conditions.isEmpty(), path + " conditions must not be empty");
            JsonObject firstCondition = conditions.get(0).getAsJsonObject();
            assertEquals("neoforge:mod_loaded", firstCondition.get("type").getAsString());
            assertEquals("create", firstCondition.get("modid").getAsString());

            assertTrue(json.has("processingTime"), path + " must define 'processingTime'");
            assertTrue(json.get("processingTime").getAsInt() > 0, path + " processingTime must be positive");
        }
    }

    @Test
    @DisplayName("Verify Common NeoForge tags (c:armors/*, c:tools/*) exist and include copper gear")
    void testCommonTags() throws Exception {
        Map<String, String> commonArmorTags = Map.of(
                "data/c/tags/item/armors/helmets.json", "minecraft:copper_helmet",
                "data/c/tags/item/armors/chestplates.json", "minecraft:copper_chestplate",
                "data/c/tags/item/armors/leggings.json", "minecraft:copper_leggings",
                "data/c/tags/item/armors/boots.json", "minecraft:copper_boots"
        );

        for (Map.Entry<String, String> entry : commonArmorTags.entrySet()) {
            JsonObject json = parseResourceJson(entry.getKey());
            assertFalse(json.get("replace").getAsBoolean(), "Tag replace must be false");
            JsonArray values = json.getAsJsonArray("values");
            assertTrue(values.contains(new com.google.gson.JsonPrimitive(entry.getValue())),
                    entry.getKey() + " must contain " + entry.getValue());
        }

        Map<String, String> commonToolTags = Map.of(
                "data/c/tags/item/tools/swords.json", "minecraft:copper_sword",
                "data/c/tags/item/tools/axes.json", "minecraft:copper_axe",
                "data/c/tags/item/tools/pickaxes.json", "minecraft:copper_pickaxe",
                "data/c/tags/item/tools/shovels.json", "minecraft:copper_shovel",
                "data/c/tags/item/tools/hoes.json", "minecraft:copper_hoe"
        );

        for (Map.Entry<String, String> entry : commonToolTags.entrySet()) {
            JsonObject json = parseResourceJson(entry.getKey());
            assertFalse(json.get("replace").getAsBoolean(), "Tag replace must be false");
            JsonArray values = json.getAsJsonArray("values");
            assertTrue(values.contains(new com.google.gson.JsonPrimitive(entry.getValue())),
                    entry.getKey() + " must contain " + entry.getValue());
        }

        // Verify parent c:armors and c:tools tags
        JsonObject armorsJson = parseResourceJson("data/c/tags/item/armors.json");
        assertFalse(armorsJson.get("replace").getAsBoolean());
        JsonArray armorValues = armorsJson.getAsJsonArray("values");
        assertTrue(armorValues.contains(new com.google.gson.JsonPrimitive("#c:armors/helmets")));
        assertTrue(armorValues.contains(new com.google.gson.JsonPrimitive("#c:armors/chestplates")));
        assertTrue(armorValues.contains(new com.google.gson.JsonPrimitive("#c:armors/leggings")));
        assertTrue(armorValues.contains(new com.google.gson.JsonPrimitive("#c:armors/boots")));

        JsonObject toolsJson = parseResourceJson("data/c/tags/item/tools.json");
        assertFalse(toolsJson.get("replace").getAsBoolean());
        JsonArray toolValues = toolsJson.getAsJsonArray("values");
        assertTrue(toolValues.contains(new com.google.gson.JsonPrimitive("#c:tools/swords")));
        assertTrue(toolValues.contains(new com.google.gson.JsonPrimitive("#c:tools/axes")));
        assertTrue(toolValues.contains(new com.google.gson.JsonPrimitive("#c:tools/pickaxes")));
        assertTrue(toolValues.contains(new com.google.gson.JsonPrimitive("#c:tools/shovels")));
        assertTrue(toolValues.contains(new com.google.gson.JsonPrimitive("#c:tools/hoes")));
    }

    @Test
    @DisplayName("Verify Vanilla tags (data/minecraft/tags/item/*) include copper tools and weapons")
    void testVanillaTags() throws Exception {
        Map<String, String> vanillaTags = Map.of(
                "data/minecraft/tags/item/swords.json", "minecraft:copper_sword",
                "data/minecraft/tags/item/axes.json", "minecraft:copper_axe",
                "data/minecraft/tags/item/pickaxes.json", "minecraft:copper_pickaxe",
                "data/minecraft/tags/item/shovels.json", "minecraft:copper_shovel",
                "data/minecraft/tags/item/hoes.json", "minecraft:copper_hoe"
        );

        for (Map.Entry<String, String> entry : vanillaTags.entrySet()) {
            JsonObject json = parseResourceJson(entry.getKey());
            assertFalse(json.get("replace").getAsBoolean(), "Tag replace must be false");
            JsonArray values = json.getAsJsonArray("values");
            assertTrue(values.contains(new com.google.gson.JsonPrimitive(entry.getValue())),
                    entry.getKey() + " must contain " + entry.getValue());
        }
    }

    @Test
    @DisplayName("Verify all language files are valid JSON and define required tooltip keys")
    void testLangFiles() throws Exception {
        List<String> langCodes = List.of(
                "en_us", "en_gb", "tr_tr", "az_az", "de_de",
                "es_es", "es_mx", "es_ar", "es_cl", "es_ec", "es_uy", "es_ve",
                "pt_br", "pt_pt", "ru_ru", "ar_sa", "zh_cn", "zh_tw", "zh_hk",
                "ja_jp", "ko_kr", "fr_fr", "fr_ca", "pl_pl", "it_it", "cs_cz",
                "hu_hu", "vi_vn", "th_th"
        );

        List<String> requiredKeys = List.of(
                "tooltip.copper_age_patch.weathering",
                "tooltip.copper_age_patch.weathering.unaffected",
                "tooltip.copper_age_patch.weathering.exposed",
                "tooltip.copper_age_patch.weathering.weathered",
                "tooltip.copper_age_patch.weathering.oxidized",
                "tooltip.copper_age_patch.waxed",
                "tooltip.copper_age_patch.not_waxed",
                "tooltip.copper_age_patch.shelf_items",
                "tooltip.copper_age_patch.shelf_empty",
                "tooltip.copper_age_patch.statue_name",
                "config.jade.plugin_copper_age_patch.copper_golem",
                "config.jade.plugin_copper_age_patch.copper_golem_statue",
                "config.jade.plugin_copper_age_patch.shelf"
        );

        for (String code : langCodes) {
            String path = "assets/copper_age_patch/lang/" + code + ".json";
            JsonObject lang = parseResourceJson(path);
            for (String key : requiredKeys) {
                assertTrue(lang.has(key), path + " must define key: " + key);
                assertFalse(lang.get(key).getAsString().isBlank(), path + " key " + key + " must not be blank");
            }
        }
    }
}
