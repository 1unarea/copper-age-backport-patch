package com.github.lunarea.copperagepatch;

import com.github.lunarea.copperagepatch.mixin.ModItemsMixin;
import com.github.smallinger.copperagebackport.registry.ModItems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bytecode structure tests for ModItemsMixin.
 * Verifies mapping-agnostic safety:
 * 1. Mixin targets ModItems with remap = false.
 * 2. Injected callback method hooks register() at RETURN with remap = false.
 * 3. Bytecode has zero net/minecraft/ references in field or method descriptors.
 * 4. Mixin contains no static initializer (<clinit>).
 */
public class ModItemsMixinStructureTest {

    private static ClassNode mixinNode;

    @BeforeAll
    static void loadClassNode() throws Exception {
        String classPath = ModItemsMixin.class.getName().replace('.', '/') + ".class";
        try (InputStream is = ModItemsMixin.class.getClassLoader().getResourceAsStream(classPath)) {
            assertNotNull(is, "Could not find class file for ModItemsMixin");
            ClassReader cr = new ClassReader(is);
            mixinNode = new ClassNode();
            cr.accept(mixinNode, 0);
        }
    }

    @Test
    @DisplayName("Verify ModItemsMixin bytecode has @Mixin targeting ModItems with remap = false")
    void testMixinTargetViaBytecode() {
        assertNotNull(mixinNode.invisibleAnnotations, "ModItemsMixin must have invisible annotations");
        AnnotationNode mixinAnnotation = null;
        for (AnnotationNode an : mixinNode.invisibleAnnotations) {
            if ("Lorg/spongepowered/asm/mixin/Mixin;".equals(an.desc)) {
                mixinAnnotation = an;
                break;
            }
        }
        assertNotNull(mixinAnnotation, "ModItemsMixin must have @Mixin annotation in bytecode");

        boolean foundTarget = false;
        boolean remapFalse = false;

        for (int i = 0; i < mixinAnnotation.values.size(); i += 2) {
            String key = (String) mixinAnnotation.values.get(i);
            Object val = mixinAnnotation.values.get(i + 1);

            if ("value".equals(key)) {
                List<?> targets = (List<?>) val;
                for (Object t : targets) {
                    if (t instanceof Type type && type.getClassName().equals(ModItems.class.getName())) {
                        foundTarget = true;
                    }
                }
            } else if ("remap".equals(key) && Boolean.FALSE.equals(val)) {
                remapFalse = true;
            }
        }

        assertTrue(foundTarget, "Mixin must target com.github.smallinger.copperagebackport.registry.ModItems");
        assertTrue(remapFalse, "Mixin remap must be explicitly false for mod classes");
    }

    @Test
    @DisplayName("Verify ModItemsMixin has an @Inject targeting register at RETURN with remap = false")
    void testInjectedMethodBytecode() {
        MethodNode injectMethod = null;
        for (MethodNode mn : mixinNode.methods) {
            if (mn.name.startsWith("copper_age_patch$")) {
                injectMethod = mn;
                break;
            }
        }
        assertNotNull(injectMethod, "ModItemsMixin must have a copper_age_patch$ prefixed injected method");

        assertNotNull(injectMethod.visibleAnnotations, "Injected method must have visible annotations");
        AnnotationNode injectAnnotation = null;
        for (AnnotationNode an : injectMethod.visibleAnnotations) {
            if ("Lorg/spongepowered/asm/mixin/injection/Inject;".equals(an.desc)) {
                injectAnnotation = an;
                break;
            }
        }
        assertNotNull(injectAnnotation, "Injected method must have @Inject annotation");

        boolean targetsRegister = false;
        boolean remapFalse = false;

        for (int i = 0; i < injectAnnotation.values.size(); i += 2) {
            String key = (String) injectAnnotation.values.get(i);
            Object val = injectAnnotation.values.get(i + 1);

            if ("method".equals(key)) {
                if (val instanceof List<?> methodList) {
                    for (Object m : methodList) {
                        if ("register".equals(m)) {
                            targetsRegister = true;
                        }
                    }
                } else if ("register".equals(val)) {
                    targetsRegister = true;
                }
            } else if ("remap".equals(key) && Boolean.FALSE.equals(val)) {
                remapFalse = true;
            }
        }

        assertTrue(targetsRegister, "@Inject must target method 'register'");
        assertTrue(remapFalse, "@Inject remap must be explicitly false for mod classes");
    }

    @Test
    @DisplayName("Verify ModItemsMixin has zero net/minecraft references for mapping-agnostic multi-loader safety")
    void testNoMinecraftBytecodeReferences() {
        if (mixinNode.fields != null) {
            for (org.objectweb.asm.tree.FieldNode fn : mixinNode.fields) {
                assertFalse(fn.desc.contains("net/minecraft/"),
                        "Field " + fn.name + " must not reference net/minecraft classes: " + fn.desc);
            }
        }
        for (MethodNode mn : mixinNode.methods) {
            assertFalse(mn.desc.contains("net/minecraft/"),
                    "Method " + mn.name + " descriptor must not reference net/minecraft classes: " + mn.desc);
        }
    }

    @Test
    @DisplayName("Verify ModItemsMixin has no static initializer (<clinit>) in bytecode")
    void testNoStaticInitializer() {
        for (MethodNode mn : mixinNode.methods) {
            assertNotEquals("<clinit>", mn.name, "ModItemsMixin must not have a <clinit> method");
        }
    }
}
