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
 * Sprite 注册表。
 */
@SideOnly(Side.CLIENT)
public final class Sprites {
    public static final Registry<Sprite> SPRITES = new SimpleRegistry<>();

    public static final Deferred<Sprite> CHIPPED_GEM = registerFromConfig(
            new ResLoc("linklink", ResType.SPRITE, "chipped_gem"));
    public static final Deferred<Sprite> GEM3 = registerFromConfig(
            new ResLoc("linklink", ResType.SPRITE, "gem3"));

    private Sprites() {
    }

    public static Deferred<Sprite> register(ResLoc spriteId, Supplier<Sprite> supplier) {
        return SPRITES.register(spriteId, supplier);
    }

    public static Deferred<Sprite> registerFromConfig(ResLoc spriteId, ResLoc configLocation) {
        return register(spriteId, () -> Sprite.fromConfig(configLocation));
    }

    public static Deferred<Sprite> registerFromConfig(ResLoc spriteId) {
        return registerFromConfig(spriteId, new ResLoc("linklink", ResType.SPRITE, spriteId.path() + ".json"));
    }

    public static void initialize() {
        SPRITES.onInitialization();
    }

    public static Optional<Deferred<Sprite>> get(ResLoc spriteId) {
        return SPRITES.get(spriteId);
    }

    public static Sprite getOrThrow(ResLoc spriteId) {
        return SPRITES.getOrThrow(spriteId).get();
    }

    public static boolean isRegistered(ResLoc spriteId) {
        return SPRITES.containsKey(spriteId);
    }

    public static void closeAll() {
        for (ResLoc spriteId : SPRITES.keys()) {
            SPRITES.get(spriteId)
                    .filter(Deferred::isInitialized)
                    .ifPresent(deferred -> deferred.get().close());
        }
    }
}
