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
    @DisplayName("Verify channeling.json data override matches #minecraft:lightning_rods and is valid JSON")
    void testChannelingEnchantmentJsonValidAndTargetsTag() throws Exception {
        File file = new File("src/main/resources/data/minecraft/enchantment/channeling.json");
        assertTrue(file.exists(), "channeling.json must exist at " + file.getAbsolutePath());

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            assertTrue(root.has("effects"), "channeling.json must contain effects");
            JsonObject effects = root.getAsJsonObject("effects");
            assertTrue(effects.has("minecraft:hit_block"), "channeling.json must contain minecraft:hit_block");

            String jsonString = root.toString();
            assertTrue(jsonString.contains("#minecraft:lightning_rods"),
                    "channeling.json must target tag '#minecraft:lightning_rods'");
            assertFalse(jsonString.contains("\"block\":\"minecraft:lightning_rod\""),
                    "channeling.json must not hardcode single block minecraft:lightning_rod");
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
                "com/github/lunarea/copperagepatch/mixin/LightningBoltMixin.class"
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
    @DisplayName("Verify CopperLightningRodPatcher.init() runs safely and idempotently")
    void testCopperLightningRodPatcherInitDoesNotThrow() {
        assertDoesNotThrow(() -> {
            CopperLightningRodPatcher.init();
            CopperLightningRodPatcher.init();
        });
    }

    @Test
    @DisplayName("Verify CopperLightningRodPatcher isCabLightningRod and isWeatheringCopper null safety")
    void testNullSafety() {
        assertFalse(CopperLightningRodPatcher.isCabLightningRod(null));
        assertFalse(CopperLightningRodPatcher.isWeatheringCopper(null));
        assertTrue(CopperLightningRodPatcher.isAir(null));
        assertNull(CopperLightningRodPatcher.getEntityLevel(null));
        assertNull(CopperLightningRodPatcher.getBlockState(null, null));
        assertNull(CopperLightningRodPatcher.getBlock(null));
        assertFalse(CopperLightningRodPatcher.setBlockAndUpdate(null, null, null));
        assertDoesNotThrow(() -> CopperLightningRodPatcher.onPowerLightningRod(null));
        assertDoesNotThrow(() -> CopperLightningRodPatcher.onClearCopper(null, null, null));
    }
}
