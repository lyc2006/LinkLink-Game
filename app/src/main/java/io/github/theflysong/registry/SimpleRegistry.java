package io.github.theflysong.registry;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;

import io.theflysong.github.data.ResLoc;

/**
 * 简易注册表实现。
 *
 * 特性：
 * 1. 使用 ConcurrentHashMap，支持并发读写。
 * 2. register 不允许重复 key，避免误覆盖。
 */
public class SimpleRegistry<V> implements Registry<V> {
    private final Map<ResLoc, Deferred<V>> values = new ConcurrentHashMap<>();

    @Override
    public Deferred<V> register(ResLoc key, Supplier<V> supplier) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(supplier, "supplier must not be null");
        Deferred<V> deferred = new Deferred<>(supplier);
        Deferred<V> previous = values.putIfAbsent(key, deferred);
        if (previous != null) {
            throw new IllegalStateException("Duplicate registration for key: " + key);
        }
        return deferred;
    }

    @Override
    public Optional<Deferred<V>> get(ResLoc key) {
        Objects.requireNonNull(key, "key must not be null");
        return Optional.ofNullable(values.get(key));
    }

    @Override
    public void onInitialization() {
        for (Deferred<V> deferred : values.values()) {
            deferred.initialize();
        }
    }

    @Override
    public boolean containsKey(ResLoc key) {
        Objects.requireNonNull(key, "key must not be null");
        return values.containsKey(key);
    }

    @Override
    public Set<ResLoc> keys() {
        return Set.copyOf(values.keySet());
    }
}
