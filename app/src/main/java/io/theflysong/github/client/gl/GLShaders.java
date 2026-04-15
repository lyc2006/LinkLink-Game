package io.theflysong.github.client.gl;

import java.util.Optional;
import java.util.function.Supplier;

import io.github.theflysong.registry.Deferred;
import io.github.theflysong.registry.Registry;
import io.github.theflysong.registry.SimpleRegistry;
import io.theflysong.github.data.ResLoc;
import io.theflysong.github.data.ResType;
import io.theflysong.github.util.Side;
import io.theflysong.github.util.SideOnly;

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

    public static final Deferred<Shader> SPRITE_OVERLAY = GLShaders.registerFromConfig(
            new ResLoc("linklink", ResType.SHADER, "sprite_overlay"));

    private GLShaders() {
    }

    /**
     * 注册一个 shader 构造器。
     */
    public static Deferred<Shader> register(ResLoc shaderId, Supplier<Shader> supplier) {
        return SHADERS.register(shaderId, supplier);
    }

    /**
     * 便捷注册：由 shader 配置文件创建 Shader。
     */
    public static Deferred<Shader> registerFromConfig(ResLoc shaderId, ResLoc configLocation) {
        return register(shaderId, () -> Shader.fromConfig(configLocation));
    }

    /**
     * 便捷注册：由 shader 配置文件创建 Shader。
     */
    public static Deferred<Shader> registerFromConfig(ResLoc shaderId) {
        return registerFromConfig(shaderId, new ResLoc("linklink", ResType.SHADER, shaderId.path() + ".json"));
    }

    public static Shader getOrThrow(ResLoc shaderId) {
        return SHADERS.getOrThrow(shaderId).get();
    }

    /**
     * 获取 Deferred Shader（不会触发构造）。
     */
    public static Optional<Deferred<Shader>> get(ResLoc shaderId) {
        return SHADERS.get(shaderId);
    }

    public static boolean isRegistered(ResLoc shaderId) {
        return SHADERS.containsKey(shaderId);
    }

    /**
     * 释放所有已构造 shader。
     */
    public static void closeAll() {
        for (ResLoc shaderId : SHADERS.keys()) {
            SHADERS.get(shaderId)
                    .filter(Deferred::isInitialized)
                    .ifPresent(deferred -> deferred.get().close());
        }
    }
}
