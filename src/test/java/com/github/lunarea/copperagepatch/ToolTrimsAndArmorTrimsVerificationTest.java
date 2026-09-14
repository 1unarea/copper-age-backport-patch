package com.github.lunarea.copperagepatch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ToolTrimsAndArmorTrimsVerificationTest {

    private static final Map<String, String> EXPECTED_PATTERNS = Map.of(
            "linear", "minecraft:tadpole_spawn_egg",
            "tracks", "minecraft:silverfish_spawn_egg",
            "charge", "minecraft:cod_spawn_egg",
            "frost", "minecraft:snow_golem_spawn_egg"
    );

    private JsonObject parseResourceJson(String resourcePath) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Resource must exist at classpath: " + resourcePath);
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                assertTrue(element.isJsonObject(), "Resource must be a valid JSON object: " + resourcePath);
                return element.getAsJsonObject();
            }
        }
    }

    @Test
    @DisplayName("Verify Tool Trims trimmable_tools tag includes all 5 copper tools")
    void testToolTrimsTag() throws Exception {
        String path = "data/tooltrims/tags/item/trimmable_tools.json";
        JsonObject json = parseResourceJson(path);
        assertFalse(json.get("replace").getAsBoolean(), "Tag replace must be false");

        JsonArray values = json.getAsJsonArray("values");
        assertNotNull(values, "Values array must exist");

        List<String> expected = List.of(
                "minecraft:copper_sword",
                "minecraft:copper_axe",
                "minecraft:copper_pickaxe",
                "minecraft:copper_shovel",
                "minecraft:copper_hoe"
        );

        for (String item : expected) {
            assertTrue(values.contains(new com.google.gson.JsonPrimitive(item)),
                    path + " must contain " + item);
        }
    }

    @Test
    @DisplayName("Verify Tool Trims pattern definitions exist and are valid")
    void testTrimPatterns() throws Exception {
        for (Map.Entry<String, String> entry : EXPECTED_PATTERNS.entrySet()) {
            String pat = entry.getKey();
            String path = "data/tooltrims/trim_pattern/" + pat + ".json";
            JsonObject json = parseResourceJson(path);

            assertEquals("tooltrims:" + pat, json.get("asset_id").getAsString());
            assertFalse(json.get("decal").getAsBoolean());
            assertEquals(entry.getValue(), json.get("template_item").getAsString());
            assertTrue(json.has("description"));
        }
    }

    @Test
    @DisplayName("Verify vanilla tool tags include all 5 copper tools")
    void testVanillaToolTags() throws Exception {
        Map<String, String> toolTagMap = Map.of(
                "swords", "minecraft:copper_sword",
                "axes", "minecraft:copper_axe",
                "pickaxes", "minecraft:copper_pickaxe",
                "shovels", "minecraft:copper_shovel",
                "hoes", "minecraft:copper_hoe"
        );

        for (Map.Entry<String, String> entry : toolTagMap.entrySet()) {
            String path = "data/minecraft/tags/item/" + entry.getKey() + ".json";
            JsonObject json = parseResourceJson(path);
            assertFalse(json.get("replace").getAsBoolean(), path + " replace must be false");
            JsonArray values = json.getAsJsonArray("values");
            assertNotNull(values, path + " values must exist");
            assertTrue(values.contains(new com.google.gson.JsonPrimitive(entry.getValue())),
                    path + " must contain " + entry.getValue());
        }
    }

    @Test
    @DisplayName("Verify trimmable_armor tag exists and includes all 4 copper armor pieces")
    void testTrimmableArmorTag() throws Exception {
        String path = "data/minecraft/tags/item/trimmable_armor.json";
        JsonObject json = parseResourceJson(path);
        assertFalse(json.get("replace").getAsBoolean(), path + " replace must be false");
        JsonArray values = json.getAsJsonArray("values");
        assertNotNull(values, path + " values must exist");

        List<String> expected = List.of(
                "minecraft:copper_helmet",
                "minecraft:copper_chestplate",
                "minecraft:copper_leggings",
                "minecraft:copper_boots"
        );
        for (String armor : expected) {
            assertTrue(values.contains(new com.google.gson.JsonPrimitive(armor)),
                    path + " must contain " + armor);
        }
    }

    @Test
    @DisplayName("Verify all 200 Tool Trims smithing transform recipes exist and are valid")
    void testToolTrims200Recipes() throws Exception {
        List<String> tools = List.of("copper_sword", "copper_axe", "copper_pickaxe", "copper_shovel", "copper_hoe");
        List<String> patterns = List.of("linear", "tracks", "charge", "frost");
        Map<String, String> materials = Map.of(
                "amethyst", "minecraft:amethyst",
                "copper", "minecraft:copper",
                "diamond", "minecraft:diamond",
                "emerald", "minecraft:emerald",
                "gold", "minecraft:gold",
                "iron", "minecraft:iron",
                "lapis", "minecraft:lapis",
                "netherite", "minecraft:netherite",
                "quartz", "minecraft:quartz",
                "redstone", "minecraft:redstone"
        );

        int count = 0;
        for (String tool : tools) {
            for (String pat : patterns) {
                for (String mat : materials.keySet()) {
                    String path = "data/tooltrims/recipe/trimming/" + tool + "_" + pat + "_" + mat + ".json";
                    JsonObject json = parseResourceJson(path);

                    assertEquals("minecraft:smithing_transform", json.get("type").getAsString(), path + " type");
                    assertTrue(json.has("template"), path + " template");
                    assertEquals(EXPECTED_PATTERNS.get(pat),
                            json.getAsJsonObject("template").get("item").getAsString(), path + " template item");
                    assertTrue(json.has("base"), path + " base");
                    assertEquals("minecraft:" + tool, json.getAsJsonObject("base").get("item").getAsString());
                    assertTrue(json.has("addition"), path + " addition");

                    JsonObject result = json.getAsJsonObject("result");
                    assertEquals("minecraft:" + tool, result.get("id").getAsString());
                    assertEquals(1, result.get("count").getAsInt());

                    JsonObject components = result.getAsJsonObject("components");
                    JsonObject trim = components.getAsJsonObject("minecraft:trim");
                    assertEquals("tooltrims:" + pat, trim.get("pattern").getAsString());
                    assertEquals(materials.get(mat), trim.get("material").getAsString());

                    assertTrue(components.has("minecraft:custom_model_data"));
                    int cmd = components.get("minecraft:custom_model_data").getAsInt();
                    assertTrue(cmd >= 311001 && cmd <= 311040, "CMD in range: " + cmd);

                    count++;
                }
            }
        }
        assertEquals(200, count, "Must verify exactly 200 recipes");
    }

    @Test
    @DisplayName("Verify copper tool models contain 40 custom_model_data overrides pointing to valid trim models")
    void testToolModelOverrides() throws Exception {
        List<String> tools = List.of("copper_sword", "copper_axe", "copper_pickaxe", "copper_shovel", "copper_hoe");
        List<String> patterns = List.of("linear", "tracks", "charge", "frost");
        List<String> materials = List.of("amethyst", "copper", "diamond", "emerald", "gold", "iron", "lapis", "netherite", "quartz", "redstone");

        for (String tool : tools) {
            String path = "assets/minecraft/models/item/" + tool + ".json";
            JsonObject json = parseResourceJson(path);

            assertEquals("minecraft:item/handheld", json.get("parent").getAsString());
            assertEquals("minecraft:item/" + tool, json.getAsJsonObject("textures").get("layer0").getAsString());

            JsonArray overrides = json.getAsJsonArray("overrides");
            assertNotNull(overrides, path + " overrides must exist");
            assertEquals(40, overrides.size(), path + " must contain 40 overrides");

            int expectedCmd = 311001;
            for (String pat : patterns) {
                for (String mat : materials) {
                    String expectedModel = "tooltrims:trims/" + tool + "_" + pat + "_" + mat;
                    boolean found = false;
                    for (JsonElement elem : overrides) {
                        JsonObject obj = elem.getAsJsonObject();
                        JsonObject pred = obj.getAsJsonObject("predicate");
                        if (pred.has("custom_model_data") && pred.get("custom_model_data").getAsInt() == expectedCmd) {
                            assertEquals(expectedModel, obj.get("model").getAsString(), "Model mapping for CMD " + expectedCmd);
                            found = true;
                            break;
                        }
                    }
                    assertTrue(found, "Override for CMD " + expectedCmd + " (" + expectedModel + ") in " + path);
                    expectedCmd++;
                }
            }
        }
    }

    @Test
    @DisplayName("Verify all 200 clean trim item models exist and reference correct textures")
    void testToolTrimModels() throws Exception {
        List<String> tools = List.of("copper_sword", "copper_axe", "copper_pickaxe", "copper_shovel", "copper_hoe");
        List<String> patterns = List.of("linear", "tracks", "charge", "frost");
        List<String> materials = List.of("amethyst", "copper", "diamond", "emerald", "gold", "iron", "lapis", "netherite", "quartz", "redstone");

        int count = 0;
        for (String tool : tools) {
            String toolType = tool.replace("copper_", "");
            for (String pat : patterns) {
                for (String mat : materials) {
                    String path = "assets/tooltrims/models/trims/" + tool + "_" + pat + "_" + mat + ".json";
                    JsonObject json = parseResourceJson(path);

                    assertEquals("minecraft:item/" + tool, json.get("parent").getAsString(), path + " parent");
                    JsonObject textures = json.getAsJsonObject("textures");
                    assertEquals("minecraft:item/" + tool, textures.get("layer0").getAsString(), path + " layer0");
                    assertEquals("tooltrims:trims/items/copper_" + toolType + "_" + pat + "_" + mat,
                            textures.get("layer1").getAsString(), path + " layer1");

                    count++;
                }
            }
        }
        assertEquals(200, count, "Must verify exactly 200 trim models");
    }

    @Test
    @DisplayName("Verify copper armor models have all 10 trim_type overrides (0.1 to 1.0) and copper_darker mapping")
    void testArmorModelOverrides() throws Exception {
        List<String> pieces = List.of("copper_helmet", "copper_chestplate", "copper_leggings", "copper_boots");
        Map<Double, String> expectedPredicates = Map.of(
                0.1, "quartz",
                0.2, "iron",
                0.3, "netherite",
                0.4, "redstone",
                0.5, "copper_darker",
                0.6, "gold",
                0.7, "emerald",
                0.8, "diamond",
                0.9, "lapis",
                1.0, "amethyst"
        );

        for (String piece : pieces) {
            String path = "assets/minecraft/models/item/" + piece + ".json";
            JsonObject json = parseResourceJson(path);

            assertEquals("minecraft:item/generated", json.get("parent").getAsString());
            assertEquals("minecraft:item/" + piece, json.getAsJsonObject("textures").get("layer0").getAsString());

            JsonArray overrides = json.getAsJsonArray("overrides");
            assertNotNull(overrides, path + " overrides");
            assertEquals(10, overrides.size(), path + " must have 10 overrides");

            for (Map.Entry<Double, String> entry : expectedPredicates.entrySet()) {
                double trimVal = entry.getKey();
                String mat = entry.getValue();
                String expectedModel = "minecraft:item/" + piece + "_" + mat + "_trim";

                boolean found = false;
                for (JsonElement elem : overrides) {
                    JsonObject obj = elem.getAsJsonObject();
                    JsonObject pred = obj.getAsJsonObject("predicate");
                    if (pred.has("trim_type")) {
                        double actualVal = pred.get("trim_type").getAsDouble();
                        if (Math.abs(actualVal - trimVal) < 0.001) {
                            assertEquals(expectedModel, obj.get("model").getAsString(),
                                    "Model for trim_type " + trimVal + " in " + path);
                            found = true;
                            break;
                        }
                    }
                }
                assertTrue(found, "Must find override for trim_type " + trimVal + " in " + path);
            }
        }
    }

    @Test
    @DisplayName("Verify all 40 copper armor _trim models exist and use proper trim overlay textures")
    void testArmorTrimModels() throws Exception {
        List<String> pieces = List.of("helmet", "chestplate", "leggings", "boots");
        List<String> materials = List.of("quartz", "iron", "netherite", "redstone", "copper_darker", "gold", "emerald", "diamond", "lapis", "amethyst");

        int count = 0;
        for (String piece : pieces) {
            String armor = "copper_" + piece;
            for (String mat : materials) {
                String path = "assets/minecraft/models/item/" + armor + "_" + mat + "_trim.json";
                JsonObject json = parseResourceJson(path);

                assertEquals("minecraft:item/generated", json.get("parent").getAsString(), path + " parent");
                JsonObject textures = json.getAsJsonObject("textures");
                assertEquals("minecraft:item/" + armor, textures.get("layer0").getAsString(), path + " layer0");
                assertEquals("minecraft:trims/items/" + piece + "_trim_" + mat, textures.get("layer1").getAsString(), path + " layer1");

                count++;
            }
        }
        assertEquals(40, count, "Must verify exactly 40 armor trim models");
    }

    @Test
    @DisplayName("Verify assets/minecraft/atlases/blocks.json defines copper_darker permutation")
    void testBlocksAtlasPermutation() throws Exception {
        String path = "assets/minecraft/atlases/blocks.json";
        JsonObject json = parseResourceJson(path);
        assertTrue(json.has("sources"), "blocks.json must have 'sources'");

        JsonArray sources = json.getAsJsonArray("sources");
        assertFalse(sources.isEmpty(), "sources must not be empty");

        boolean found = false;
        for (JsonElement elem : sources) {
            JsonObject src = elem.getAsJsonObject();
            if ("paletted_permutations".equals(src.get("type").getAsString())) {
                assertEquals("trims/color_palettes/trim_palette", src.get("palette_key").getAsString());
                JsonObject perms = src.getAsJsonObject("permutations");
                assertTrue(perms.has("copper_darker"), "Must define 'copper_darker' permutation");
                assertEquals("trims/color_palettes/copper_darker", perms.get("copper_darker").getAsString());

                JsonArray textures = src.getAsJsonArray("textures");
                assertTrue(textures.contains(new com.google.gson.JsonPrimitive("trims/items/helmet_trim")));
                assertTrue(textures.contains(new com.google.gson.JsonPrimitive("trims/items/chestplate_trim")));
                assertTrue(textures.contains(new com.google.gson.JsonPrimitive("trims/items/leggings_trim")));
                assertTrue(textures.contains(new com.google.gson.JsonPrimitive("trims/items/boots_trim")));
                found = true;
                break;
            }
        }
        assertTrue(found, "Must find paletted_permutations source with copper_darker in blocks.json");

        // Verify copper_darker.png palette exists in resources
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("assets/minecraft/textures/trims/color_palettes/copper_darker.png")) {
            assertNotNull(is, "copper_darker.png palette texture must exist");
            assertTrue(is.readAllBytes().length > 0, "copper_darker.png must not be empty");
        }
    }
}
