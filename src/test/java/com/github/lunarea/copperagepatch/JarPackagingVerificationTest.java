package com.github.lunarea.copperagepatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

public class JarPackagingVerificationTest {

    @Test
    @DisplayName("Verify output NeoForge jar contains all required files, valid toml and valid mixin config")
    void testJarContents() throws Exception {
        File jarFile = new File("build/libs/copper_age_patch-neoforge-1.21.1-1.0.0.jar");
        assertTrue(jarFile.exists(), "Built jar file must exist at " + jarFile.getAbsolutePath());

        try (ZipFile zip = new ZipFile(jarFile)) {
            // Check essential entries
            assertNotNull(zip.getEntry("META-INF/neoforge.mods.toml"), "neoforge.mods.toml missing in jar");
            assertNull(zip.getEntry("fabric.mod.json"), "fabric.mod.json should not be in NeoForge jar");
            assertNotNull(zip.getEntry("copper_age_patch.mixins.json"), "copper_age_patch.mixins.json missing in jar");
            assertNotNull(zip.getEntry("copper_age_patch.refmap.json"), "copper_age_patch.refmap.json missing in jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/CopperAgePatch.class"), "CopperAgePatch.class missing in jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/durability/CopperArmorDurabilityPatcher.class"), "CopperArmorDurabilityPatcher.class missing in NeoForge jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/creative/CopperCombatTabPatcher.class"), "CopperCombatTabPatcher.class missing in NeoForge jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/creative/CopperSpawnEggTabPatcher.class"), "CopperSpawnEggTabPatcher.class missing in NeoForge jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/config/CopperAgeConfig.class"), "CopperAgeConfig.class missing in NeoForge jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/spawnegg/CopperSpawnEggPatcher.class"), "CopperSpawnEggPatcher.class missing in NeoForge jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/mixin/ModItemsMixin.class"), "ModItemsMixin.class missing in NeoForge jar");
            assertNull(zip.getEntry("com/github/lunarea/copperagepatch/fabric/CopperAgePatchFabric.class"), "CopperAgePatchFabric.class should not be in NeoForge jar");
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
            assertNotNull(zip.getEntry("data/minecraft/tags/item/trimmable_armor.json"), "trimmable_armor tag missing in jar");
            assertNotNull(zip.getEntry("data/tooltrims/tags/item/trimmable_tools.json"), "trimmable_tools tag missing in jar");
            assertNotNull(zip.getEntry("data/tooltrims/recipe/trimming/copper_sword_linear_amethyst.json"), "copper_sword_linear_amethyst recipe missing in jar");
            assertNotNull(zip.getEntry("assets/minecraft/atlases/blocks.json"), "atlases/blocks.json missing in jar");
            assertNotNull(zip.getEntry("assets/minecraft/models/item/copper_helmet.json"), "copper_helmet model missing in jar");
            assertNotNull(zip.getEntry("assets/minecraft/models/item/copper_chestplate_copper_darker_trim.json"), "copper_chestplate_copper_darker_trim model missing in jar");
            assertNotNull(zip.getEntry("assets/tooltrims/models/trims/copper_sword_linear_amethyst.json"), "copper_sword_linear_amethyst trim model missing in jar");
            assertNotNull(zip.getEntry("resourcepacks/modern_copper_golem_spawn_egg/pack.mcmeta"), "modern spawn egg pack.mcmeta missing in jar");
            assertNotNull(zip.getEntry("resourcepacks/modern_copper_golem_spawn_egg/assets/minecraft/textures/item/copper_golem_spawn_egg.png"), "modern spawn egg texture missing in jar");
            assertNotNull(zip.getEntry("assets/minecraft/textures/trims/color_palettes/copper_darker.png"), "copper_darker palette missing in jar");
            assertNotNull(zip.getEntry("assets/copper_age_patch/lang/en_us.json"), "en_us.json missing in jar");

            // Verify neoforge.mods.toml contents
            ZipEntry tomlEntry = zip.getEntry("META-INF/neoforge.mods.toml");
            try (InputStream is = zip.getInputStream(tomlEntry)) {
                String toml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(toml.contains("modId = \"copper_age_patch\""), "toml must specify modId = 'copper_age_patch'");
                assertTrue(toml.contains("version = \"1.0.0\""), "toml must specify version = '1.0.0'");
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
                assertTrue(json.contains("\"package\": \"com.github.lunarea.copperagepatch.mixin\""), "mixin package must match");
                assertTrue(json.contains("\"CopperArmorMaterialMixin\""), "CopperArmorMaterialMixin must be registered");
                assertTrue(json.contains("\"ModItemsMixin\""), "ModItemsMixin must be registered");
            }

            // Verify copper_age_patch.refmap.json contents
            ZipEntry refmapEntry = zip.getEntry("copper_age_patch.refmap.json");
            try (InputStream is = zip.getInputStream(refmapEntry)) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(json.contains("\"mappings\": {"), "refmap must contain mappings object");
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
    @DisplayName("Verify built NeoForge jar file exists on filesystem and has valid size")
    void testBuiltJarExists() {
        File builtJar = new File("build/libs/copper_age_patch-neoforge-1.21.1-1.0.0.jar");
        assertTrue(builtJar.exists(), "Built NeoForge mod jar must exist at " + builtJar.getAbsolutePath());
        assertTrue(builtJar.length() > 0, "Built NeoForge mod jar size must be greater than 0");
    }

    @Test
    @DisplayName("Verify Fabric jar exists and has valid structure and metadata")
    void testFabricJarContents() throws Exception {
        File fabricJar = new File("build/libs/copper_age_patch-fabric-1.21.1-1.0.0.jar");
        assertTrue(fabricJar.exists(), "Built Fabric jar must exist at " + fabricJar.getAbsolutePath());
        assertTrue(fabricJar.length() > 0, "Built Fabric jar size must be greater than 0");

        try (ZipFile zip = new ZipFile(fabricJar)) {
            assertNotNull(zip.getEntry("fabric.mod.json"), "fabric.mod.json missing in Fabric jar");
            assertNull(zip.getEntry("META-INF/neoforge.mods.toml"), "neoforge.mods.toml should not be in Fabric jar");
            assertNull(zip.getEntry("com/github/lunarea/copperagepatch/CopperAgePatch.class"), "NeoForge @Mod class should not be in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/fabric/CopperAgePatchFabric.class"), "CopperAgePatchFabric.class missing in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/fabric/CopperAgePatchFabricClient.class"), "CopperAgePatchFabricClient.class missing in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/durability/CopperArmorDurabilityPatcher.class"), "CopperArmorDurabilityPatcher.class missing in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/creative/CopperCombatTabPatcher.class"), "CopperCombatTabPatcher.class missing in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/creative/CopperSpawnEggTabPatcher.class"), "CopperSpawnEggTabPatcher.class missing in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/config/CopperAgeConfig.class"), "CopperAgeConfig.class missing in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/spawnegg/CopperSpawnEggPatcher.class"), "CopperSpawnEggPatcher.class missing in Fabric jar");
            assertNotNull(zip.getEntry("com/github/lunarea/copperagepatch/mixin/ModItemsMixin.class"), "ModItemsMixin.class missing in Fabric jar");
            assertNotNull(zip.getEntry("copper_age_patch.mixins.json"), "copper_age_patch.mixins.json missing in Fabric jar");
            assertNotNull(zip.getEntry("copper_age_patch.refmap.json"), "copper_age_patch.refmap.json missing in Fabric jar");
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
            assertNotNull(zip.getEntry("data/minecraft/tags/item/trimmable_armor.json"), "trimmable_armor tag missing in Fabric jar");
            assertNotNull(zip.getEntry("data/tooltrims/tags/item/trimmable_tools.json"), "trimmable_tools tag missing in Fabric jar");
            assertNotNull(zip.getEntry("data/tooltrims/recipe/trimming/copper_sword_linear_amethyst.json"), "copper_sword_linear_amethyst recipe missing in Fabric jar");
            assertNotNull(zip.getEntry("assets/minecraft/atlases/blocks.json"), "atlases/blocks.json missing in Fabric jar");
            assertNotNull(zip.getEntry("assets/minecraft/models/item/copper_helmet.json"), "copper_helmet model missing in Fabric jar");
            assertNotNull(zip.getEntry("assets/minecraft/models/item/copper_chestplate_copper_darker_trim.json"), "copper_chestplate_copper_darker_trim model missing in Fabric jar");
            assertNotNull(zip.getEntry("assets/tooltrims/models/trims/copper_sword_linear_amethyst.json"), "copper_sword_linear_amethyst trim model missing in Fabric jar");
            assertNotNull(zip.getEntry("resourcepacks/modern_copper_golem_spawn_egg/pack.mcmeta"), "modern spawn egg pack.mcmeta missing in Fabric jar");
            assertNotNull(zip.getEntry("resourcepacks/modern_copper_golem_spawn_egg/assets/minecraft/textures/item/copper_golem_spawn_egg.png"), "modern spawn egg texture missing in Fabric jar");
            assertNotNull(zip.getEntry("assets/minecraft/textures/trims/color_palettes/copper_darker.png"), "copper_darker palette missing in Fabric jar");
            assertNotNull(zip.getEntry("assets/copper_age_patch/lang/en_us.json"), "en_us.json missing in Fabric jar");

            ZipEntry fabricEntry = zip.getEntry("fabric.mod.json");
            try (InputStream is = zip.getInputStream(fabricEntry)) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(json.contains("\"id\": \"copper_age_patch\""), "fabric.mod.json must specify id = 'copper_age_patch'");
                assertTrue(json.contains("\"version\": \"1.0.0\""), "fabric.mod.json must specify version = '1.0.0'");
                assertTrue(json.contains("\"copper_age_patch.mixins.json\""), "fabric.mod.json must declare mixin config");
                assertTrue(json.contains("\"copperagebackport\""), "fabric.mod.json must declare dependency on copperagebackport");
                assertTrue(json.contains("\"waila\""), "fabric.mod.json must declare waila entrypoint");
                assertTrue(json.contains("\"com.github.lunarea.copperagepatch.compat.jade.CopperAgeJadePlugin\""), "fabric.mod.json must reference CopperAgeJadePlugin");
                assertTrue(json.contains("\"com.github.lunarea.copperagepatch.fabric.CopperAgePatchFabric\""), "fabric.mod.json must reference CopperAgePatchFabric");
                assertTrue(json.contains("\"com.github.lunarea.copperagepatch.fabric.CopperAgePatchFabricClient\""), "fabric.mod.json must reference CopperAgePatchFabricClient");
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
        File genericJar100 = new File("build/libs/copper_age_patch-1.0.0.jar");
        assertFalse(genericJar100.exists(), "Generic jar without loader/MC in name must not exist: " + genericJar100.getAbsolutePath());
        File genericJar014 = new File("build/libs/copper_age_patch-0.1.4.jar");
        assertFalse(genericJar014.exists(), "Generic jar without loader/MC in name must not exist: " + genericJar014.getAbsolutePath());
        File genericJar013 = new File("build/libs/copper_age_patch-0.1.3.jar");
        assertFalse(genericJar013.exists(), "Generic jar without loader/MC in name must not exist: " + genericJar013.getAbsolutePath());
        File genericJar012 = new File("build/libs/copper_age_patch-0.1.2.jar");
        assertFalse(genericJar012.exists(), "Generic jar without loader/MC in name must not exist: " + genericJar012.getAbsolutePath());
        File genericJar011 = new File("build/libs/copper_age_patch-0.1.1.jar");
        assertFalse(genericJar011.exists(), "Generic jar without loader/MC in name must not exist: " + genericJar011.getAbsolutePath());
        File genericJar010 = new File("build/libs/copper_age_patch-0.1.0.jar");
        assertFalse(genericJar010.exists(), "Old 0.1.0 generic jar must not exist: " + genericJar010.getAbsolutePath());
    }

    @Test
    @DisplayName("Verify core Fabric jar classes have zero net/minecraft references in bytecode descriptors")
    void testFabricJarCoreClassesHaveZeroMinecraftBytecodeReferences() throws Exception {
        File fabricJar = new File("build/libs/copper_age_patch-fabric-1.21.1-1.0.0.jar");
        assertTrue(fabricJar.exists());

        String[] coreClasses = new String[]{
                "com/github/lunarea/copperagepatch/fabric/CopperAgePatchFabric.class",
                "com/github/lunarea/copperagepatch/fabric/CopperAgePatchFabricClient.class",
                "com/github/lunarea/copperagepatch/durability/CopperArmorDurabilityPatcher.class",
                "com/github/lunarea/copperagepatch/creative/CopperCombatTabPatcher.class",
                "com/github/lunarea/copperagepatch/creative/CopperSpawnEggTabPatcher.class",
                "com/github/lunarea/copperagepatch/config/CopperAgeConfig.class",
                "com/github/lunarea/copperagepatch/spawnegg/CopperSpawnEggPatcher.class",
                "com/github/lunarea/copperagepatch/mixin/CopperArmorMaterialMixin.class",
                "com/github/lunarea/copperagepatch/mixin/ModItemsMixin.class",
                "com/github/lunarea/copperagepatch/util/MemoizedSupplier.class"
        };

        try (ZipFile zip = new ZipFile(fabricJar)) {
            for (String classPath : coreClasses) {
                ZipEntry entry = zip.getEntry(classPath);
                assertNotNull(entry, "Core class missing: " + classPath);
                try (InputStream is = zip.getInputStream(entry)) {
                    org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(is);
                    org.objectweb.asm.tree.ClassNode cn = new org.objectweb.asm.tree.ClassNode();
                    cr.accept(cn, 0);

                    if (cn.superName != null) {
                        assertFalse(cn.superName.contains("net/minecraft/"),
                                classPath + " superclass must not reference net/minecraft: " + cn.superName);
                    }
                    if (cn.interfaces != null) {
                        for (String iface : cn.interfaces) {
                            assertFalse(iface.contains("net/minecraft/"),
                                    classPath + " interface must not reference net/minecraft: " + iface);
                        }
                    }
                    if (cn.fields != null) {
                        for (org.objectweb.asm.tree.FieldNode fn : cn.fields) {
                            assertFalse(fn.desc.contains("net/minecraft/"),
                                    classPath + " field " + fn.name + " must not reference net/minecraft: " + fn.desc);
                        }
                    }
                    if (cn.methods != null) {
                        for (org.objectweb.asm.tree.MethodNode mn : cn.methods) {
                            assertFalse(mn.desc.contains("net/minecraft/"),
                                    classPath + " method " + mn.name + " descriptor must not reference net/minecraft: " + mn.desc);
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Verify Fabric jar classes initialize in isolated classloader with zero net/minecraft classes")
    void testFabricJarLoadsWithoutMinecraftOnClasspath() throws Exception {
        File fabricJar = new File("build/libs/copper_age_patch-fabric-1.21.1-1.0.0.jar");
        assertTrue(fabricJar.exists());

        ClassLoader systemParent = ClassLoader.getPlatformClassLoader();
        ClassLoader filteringParent = new ClassLoader(systemParent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraft.") || name.startsWith("com.github.lunarea.copperagepatch.")) {
                    throw new ClassNotFoundException("Blocked from parent: " + name);
                }
                try {
                    return JarPackagingVerificationTest.class.getClassLoader().loadClass(name);
                } catch (ClassNotFoundException e) {
                    return super.loadClass(name, resolve);
                }
            }
        };

        try (java.net.URLClassLoader isolatedLoader = new java.net.URLClassLoader(
                new java.net.URL[]{fabricJar.toURI().toURL()},
                filteringParent
        )) {
            Class<?> patcherClass = Class.forName("com.github.lunarea.copperagepatch.durability.CopperArmorDurabilityPatcher", true, isolatedLoader);
            assertNotNull(patcherClass);
            assertSame(isolatedLoader, patcherClass.getClassLoader(), "Class must be loaded by isolatedLoader from fabricJar");

            Class<?> creativeClass = Class.forName("com.github.lunarea.copperagepatch.creative.CopperCombatTabPatcher", true, isolatedLoader);
            assertNotNull(creativeClass);
            assertSame(isolatedLoader, creativeClass.getClassLoader(), "Class must be loaded by isolatedLoader from fabricJar");

            Class<?> fabricEntrypoint = Class.forName("com.github.lunarea.copperagepatch.fabric.CopperAgePatchFabric", true, isolatedLoader);
            assertNotNull(fabricEntrypoint);
            assertSame(isolatedLoader, fabricEntrypoint.getClassLoader(), "Class must be loaded by isolatedLoader from fabricJar");

            Method findComp = patcherClass.getMethod("findComponentsField");
            assertNull(findComp.invoke(null));
        }
    }

    @Test
    @DisplayName("Verify Fabric jar operates seamlessly against real Minecraft 1.21.1 Intermediary jar")
    void testFabricJarAgainstRealIntermediaryMinecraftJar() throws Exception {
        File fabricJar = new File("build/libs/copper_age_patch-fabric-1.21.1-1.0.0.jar");
        assertTrue(fabricJar.exists());

        File intermediaryJar = new File("/home/lunarea/.var/app/com.modrinth.ModrinthApp/data/ModrinthApp/profiles/cabp fabric/.fabric/remappedJars/minecraft-1.21.1-0.19.5/client-intermediary.jar");
        if (!intermediaryJar.exists()) {
            return; // Skip if client-intermediary is not available in local test profile
        }

        ClassLoader systemParent = ClassLoader.getPlatformClassLoader();
        ClassLoader filteringParent = new ClassLoader(systemParent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                // Block Mojang mapped Minecraft classes and mod classes from parent to guarantee purity
                if (name.startsWith("net.minecraft.world.") || name.startsWith("net.minecraft.core.")
                        || name.startsWith("net.minecraft.resources.") || name.startsWith("com.github.lunarea.copperagepatch.")) {
                    throw new ClassNotFoundException("Simulating Fabric Intermediary pure runtime: " + name);
                }
                try {
                    return JarPackagingVerificationTest.class.getClassLoader().loadClass(name);
                } catch (ClassNotFoundException e) {
                    return super.loadClass(name, resolve);
                }
            }
        };

        try (java.net.URLClassLoader intermediaryLoader = new java.net.URLClassLoader(
                new java.net.URL[]{fabricJar.toURI().toURL(), intermediaryJar.toURI().toURL()},
                filteringParent
        )) {
            Class<?> patcherClass = Class.forName("com.github.lunarea.copperagepatch.durability.CopperArmorDurabilityPatcher", true, intermediaryLoader);
            Class<?> creativeClass = Class.forName("com.github.lunarea.copperagepatch.creative.CopperCombatTabPatcher", true, intermediaryLoader);

            // 1. Verify Item class resolves to net.minecraft.class_1792
            Method getItemClassMethod = patcherClass.getMethod("getItemClass");
            Class<?> itemClass = (Class<?>) getItemClassMethod.invoke(null);
            assertNotNull(itemClass, "Must resolve Item class in Intermediary");
            assertEquals("net.minecraft.class_1792", itemClass.getName());

            // 2. Verify findComponentsField resolves field_49263
            Method findComponentsFieldMethod = patcherClass.getMethod("findComponentsField");
            java.lang.reflect.Field compField = (java.lang.reflect.Field) findComponentsFieldMethod.invoke(null);
            assertNotNull(compField, "Must resolve components field in Intermediary");
            assertEquals("field_49263", compField.getName());

            // 3. Verify createIdentifier returns a net.minecraft.class_2960 instance
            Method createIdMethod = patcherClass.getMethod("createIdentifier", String.class, String.class);
            Object idObj = createIdMethod.invoke(null, "minecraft", "stone_axe");
            assertNotNull(idObj, "Must create Identifier");
            assertEquals("net.minecraft.class_2960", idObj.getClass().getName());

            // 4. Verify Stone Axe field field_8062 exists on class_1802 and is type class_1792
            Class<?> itemsClass = Class.forName("net.minecraft.class_1802", false, intermediaryLoader);
            java.lang.reflect.Field stoneAxeField = itemsClass.getDeclaredField("field_8062");
            assertNotNull(stoneAxeField, "Must resolve Stone Axe field field_8062 on class_1802");
            assertEquals("net.minecraft.class_1792", stoneAxeField.getType().getName());

            // 5. Verify Combat tab key field field_40202 exists on class_7706
            Class<?> itemGroupsClass = Class.forName("net.minecraft.class_7706", false, intermediaryLoader);
            java.lang.reflect.Field combatField = itemGroupsClass.getDeclaredField("field_40202");
            assertNotNull(combatField, "Must resolve Combat tab field field_40202 on class_7706");

            // 6. Verify DataComponent fields on class_9334
            Class<?> dcClass = Class.forName("net.minecraft.class_9334", false, intermediaryLoader);
            assertNotNull(dcClass.getDeclaredField("field_50072"), "Must resolve MAX_DAMAGE (field_50072)");
            assertNotNull(dcClass.getDeclaredField("field_49629"), "Must resolve DAMAGE (field_49629)");
            assertNotNull(dcClass.getDeclaredField("field_50071"), "Must resolve MAX_STACK_SIZE (field_50071)");

            // 7. Verify DataComponentMap and Builder methods
            Class<?> mapClass = Class.forName("net.minecraft.class_9323", false, intermediaryLoader);
            assertNotNull(mapClass.getMethod("method_57827"), "Must resolve DataComponentMap.builder() (method_57827)");
            Class<?> builderClass = Class.forName("net.minecraft.class_9323$class_9324", false, intermediaryLoader);
            assertNotNull(builderClass.getMethod("method_57838"), "Must resolve Builder.build() (method_57838)");
            assertNotNull(builderClass.getMethod("method_57839", mapClass), "Must resolve Builder.addAll() (method_57839)");

            // 8. Verify resolveCombatTabKey executes against Intermediary classes
            Method resolveTabKeyMethod = creativeClass.getMethod("resolveCombatTabKey");
            Object tabKey = resolveTabKeyMethod.invoke(null);
            assertNotNull(tabKey, "Must resolve Combat tab key in Intermediary");
            assertTrue(tabKey.toString().contains("combat"), "Tab key must contain 'combat'");

            // 9. Verify Iron Axe field field_8475 exists on class_1802 and is type class_1792
            java.lang.reflect.Field ironAxeField = itemsClass.getDeclaredField("field_8475");
            assertNotNull(ironAxeField, "Must resolve Iron Axe field field_8475 on class_1802");
            assertEquals("net.minecraft.class_1792", ironAxeField.getType().getName());

            // 10. Verify isAir null handling mapping-agnostic behavior
            Method isAirMethod = patcherClass.getMethod("isAir", Object.class);
            assertTrue((Boolean) isAirMethod.invoke(null, (Object) null), "null item must be air");

            // 11. Verify CopperSpawnEggPatcher.createResourceLocation resolves net.minecraft.class_2960 in Intermediary
            Class<?> spawnEggClass = Class.forName("com.github.lunarea.copperagepatch.spawnegg.CopperSpawnEggPatcher", true, intermediaryLoader);
            Method createResourceLocationMethod = spawnEggClass.getMethod("createResourceLocation", String.class, String.class);
            Object rlObj = createResourceLocationMethod.invoke(null, "copper_age_patch", "modern_copper_golem_spawn_egg");
            assertNotNull(rlObj, "createResourceLocation must create Identifier in Intermediary");
            assertEquals("net.minecraft.class_2960", rlObj.getClass().getName());
            assertEquals("copper_age_patch:modern_copper_golem_spawn_egg", rlObj.toString());
        }
    }
}
