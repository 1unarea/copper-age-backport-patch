package com.github.lunarea.copperagepatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

public class JarPackagingVerificationTest {

    @Test
    @DisplayName("Verify output NeoForge jar contains all required files, valid toml and valid mixin config")
    void testJarContents() throws Exception {
        File jarFile = new File("build/libs/copper_age_patch-neoforge-1.21.1-0.1.1.jar");
        assertTrue(jarFile.exists(), "Built jar file must exist at " + jarFile.getAbsolutePath());

        try (ZipFile zip = new ZipFile(jarFile)) {
            // Check essential entries
            assertNotNull(zip.getEntry("META-INF/neoforge.mods.toml"), "neoforge.mods.toml missing in jar");
            assertNotNull(zip.getEntry("copper_age_patch.mixins.json"), "copper_age_patch.mixins.json missing in jar");
            assertNotNull(zip.getEntry("copper_age_patch.refmap.json"), "copper_age_patch.refmap.json missing in jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/CopperAgePatch.class"), "CopperAgePatch.class missing in jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/mixin/CopperArmorMaterialMixin.class"), "CopperArmorMaterialMixin.class missing in jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/util/MemoizedSupplier.class"), "MemoizedSupplier.class missing in jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/compat/jade/CopperAgeJadePlugin.class"), "CopperAgeJadePlugin.class missing in jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/compat/jade/CopperGolemEntityProvider.class"), "CopperGolemEntityProvider.class missing in jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/compat/jade/CopperGolemStatueBlockProvider.class"), "CopperGolemStatueBlockProvider.class missing in jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/compat/jade/ShelfBlockProvider.class"), "ShelfBlockProvider.class missing in jar");

            // Check resource files
            assertNotNull(zip.getEntry("data/copperagebackport/weapon_attributes/copper_sword.json"), "copper_sword weapon attributes missing in jar");
            assertNotNull(zip.getEntry("data/copper_age_patch/recipe/crushing/copper_helmet.json"), "copper_helmet crushing recipe missing in jar");
            assertNotNull(zip.getEntry("data/copper_age_patch/recipe/copper_ingot_from_nuggets.json"), "copper_ingot_from_nuggets recipe missing in jar");
            assertNotNull(zip.getEntry("data/copper_age_patch/recipe/copper_nugget.json"), "copper_nugget recipe missing in jar");
            assertNotNull(zip.getEntry("data/minecraft/recipe/copper_ingot_from_nuggets.json"), "minecraft copper_ingot_from_nuggets recipe missing in jar");
            assertNotNull(zip.getEntry("data/minecraft/recipe/copper_nugget.json"), "minecraft copper_nugget recipe missing in jar");
            assertNotNull(zip.getEntry("data/c/tags/item/armors/helmets.json"), "helmets tag missing in jar");
            assertNotNull(zip.getEntry("data/c/tags/item/tools/swords.json"), "swords tag missing in jar");
            assertNotNull(zip.getEntry("data/c/tags/item/armors.json"), "armors parent tag missing in jar");
            assertNotNull(zip.getEntry("data/c/tags/item/tools.json"), "tools parent tag missing in jar");
            assertNotNull(zip.getEntry("data/c/tags/item/nuggets.json"), "nuggets parent tag missing in jar");
            assertNotNull(zip.getEntry("data/c/tags/item/nuggets/copper.json"), "copper nuggets tag missing in jar");
            assertNotNull(zip.getEntry("data/c/tags/item/copper_nuggets.json"), "copper_nuggets tag missing in jar");
            assertNotNull(zip.getEntry("assets/copper_age_patch/lang/en_us.json"), "en_us.json missing in jar");

            // Verify neoforge.mods.toml contents
            ZipEntry tomlEntry = zip.getEntry("META-INF/neoforge.mods.toml");
            try (InputStream is = zip.getInputStream(tomlEntry)) {
                String toml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(toml.contains("modId = \"copper_age_patch\""), "toml must specify modId = 'copper_age_patch'");
                assertTrue(toml.contains("config = \"copper_age_patch.mixins.json\""), "toml must reference copper_age_patch.mixins.json");
                assertTrue(toml.contains("modId = \"copperagebackport\""), "toml must declare dependency on copperagebackport");
                assertTrue(toml.contains("modId = \"neoforge\""), "toml must declare dependency on neoforge");
                assertTrue(toml.contains("modId = \"minecraft\""), "toml must declare dependency on minecraft");
                assertTrue(toml.contains("modId = \"jade\""), "toml must declare optional dependency on jade");
                assertTrue(toml.contains("modId = \"bettercombat\""), "toml must declare optional dependency on bettercombat");
                assertTrue(toml.contains("modId = \"create\""), "toml must declare optional dependency on create");
            }

            // Verify copper_age_patch.mixins.json contents
            ZipEntry mixinEntry = zip.getEntry("copper_age_patch.mixins.json");
            try (InputStream is = zip.getInputStream(mixinEntry)) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(json.contains("\"package\": \"com.github.lunarea.copperagepatch.mixin\""), "mixin json must define package");
                assertTrue(json.contains("\"CopperArmorMaterialMixin\""), "mixin json must list CopperArmorMaterialMixin");
                assertTrue(json.contains("\"refmap\": \"copper_age_patch.refmap.json\""), "mixin json must specify refmap");
            }

            // Ensure we do NOT bundle third-party or minecraft classes into the patch jar
            for (var entry : java.util.Collections.list(zip.entries())) {
                String name = entry.getName();
                assertFalse(name.startsWith("net/minecraft/"), "Minecraft classes must not be bundled in mod jar: " + name);
                assertFalse(name.startsWith("org/spongepowered/"), "SpongePowered classes must not be bundled in mod jar: " + name);
                assertFalse(name.startsWith("com/github/smallinger/"), "Upstream mod classes must not be bundled in mod jar: " + name);
                assertFalse(name.startsWith("snownee/jade/"), "Jade classes must not be bundled in mod jar: " + name);
                assertFalse(name.startsWith("com/mojang/"), "Mojang classes must not be bundled in mod jar: " + name);
                assertFalse(name.startsWith("com/google/gson/"), "Gson classes must not be bundled in mod jar: " + name);
            }
        }
    }

    @Test
    @DisplayName("Verify built NeoForge jar exists and has non-zero size")
    void testBuiltJarExists() {
        File builtJar = new File("build/libs/copper_age_patch-neoforge-1.21.1-0.1.1.jar");
        assertTrue(builtJar.exists(), "Built NeoForge mod jar must exist at " + builtJar.getAbsolutePath());
        assertTrue(builtJar.length() > 0, "Built NeoForge mod jar size must be greater than 0");
    }

    @Test
    @DisplayName("Verify Fabric jar exists and has valid structure and metadata")
    void testFabricJarContents() throws Exception {
        File fabricJar = new File("build/libs/copper_age_patch-fabric-1.21.1-0.1.1.jar");
        assertTrue(fabricJar.exists(), "Built Fabric jar must exist at " + fabricJar.getAbsolutePath());
        assertTrue(fabricJar.length() > 0, "Built Fabric jar size must be greater than 0");

        try (ZipFile zip = new ZipFile(fabricJar)) {
            assertNotNull(zip.getEntry("fabric.mod.json"), "fabric.mod.json missing in Fabric jar");
            assertNull(zip.getEntry("META-INF/neoforge.mods.toml"), "neoforge.mods.toml should not be in Fabric jar");
            assertNull(zip.getEntry("com/github/lunarea/copperagepatch/CopperAgePatch.class"), "NeoForge @Mod class should not be in Fabric jar");
            assertNotNull(zip.getEntry("copper_age_patch.mixins.json"), "copper_age_patch.mixins.json missing in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/mixin/CopperArmorMaterialMixin.class"), "CopperArmorMaterialMixin.class missing in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/util/MemoizedSupplier.class"), "MemoizedSupplier.class missing in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/compat/jade/CopperAgeJadePlugin.class"), "CopperAgeJadePlugin.class missing in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/compat/jade/CopperGolemEntityProvider.class"), "CopperGolemEntityProvider.class missing in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/compat/jade/CopperGolemStatueBlockProvider.class"), "CopperGolemStatueBlockProvider.class missing in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/compat/jade/ShelfBlockProvider.class"), "ShelfBlockProvider.class missing in Fabric jar");

            // Check resource files in Fabric jar
            assertNotNull(zip.getEntry("data/copperagebackport/weapon_attributes/copper_sword.json"), "copper_sword weapon attributes missing in Fabric jar");
            assertNotNull(zip.getEntry("data/copper_age_patch/recipe/crushing/copper_helmet.json"), "copper_helmet crushing recipe missing in Fabric jar");
            assertNotNull(zip.getEntry("data/copper_age_patch/recipe/copper_ingot_from_nuggets.json"), "copper_ingot_from_nuggets recipe missing in Fabric jar");
            assertNotNull(zip.getEntry("data/copper_age_patch/recipe/copper_nugget.json"), "copper_nugget recipe missing in Fabric jar");
            assertNotNull(zip.getEntry("data/minecraft/recipe/copper_ingot_from_nuggets.json"), "minecraft copper_ingot_from_nuggets recipe missing in Fabric jar");
            assertNotNull(zip.getEntry("data/minecraft/recipe/copper_nugget.json"), "minecraft copper_nugget recipe missing in Fabric jar");
            assertNotNull(zip.getEntry("data/c/tags/item/armors/helmets.json"), "helmets tag missing in Fabric jar");
            assertNotNull(zip.getEntry("data/c/tags/item/tools/swords.json"), "swords tag missing in Fabric jar");
            assertNotNull(zip.getEntry("data/c/tags/item/armors.json"), "armors parent tag missing in Fabric jar");
            assertNotNull(zip.getEntry("data/c/tags/item/tools.json"), "tools parent tag missing in Fabric jar");
            assertNotNull(zip.getEntry("data/c/tags/item/nuggets.json"), "nuggets parent tag missing in Fabric jar");
            assertNotNull(zip.getEntry("data/c/tags/item/nuggets/copper.json"), "copper nuggets tag missing in Fabric jar");
            assertNotNull(zip.getEntry("data/c/tags/item/copper_nuggets.json"), "copper_nuggets tag missing in Fabric jar");
            assertNotNull(zip.getEntry("assets/copper_age_patch/lang/en_us.json"), "en_us.json missing in Fabric jar");

            ZipEntry fabricEntry = zip.getEntry("fabric.mod.json");
            try (InputStream is = zip.getInputStream(fabricEntry)) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(json.contains("\"id\": \"copper_age_patch\""), "fabric.mod.json must specify id = 'copper_age_patch'");
                assertTrue(json.contains("\"version\": \"0.1.1\""), "fabric.mod.json must specify version = '0.1.1'");
                assertTrue(json.contains("\"copper_age_patch.mixins.json\""), "fabric.mod.json must declare mixin config");
                assertTrue(json.contains("\"copperagebackport\""), "fabric.mod.json must declare dependency on copperagebackport");
                assertTrue(json.contains("\"waila\""), "fabric.mod.json must declare waila entrypoint");
                assertTrue(json.contains("\"com.github.lunarea.copperagepatch.compat.jade.CopperAgeJadePlugin\""), "fabric.mod.json must reference CopperAgeJadePlugin");
                assertTrue(json.contains("\"create\""), "fabric.mod.json must declare create suggestion");
            }

            // Ensure we do NOT bundle third-party or minecraft classes into the Fabric jar
            for (var entry : java.util.Collections.list(zip.entries())) {
                String name = entry.getName();
                assertFalse(name.startsWith("net/minecraft/"), "Minecraft classes must not be bundled in Fabric jar: " + name);
                assertFalse(name.startsWith("org/spongepowered/"), "SpongePowered classes must not be bundled in Fabric jar: " + name);
                assertFalse(name.startsWith("com/github/smallinger/"), "Upstream mod classes must not be bundled in Fabric jar: " + name);
                assertFalse(name.startsWith("snownee/jade/"), "Jade classes must not be bundled in Fabric jar: " + name);
                assertFalse(name.startsWith("com/mojang/"), "Mojang classes must not be bundled in Fabric jar: " + name);
                assertFalse(name.startsWith("com/google/gson/"), "Gson classes must not be bundled in Fabric jar: " + name);
            }
        }
    }

    @Test
    @DisplayName("Verify generic unnamed jar does not exist")
    void testGenericJarDoesNotExist() {
        File genericJar011 = new File("build/libs/copper_age_patch-0.1.1.jar");
        assertFalse(genericJar011.exists(), "Generic jar without loader/MC in name must not exist: " + genericJar011.getAbsolutePath());
        File genericJar010 = new File("build/libs/copper_age_patch-0.1.0.jar");
        assertFalse(genericJar010.exists(), "Old 0.1.0 generic jar must not exist: " + genericJar010.getAbsolutePath());
    }
}
