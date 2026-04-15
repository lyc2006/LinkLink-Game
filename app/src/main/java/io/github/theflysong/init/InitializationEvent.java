package io.github.theflysong.init;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.theflysong.util.event.Event;

/**
 * 初始化事件，承载阶段信息与各初始化步骤耗时。
 */
public final class InitializationEvent extends Event {
    public enum Stage {
        CLIENT_REGISTRIES
    }

    private final Stage stage;
    private final Map<String, Long> initializeNanos = new LinkedHashMap<>();

    public InitializationEvent(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("stage must not be null");
        }
        this.stage = stage;
    }

    public Stage stage() {
        return stage;
    }

    public void measure(String name, Runnable initializer) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (initializer == null) {
            throw new IllegalArgumentException("initializer must not be null");
        }
        long start = System.nanoTime();
        initializer.run();
        long elapsed = System.nanoTime() - start;
        initializeNanos.put(name, elapsed);
    }

    public Map<String, Long> initializeNanos() {
        return Map.copyOf(initializeNanos);
    }
}
