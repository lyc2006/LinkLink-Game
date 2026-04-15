package io.github.theflysong.client.sprite;

import java.util.Optional;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import static io.github.theflysong.App.LOGGER;

import io.github.theflysong.data.Identifier;
import io.github.theflysong.event.InitializationEvent;
import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceType;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;
import io.github.theflysong.util.event.EventPriority;
import io.github.theflysong.util.event.EventSubscriber;
import io.github.theflysong.util.event.SubscribeEvent;
import io.github.theflysong.util.registry.Deferred;
import io.github.theflysong.util.registry.Registry;
import io.github.theflysong.util.registry.SimpleRegistry;

/**
 * Model 注册表。
 */
@SideOnly(Side.CLIENT)
public final class Models {
    public static final Registry<Model> MODELS = new SimpleRegistry<>();

    public static final Deferred<Model> GEM = registerFromConfig("gem");

    private Models() {
    }

    public static Deferred<Model> register(Identifier modelId, Supplier<Model> supplier) {
        return MODELS.register(modelId, supplier);
    }

    public static Deferred<Model> register(String modelId, Supplier<Model> supplier) {
        return register(new Identifier(modelId), supplier);
    }

    public static Deferred<Model> registerFromConfig(Identifier modelId, ResourceLocation configLocation) {
        return register(modelId, () -> {
            @Nullable
            Model model = null;
            try {
                model = Model.fromConfig(configLocation);
            } catch (Exception ex) {
                LOGGER.error("Failed to load model from config: {}", configLocation, ex);
                throw new RuntimeException("Failed to load model from config: " + configLocation, ex);
            } finally {
            }
            return model;
        });
    }

    public static Deferred<Model> registerFromConfig(Identifier modelId) {
        return registerFromConfig(modelId,
                new ResourceLocation("linklink", ResourceType.MODEL, modelId.path() + ".json"));
    }

    public static Deferred<Model> registerFromConfig(String modelId) {
        return registerFromConfig(new Identifier(modelId));
    }

    public static void initialize() {
        MODELS.onInitialization();
    }

    public static Optional<Model> get(Identifier modelId) {
        return MODELS.get(modelId);
    }

    public static Optional<Model> get(String modelId) {
        return get(new Identifier(modelId));
    }

    public static Model getOrThrow(Identifier modelId) {
        return MODELS.getOrThrow(modelId);
    }

    public static Model getOrThrow(String modelId) {
        return getOrThrow(new Identifier(modelId));
    }

    public static boolean isRegistered(Identifier modelId) {
        return MODELS.containsKey(modelId);
    }

    public static boolean isRegistered(String modelId) {
        return isRegistered(new Identifier(modelId));
    }

    public static void closeAll() {
        for (Identifier modelId : MODELS.keys()) {
            MODELS.get(modelId)
                    .ifPresent(model -> model.close());
        }
    }

    @EventSubscriber
    public static final class InitializationListener {
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public void onClientRegistriesInit(InitializationEvent event) {
            if (event.stage() != InitializationEvent.Stage.CLIENT_REGISTRIES) {
                return;
            }
            event.measure("models", Models::initialize);
        }
    }
}
