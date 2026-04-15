package io.github.theflysong.client.gl.shader;

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
 * 着色器注册表。
 *
 * 职责：
 * 1. 以 ResLoc 作为键注册 Shader Supplier。
 * 2. 在统一时机构造所有 Shader。
 * 3. 提供统一释放入口，避免资源泄漏。
 */
@SideOnly(Side.CLIENT)
public final class GLShaders {
    public static final Registry<Shader> SHADERS = new SimpleRegistry<>();

    public static final Deferred<Shader> SPRITE = GLShaders.registerFromConfig("sprite");
    public static final Deferred<Shader> TEXTURE = GLShaders.registerFromConfig("texture");
    public static final Deferred<Shader> SPRITE_META = GLShaders.registerFromConfig("sprite_meta");
    public static final Deferred<Shader> SPRITE_OVERLAY = GLShaders.registerFromConfig("sprite_overlay");
    public static final Deferred<Shader> SPRITE_META_OVERLAY = GLShaders.registerFromConfig("sprite_meta_overlay");

    private GLShaders() {
    }

    /**
     * 注册一个 shader 构造器。
     */
    public static Deferred<Shader> register(Identifier shaderId, Supplier<Shader> supplier) {
        return SHADERS.register(shaderId, supplier);
    }

    public static Deferred<Shader> register(String shaderId, Supplier<Shader> supplier) {
        return register(new Identifier(shaderId), supplier);
    }

    /**
     * 便捷注册：由 shader 配置文件创建 Shader。
     */
    public static Deferred<Shader> registerFromConfig(Identifier shaderId, ResourceLocation configLocation) {
        return register(shaderId, () -> {
            @Nullable Shader shader = null;
            try {
                shader = Shader.fromConfig(configLocation);
            } catch (Exception ex) {
                LOGGER.error("Failed to load shader from config: {}", configLocation, ex);
                throw new RuntimeException("Failed to load shader from config: " + configLocation, ex);
            } finally {
            }
            return shader;
        });
    }

    /**
     * 便捷注册：由 shader 配置文件创建 Shader。
     */
    public static Deferred<Shader> registerFromConfig(Identifier shaderId) {
        return registerFromConfig(shaderId, new ResourceLocation("linklink", ResourceType.SHADER, shaderId.path() + ".json"));
    }

    /**
     * 便捷注册：由 shader 配置文件创建 Shader。
     */
    public static Deferred<Shader> registerFromConfig(String shaderId) {
        return registerFromConfig(new Identifier(shaderId));
    }

    public static Shader getOrThrow(Identifier shaderId) {
        return SHADERS.getOrThrow(shaderId);
    }

    public static Shader getOrThrow(String shaderId) {
        return getOrThrow(new Identifier(shaderId));
    }

    public static Optional<Shader> get(Identifier shaderId) {
        return SHADERS.get(shaderId);
    }

    public static Optional<Shader> get(String shaderId) {
        return get(new Identifier(shaderId));
    }

    public static boolean isRegistered(Identifier shaderId) {
        return SHADERS.containsKey(shaderId);
    }

    public static boolean isRegistered(String shaderId) {
        return isRegistered(new Identifier(shaderId));
    }

    /**
     * 释放所有已构造 shader。
     */
    public static void closeAll() {
        for (Identifier shaderId : SHADERS.keys()) {
            SHADERS.get(shaderId)
                    .ifPresent(shader -> shader.close());
        }
    }

    @EventSubscriber
    public static final class InitializationListener {
        @SubscribeEvent(priority = EventPriority.LOW)
        public void onClientRegistriesInit(InitializationEvent event) {
            if (event.stage() != InitializationEvent.Stage.CLIENT_REGISTRIES) {
                return;
            }
            event.measure("shaders", GLShaders.SHADERS::onInitialization);
        }
    }
}
