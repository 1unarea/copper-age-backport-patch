package com.github.lunarea.copperagepatch;

import com.github.lunarea.copperagepatch.compat.jei.CopperAgeJeiPlugin;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JeiPluginVerificationTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Verify CopperAgeJeiPlugin has @JeiPlugin annotation in bytecode and valid UID")
    void testPluginMetadata() throws Exception {
        // JEI uses CLASS retention for @JeiPlugin, scanned at launch
        String classPath = CopperAgeJeiPlugin.class.getName().replace('.', '/') + ".class";
        try (InputStream is = CopperAgeJeiPlugin.class.getClassLoader().getResourceAsStream(classPath)) {
            assertNotNull(is, "Could not find class file for CopperAgeJeiPlugin");
            org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(is);
            org.objectweb.asm.tree.ClassNode cn = new org.objectweb.asm.tree.ClassNode();
            cr.accept(cn, 0);

            boolean hasJeiPluginAnnotation = false;
            if (cn.invisibleAnnotations != null) {
                for (org.objectweb.asm.tree.AnnotationNode an : cn.invisibleAnnotations) {
                    if ("Lmezz/jei/api/JeiPlugin;".equals(an.desc)) {
                        hasJeiPluginAnnotation = true;
                        break;
                    }
                }
            }
            assertTrue(hasJeiPluginAnnotation, "CopperAgeJeiPlugin must have @JeiPlugin annotation in bytecode");
        }

        CopperAgeJeiPlugin plugin = new CopperAgeJeiPlugin();
        ResourceLocation uid = plugin.getPluginUid();
        assertNotNull(uid, "Plugin UID must not be null");
        assertEquals("copper_age_patch", uid.getNamespace());
        assertEquals("jei_plugin", uid.getPath());
        assertEquals(CopperAgeJeiPlugin.UID, uid);
    }

    record InfoCall(List<?> ingredients, IIngredientType<?> type, Component[] description) {}

    @Test
    @DisplayName("Verify registerRecipes safely registers ingredient info pages")
    void testRegisterRecipes() {
        List<InfoCall> calls = new ArrayList<>();

        InvocationHandler handler = (proxy, method, args) -> {
            if ("addIngredientInfo".equals(method.getName())) {
                if (args.length == 3 && args[0] instanceof List<?> list && args[1] instanceof IIngredientType<?> type && args[2] instanceof Component[] desc) {
                    calls.add(new InfoCall(list, type, desc));
                    return null;
                } else if (args.length == 3 && args[0] instanceof List<?> list && args[1] instanceof IIngredientType<?> type && args[2] instanceof Component singleDesc) {
                    calls.add(new InfoCall(list, type, new Component[]{singleDesc}));
                    return null;
                }
            }
            return null;
        };

        IRecipeRegistration mockRegistration = (IRecipeRegistration) Proxy.newProxyInstance(
                IRecipeRegistration.class.getClassLoader(),
                new Class<?>[]{IRecipeRegistration.class},
                handler
        );

        CopperAgeJeiPlugin plugin = new CopperAgeJeiPlugin();
        assertDoesNotThrow(() -> plugin.registerRecipes(mockRegistration));

        // Vanilla blocks like copper_block and lightning_rod exist in bootstrap, so copper golem info will register
        assertFalse(calls.isEmpty(), "registerRecipes must make at least one addIngredientInfo call for vanilla items");

        for (InfoCall call : calls) {
            assertEquals(VanillaTypes.ITEM_STACK, call.type());
            assertNotNull(call.ingredients());
            assertFalse(call.ingredients().isEmpty());
            for (Object obj : call.ingredients()) {
                assertTrue(obj instanceof ItemStack);
                assertFalse(((ItemStack) obj).isEmpty());
            }
            assertNotNull(call.description());
            assertTrue(call.description().length > 0);
        }
    }

    @Test
    @DisplayName("Verify all 29 language files contain all 8 JEI info keys with no emojis")
    void testJeiLangKeys() throws Exception {
        List<String> langCodes = List.of(
                "en_us", "en_gb", "tr_tr", "az_az", "de_de",
                "es_es", "es_mx", "es_ar", "es_cl", "es_ec", "es_uy", "es_ve",
                "pt_br", "pt_pt", "ru_ru", "ar_sa", "zh_cn", "zh_tw", "zh_hk",
                "ja_jp", "ko_kr", "fr_fr", "fr_ca", "pl_pl", "it_it", "cs_cz",
                "hu_hu", "vi_vn", "th_th"
        );

        List<String> jeiKeys = List.of(
                "gui.copper_age_patch.jei.info.copper_golem",
                "gui.copper_age_patch.jei.info.copper_golem_statue",
                "gui.copper_age_patch.jei.info.shelf",
                "gui.copper_age_patch.jei.info.copper_armor",
                "gui.copper_age_patch.jei.info.copper_tools",
                "gui.copper_age_patch.jei.info.copper_chest",
                "gui.copper_age_patch.jei.info.copper_recycling",
                "gui.copper_age_patch.jei.info.copper_horse_armor"
        );

        for (String code : langCodes) {
            String path = "assets/copper_age_patch/lang/" + code + ".json";
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(is, "Lang resource must exist: " + path);
                JsonObject json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : jeiKeys) {
                    assertTrue(json.has(key), path + " must define JEI info key: " + key);
                    String val = json.get(key).getAsString();
                    assertFalse(val.isBlank(), path + " key " + key + " must not be blank");

                    // Verify NO emojis (Unicode ranges for pictographs, emoticons, symbols, and variation selectors)
                    for (int i = 0; i < val.length(); ) {
                        int codePoint = val.codePointAt(i);
                        boolean isPictographOrEmoji = (codePoint >= 0x1F000 && codePoint <= 0x1FAFF)
                                || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                                || (codePoint >= 0xFE00 && codePoint <= 0xFE0F)
                                || Character.isEmojiPresentation(codePoint);
                        assertFalse(isPictographOrEmoji,
                                path + " key " + key + " must not contain emojis. Found code point: " + codePoint + " ('" + new String(Character.toChars(codePoint)) + "')");
                        i += Character.charCount(codePoint);
                    }
                }
            }
        }
    }
}
