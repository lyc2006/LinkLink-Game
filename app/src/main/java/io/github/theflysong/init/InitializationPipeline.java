package io.github.theflysong.init;

import io.github.theflysong.event.GameEvents;
import io.github.theflysong.event.InitializationEvent;

import static io.github.theflysong.App.LOGGER;

/**
 * 基于事件总线的初始化时机管线。
 */
public final class InitializationPipeline {
    private InitializationPipeline() {
    }

    public static synchronized InitializationEvent initializeClientRegistries() {
        LOGGER.info("Posting initialization event: {}", InitializationEvent.Stage.CLIENT_REGISTRIES);
        InitializationEvent event = new InitializationEvent(InitializationEvent.Stage.CLIENT_REGISTRIES);
        GameEvents.BUS.post(event);
        return event;
    }
}
