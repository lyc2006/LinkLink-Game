package io.github.theflysong.util.event;

/**
 * 事件订阅句柄，用于取消注册。
 */
public interface EventSubscription extends AutoCloseable {
    void unsubscribe();

    @Override
    default void close() {
        unsubscribe();
    }
}
