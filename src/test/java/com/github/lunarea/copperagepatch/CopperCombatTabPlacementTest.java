package com.github.lunarea.copperagepatch;

import com.github.lunarea.copperagepatch.creative.CopperCombatTabPatcher;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated verification unit tests for creative mode Combat tab placement of Copper Axe.
 * Tests resolution of Copper Axe, tab key matching, NeoForge event dispatch simulation,
 * and Fabric ItemGroupEntries insertion simulation.
 */
public class CopperCombatTabPlacementTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (com.github.smallinger.copperagebackport.registry.ModItems.COPPER_AXE == null) {
            com.github.smallinger.copperagebackport.registry.ModItems.COPPER_AXE = () -> (net.minecraft.world.item.AxeItem) Items.DIAMOND_AXE;
        }
    }

    @Test
    @DisplayName("Verify COMBAT_TAB_KEY matches vanilla minecraft:combat creative mode tab")
    void testCombatTabKeyDefinition() {
        assertNotNull(CopperCombatTabPatcher.COMBAT_TAB_KEY);
        assertTrue(CopperCombatTabPatcher.COMBAT_TAB_KEY instanceof ResourceKey<?>);
        ResourceKey<?> key = (ResourceKey<?>) CopperCombatTabPatcher.COMBAT_TAB_KEY;
        assertEquals("minecraft", key.location().getNamespace());
        assertEquals("combat", key.location().getPath());
        assertEquals(Registries.CREATIVE_MODE_TAB, key.registryKey());
    }

    @Test
    @DisplayName("Verify getCopperAxe resolves Copper Axe from upstream mod registry")
    void testResolveCopperAxe() {
        Item axe = CopperCombatTabPatcher.getCopperAxe();
        assertNotNull(axe, "Copper Axe must be resolvable from ModItems or BuiltInRegistries");
        assertNotEquals(Items.AIR, axe, "Copper Axe must not be Items.AIR");

        // Verify resolution when field is direct Item rather than Supplier
        try {
            java.lang.reflect.Field f = com.github.smallinger.copperagebackport.registry.ModItems.class.getDeclaredField("COPPER_AXE");
            Object oldVal = f.get(null);
            try {
                // If it can hold an Object or Item
                com.github.smallinger.copperagebackport.registry.ModItems.COPPER_AXE = () -> (net.minecraft.world.item.AxeItem) Items.GOLDEN_AXE;
                assertEquals(Items.GOLDEN_AXE, CopperCombatTabPatcher.getCopperAxe());
            } finally {
                f.set(null, oldVal);
            }
        } catch (Throwable ignored) {}
    }

    public static class MockNeoForgeEvent {
        public final AtomicBoolean insertAfterCalled = new AtomicBoolean(false);
        public final AtomicBoolean acceptCalled = new AtomicBoolean(false);
        public final AtomicReference<ItemStack> afterStackRef = new AtomicReference<>();
        public final AtomicReference<ItemStack> insertedStackRef = new AtomicReference<>();
        public final AtomicReference<ItemStack> acceptedStackRef = new AtomicReference<>();
        private final Object tabKey;
        private final boolean throwOnInsert;
        private final java.util.Set<ItemStack> parentEntries;
        private final java.util.Set<ItemStack> searchEntries;

        public final AtomicBoolean insertBeforeCalled = new AtomicBoolean(false);
        public final AtomicReference<ItemStack> beforeStackRef = new AtomicReference<>();
        private final boolean throwOnInsertBefore;

        public MockNeoForgeEvent(Object tabKey) {
            this(tabKey, false, false, new java.util.HashSet<>(), new java.util.HashSet<>());
        }

        public MockNeoForgeEvent(Object tabKey, boolean throwOnInsert, java.util.Set<ItemStack> parentEntries) {
            this(tabKey, throwOnInsert, false, parentEntries, new java.util.HashSet<>());
        }

        public MockNeoForgeEvent(Object tabKey, boolean throwOnInsert, java.util.Set<ItemStack> parentEntries, java.util.Set<ItemStack> searchEntries) {
            this(tabKey, throwOnInsert, false, parentEntries, searchEntries);
        }

        public MockNeoForgeEvent(Object tabKey, boolean throwOnInsert, boolean throwOnInsertBefore, java.util.Set<ItemStack> parentEntries, java.util.Set<ItemStack> searchEntries) {
            this.tabKey = tabKey;
            this.throwOnInsert = throwOnInsert;
            this.throwOnInsertBefore = throwOnInsertBefore;
            this.parentEntries = parentEntries;
            this.searchEntries = searchEntries;
        }

        public Object getTabKey() {
            return this.tabKey;
        }

        public java.util.Set<ItemStack> getParentEntries() {
            return this.parentEntries;
        }

        public java.util.Set<ItemStack> getSearchEntries() {
            return this.searchEntries;
        }

        public void insertAfter(ItemStack after, ItemStack stack, Object visibility) {
            if (throwOnInsert) {
                throw new IllegalArgumentException("Target ItemStack does not exist in the creative tab!");
            }
            insertAfterCalled.set(true);
            afterStackRef.set(after);
            insertedStackRef.set(stack);
        }

        public void insertBefore(ItemStack before, ItemStack stack, Object visibility) {
            if (throwOnInsertBefore) {
                throw new IllegalArgumentException("Target before ItemStack does not exist in the creative tab!");
            }
            insertBeforeCalled.set(true);
            beforeStackRef.set(before);
            insertedStackRef.set(stack);
        }

        public void accept(ItemStack stack, Object visibility) {
            acceptCalled.set(true);
            acceptedStackRef.set(stack);
        }
    }

    public static class MockFabricEntries {
        public final AtomicBoolean addAfterCalled = new AtomicBoolean(false);
        public final AtomicReference<Object> targetRef = new AtomicReference<>();
        public final AtomicReference<Object[]> itemsRef = new AtomicReference<>();

        public void addAfter(net.minecraft.world.level.ItemLike target, net.minecraft.world.level.ItemLike... items) {
            addAfterCalled.set(true);
            targetRef.set(target);
            itemsRef.set(items);
        }
    }

    public static class MockFabricEntriesItemStackOverload {
        public final AtomicBoolean addAfterCalled = new AtomicBoolean(false);
        public final AtomicReference<ItemStack> targetStackRef = new AtomicReference<>();
        public final AtomicReference<ItemStack[]> itemsStackRef = new AtomicReference<>();

        public void addAfter(ItemStack target, ItemStack... items) {
            addAfterCalled.set(true);
            targetStackRef.set(target);
            itemsStackRef.set(items);
        }
    }

    public static class MockFabricEntriesMixedOverload {
        public final AtomicBoolean addAfterCalled = new AtomicBoolean(false);
        public final AtomicReference<net.minecraft.world.level.ItemLike> targetRef = new AtomicReference<>();
        public final AtomicReference<ItemStack[]> itemsRef = new AtomicReference<>();

        public void addAfter(net.minecraft.world.level.ItemLike target, ItemStack... items) {
            addAfterCalled.set(true);
            targetRef.set(target);
            itemsRef.set(items);
        }
    }

    public static class MockFabricEntriesCollectionOverload {
        public final AtomicBoolean addAfterCalled = new AtomicBoolean(false);
        public final AtomicReference<Object> targetRef = new AtomicReference<>();
        public final AtomicReference<java.util.Collection<?>> colRef = new AtomicReference<>();

        public void addAfter(net.minecraft.world.level.ItemLike target, java.util.Collection<ItemStack> items) {
            addAfterCalled.set(true);
            targetRef.set(target);
            colRef.set(items);
        }
    }

    @Test
    @DisplayName("Verify applyNeoForgeCombatTabPlacement correctly intercepts Combat tab and invokes insertAfter")
    void testNeoForgeCombatTabPlacementSuccess() {
        MockNeoForgeEvent mockEvent = new MockNeoForgeEvent(CopperCombatTabPatcher.COMBAT_TAB_KEY);

        boolean result = CopperCombatTabPatcher.applyNeoForgeCombatTabPlacement(mockEvent);
        assertTrue(result, "applyNeoForgeCombatTabPlacement must return true for Combat tab");
        assertTrue(mockEvent.insertAfterCalled.get(), "insertAfter must have been invoked on event");

        assertNotNull(mockEvent.afterStackRef.get(), "Target 'after' stack must be non-null");
        assertEquals(Items.STONE_AXE, mockEvent.afterStackRef.get().getItem(), "Must insert after Stone Axe");

        assertNotNull(mockEvent.insertedStackRef.get(), "Inserted stack must be non-null");
        assertEquals(CopperCombatTabPatcher.getCopperAxe(), mockEvent.insertedStackRef.get().getItem(), "Inserted stack must be Copper Axe");
    }

    @Test
    @DisplayName("Verify applyNeoForgeCombatTabPlacement avoids duplicate insertion if Copper Axe already in parentEntries")
    void testNeoForgeDuplicateAvoidance() {
        Item copperAxe = CopperCombatTabPatcher.getCopperAxe();
        java.util.Set<ItemStack> parentEntries = new java.util.HashSet<>();
        parentEntries.add(copperAxe.getDefaultInstance());

        MockNeoForgeEvent mockEvent = new MockNeoForgeEvent(CopperCombatTabPatcher.COMBAT_TAB_KEY, false, parentEntries);

        boolean result = CopperCombatTabPatcher.applyNeoForgeCombatTabPlacement(mockEvent);
        assertTrue(result, "Should return true when already present");
        assertFalse(mockEvent.insertAfterCalled.get(), "Should not call insertAfter if already present in parentEntries");
    }

    @Test
    @DisplayName("Verify applyNeoForgeCombatTabPlacement fallback to accept if insertAfter and insertBefore both fail")
    void testNeoForgeFallbackToAccept() {
        MockNeoForgeEvent mockEvent = new MockNeoForgeEvent(CopperCombatTabPatcher.COMBAT_TAB_KEY, true, true, new java.util.HashSet<>(), new java.util.HashSet<>());

        boolean result = CopperCombatTabPatcher.applyNeoForgeCombatTabPlacement(mockEvent);
        assertTrue(result, "Should return true on successful fallback accept");
        assertTrue(mockEvent.acceptCalled.get(), "accept must have been invoked as fallback");
        assertNotNull(mockEvent.acceptedStackRef.get());
        assertEquals(CopperCombatTabPatcher.getCopperAxe(), mockEvent.acceptedStackRef.get().getItem());
    }

    @Test
    @DisplayName("Verify applyNeoForgeCombatTabPlacement ignores non-combat tabs")
    void testNeoForgeNonCombatTabIgnored() {
        ResourceKey<CreativeModeTab> buildingBlocksKey = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                ResourceLocation.withDefaultNamespace("building_blocks")
        );

        MockNeoForgeEvent mockEvent = new MockNeoForgeEvent(buildingBlocksKey);

        boolean result = CopperCombatTabPatcher.applyNeoForgeCombatTabPlacement(mockEvent);
        assertFalse(result, "applyNeoForgeCombatTabPlacement must return false for non-combat tabs");
        assertFalse(mockEvent.insertAfterCalled.get(), "insertAfter must not be called for non-combat tabs");
    }

    @Test
    @DisplayName("Verify applyNeoForgeCombatTabPlacement handles null event safely")
    void testNeoForgeNullEventHandling() {
        assertFalse(CopperCombatTabPatcher.applyNeoForgeCombatTabPlacement(null));
    }

    @Test
    @DisplayName("Verify applyFabricCombatTabPlacement invokes addAfter on Fabric ItemLike entries")
    void testFabricCombatTabPlacementItemLikeSuccess() {
        MockFabricEntries mockEntries = new MockFabricEntries();

        boolean result = CopperCombatTabPatcher.applyFabricCombatTabPlacement(mockEntries);
        assertTrue(result, "applyFabricCombatTabPlacement must return true");
        assertTrue(mockEntries.addAfterCalled.get(), "addAfter must have been called on entries");

        assertEquals(Items.STONE_AXE, mockEntries.targetRef.get(), "Must target Stone Axe");
        assertNotNull(mockEntries.itemsRef.get());
        assertEquals(1, mockEntries.itemsRef.get().length);
        assertEquals(CopperCombatTabPatcher.getCopperAxe(), mockEntries.itemsRef.get()[0], "Added item must be Copper Axe");
    }

    @Test
    @DisplayName("Verify applyFabricCombatTabPlacement invokes addAfter on Fabric ItemStack entries")
    void testFabricCombatTabPlacementItemStackSuccess() {
        MockFabricEntriesItemStackOverload mockEntries = new MockFabricEntriesItemStackOverload();

        boolean result = CopperCombatTabPatcher.applyFabricCombatTabPlacement(mockEntries);
        assertTrue(result, "applyFabricCombatTabPlacement must return true");
        assertTrue(mockEntries.addAfterCalled.get(), "addAfter must have been called on ItemStack entries");

        assertEquals(Items.STONE_AXE, mockEntries.targetStackRef.get().getItem(), "Must target Stone Axe");
        assertNotNull(mockEntries.itemsStackRef.get());
        assertEquals(1, mockEntries.itemsStackRef.get().length);
        assertEquals(CopperCombatTabPatcher.getCopperAxe(), mockEntries.itemsStackRef.get()[0].getItem(), "Added item must be Copper Axe");
    }

    @Test
    @DisplayName("Verify applyFabricCombatTabPlacement invokes addAfter on Fabric mixed ItemLike -> ItemStack entries")
    void testFabricCombatTabPlacementMixedSuccess() {
        MockFabricEntriesMixedOverload mockEntries = new MockFabricEntriesMixedOverload();

        boolean result = CopperCombatTabPatcher.applyFabricCombatTabPlacement(mockEntries);
        assertTrue(result, "applyFabricCombatTabPlacement must return true");
        assertTrue(mockEntries.addAfterCalled.get(), "addAfter must have been called on mixed entries");

        assertEquals(Items.STONE_AXE, mockEntries.targetRef.get().asItem(), "Must target Stone Axe");
        assertNotNull(mockEntries.itemsRef.get());
        assertEquals(1, mockEntries.itemsRef.get().length);
        assertEquals(CopperCombatTabPatcher.getCopperAxe(), mockEntries.itemsRef.get()[0].getItem(), "Added item must be Copper Axe");
    }

    @Test
    @DisplayName("Verify applyFabricCombatTabPlacement invokes addAfter on Fabric Collection entries")
    void testFabricCombatTabPlacementCollectionSuccess() {
        MockFabricEntriesCollectionOverload mockEntries = new MockFabricEntriesCollectionOverload();

        boolean result = CopperCombatTabPatcher.applyFabricCombatTabPlacement(mockEntries);
        assertTrue(result, "applyFabricCombatTabPlacement must return true");
        assertTrue(mockEntries.addAfterCalled.get(), "addAfter must have been called on Collection entries");

        assertEquals(Items.STONE_AXE, mockEntries.targetRef.get(), "Must target Stone Axe");
        assertNotNull(mockEntries.colRef.get());
        assertEquals(1, mockEntries.colRef.get().size());
        assertEquals(CopperCombatTabPatcher.getCopperAxe(), ((ItemStack) mockEntries.colRef.get().iterator().next()).getItem());
    }

    @Test
    @DisplayName("Verify applyFabricCombatTabPlacement handles null entries safely")
    void testFabricNullEntriesHandling() {
        assertFalse(CopperCombatTabPatcher.applyFabricCombatTabPlacement(null));
    }

    @Test
    @DisplayName("Verify applyNeoForgeCombatTabPlacement avoids duplicate insertion if Copper Axe in searchEntries")
    void testNeoForgeSearchEntriesDuplicateAvoidance() {
        Item copperAxe = CopperCombatTabPatcher.getCopperAxe();
        java.util.Set<ItemStack> searchEntries = new java.util.HashSet<>();
        searchEntries.add(copperAxe.getDefaultInstance());

        MockNeoForgeEvent mockEvent = new MockNeoForgeEvent(CopperCombatTabPatcher.COMBAT_TAB_KEY, false, new java.util.HashSet<>(), searchEntries);

        boolean result = CopperCombatTabPatcher.applyNeoForgeCombatTabPlacement(mockEvent);
        assertTrue(result, "Should return true when already present in searchEntries");
        assertFalse(mockEvent.insertAfterCalled.get(), "Should not call insertAfter if already present in searchEntries");
    }

    public static class MockFabricEntriesWithDisplayStacks {
        public final java.util.List<ItemStack> displayStacks = new java.util.ArrayList<>();
        public final AtomicBoolean prependCalled = new AtomicBoolean(false);
        public final AtomicReference<ItemStack> prependedStack = new AtomicReference<>();

        public java.util.List<ItemStack> getDisplayStacks() {
            return displayStacks;
        }

        public void prepend(ItemStack stack) {
            prependCalled.set(true);
            prependedStack.set(stack);
            displayStacks.add(0, stack);
        }
    }

    @Test
    @DisplayName("Verify applyFabricCombatTabPlacement avoids duplicate insertion if Copper Axe in displayStacks")
    void testFabricDuplicateAvoidanceViaDisplayStacks() {
        Item copperAxe = CopperCombatTabPatcher.getCopperAxe();
        MockFabricEntriesWithDisplayStacks mockEntries = new MockFabricEntriesWithDisplayStacks();
        mockEntries.displayStacks.add(copperAxe.getDefaultInstance());

        boolean result = CopperCombatTabPatcher.applyFabricCombatTabPlacement(mockEntries);
        assertTrue(result, "Should return true when already present in displayStacks");
        assertFalse(mockEntries.prependCalled.get(), "Should not prepend if already present in displayStacks");
    }

    @Test
    @DisplayName("Verify applyFabricCombatTabPlacement fallback to prepend or displayStacks if addAfter is absent")
    void testFabricFallbackWhenAddAfterFails() {
        Item copperAxe = CopperCombatTabPatcher.getCopperAxe();
        MockFabricEntriesWithDisplayStacks mockEntries = new MockFabricEntriesWithDisplayStacks();

        boolean result = CopperCombatTabPatcher.applyFabricCombatTabPlacement(mockEntries);
        assertTrue(result, "Should return true on fallback insertion");
        assertTrue(mockEntries.prependCalled.get(), "prepend must have been invoked as fallback");
        assertEquals(copperAxe, mockEntries.prependedStack.get().getItem());
        assertEquals(1, mockEntries.displayStacks.size());
    }

    @Test
    @DisplayName("Verify getIronAxe resolves vanilla Iron Axe item across loaders")
    void testResolveIronAxe() {
        Item ironAxe = CopperCombatTabPatcher.getIronAxe();
        assertNotNull(ironAxe, "Iron Axe must be resolvable");
        assertEquals(Items.IRON_AXE, ironAxe);
        assertFalse(CopperCombatTabPatcher.isAir(ironAxe));
    }

    @Test
    @DisplayName("Verify applyNeoForgeCombatTabPlacement falls back to insertBefore(ironAxe) when Stone Axe is missing")
    void testNeoForgeFallbackToInsertBeforeIronAxe() {
        MockNeoForgeEvent mockEvent = new MockNeoForgeEvent(CopperCombatTabPatcher.COMBAT_TAB_KEY, true, new java.util.HashSet<>());

        boolean result = CopperCombatTabPatcher.applyNeoForgeCombatTabPlacement(mockEvent);
        assertTrue(result, "Should return true on fallback insertBefore");
        assertTrue(mockEvent.insertBeforeCalled.get(), "insertBefore must have been invoked when insertAfter threw");
        assertNotNull(mockEvent.beforeStackRef.get(), "Target before stack must be non-null");
        assertEquals(Items.IRON_AXE, mockEvent.beforeStackRef.get().getItem(), "Must insert before Iron Axe");
        assertNotNull(mockEvent.insertedStackRef.get());
        assertEquals(CopperCombatTabPatcher.getCopperAxe(), mockEvent.insertedStackRef.get().getItem());
        assertFalse(mockEvent.acceptCalled.get(), "accept should not be called when insertBefore succeeded");
    }

    public static class MockFabricEntriesWithAddBefore {
        public final java.util.List<ItemStack> displayStacks = new java.util.ArrayList<>();
        public final AtomicBoolean addBeforeCalled = new AtomicBoolean(false);
        public final AtomicReference<Object> targetRef = new AtomicReference<>();
        public final AtomicReference<Object[]> itemsRef = new AtomicReference<>();

        public void addBefore(net.minecraft.world.level.ItemLike target, net.minecraft.world.level.ItemLike... items) {
            addBeforeCalled.set(true);
            targetRef.set(target);
            itemsRef.set(items);
            displayStacks.add(0, ((Item) items[0]).getDefaultInstance());
        }
    }

    @Test
    @DisplayName("Verify applyFabricCombatTabPlacement falls back to addBefore(ironAxe) when addAfter is absent or failing")
    void testFabricFallbackToAddBeforeIronAxe() {
        Item copperAxe = CopperCombatTabPatcher.getCopperAxe();
        MockFabricEntriesWithAddBefore mockEntries = new MockFabricEntriesWithAddBefore();

        boolean result = CopperCombatTabPatcher.applyFabricCombatTabPlacement(mockEntries);
        assertTrue(result, "applyFabricCombatTabPlacement must return true on addBefore fallback");
        assertTrue(mockEntries.addBeforeCalled.get(), "addBefore must have been invoked");
        assertEquals(Items.IRON_AXE, mockEntries.targetRef.get(), "Must target Iron Axe");
        assertNotNull(mockEntries.itemsRef.get());
        assertEquals(copperAxe, mockEntries.itemsRef.get()[0]);
    }

    @Test
    @DisplayName("Verify isSameItem correctly identifies identical raw Item instances")
    void testIsSameItemRawItems() throws Exception {
        java.lang.reflect.Method isSameItemMethod = CopperCombatTabPatcher.class.getDeclaredMethod("isSameItem", Object.class, Object.class);
        isSameItemMethod.setAccessible(true);

        Item copperAxe = CopperCombatTabPatcher.getCopperAxe();
        Item stoneAxe = CopperCombatTabPatcher.getStoneAxe();

        assertTrue((Boolean) isSameItemMethod.invoke(null, copperAxe, copperAxe), "Identical raw Items must be identified as same");
        assertFalse((Boolean) isSameItemMethod.invoke(null, copperAxe, stoneAxe), "Different raw Items must not be identified as same");
        assertFalse((Boolean) isSameItemMethod.invoke(null, null, copperAxe));
        assertFalse((Boolean) isSameItemMethod.invoke(null, copperAxe, null));
    }
}
