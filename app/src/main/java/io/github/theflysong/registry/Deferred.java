package io.github.theflysong.registry;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 延迟值容器。
 *
 * 语义：
 * 1. 实例创建时仅持有 Supplier，不会立刻构造值。
 * 2. 由注册表在统一时机调用 initialize() 完成构造。
 * 3. initialize() 前调用 get() 会抛异常。
 */
public final class Deferred<V> implements Supplier<V> {
    private Supplier<V> supplier;
    private volatile V value;
    private volatile boolean initialized;

    Deferred(Supplier<V> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier must not be null");
        this.initialized = false;
    }

    /**
     * 执行一次构造；重复调用不会重复构造。
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        V built = supplier.get();
        if (built == null) {
            throw new IllegalStateException("Deferred supplier returned null");
        }
        this.value = built;
        this.initialized = true;
        this.supplier = null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public V get() {
        if (!initialized) {
            throw new IllegalStateException("Deferred value is not initialized yet");
        }
        return value;
    }
}
