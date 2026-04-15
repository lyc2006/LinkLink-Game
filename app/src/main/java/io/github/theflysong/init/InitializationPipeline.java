package io.github.theflysong.init;

import io.github.theflysong.client.gl.mesh.GLVertexLayouts;
import io.github.theflysong.client.gl.shader.GLShaders;
import io.github.theflysong.client.sprite.Models;
import io.github.theflysong.client.sprite.Sprites;
import io.github.theflysong.gem.Gems;
import io.github.theflysong.util.event.EventBus;
import io.github.theflysong.util.event.EventPriority;

import static io.github.theflysong.App.LOGGER;

/**
 * 基于事件总线的初始化时机管线。
 */
public final class InitializationPipeline {
    private static final EventBus BUS = new EventBus();
    private static boolean listenersRegistered;

    private InitializationPipeline() {
    }

    public static synchronized InitializationEvent initializeClientRegistries() {
        registerDefaultListeners();
        LOGGER.info("Posting initialization event: {}", InitializationEvent.Stage.CLIENT_REGISTRIES);
        InitializationEvent event = new InitializationEvent(InitializationEvent.Stage.CLIENT_REGISTRIES);
        BUS.post(event);
        return event;
    }

    private static void registerDefaultListeners() {
        if (listenersRegistered) {
            return;
        }

        LOGGER.debug("Registering default initialization listeners");

        BUS.register(InitializationEvent.class, event -> {
            if (event.stage() != InitializationEvent.Stage.CLIENT_REGISTRIES) {
                return;
            }
            event.measure("gems", Gems::initialize);
        }, EventPriority.HIGHEST, false);

        BUS.register(InitializationEvent.class, event -> {
            if (event.stage() != InitializationEvent.Stage.CLIENT_REGISTRIES) {
                return;
            }
            event.measure("vertex_layouts", GLVertexLayouts.LAYOUTS::onInitialization);
        }, EventPriority.HIGH, false);

        BUS.register(InitializationEvent.class, event -> {
            if (event.stage() != InitializationEvent.Stage.CLIENT_REGISTRIES) {
                return;
            }
            event.measure("models", Models::initialize);
        }, EventPriority.NORMAL, false);

        BUS.register(InitializationEvent.class, event -> {
            if (event.stage() != InitializationEvent.Stage.CLIENT_REGISTRIES) {
                return;
            }
            event.measure("shaders", GLShaders.SHADERS::onInitialization);
        }, EventPriority.LOW, false);

        BUS.register(InitializationEvent.class, event -> {
            if (event.stage() != InitializationEvent.Stage.CLIENT_REGISTRIES) {
                return;
            }
            event.measure("sprites", Sprites::initialize);
        }, EventPriority.LOWEST, false);

        listenersRegistered = true;
        LOGGER.debug("Default initialization listeners registered");
    }
}
