package com.github.lunarea.copperagepatch;

import com.github.lunarea.copperagepatch.util.MemoizedSupplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class RegistryDuplicateKeySimulationTest {

    static class MockRegistry<T> {
        private final Set<String> registeredKeys = new HashSet<>();

        public synchronized T register(String key, T value) {
            if (registeredKeys.contains(key)) {
                throw new IllegalStateException("Adding duplicate key 'ResourceKey[minecraft:armor_material / " + key + "]' to registry");
            }
            registeredKeys.add(key);
            return value;
        }

        public boolean contains(String key) {
            return registeredKeys.contains(key);
        }
    }

    @Test
    @DisplayName("Simulate upstream bug: unmemoized supplier throws IllegalStateException on duplicate registry key")
    void testUpstreamBugReproduced() {
        MockRegistry<Object> armorRegistry = new MockRegistry<>();
        final String copperKey = "minecraft:copper";

        // Upstream behavior: COPPER = () -> createCopper() where createCopper() registers into the registry
        Supplier<Object> unmemoizedSupplier = () -> {
            Object armorMaterial = new Object();
            return armorRegistry.register(copperKey, armorMaterial);
        };

        // First item (helmet) succeeds
        Object helmetMaterial = unmemoizedSupplier.get();
        assertNotNull(helmetMaterial);

        // Second item (chestplate) crashes with duplicate key error
        IllegalStateException exception = assertThrows(IllegalStateException.class, unmemoizedSupplier::get);
        assertTrue(exception.getMessage().contains("Adding duplicate key 'ResourceKey[minecraft:armor_material / minecraft:copper]' to registry"));
    }

    @Test
    @DisplayName("Simulate patch fix: MemoizedSupplier prevents duplicate registration and safely provides material to all 5 armor pieces")
    void testPatchFixSimulation() {
        MockRegistry<Object> armorRegistry = new MockRegistry<>();
        final String copperKey = "minecraft:copper";

        Supplier<Object> rawSupplier = () -> {
            Object armorMaterial = new Object();
            return armorRegistry.register(copperKey, armorMaterial);
        };

        // Apply our patch memoization
        Supplier<Object> patchedSupplier = new MemoizedSupplier<>(rawSupplier);

        // Register all 5 copper armor items:
        // 1. Helmet
        // 2. Chestplate
        // 3. Leggings
        // 4. Boots
        // 5. Horse Armor
        Object helmetMaterial = patchedSupplier.get();
        Object chestplateMaterial = patchedSupplier.get();
        Object leggingsMaterial = patchedSupplier.get();
        Object bootsMaterial = patchedSupplier.get();
        Object horseArmorMaterial = patchedSupplier.get();

        assertNotNull(helmetMaterial);
        // All 5 pieces must resolve the exact same Holder / ArmorMaterial instance
        assertSame(helmetMaterial, chestplateMaterial);
        assertSame(helmetMaterial, leggingsMaterial);
        assertSame(helmetMaterial, bootsMaterial);
        assertSame(helmetMaterial, horseArmorMaterial);

        // Registry only has 1 entry, never received duplicate registration
        assertTrue(armorRegistry.contains(copperKey));
    }

    /**
     * Complete simulation of CopperArmorMaterial and CopperArmorMaterialMixin combined,
     * matching the bytecode transformation behavior.
     */
    static class SimulatedCopperArmorMaterialWithMixin {
        static Supplier<Object> COPPER;
        static volatile Object cachedHolder;
        static final MockRegistry<Object> REGISTRY = new MockRegistry<>();
        static final String COPPER_KEY = "minecraft:copper";

        static void reset() {
            COPPER = null;
            cachedHolder = null;
            REGISTRY.registeredKeys.clear();
        }

        // Method representing CopperArmorMaterial.createCopper() with Mixin injections
        static Object createCopper() {
            // @Inject(method = "createCopper", at = @At("HEAD"), cancellable = true)
            if (cachedHolder != null) {
                return cachedHolder;
            }

            // Original method body: registers into ARMOR_MATERIAL registry
            Object registered = REGISTRY.register(COPPER_KEY, new Object());

            // @Inject(method = "createCopper", at = @At("RETURN"))
            if (cachedHolder == null) {
                cachedHolder = registered;
            }
            return registered;
        }

        // Method representing CopperArmorMaterial.init() with Mixin injection
        static void init() {
            // Original method body: COPPER = () -> createCopper()
            COPPER = SimulatedCopperArmorMaterialWithMixin::createCopper;

            // @Inject(method = "init", at = @At("RETURN"))
            if (COPPER != null && !(COPPER instanceof MemoizedSupplier)) {
                COPPER = new MemoizedSupplier<>(COPPER);
            }
        }
    }

    @Test
    @DisplayName("Verify third-party mod reflectively re-assigning COPPER does not crash due to createCopper HEAD interceptor")
    void testThirdPartyReassignmentInteroperability() {
        SimulatedCopperArmorMaterialWithMixin.reset();
        SimulatedCopperArmorMaterialWithMixin.init();

        // 1. First armor piece registers normally via memoized supplier
        Object helmet = SimulatedCopperArmorMaterialWithMixin.COPPER.get();
        assertNotNull(helmet);

        // 2. Third-party mod reflectively overwrites COPPER with a raw unmemoized supplier
        SimulatedCopperArmorMaterialWithMixin.COPPER = SimulatedCopperArmorMaterialWithMixin::createCopper;
        assertFalse(SimulatedCopperArmorMaterialWithMixin.COPPER instanceof MemoizedSupplier);

        // 3. Registering subsequent armor pieces (chestplate, leggings, boots, horse armor)
        // must NOT crash because createCopper() HEAD injection intercepts the call and returns cachedHolder
        Object chestplate = assertDoesNotThrow(() -> SimulatedCopperArmorMaterialWithMixin.COPPER.get());
        Object leggings = assertDoesNotThrow(() -> SimulatedCopperArmorMaterialWithMixin.COPPER.get());
        Object boots = assertDoesNotThrow(() -> SimulatedCopperArmorMaterialWithMixin.COPPER.get());
        Object horseArmor = assertDoesNotThrow(() -> SimulatedCopperArmorMaterialWithMixin.COPPER.get());

        assertSame(helmet, chestplate);
        assertSame(helmet, leggings);
        assertSame(helmet, boots);
        assertSame(helmet, horseArmor);
    }

    @Test
    @DisplayName("Verify multiple init() calls remain idempotent and do not re-register")
    void testRepeatedInitIdempotency() {
        SimulatedCopperArmorMaterialWithMixin.reset();
        SimulatedCopperArmorMaterialWithMixin.init();

        Object helmet = SimulatedCopperArmorMaterialWithMixin.COPPER.get();
        Object chestplate = SimulatedCopperArmorMaterialWithMixin.COPPER.get();

        // Re-call init() (e.g., world reload or mod re-init)
        SimulatedCopperArmorMaterialWithMixin.init();
        assertTrue(SimulatedCopperArmorMaterialWithMixin.COPPER instanceof MemoizedSupplier);

        Object leggings = assertDoesNotThrow(() -> SimulatedCopperArmorMaterialWithMixin.COPPER.get());
        Object boots = assertDoesNotThrow(() -> SimulatedCopperArmorMaterialWithMixin.COPPER.get());
        Object horseArmor = assertDoesNotThrow(() -> SimulatedCopperArmorMaterialWithMixin.COPPER.get());

        assertSame(helmet, chestplate);
        assertSame(helmet, leggings);
        assertSame(helmet, boots);
        assertSame(helmet, horseArmor);
    }

    @Test
    @DisplayName("Verify direct createCopper() invocation is safely memoized and never duplicates")
    void testDirectCreateCopperCall() {
        SimulatedCopperArmorMaterialWithMixin.reset();

        // Direct call without init()
        Object first = SimulatedCopperArmorMaterialWithMixin.createCopper();
        assertNotNull(first);

        // Subsequent direct calls must return cachedHolder
        for (int i = 0; i < 5; i++) {
            Object next = assertDoesNotThrow(SimulatedCopperArmorMaterialWithMixin::createCopper);
            assertSame(first, next);
        }
    }

    @Test
    @DisplayName("Verify concurrent multithreaded registration across 20 threads has zero collisions")
    void testConcurrentItemRegistration() throws InterruptedException {
        SimulatedCopperArmorMaterialWithMixin.reset();
        SimulatedCopperArmorMaterialWithMixin.init();

        int threadCount = 20;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.List<Object> results = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        java.util.List<Throwable> errors = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    results.add(SimulatedCopperArmorMaterialWithMixin.COPPER.get());
                } catch (Throwable t) {
                    errors.add(t);
                }
            });
        }

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        assertTrue(errors.isEmpty(), "No thread should throw an exception: " + errors);
        assertEquals(threadCount, results.size());
        Object first = results.get(0);
        for (Object r : results) {
            assertSame(first, r, "All concurrent threads must receive identical Holder instance");
        }
    }

    @Test
    @DisplayName("Verify direct createCopper() called before init() does not cause duplicate registration when COPPER is later accessed")
    void testDirectCreateCopperBeforeInit() {
        SimulatedCopperArmorMaterialWithMixin.reset();

        // 1. Someone calls createCopper() directly before init() was ever called
        Object preInitResult = SimulatedCopperArmorMaterialWithMixin.createCopper();
        assertNotNull(preInitResult);

        // 2. Mod initializes CopperArmorMaterial.init()
        SimulatedCopperArmorMaterialWithMixin.init();

        // 3. DeferredRegister / ModItems access COPPER.get()
        Object postInitResult = assertDoesNotThrow(() -> SimulatedCopperArmorMaterialWithMixin.COPPER.get());

        // Must return the exact same instance, without attempting duplicate registration
        assertSame(preInitResult, postInitResult);
    }

    @Test
    @DisplayName("Verify exception during registration does not cache corrupt state or break recovery")
    void testExceptionDuringRegistrationDoesNotCacheNullOrCorruptState() {
        SimulatedCopperArmorMaterialWithMixin.reset();

        // Simulate temporary registry failure by injecting a key collision pre-emptively
        SimulatedCopperArmorMaterialWithMixin.REGISTRY.registeredKeys.add(SimulatedCopperArmorMaterialWithMixin.COPPER_KEY);

        // Calling createCopper() fails
        assertThrows(IllegalStateException.class, SimulatedCopperArmorMaterialWithMixin::createCopper);
        assertNull(SimulatedCopperArmorMaterialWithMixin.cachedHolder, "cachedHolder must remain null after failed registration");

        // Now resolve external collision
        SimulatedCopperArmorMaterialWithMixin.REGISTRY.registeredKeys.clear();

        // Retry succeeds and caches valid result
        Object recovered = assertDoesNotThrow(SimulatedCopperArmorMaterialWithMixin::createCopper);
        assertNotNull(recovered);
        assertSame(recovered, SimulatedCopperArmorMaterialWithMixin.cachedHolder);
    }
}
