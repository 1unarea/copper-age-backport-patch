package com.github.lunarea.copperagepatch;

import com.github.lunarea.copperagepatch.mixin.CopperArmorMaterialMixin;
import com.github.smallinger.copperagebackport.item.armor.CopperArmorMaterial;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CopperArmorMaterialMixinStructureTest {

    private static ClassNode mixinNode;

    @BeforeAll
    static void loadClassNode() throws Exception {
        String classPath = CopperArmorMaterialMixin.class.getName().replace('.', '/') + ".class";
        try (InputStream is = CopperArmorMaterialMixin.class.getClassLoader().getResourceAsStream(classPath)) {
            assertNotNull(is, "Could not find class file for CopperArmorMaterialMixin");
            ClassReader cr = new ClassReader(is);
            mixinNode = new ClassNode();
            cr.accept(mixinNode, 0);
        }
    }

    @Test
    @DisplayName("Verify CopperArmorMaterialMixin bytecode has @Mixin targeting CopperArmorMaterial with remap = false")
    void testMixinTargetViaBytecode() {
        assertNotNull(mixinNode.invisibleAnnotations, "CopperArmorMaterialMixin must have invisible annotations");
        AnnotationNode mixinAnnotation = null;
        for (AnnotationNode an : mixinNode.invisibleAnnotations) {
            if ("Lorg/spongepowered/asm/mixin/Mixin;".equals(an.desc)) {
                mixinAnnotation = an;
                break;
            }
        }
        assertNotNull(mixinAnnotation, "CopperArmorMaterialMixin must have @Mixin annotation in bytecode");

        // Validate values of @Mixin
        boolean foundTarget = false;
        boolean remapFalse = false;

        for (int i = 0; i < mixinAnnotation.values.size(); i += 2) {
            String key = (String) mixinAnnotation.values.get(i);
            Object val = mixinAnnotation.values.get(i + 1);

            if ("value".equals(key)) {
                List<?> targets = (List<?>) val;
                for (Object t : targets) {
                    if (t instanceof Type type && type.getClassName().equals(CopperArmorMaterial.class.getName())) {
                        foundTarget = true;
                    }
                }
            } else if ("remap".equals(key)) {
                if (Boolean.FALSE.equals(val)) {
                    remapFalse = true;
                }
            }
        }

        assertTrue(foundTarget, "Mixin target must include CopperArmorMaterial.class");
        assertTrue(remapFalse, "Mixin remap attribute must be false when targeting mod classes");
    }

    @Test
    @DisplayName("Verify shadowed COPPER field exists in target CopperArmorMaterial")
    void testShadowedFieldMatchesTarget() throws NoSuchFieldException {
        Field targetField = CopperArmorMaterial.class.getDeclaredField("COPPER");
        assertTrue(Modifier.isStatic(targetField.getModifiers()), "Target COPPER field must be static");
        assertTrue(Modifier.isPublic(targetField.getModifiers()), "Target COPPER field must be public");
    }

    @Test
    @DisplayName("Verify injected methods in bytecode target init and createCopper with remap = false")
    void testInjectedMethodsBytecode() {
        boolean foundInit = false;
        boolean foundPreCreate = false;
        boolean foundPostCreate = false;

        for (MethodNode mn : mixinNode.methods) {
            if (mn.visibleAnnotations == null) continue;
            for (AnnotationNode an : mn.visibleAnnotations) {
                if ("Lorg/spongepowered/asm/mixin/injection/Inject;".equals(an.desc)) {
                    boolean remapFalse = false;
                    boolean cancellable = false;
                    String targetMethod = null;

                    for (int i = 0; i < an.values.size(); i += 2) {
                        String key = (String) an.values.get(i);
                        Object val = an.values.get(i + 1);

                        if ("remap".equals(key) && Boolean.FALSE.equals(val)) {
                            remapFalse = true;
                        } else if ("cancellable".equals(key) && Boolean.TRUE.equals(val)) {
                            cancellable = true;
                        } else if ("method".equals(key)) {
                            List<?> methods = (List<?>) val;
                            if (!methods.isEmpty()) {
                                targetMethod = (String) methods.get(0);
                            }
                        }
                    }

                    assertTrue(remapFalse, "Injection on " + mn.name + " must have remap = false");

                    if ("init".equals(targetMethod)) {
                        foundInit = true;
                    } else if ("createCopper".equals(targetMethod)) {
                        if (cancellable) {
                            foundPreCreate = true;
                        } else {
                            foundPostCreate = true;
                        }
                    }
                }
            }
        }

        assertTrue(foundInit, "Must have an injection into init()");
        assertTrue(foundPreCreate, "Must have a cancellable injection into createCopper() at HEAD");
        assertTrue(foundPostCreate, "Must have an injection into createCopper() at RETURN");
    }

    @Test
    @DisplayName("Verify target CopperArmorMaterial has init and createCopper methods")
    void testTargetMethodsExist() {
        boolean hasInit = false;
        boolean hasCreateCopper = false;

        for (Method m : CopperArmorMaterial.class.getDeclaredMethods()) {
            if ("init".equals(m.getName()) && m.getParameterCount() == 0) {
                hasInit = true;
            }
            if ("createCopper".equals(m.getName()) && m.getParameterCount() == 0) {
                hasCreateCopper = true;
            }
        }

        assertTrue(hasInit, "Target CopperArmorMaterial must have init() method");
        assertTrue(hasCreateCopper, "Target CopperArmorMaterial must have createCopper() method");
    }

    @Test
    @DisplayName("Verify copper_age_patch$cachedHolder field has @Unique, private, static, volatile modifiers and Holder descriptor")
    void testCachedHolderFieldBytecode() {
        org.objectweb.asm.tree.FieldNode cachedHolderField = null;
        for (org.objectweb.asm.tree.FieldNode fn : mixinNode.fields) {
            if ("copper_age_patch$cachedHolder".equals(fn.name)) {
                cachedHolderField = fn;
                break;
            }
        }

        assertNotNull(cachedHolderField, "copper_age_patch$cachedHolder field must exist in mixin bytecode");
        assertTrue((cachedHolderField.access & org.objectweb.asm.Opcodes.ACC_PRIVATE) != 0, "cachedHolder must be private");
        assertTrue((cachedHolderField.access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0, "cachedHolder must be static");
        assertTrue((cachedHolderField.access & org.objectweb.asm.Opcodes.ACC_VOLATILE) != 0, "cachedHolder must be volatile for thread-safety");
        assertEquals("Lnet/minecraft/core/Holder;", cachedHolderField.desc, "cachedHolder descriptor must be Lnet/minecraft/core/Holder;");

        boolean hasUnique = false;
        if (cachedHolderField.visibleAnnotations != null) {
            for (AnnotationNode an : cachedHolderField.visibleAnnotations) {
                if ("Lorg/spongepowered/asm/mixin/Unique;".equals(an.desc)) {
                    hasUnique = true;
                    break;
                }
            }
        }
        if (!hasUnique && cachedHolderField.invisibleAnnotations != null) {
            for (AnnotationNode an : cachedHolderField.invisibleAnnotations) {
                if ("Lorg/spongepowered/asm/mixin/Unique;".equals(an.desc)) {
                    hasUnique = true;
                    break;
                }
            }
        }
        assertTrue(hasUnique, "cachedHolder must have @Unique annotation in bytecode");
    }

    @Test
    @DisplayName("Verify CopperArmorMaterialMixin has no static initializer (<clinit>) in bytecode")
    void testNoStaticInitializerInMixin() {
        for (MethodNode mn : mixinNode.methods) {
            assertNotEquals("<clinit>", mn.name, "CopperArmorMaterialMixin must not have a <clinit> method to avoid static initializer issues in Mixin");
        }
    }
}
