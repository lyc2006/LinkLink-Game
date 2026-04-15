package io.github.theflysong.util.registry;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.github.theflysong.data.Identifier;


/**
 * 简易注册表实现。
 *
 * 特性：
 * 1. 使用 ConcurrentHashMap，支持并发读写。
 * 2. register 不允许重复 key，避免误覆盖。
 */
public class SimpleRegistry<V> implements Registry<V> {
    private final BiMap<Identifier, Deferred<V>> defers = HashBiMap.create();
    private final BiMap<Identifier, V> values = HashBiMap.create();

    @Override
    public Deferred<V> register(Identifier key, Supplier<V> supplier) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(supplier, "supplier must not be null");
        Deferred<V> deferred = new Deferred<>(supplier);
        Deferred<V> previous = defers.putIfAbsent(key, deferred);
        if (previous != null) {
            throw new IllegalStateException("Duplicate registration for key: " + key);
        }
        return deferred;
    }

    @Override
    public Optional<V> get(String key) {
        Objects.requireNonNull(key, "key must not be null");
        return get(new Identifier(key));
    }

    @Override
    public Optional<V> get(Identifier key) {
        Objects.requireNonNull(key, "key must not be null");
        return Optional.ofNullable(values.get(key));
    }

    @Override
    public void onInitialization() {
        for (var entry : defers.entrySet()) {
            Deferred<V> deferred = entry.getValue();
            Identifier key = entry.getKey();
            if (key == null || deferred == null) {
                continue;
            }
            deferred.initialize();
            values.put(key, deferred.get());
        }
    }

    @Override
    public boolean containsKey(Identifier key) {
        Objects.requireNonNull(key, "key must not be null");
        return defers.containsKey(key);
    }

    @Override
    public boolean containsKey(String key) {
        Objects.requireNonNull(key, "key must not be null");
        return containsKey(new Identifier(key));
    }

    @Override
    public Set<Identifier> keys() {
        return Set.copyOf(defers.keySet());
    }

    @Override
    public Identifier getKey(V value) {
        return values.inverse().get(value);
    }
}
