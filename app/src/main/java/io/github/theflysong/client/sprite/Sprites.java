package io.github.theflysong.client.sprite;

import java.util.Optional;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import io.github.theflysong.data.Identifier;
import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceType;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;
import io.github.theflysong.util.registry.Deferred;
import io.github.theflysong.util.registry.Registry;
import io.github.theflysong.util.registry.SimpleRegistry;

/**
 * Sprite 注册表。
 */
@SideOnly(Side.CLIENT)
public final class Sprites {
    public static final Registry<Sprite> SPRITES = new SimpleRegistry<>();

    public static final Deferred<Sprite> CHIPPED_GEM = registerFromConfig(
            "gem.chipped");
    public static final Deferred<Sprite> GEM3 = registerFromConfig(
            "gem3");

    private Sprites() {
    }

    public static Deferred<Sprite> register(Identifier spriteId, Supplier<Sprite> supplier) {
        return SPRITES.register(spriteId, supplier);
    }

    public static Deferred<Sprite> register(String spriteId, Supplier<Sprite> supplier) {
        return register(new Identifier(spriteId), supplier);
    }

    public static Deferred<Sprite> registerFromConfig(Identifier spriteId, ResourceLocation configLocation) {
        return register(spriteId, () -> {
            @Nullable
            Sprite sprite = null;
            try {
                sprite = Sprite.fromConfig(configLocation);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to load sprite from config: " + configLocation, ex);
            } finally {
            }
            return sprite;
        });
    }

    public static Deferred<Sprite> registerFromConfig(Identifier spriteId) {
        return registerFromConfig(spriteId,
                new ResourceLocation("linklink", ResourceType.SPRITE, spriteId.path() + ".json"));
    }

    public static Deferred<Sprite> registerFromConfig(String spriteId) {
        return registerFromConfig(new Identifier(spriteId));
    }

    public static void initialize() {
        SPRITES.onInitialization();
    }

    public static Optional<Sprite> get(Identifier spriteId) {
        return SPRITES.get(spriteId);
    }

    public static Optional<Sprite> get(String spriteId) {
        return get(new Identifier(spriteId));
    }

    public static Sprite getOrThrow(Identifier spriteId) {
        return SPRITES.getOrThrow(spriteId);
    }

    public static Sprite getOrThrow(String spriteId) {
        return getOrThrow(new Identifier(spriteId));
    }

    public static boolean isRegistered(Identifier spriteId) {
        return SPRITES.containsKey(spriteId);
    }

    public static boolean isRegistered(String spriteId) {
        return isRegistered(new Identifier(spriteId));
    }

    public static void closeAll() {
        for (Identifier spriteId : SPRITES.keys()) {
            SPRITES.get(spriteId)
                    .ifPresent(sprite -> sprite.close());
        }
    }
}
