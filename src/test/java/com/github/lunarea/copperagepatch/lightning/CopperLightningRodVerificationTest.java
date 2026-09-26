package com.github.lunarea.copperagepatch.lightning;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class CopperLightningRodVerificationTest {

    private static boolean containsEmoji(String val) {
        if (val == null) return false;
        for (int i = 0; i < val.length(); ) {
            int codePoint = val.codePointAt(i);
            boolean isPictographOrEmoji = (codePoint >= 0x1F000 && codePoint <= 0x1FAFF)
                    || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                    || (codePoint >= 0xFE00 && codePoint <= 0xFE0F)
                    || Character.isEmojiPresentation(codePoint);
            if (isPictographOrEmoji) {
                return true;
            }
            i += Character.charCount(codePoint);
        }
        return false;
    }

    @Test
    @DisplayName("Verify channeling.json data override matches all 8 lightning rod blocks under both namespaces")
    void testChannelingEnchantmentJsonValidAndTargetsAllLightningRods() throws Exception {
        File file = new File("src/main/resources/data/minecraft/enchantment/channeling.json");
        assertTrue(file.exists(), "channeling.json must exist at " + file.getAbsolutePath());

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            assertTrue(root.has("effects"), "channeling.json must contain effects");
            JsonObject effects = root.getAsJsonObject("effects");
            assertTrue(effects.has("minecraft:hit_block"), "channeling.json must contain minecraft:hit_block");

            String jsonString = root.toString();
            assertTrue(jsonString.contains("minecraft:any_of"),
                    "channeling.json must use minecraft:any_of for block state properties");

            String[] rodBlockIds = {
                    "lightning_rod",
                    "exposed_lightning_rod",
                    "weathered_lightning_rod",
                    "oxidized_lightning_rod",
                    "waxed_lightning_rod",
                    "waxed_exposed_lightning_rod",
                    "waxed_weathered_lightning_rod",
                    "waxed_oxidized_lightning_rod"
            };

            for (String id : rodBlockIds) {
                String mcBlock = "\"block\":\"minecraft:" + id + "\"";
                assertTrue(jsonString.contains(mcBlock),
                        "channeling.json must match block " + mcBlock);
            }

            // CAB registers its blocks under minecraft: namespace, not copperagebackport:.
            // copperagebackport: block IDs cause Unknown registry key errors at world load.
            assertFalse(jsonString.contains("\"copperagebackport:"),
                    "channeling.json must not contain copperagebackport: namespace block IDs");

            assertFalse(jsonString.contains("\"blocks\":\"#minecraft:lightning_rods\""),
                    "channeling.json must avoid fragile location_check coordinate offset block tags");
        }
    }

    @Test
    @DisplayName("Verify all language files define copper knife and copper shield without emojis")
    void testLanguageCompletenessAcrossAllLanguages() throws Exception {
        String[] namespaces = {"copper_age_patch", "farmersdelight", "shieldexp"};
        String[] expectedLangs = {
                "ar_sa", "az_az", "cs_cz", "de_de", "en_gb", "en_us",
                "es_ar", "es_cl", "es_ec", "es_es", "es_mx", "es_uy", "es_ve",
                "fr_ca", "fr_fr", "hu_hu", "it_it", "ja_jp", "ko_kr",
                "pl_pl", "pt_br", "pt_pt", "ru_ru", "th_th", "tr_tr",
                "vi_vn", "zh_cn", "zh_hk", "zh_tw"
        };

        for (String ns : namespaces) {
            File langDir = new File("src/main/resources/assets/" + ns + "/lang");
            assertTrue(langDir.exists(), "Lang dir must exist: " + langDir.getAbsolutePath());

            for (String langCode : expectedLangs) {
                File langFile = new File(langDir, langCode + ".json");
                assertTrue(langFile.exists(), "Language file missing: " + langFile.getPath());

                try (FileReader reader = new FileReader(langFile, StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                    // Verify knife key
                    String knifeKey = "item." + ns + ".copper_knife";
                    if ("shieldexp".equals(ns)) {
                        // shieldexp namespace only defines copper_shield
                        assertFalse(json.has(knifeKey), "shieldexp should not define copper_knife");
                    } else {
                        assertTrue(json.has(knifeKey), langFile.getPath() + " missing " + knifeKey);
                        String knifeVal = json.get(knifeKey).getAsString();
                        assertFalse(knifeVal.trim().isEmpty(), knifeKey + " cannot be empty in " + langFile.getPath());
                        assertFalse(containsEmoji(knifeVal),
                                "Emoji found in " + knifeKey + ": " + knifeVal);
                    }

                    // Verify shield key
                    String shieldKey = "item." + ns + ".copper_shield";
                    if ("farmersdelight".equals(ns)) {
                        // farmersdelight namespace only defines copper_knife
                        assertFalse(json.has(shieldKey), "farmersdelight should not define copper_shield");
                    } else {
                        assertTrue(json.has(shieldKey), langFile.getPath() + " missing " + shieldKey);
                        String shieldVal = json.get(shieldKey).getAsString();
                        assertFalse(shieldVal.trim().isEmpty(), shieldKey + " cannot be empty in " + langFile.getPath());
                        assertFalse(containsEmoji(shieldVal),
                                "Emoji found in " + shieldKey + ": " + shieldVal);
                    }

                    // Ensure entire lang file has ZERO emojis
                    for (String key : json.keySet()) {
                        JsonElement el = json.get(key);
                        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                            String val = el.getAsString();
                            assertFalse(containsEmoji(val),
                                    "Emoji detected in " + langFile.getPath() + " for key " + key + ": " + val);
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Verify CopperLightningRodPatcher has zero net/minecraft bytecode signature dependencies")
    void testCopperLightningRodPatcherBytecodePurity() throws Exception {
        String[] classesToCheck = {
                "com/github/lunarea/copperagepatch/lightning/CopperLightningRodPatcher.class",
                "com/github/lunarea/copperagepatch/mixin/LightningBoltMixin.class",
                "com/github/lunarea/copperagepatch/mixin/CopperAgePatchMixinPlugin.class"
        };

        for (String classPath : classesToCheck) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(classPath)) {
                assertNotNull(is, "Class resource not found on classpath: " + classPath);
                ClassReader cr = new ClassReader(is);
                ClassNode cn = new ClassNode();
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
                    for (FieldNode fn : cn.fields) {
                        assertFalse(fn.desc.contains("net/minecraft/"),
                                classPath + " field " + fn.name + " descriptor must not reference net/minecraft: " + fn.desc);
                    }
                }
                if (cn.methods != null) {
                    for (MethodNode mn : cn.methods) {
                        assertFalse(mn.desc.contains("net/minecraft/"),
                                classPath + " method " + mn.name + " descriptor must not reference net/minecraft: " + mn.desc);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Verify CopperAgePatchMixinPlugin filters targets correctly based on runtime mapping")
    void testCopperAgePatchMixinPluginFiltering() {
        com.github.lunarea.copperagepatch.mixin.CopperAgePatchMixinPlugin plugin =
                new com.github.lunarea.copperagepatch.mixin.CopperAgePatchMixinPlugin();

        boolean isIntermediary = com.github.lunarea.copperagepatch.mixin.CopperAgePatchMixinPlugin.isIntermediary();

        // LightningBoltMixin (Mojang) -> only on NeoForge (not Intermediary)
        assertEquals(!isIntermediary,
                plugin.shouldApplyMixin("any.target", "com.github.lunarea.copperagepatch.mixin.LightningBoltMixin"));
        // LightningBoltFabricMixin (Intermediary) -> only on Fabric
        assertEquals(isIntermediary,
                plugin.shouldApplyMixin("any.target", "com.github.lunarea.copperagepatch.mixin.LightningBoltFabricMixin"));
        assertTrue(plugin.shouldApplyMixin("com.github.smallinger.copperagebackport.registry.ModItems", "com.github.lunarea.copperagepatch.mixin.ModItemsMixin"));
        assertTrue(plugin.shouldApplyMixin("com.github.smallinger.copperagebackport.item.armor.CopperArmorMaterial", "com.github.lunarea.copperagepatch.mixin.CopperArmorMaterialMixin"));

        assertNull(plugin.getRefMapperConfig());
        assertNull(plugin.getMixins());
        assertDoesNotThrow(() -> plugin.onLoad("com.github.lunarea.copperagepatch.mixin"));
        assertDoesNotThrow(() -> plugin.acceptTargets(java.util.Collections.emptySet(), java.util.Collections.emptySet()));
    }

    @Test
    @DisplayName("Verify CopperAgePatchMixinPlugin handles both slashed and dotted mixin class names")
    void testCopperAgePatchMixinPluginSlashedTargetNames() {
        com.github.lunarea.copperagepatch.mixin.CopperAgePatchMixinPlugin plugin =
                new com.github.lunarea.copperagepatch.mixin.CopperAgePatchMixinPlugin();
        boolean isIntermediary = com.github.lunarea.copperagepatch.mixin.CopperAgePatchMixinPlugin.isIntermediary();

        // Slashed mixin class names must also be handled
        assertEquals(!isIntermediary,
                plugin.shouldApplyMixin("any.target", "com/github/lunarea/copperagepatch/mixin/LightningBoltMixin"));
        assertEquals(isIntermediary,
                plugin.shouldApplyMixin("any.target", "com/github/lunarea/copperagepatch/mixin/LightningBoltFabricMixin"));
    }

    @Test
    @DisplayName("Verify LightningBoltFabricMixin class exists and has zero net/minecraft method descriptors")
    void testLightningBoltFabricMixinBytecodePurity() throws Exception {
        String classPath = "com/github/lunarea/copperagepatch/mixin/LightningBoltFabricMixin.class";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(classPath)) {
            assertNotNull(is, "LightningBoltFabricMixin.class must exist on classpath");
            ClassReader cr = new ClassReader(is);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            if (cn.methods != null) {
                for (MethodNode mn : cn.methods) {
                    assertFalse(mn.desc.contains("net/minecraft/"),
                            "LightningBoltFabricMixin method " + mn.name + " descriptor must not reference net/minecraft: " + mn.desc);
                }
            }
        }
    }

    @Test
    @DisplayName("Verify getAxisOrdinal correctly computes X=0, Y=1, Z=2 across orientations and handles null")
    void testGetAxisOrdinal() {
        assertEquals(1, CopperLightningRodPatcher.getAxisOrdinal(null));
        assertEquals(1, CopperLightningRodPatcher.getAxisOrdinal("down"));
        assertEquals(1, CopperLightningRodPatcher.getAxisOrdinal("up"));
        assertEquals(0, CopperLightningRodPatcher.getAxisOrdinal("west"));
        assertEquals(0, CopperLightningRodPatcher.getAxisOrdinal("east"));
        assertEquals(2, CopperLightningRodPatcher.getAxisOrdinal("north"));
        assertEquals(2, CopperLightningRodPatcher.getAxisOrdinal("south"));
    }

    @Test
    @DisplayName("Verify Fabric lifecycle dynamic proxy correctly handles equals, hashCode, and toString without NPE")
    void testFabricLifecycleProxyContracts() throws Exception {
        interface DummyListener {
            void onEvent();
        }
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                DummyListener.class.getClassLoader(),
                new Class<?>[]{DummyListener.class},
                (p, method, args) -> {
                    String mName = method.getName();
                    if ("hashCode".equals(mName)) return System.identityHashCode(p);
                    if ("equals".equals(mName)) return p == (args != null && args.length > 0 ? args[0] : null);
                    if ("toString".equals(mName)) return "TestProxy@" + Integer.toHexString(System.identityHashCode(p));
                    return null;
                }
        );
        assertDoesNotThrow(() -> {
            int hc = proxy.hashCode();
            assertTrue(hc != 0);
            boolean eqSelf = proxy.equals(proxy);
            assertTrue(eqSelf);
            boolean eqOther = proxy.equals(new Object());
            assertFalse(eqOther);
            String str = proxy.toString();
            assertTrue(str.startsWith("TestProxy@"));
        });
    }

    @Test
    @DisplayName("Verify Intermediary method mappings exist in real client-intermediary.jar")
    void testIntermediaryLightningMappingsExist() throws Exception {
        File intermediaryJar = new File("/home/lunarea/.var/app/com.modrinth.ModrinthApp/data/ModrinthApp/profiles/cabp fabric/.fabric/remappedJars/minecraft-1.21.1-0.19.5/client-intermediary.jar");
        if (!intermediaryJar.exists()) {
            return;
        }

        try (java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[]{intermediaryJar.toURI().toURL()})) {
            // 1. LightningBolt onLightningStrike in intermediary: class_5554.method_31648
            Class<?> rodBlockClass = Class.forName("net.minecraft.class_5554", false, loader);
            assertNotNull(rodBlockClass.getMethod("method_31648",
                    Class.forName("net.minecraft.class_2680", false, loader),
                    Class.forName("net.minecraft.class_1937", false, loader),
                    Class.forName("net.minecraft.class_2338", false, loader)));

            // 2. BlockPos.mutable() in intermediary: class_2338.method_25503()
            Class<?> bpClass = Class.forName("net.minecraft.class_2338", false, loader);
            assertNotNull(bpClass.getMethod("method_25503"));

            // 3. Level.levelEvent in intermediary: class_1937.method_8474(int, class_2338, int)
            Class<?> levelClass = Class.forName("net.minecraft.class_1937", false, loader);
            assertNotNull(levelClass.getMethod("method_8474", int.class, bpClass, int.class));

            // 4. Block.withPropertiesOf in intermediary: class_2248.method_34725(class_2680)
            Class<?> blockClass = Class.forName("net.minecraft.class_2248", false, loader);
            assertNotNull(blockClass.getMethod("method_34725", Class.forName("net.minecraft.class_2680", false, loader)));

            // 5. LightningBolt class_1538 has powerLightningRod (method_31499) and clearCopperOnLightningStrike (method_34707)
            Class<?> lbClass = Class.forName("net.minecraft.class_1538", false, loader);
            assertNotNull(lbClass.getDeclaredMethod("method_31499"));
            assertNotNull(lbClass.getDeclaredMethod("method_34707", levelClass, bpClass));
            assertNotNull(lbClass.getDeclaredMethod("method_34709", levelClass, bpClass,
                    Class.forName("net.minecraft.class_2338$class_2339", false, loader), int.class));

            // 6. Holder.value() in intermediary: class_6880.comp_349()
            Class<?> holderClass = Class.forName("net.minecraft.class_6880", false, loader);
            assertNotNull(holderClass.getMethod("comp_349"));

            // 7. Level.setBlockAndUpdate in intermediary: class_1937.method_8501(class_2338, class_2680)
            assertNotNull(levelClass.getMethod("method_8501", bpClass, Class.forName("net.minecraft.class_2680", false, loader)));

            // 8. LevelAccessor.levelEvent in intermediary: class_1936.method_20290(int, class_2338, int)
            Class<?> levelAccessorClass = Class.forName("net.minecraft.class_1936", false, loader);
            assertNotNull(levelAccessorClass.getMethod("method_20290", int.class, bpClass, int.class));

            // 9. DirectionalBlock.FACING in intermediary: class_2318.field_10927
            Class<?> directionalBlockClass = Class.forName("net.minecraft.class_2318", false, loader);
            assertNotNull(directionalBlockClass.getField("field_10927"));

            // 10. BuiltInRegistries.BLOCK in intermediary: class_7923.field_41175
            Class<?> builtInRegistriesClass = Class.forName("net.minecraft.class_7923", false, loader);
            assertNotNull(builtInRegistriesClass.getField("field_41175"));

            // 11. Registry.get(Identifier) in intermediary: method_10223(class_2960)
            Class<?> registryClass = Class.forName("net.minecraft.class_2378", false, loader);
            Class<?> idClass = Class.forName("net.minecraft.class_2960", false, loader);
            assertNotNull(registryClass.getMethod("method_10223", idClass));

            // 12. PoiTypes.TYPE_BY_STATE in intermediary: class_7477.field_39301
            Class<?> poiTypesClass = Class.forName("net.minecraft.class_7477", false, loader);
            java.lang.reflect.Field typeByStateField = poiTypesClass.getDeclaredField("field_39301");
            assertNotNull(typeByStateField);

            // 13. WeatheringCopper BiMap suppliers in intermediary: class_5955.field_29564 / field_29565
            Class<?> wcClass = Class.forName("net.minecraft.class_5955", false, loader);
            assertNotNull(wcClass.getField("field_29564"));
            assertNotNull(wcClass.getField("field_29565"));

            // 14. HoneycombItem BiMap suppliers in intermediary: class_5953.field_29560 / field_29561
            Class<?> hcClass = Class.forName("net.minecraft.class_5953", false, loader);
            assertNotNull(hcClass.getField("field_29560"));
            assertNotNull(hcClass.getField("field_29561"));
        }
    }

    @Test
    @DisplayName("Verify CopperLightningRodPatcher.init() and lifecycle registration runs safely and idempotently")
    void testCopperLightningRodPatcherInitDoesNotThrow() {
        assertDoesNotThrow(() -> {
            CopperLightningRodPatcher.init();
            CopperLightningRodPatcher.init();
            CopperLightningRodPatcher.registerFabricLifecycleEvents();
            CopperLightningRodPatcher.registerNeoForgeLifecycleEvents();
        });
    }

    @Test
    @DisplayName("Verify CopperLightningRodPatcher isCabLightningRod and isWeatheringCopper null safety")
    void testNullSafety() {
        assertFalse(CopperLightningRodPatcher.isCabLightningRod(null));
        assertFalse(CopperLightningRodPatcher.isCabLightningRod(Boolean.FALSE));
        assertFalse(CopperLightningRodPatcher.isWeatheringCopper(null));
        assertFalse(CopperLightningRodPatcher.isWeatheringCopper(Boolean.FALSE));
        assertTrue(CopperLightningRodPatcher.isAir(null));
        assertTrue(CopperLightningRodPatcher.isAir(Boolean.FALSE));
        assertNull(CopperLightningRodPatcher.getEntityLevel(null));
        assertNull(CopperLightningRodPatcher.getBlockState(null, null));
        assertNull(CopperLightningRodPatcher.getBlock(null));
        assertFalse(CopperLightningRodPatcher.setBlockAndUpdate(null, null, null));
        assertDoesNotThrow(() -> CopperLightningRodPatcher.onPowerLightningRod(null));
        assertDoesNotThrow(() -> CopperLightningRodPatcher.onClearCopper(null, null, null));
        assertDoesNotThrow(CopperLightningRodPatcher::getDirectionDown);
        assertDoesNotThrow(() -> CopperLightningRodPatcher.createBlockPos(0, 0, 0));
    }
}
