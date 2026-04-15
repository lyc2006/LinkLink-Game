package io.github.theflysong.event;

import io.github.theflysong.util.event.EventBus;

import static io.github.theflysong.App.LOGGER;

/**
 * 全局事件入口。
 *
 * 说明：
 * 1. 启动时自动扫描并注册所有注解监听器。
 * 2. 所有事件统一通过该总线派发。
 */
public final class GameEvents {
    public static final EventBus BUS = createBus();

    private GameEvents() {
    }

    private static EventBus createBus() {
        EventBus bus = new EventBus();
        LOGGER.debug("Scanning package for @EventSubscriber classes (global)");
        bus.registerAnnotatedInPackage("io.github.theflysong");
        LOGGER.debug("Global annotated listeners registered");
        return bus;
    }
}
