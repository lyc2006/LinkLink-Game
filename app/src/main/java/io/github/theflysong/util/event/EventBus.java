package io.github.theflysong.util.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 无事件总线。
 *
 * 特性：
 * 1. 显式注册/取消注册监听器。
 * 2. 支持优先级与是否接收已取消事件。
 * 3. post 时按事件类型继承树分发（子类事件会被父类监听器接收）。
 */
public final class EventBus {
    private final Map<Class<? extends Event>, CopyOnWriteArrayList<ListenerEntry<? extends Event>>> listeners = new ConcurrentHashMap<>();

    public <E extends Event> EventSubscription register(Class<E> eventType, EventListener<? super E> listener) {
        return register(eventType, listener, EventPriority.NORMAL, false);
    }

    public <E extends Event> EventSubscription register(Class<E> eventType,
                                                        EventListener<? super E> listener,
                                                        EventPriority priority,
                                                        boolean receiveCanceled) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        Objects.requireNonNull(priority, "priority must not be null");

        ListenerEntry<E> entry = new ListenerEntry<>(eventType, listener, priority, receiveCanceled);
        CopyOnWriteArrayList<ListenerEntry<? extends Event>> bucket =
                listeners.computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>());
        bucket.add(entry);
        bucket.sort(Comparator.comparingInt((ListenerEntry<? extends Event> e) -> e.priority().level()).reversed());

        return () -> bucket.remove(entry);
    }

    public <E extends Event> E post(E event) {
        Objects.requireNonNull(event, "event must not be null");

        List<ListenerEntry<? extends Event>> entries = collectApplicable(event.getClass());
        entries.sort(Comparator.comparingInt((ListenerEntry<? extends Event> e) -> e.priority().level()).reversed());

        for (ListenerEntry<? extends Event> raw : entries) {
            @SuppressWarnings("unchecked")
            ListenerEntry<E> entry = (ListenerEntry<E>) raw;
            if (event.isCanceled() && !entry.receiveCanceled()) {
                continue;
            }
            entry.listener().onEvent(event);
        }
        return event;
    }

    public int listenerCount(Class<? extends Event> eventType) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        List<ListenerEntry<? extends Event>> bucket = listeners.get(eventType);
        return bucket == null ? 0 : bucket.size();
    }

    public void clear() {
        listeners.clear();
    }

    private List<ListenerEntry<? extends Event>> collectApplicable(Class<?> eventClass) {
        List<ListenerEntry<? extends Event>> result = new ArrayList<>();
        for (Map.Entry<Class<? extends Event>, CopyOnWriteArrayList<ListenerEntry<? extends Event>>> bucket : listeners.entrySet()) {
            if (bucket.getKey().isAssignableFrom(eventClass)) {
                result.addAll(bucket.getValue());
            }
        }
        return result;
    }

    private record ListenerEntry<E extends Event>(Class<E> eventType,
                                                  EventListener<? super E> listener,
                                                  EventPriority priority,
                                                  boolean receiveCanceled) {
    }
}
