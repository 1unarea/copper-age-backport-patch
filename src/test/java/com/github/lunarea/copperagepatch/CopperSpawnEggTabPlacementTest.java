package com.github.lunarea.copperagepatch;

import com.github.lunarea.copperagepatch.config.CopperAgeConfig;
import com.github.lunarea.copperagepatch.creative.CopperSpawnEggTabPatcher;
import com.github.lunarea.copperagepatch.spawnegg.CopperSpawnEggPatcher;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class CopperSpawnEggTabPlacementTest {

    @BeforeAll
    static void initAll() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (com.github.smallinger.copperagebackport.registry.ModItems.COPPER_GOLEM_SPAWN_EGG == null) {
            com.github.smallinger.copperagebackport.registry.ModItems.COPPER_GOLEM_SPAWN_EGG = () -> Items.ZOMBIE_SPAWN_EGG;
        }
    }

    @BeforeEach
    void setUp() {
        CopperSpawnEggTabPatcher.resetForTesting();
        CopperSpawnEggPatcher.resetForTesting();
        CopperAgeConfig.resetForTesting();
    }

    @Test
    @DisplayName("Verify spawn eggs tab key is resolved cleanly across mappings")
    void testSpawnEggsTabKeyResolution() {
        Object tabKey = CopperSpawnEggTabPatcher.resolveSpawnEggsTabKey();
        assertNotNull(tabKey, "Spawn eggs tab key must not be null");
        assertTrue(tabKey.toString().contains("spawn_eggs") || tabKey.toString().contains("SPAWN_EGGS"),
                "Tab key should reference spawn_eggs: " + tabKey);
    }

    @Test
    @DisplayName("Verify Iron Golem and Sniffer spawn egg items resolve from Minecraft jar")
    void testVanillaSpawnEggItemsResolution() {
        Object ironEgg = CopperSpawnEggTabPatcher.getIronGolemSpawnEgg();
        assertNotNull(ironEgg, "Iron Golem spawn egg must resolve from classpath");

        Object snifferEgg = CopperSpawnEggTabPatcher.getSnifferSpawnEgg();
        assertNotNull(snifferEgg, "Sniffer spawn egg must resolve from classpath");
    }

    public static class MockNeoForgeEvent {
        public final AtomicBoolean insertBeforeCalled = new AtomicBoolean(false);
        public final AtomicBoolean insertAfterCalled = new AtomicBoolean(false);
        public final AtomicBoolean acceptCalled = new AtomicBoolean(false);
        public final AtomicReference<ItemStack> beforeStackRef = new AtomicReference<>();
        public final AtomicReference<ItemStack> insertedStackRef = new AtomicReference<>();
        private final Object tabKey;
        private final Set<ItemStack> parentEntries;
        private final Set<ItemStack> searchEntries;

        public MockNeoForgeEvent(Object tabKey) {
            this(tabKey, new HashSet<>(), new HashSet<>());
        }

        public MockNeoForgeEvent(Object tabKey, Set<ItemStack> parentEntries, Set<ItemStack> searchEntries) {
            this.tabKey = tabKey;
            this.parentEntries = parentEntries;
            this.searchEntries = searchEntries;
        }

        public Object getTabKey() {
            return this.tabKey;
        }

        public Set<ItemStack> getParentEntries() {
            return this.parentEntries;
        }

        public Set<ItemStack> getSearchEntries() {
            return this.searchEntries;
        }

        public void insertBefore(ItemStack before, ItemStack stack, Object visibility) {
            insertBeforeCalled.set(true);
            beforeStackRef.set(before);
            insertedStackRef.set(stack);
        }

        public void insertAfter(ItemStack after, ItemStack stack, Object visibility) {
            insertAfterCalled.set(true);
        }

        public void accept(ItemStack stack, Object visibility) {
            acceptCalled.set(true);
        }
    }

    public static class MockFabricEntries {
        public final AtomicBoolean addBeforeCalled = new AtomicBoolean(false);
        public final AtomicReference<Object> targetRef = new AtomicReference<>();
        public final AtomicReference<Object[]> itemsRef = new AtomicReference<>();
        public final List<ItemStack> displayStacks = new ArrayList<>();

        public List<ItemStack> getDisplayStacks() {
            return displayStacks;
        }

        public void addBefore(ItemLike target, ItemLike... items) {
            addBeforeCalled.set(true);
            targetRef.set(target);
            itemsRef.set(items);
        }
    }

    @Test
    @DisplayName("Verify NeoForge spawn egg tab placement inserts before Iron Golem")
    void testNeoForgeSpawnEggTabPlacementBeforeIronGolem() {
        Object tabKey = CopperSpawnEggTabPatcher.resolveSpawnEggsTabKey();
        assertNotNull(tabKey);

        MockNeoForgeEvent event = new MockNeoForgeEvent(tabKey);
        boolean placed = CopperSpawnEggTabPatcher.applyNeoForgeSpawnEggTabPlacement(event);
        assertTrue(placed, "Placement should succeed");
        assertTrue(event.insertBeforeCalled.get(), "Should call insertBefore");
        assertNotNull(event.beforeStackRef.get(), "Target before stack must not be null");
        assertNotNull(event.insertedStackRef.get(), "Inserted stack must not be null");

        Object ironEgg = CopperSpawnEggTabPatcher.getIronGolemSpawnEgg();
        assertNotNull(ironEgg);
        assertTrue(CopperSpawnEggTabPatcher.isSameItem(event.beforeStackRef.get(), ironEgg),
                "Target must be the Iron Golem spawn egg");
    }

    @Test
    @DisplayName("Verify NeoForge tab placement prevents duplicate insertions")
    void testNeoForgeSpawnEggTabPlacementPreventsDuplicates() {
        Object tabKey = CopperSpawnEggTabPatcher.resolveSpawnEggsTabKey();
        Object copperEgg = CopperSpawnEggTabPatcher.getCopperGolemSpawnEgg();
        assertNotNull(copperEgg);

        Set<ItemStack> parentEntries = new HashSet<>();
        parentEntries.add(new ItemStack((Item) copperEgg));

        MockNeoForgeEvent event = new MockNeoForgeEvent(tabKey, parentEntries, new HashSet<>());
        boolean result = CopperSpawnEggTabPatcher.applyNeoForgeSpawnEggTabPlacement(event);
        assertTrue(result, "Should report already present without failing");
        assertFalse(event.insertBeforeCalled.get(), "insertBefore should NOT be called if already in tab");
    }

    @Test
    @DisplayName("Verify Fabric spawn egg tab placement inserts before Iron Golem")
    void testFabricSpawnEggTabPlacementBeforeIronGolem() {
        Object ironEgg = CopperSpawnEggTabPatcher.getIronGolemSpawnEgg();
        Object copperEgg = CopperSpawnEggTabPatcher.getCopperGolemSpawnEgg();
        assertNotNull(ironEgg);
        assertNotNull(copperEgg);

        MockFabricEntries entries = new MockFabricEntries();
        boolean result = CopperSpawnEggTabPatcher.applyFabricSpawnEggTabPlacement(entries);
        assertTrue(result, "Fabric placement should succeed");
        assertTrue(entries.addBeforeCalled.get(), "addBefore must be called");
        assertNotNull(entries.targetRef.get(), "Target must be set");
        assertTrue(CopperSpawnEggTabPatcher.isSameItem(entries.targetRef.get(), ironEgg),
                "Target must be Iron Golem spawn egg");
    }

    @Test
    @DisplayName("Verify Fabric tab placement prevents duplicate insertions")
    void testFabricSpawnEggTabPlacementPreventsDuplicates() {
        Object copperEgg = CopperSpawnEggTabPatcher.getCopperGolemSpawnEgg();
        assertNotNull(copperEgg);

        MockFabricEntries entries = new MockFabricEntries();
        entries.displayStacks.add(new ItemStack((Item) copperEgg));

        boolean result = CopperSpawnEggTabPatcher.applyFabricSpawnEggTabPlacement(entries);
        assertTrue(result, "Should report success when already present");
        assertFalse(entries.addBeforeCalled.get(), "addBefore should NOT be called if already in displayStacks");
    }

    @Test
    @DisplayName("Verify ItemColor provider returns -1 (no tint) when modern egg is enabled, and classic colors when disabled")
    void testModernSpawnEggItemColorProvider() throws Exception {
        Class<?> itemColorClass = Class.forName("net.minecraft.client.color.item.ItemColor");
        Object colorProxy = CopperSpawnEggPatcher.createItemColorProxy(itemColorClass);
        assertNotNull(colorProxy);

        Method getColorMethod = itemColorClass.getMethod("getColor", Class.forName("net.minecraft.world.item.ItemStack"), int.class);

        // 1. Modern egg mode (default / explicit MODERN)
        CopperAgeConfig.setSpawnEggMode(CopperAgeConfig.SpawnEggTextureMode.MODERN);
        assertTrue(CopperAgeConfig.shouldUseModernEgg());

        int modernTint0 = (int) getColorMethod.invoke(colorProxy, null, 0);
        int modernTint1 = (int) getColorMethod.invoke(colorProxy, null, 1);
        int modernTintDefault = (int) getColorMethod.invoke(colorProxy, null, -1);

        assertEquals(-1, modernTint0, "Modern egg tint 0 must be -1 (0xFFFFFFFF) to cancel Minecraft copper tint filter");
        assertEquals(-1, modernTint1, "Modern egg tint 1 must be -1 (0xFFFFFFFF) to cancel Minecraft copper tint filter");
        assertEquals(-1, modernTintDefault, "Modern egg any tint must be -1");

        // 2. Classic egg mode (CLASSIC)
        CopperAgeConfig.setSpawnEggMode(CopperAgeConfig.SpawnEggTextureMode.CLASSIC);
        assertFalse(CopperAgeConfig.shouldUseModernEgg());

        int classicTint0 = (int) getColorMethod.invoke(colorProxy, null, 0);
        int classicTint1 = (int) getColorMethod.invoke(colorProxy, null, 1);
        int classicTintDefault = (int) getColorMethod.invoke(colorProxy, null, 2);

        assertEquals(12088115, classicTint0, "Classic egg tint 0 must return copper primary color (12088115 / 0xB87333)");
        assertEquals(4772300, classicTint1, "Classic egg tint 1 must return copper oxidation secondary color (4772300 / 0x48D1CC)");
        assertEquals(-1, classicTintDefault, "Classic egg other tints should return -1");
    }

    @Test
    @DisplayName("Verify modern copper golem spawn egg texture is a valid 16x16 32-bit RGBA PNG")
    void testModernSpawnEggTextureValidity() throws Exception {
        File textureFile = new File("src/main/resources/resourcepacks/modern_copper_golem_spawn_egg/assets/minecraft/textures/item/copper_golem_spawn_egg.png");
        assertTrue(textureFile.exists(), "Modern spawn egg texture must exist: " + textureFile.getAbsolutePath());
        assertTrue(textureFile.length() > 0, "Texture file must not be empty");

        BufferedImage image = ImageIO.read(textureFile);
        assertNotNull(image, "ImageIO must successfully decode modern spawn egg PNG");
        assertEquals(16, image.getWidth(), "Texture width must be 16 pixels");
        assertEquals(16, image.getHeight(), "Texture height must be 16 pixels");

        // Verify non-transparent pixels exist
        int nonTransparentPixels = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 0) {
                    nonTransparentPixels++;
                }
            }
        }
        assertTrue(nonTransparentPixels > 50, "Texture must have non-transparent egg pixels (found " + nonTransparentPixels + ")");
    }
}
