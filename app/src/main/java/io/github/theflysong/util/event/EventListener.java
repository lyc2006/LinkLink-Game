package io.github.theflysong.util.event;

/**
 * 事件监听器。
 */
@FunctionalInterface
public interface EventListener<E extends Event> {
    void onEvent(E event);
}
