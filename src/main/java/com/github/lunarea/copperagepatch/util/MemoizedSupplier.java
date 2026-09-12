package com.github.lunarea.copperagepatch.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Thread-safe memoizing supplier implementation.
 * Guarantees that the underlying supplier is evaluated at most once,
 * caching and returning the resulting value on all subsequent calls.
 *
 * @param <T> value type
 */
public class MemoizedSupplier<T> implements Supplier<T> {
    private final Supplier<T> delegate;
    private volatile boolean initialized;
    private volatile T value;

    public MemoizedSupplier(Supplier<T> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate supplier must not be null");
    }

    @Override
    public T get() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    value = delegate.get();
                    initialized = true;
                }
            }
        }
        return value;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Supplier<T> getDelegate() {
        return delegate;
    }
}
