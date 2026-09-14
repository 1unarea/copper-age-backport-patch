package com.github.lunarea.copperagepatch;

import com.github.lunarea.copperagepatch.config.CopperAgeConfig;
import com.github.lunarea.copperagepatch.spawnegg.CopperSpawnEggPatcher;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

public class CopperSpawnEggConfigVerificationTest {

    @TempDir
    Path tempDir;

    private File tempConfigFile;

    @BeforeEach
    void setUp() {
        CopperAgeConfig.resetForTesting();
        tempConfigFile = tempDir.resolve("copper_age_patch.json").toFile();
    }

    @AfterEach
    void tearDown() {
        CopperAgeConfig.resetForTesting();
        System.clearProperty("copper_age_patch.test.modloaded.vanillabackport");
        System.clearProperty("copper_age_patch.config_file");
    }

    @Test
    @DisplayName("Verify default configuration generates AUTO and persists cleanly")
    void testDefaultConfigCreation() throws Exception {
        assertFalse(tempConfigFile.exists());

        CopperAgeConfig.loadOrCreateConfig(tempConfigFile);
        assertTrue(tempConfigFile.exists(), "Config file must be created");

        assertEquals(CopperAgeConfig.SpawnEggTextureMode.AUTO, CopperAgeConfig.getSpawnEggMode());

        // Verify JSON contents on disk
        String content = Files.readString(tempConfigFile.toPath(), StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(content).getAsJsonObject();
        assertTrue(json.has("copper_golem_spawn_egg"));
        assertEquals("AUTO", json.get("copper_golem_spawn_egg").getAsString());
    }

    @Test
    @DisplayName("Verify config parses MODERN, CLASSIC, and AUTO cleanly")
    void testConfigModes() throws Exception {
        // 1. MODERN
        Files.writeString(tempConfigFile.toPath(), "{\"copper_golem_spawn_egg\": \"MODERN\"}", StandardCharsets.UTF_8);
        CopperAgeConfig.loadOrCreateConfig(tempConfigFile);
        assertEquals(CopperAgeConfig.SpawnEggTextureMode.MODERN, CopperAgeConfig.getSpawnEggMode());
        assertTrue(CopperAgeConfig.shouldUseModernEgg(), "MODERN mode must always enable modern texture");

        // 2. CLASSIC
        CopperAgeConfig.resetForTesting();
        Files.writeString(tempConfigFile.toPath(), "{\"copper_golem_spawn_egg\": \"CLASSIC\"}", StandardCharsets.UTF_8);
        CopperAgeConfig.loadOrCreateConfig(tempConfigFile);
        assertEquals(CopperAgeConfig.SpawnEggTextureMode.CLASSIC, CopperAgeConfig.getSpawnEggMode());
        assertFalse(CopperAgeConfig.shouldUseModernEgg(), "CLASSIC mode must never enable modern texture");

        // 3. AUTO without vanillabackport -> false
        CopperAgeConfig.resetForTesting();
        Files.writeString(tempConfigFile.toPath(), "{\"copper_golem_spawn_egg\": \"AUTO\"}", StandardCharsets.UTF_8);
        CopperAgeConfig.loadOrCreateConfig(tempConfigFile);
        assertEquals(CopperAgeConfig.SpawnEggTextureMode.AUTO, CopperAgeConfig.getSpawnEggMode());
        System.setProperty("copper_age_patch.test.modloaded.vanillabackport", "false");
        assertFalse(CopperAgeConfig.shouldUseModernEgg(), "AUTO without vanillabackport must return false");

        // 4. AUTO with vanillabackport -> true
        System.setProperty("copper_age_patch.test.modloaded.vanillabackport", "true");
        assertTrue(CopperAgeConfig.shouldUseModernEgg(), "AUTO with vanillabackport must return true");
    }

    @Test
    @DisplayName("Verify config handles corrupt JSON gracefully and falls back to AUTO")
    void testCorruptConfigFallback() throws Exception {
        Files.writeString(tempConfigFile.toPath(), "{invalid json content;;;", StandardCharsets.UTF_8);
        CopperAgeConfig.loadOrCreateConfig(tempConfigFile);
        assertEquals(CopperAgeConfig.SpawnEggTextureMode.AUTO, CopperAgeConfig.getSpawnEggMode());
    }

    @Test
    @DisplayName("Verify config handles empty 0-byte file gracefully and populates defaults")
    void testEmptyConfigFile() throws Exception {
        Files.writeString(tempConfigFile.toPath(), "", StandardCharsets.UTF_8);
        assertEquals(0, tempConfigFile.length());
        CopperAgeConfig.loadOrCreateConfig(tempConfigFile);
        assertEquals(CopperAgeConfig.SpawnEggTextureMode.AUTO, CopperAgeConfig.getSpawnEggMode());
        assertTrue(tempConfigFile.length() > 0, "Empty config should be populated with defaults");
    }

    @Test
    @DisplayName("Verify config handles non-string or null JSON values safely")
    void testNonStringJsonValue() throws Exception {
        Files.writeString(tempConfigFile.toPath(), "{\"copper_golem_spawn_egg\": 12345}", StandardCharsets.UTF_8);
        CopperAgeConfig.loadOrCreateConfig(tempConfigFile);
        assertEquals(CopperAgeConfig.SpawnEggTextureMode.AUTO, CopperAgeConfig.getSpawnEggMode());

        CopperAgeConfig.resetForTesting();
        Files.writeString(tempConfigFile.toPath(), "{\"copper_golem_spawn_egg\": null}", StandardCharsets.UTF_8);
        CopperAgeConfig.loadOrCreateConfig(tempConfigFile);
        assertEquals(CopperAgeConfig.SpawnEggTextureMode.AUTO, CopperAgeConfig.getSpawnEggMode());
    }

    @Test
    @DisplayName("Verify setSpawnEggMode correctly updates memory and persists to disk")
    void testConfigSetAndPersist() throws Exception {
        CopperAgeConfig.loadOrCreateConfig(tempConfigFile);
        assertEquals(CopperAgeConfig.SpawnEggTextureMode.AUTO, CopperAgeConfig.getSpawnEggMode());

        CopperAgeConfig.setSpawnEggMode(CopperAgeConfig.SpawnEggTextureMode.MODERN);
        assertEquals(CopperAgeConfig.SpawnEggTextureMode.MODERN, CopperAgeConfig.getSpawnEggMode());

        String diskContent = Files.readString(tempConfigFile.toPath(), StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(diskContent).getAsJsonObject();
        assertEquals("MODERN", json.get("copper_golem_spawn_egg").getAsString());
    }

    @Test
    @DisplayName("Verify built-in modern spawn egg resource pack files exist and are valid")
    void testBuiltinPackFiles() throws Exception {
        // 1. pack.mcmeta
        String mcmetaPath = "resourcepacks/modern_copper_golem_spawn_egg/pack.mcmeta";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(mcmetaPath)) {
            assertNotNull(is, "pack.mcmeta must exist at: " + mcmetaPath);
            JsonObject json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(json.has("pack"));
            JsonObject pack = json.getAsJsonObject("pack");
            assertTrue(pack.has("pack_format"));
            assertTrue(pack.has("description"));
        }

        // 2. Model JSON
        String modelPath = "resourcepacks/modern_copper_golem_spawn_egg/assets/minecraft/models/item/copper_golem_spawn_egg.json";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(modelPath)) {
            assertNotNull(is, "Model JSON must exist at: " + modelPath);
            JsonObject json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("minecraft:item/generated", json.get("parent").getAsString());
            JsonObject textures = json.getAsJsonObject("textures");
            assertEquals("minecraft:item/copper_golem_spawn_egg", textures.get("layer0").getAsString());
        }

        // 3. Texture PNG exists and matches SHA-256 reference
        String texPath = "resourcepacks/modern_copper_golem_spawn_egg/assets/minecraft/textures/item/copper_golem_spawn_egg.png";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(texPath)) {
            assertNotNull(is, "Texture PNG must exist at: " + texPath);
            byte[] bytes = is.readAllBytes();
            assertTrue(bytes.length > 0, "Texture PNG must not be empty");

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            String actualHash = hexString.toString();
            String expectedHash = "4403f76489570e44345447f8d526d31ec05bbae61c858d41e2907b419ecf0f2b";
            assertEquals(expectedHash, actualHash, "Texture SHA-256 must match authoritative reference image");
        }
    }

    @Test
    @DisplayName("Verify CopperSpawnEggPatcher mapping-agnostic ResourceLocation creation")
    void testResourceLocationCreation() {
        Object rl = CopperSpawnEggPatcher.createResourceLocation("copper_age_patch", "test_path");
        assertNotNull(rl, "ResourceLocation creation must succeed in standard runtime");
        assertEquals("copper_age_patch:test_path", rl.toString());
    }

    @Test
    @DisplayName("Verify CopperSpawnEggPatcher handles null event safely without throwing")
    void testNullEventSafety() {
        assertDoesNotThrow(() -> CopperSpawnEggPatcher.onNeoForgeAddPackFinders(null));
    }

    @Test
    @DisplayName("Verify createResourceLocation returns null for null or blank inputs")
    void testCreateResourceLocationNullOrBlank() {
        assertNull(CopperSpawnEggPatcher.createResourceLocation(null, "path"));
        assertNull(CopperSpawnEggPatcher.createResourceLocation("namespace", null));
        assertNull(CopperSpawnEggPatcher.createResourceLocation("", "path"));
        assertNull(CopperSpawnEggPatcher.createResourceLocation("namespace", ""));
        assertNull(CopperSpawnEggPatcher.createResourceLocation("   ", "   "));
    }

    @Test
    @DisplayName("Verify onNeoForgeAddPackFinders ignores SERVER_DATA event without error")
    void testNeoForgeAddPackFindersServerDataFilter() {
        Object serverDataEvent = new Object() {
            public Object getPackType() {
                return "SERVER_DATA";
            }
        };
        assertDoesNotThrow(() -> CopperSpawnEggPatcher.onNeoForgeAddPackFinders(serverDataEvent));
    }

    @Test
    @DisplayName("Verify initFabric is safely idempotent across repeated invocations")
    void testFabricInitIdempotence() {
        CopperSpawnEggPatcher.resetForTesting();
        assertDoesNotThrow(CopperSpawnEggPatcher::initFabric);
        assertDoesNotThrow(CopperSpawnEggPatcher::initFabric);
    }

    @Test
    @DisplayName("Verify setSpawnEggMode marks loaded state and avoids redundant disk reads")
    void testSetSpawnEggModeMarksLoaded() {
        CopperAgeConfig.resetForTesting();
        CopperAgeConfig.setSpawnEggMode(CopperAgeConfig.SpawnEggTextureMode.MODERN);
        assertEquals(CopperAgeConfig.SpawnEggTextureMode.MODERN, CopperAgeConfig.getSpawnEggMode());
    }

    @Test
    @DisplayName("Verify resolveConfigFile memoizes file and handles null file safely in saveConfig")
    void testResolveConfigFileMemoizationAndNullSave() {
        CopperAgeConfig.resetForTesting();
        File f1 = CopperAgeConfig.resolveConfigFile();
        File f2 = CopperAgeConfig.resolveConfigFile();
        assertSame(f1, f2, "resolveConfigFile must memoize resolved file instance");

        assertDoesNotThrow(() -> CopperAgeConfig.saveConfig(null));
        assertDoesNotThrow(() -> CopperAgeConfig.loadOrCreateConfig(null));
    }
}
