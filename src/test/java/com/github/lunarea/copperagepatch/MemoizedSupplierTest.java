package com.github.lunarea.copperagepatch;

import com.github.lunarea.copperagepatch.util.MemoizedSupplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class MemoizedSupplierTest {

    @Test
    @DisplayName("Underlying supplier is called exactly once upon repeated get() calls")
    void testSingleInvocation() {
        AtomicInteger callCount = new AtomicInteger(0);
        MemoizedSupplier<String> supplier = new MemoizedSupplier<>(() -> {
            callCount.incrementAndGet();
            return "copper_material_holder";
        });

        assertFalse(supplier.isInitialized());
        assertEquals(0, callCount.get());

        // Simulate 5 armor item registrations (helmet, chestplate, leggings, boots, horse armor)
        for (int i = 0; i < 5; i++) {
            String value = supplier.get();
            assertEquals("copper_material_holder", value);
            assertTrue(supplier.isInitialized());
            assertEquals(1, callCount.get(), "Supplier should have only been invoked once across 5 calls");
        }
    }

    @Test
    @DisplayName("Supplier returning null is evaluated exactly once and marked as initialized")
    void testSupplierReturningNullEvaluatedOnlyOnce() {
        AtomicInteger callCount = new AtomicInteger(0);
        MemoizedSupplier<Object> supplier = new MemoizedSupplier<>(() -> {
            callCount.incrementAndGet();
            return null;
        });

        assertFalse(supplier.isInitialized());
        assertNull(supplier.get());
        assertTrue(supplier.isInitialized());
        assertEquals(1, callCount.get());

        // Subsequent calls must return null without re-evaluating delegate
        assertNull(supplier.get());
        assertNull(supplier.get());
        assertEquals(1, callCount.get(), "Delegate should not be evaluated again when returning null");
    }

    @Test
    @DisplayName("Concurrent multi-threaded calls evaluate delegate exactly once")
    void testConcurrentAccess() throws InterruptedException {
        AtomicInteger callCount = new AtomicInteger(0);
        MemoizedSupplier<Object> supplier = new MemoizedSupplier<>(() -> {
            callCount.incrementAndGet();
            try {
                Thread.sleep(10); // Simulate registry work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new Object();
        });

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Object> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    results.add(supplier.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals(threadCount, results.size());
        assertEquals(1, callCount.get(), "Delegate should be invoked exactly once even under heavy concurrency");
        Object first = results.get(0);
        for (Object res : results) {
            assertSame(first, res, "All threads must receive the identical cached instance");
        }
    }

    @Test
    @DisplayName("Null delegate throws NullPointerException immediately")
    void testNullDelegate() {
        assertThrows(NullPointerException.class, () -> new MemoizedSupplier<>(null));
    }

    @Test
    @DisplayName("Exceptions from delegate propagate correctly")
    void testExceptionPropagation() {
        MemoizedSupplier<String> supplier = new MemoizedSupplier<>(() -> {
            throw new IllegalStateException("Simulated registry failure");
        });

        IllegalStateException ex = assertThrows(IllegalStateException.class, supplier::get);
        assertEquals("Simulated registry failure", ex.getMessage());
        assertFalse(supplier.isInitialized());
    }
}
