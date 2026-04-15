package io.theflysong.github.client.sprite;

import io.github.theflysong.registry.Deferred;
import io.github.theflysong.registry.Registry;
import io.github.theflysong.registry.SimpleRegistry;
import io.theflysong.github.data.ResLoc;
import io.theflysong.github.data.ResType;
import io.theflysong.github.util.Side;
import io.theflysong.github.util.SideOnly;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Model 注册表。
 */
@SideOnly(Side.CLIENT)
public final class Models {
    public static final Registry<Model> MODELS = new SimpleRegistry<>();

    public static final Deferred<Model> GEM = registerFromConfig(new ResLoc("linklink", ResType.MODEL, "gem"));

    private Models() {
    }

    public static Deferred<Model> register(ResLoc modelId, Supplier<Model> supplier) {
        return MODELS.register(modelId, supplier);
    }

    public static Deferred<Model> registerFromConfig(ResLoc modelId, ResLoc configLocation) {
        return register(modelId, () -> Model.fromConfig(configLocation));
    }

    public static Deferred<Model> registerFromConfig(ResLoc modelId) {
        return registerFromConfig(modelId, new ResLoc("linklink", ResType.MODEL, modelId.path() + ".json"));
    }

    public static void initialize() {
        MODELS.onInitialization();
    }

    public static Optional<Deferred<Model>> get(ResLoc modelId) {
        return MODELS.get(modelId);
    }

    public static Model getOrThrow(ResLoc modelId) {
        return MODELS.getOrThrow(modelId).get();
    }

    public static boolean isRegistered(ResLoc modelId) {
        return MODELS.containsKey(modelId);
    }

    public static void closeAll() {
        for (ResLoc modelId : MODELS.keys()) {
            MODELS.get(modelId)
                    .filter(Deferred::isInitialized)
                    .ifPresent(deferred -> deferred.get().close());
        }
    }
}
