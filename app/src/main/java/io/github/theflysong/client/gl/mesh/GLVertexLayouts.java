package io.github.theflysong.client.gl.mesh;

import java.util.List;
import java.util.function.Supplier;

import io.github.theflysong.data.Identifier;
import io.github.theflysong.event.InitializationEvent;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;
import io.github.theflysong.util.event.EventPriority;
import io.github.theflysong.util.event.EventSubscriber;
import io.github.theflysong.util.event.SubscribeEvent;
import io.github.theflysong.util.registry.Deferred;
import io.github.theflysong.util.registry.Registry;
import io.github.theflysong.util.registry.SimpleRegistry;

import static org.lwjgl.opengl.GL11C.GL_FLOAT;

/**
 * 顶点布局描述（无流版本）。
 *
 * 说明：
 * 1. 本类只描述“一个顶点在内存中的排布方式”，不保存具体顶点数据。
 * 2. 当前架构不引入 stream 概念，因此默认所有 attribute 都来自同一个 VBO。
 * 3. 绘制前通常流程为：绑定 VAO -> 绑定 VBO -> 调用 apply() 配置 attribute 指针。
 */
@SideOnly(Side.CLIENT)
public class GLVertexLayouts {
    /**
     * 顶点布局注册表。
     *
     * 通过注册表替代 switch，便于在运行期扩展更多布局。
     */
    public static final Registry<GLVertexLayout> LAYOUTS = new SimpleRegistry<>();

    /**
     * 可直接用于 2D Sprite 的布局：position2 + uv2。
     *
     * 约定：
     * - location 0: vec2 position
     * - location 1: vec2 uv
     *
     * 单顶点占用 16 字节。
     */
    public static final Deferred<GLVertexLayout> SPRITE = register("sprite",
            () -> new GLVertexLayout(
                    16,
                    List.of(
                            GLVertexAttribute.floating(0, 2, GL_FLOAT, false, 0),
                            GLVertexAttribute.floating(1, 2, GL_FLOAT, false, 8))));

    /**
     * 可直接用于 3D 网格的布局：position3 + uv2。
     *
     * 约定：
     * - location 0: vec3 position
     * - location 1: vec2 uv
     *
     * 单顶点占用 20 字节。
     */
    public static final Deferred<GLVertexLayout> P3U2 = register("p3u2",
            () -> new GLVertexLayout(
                    20,
                    List.of(
                            GLVertexAttribute.floating(0, 3, GL_FLOAT, false, 0),
                            GLVertexAttribute.floating(1, 2, GL_FLOAT, false, 12))));

    /**
     * 注册一个新布局。
     */
    public static Deferred<GLVertexLayout> register(Identifier layoutId,
            Supplier<GLVertexLayout> supplier) {
        return LAYOUTS.register(layoutId, supplier);
    }

    /**
     * 注册一个新布局。
     */
    public static Deferred<GLVertexLayout> register(String layoutId,
            Supplier<GLVertexLayout> supplier) {
        return LAYOUTS.register(new Identifier(layoutId), supplier);
    }

    /**
     * 根据配置文件中的布局名解析预设布局。
     */
    public static GLVertexLayout resolve(Identifier layoutId) {
        return LAYOUTS.getOrThrow(layoutId);
    }

    @EventSubscriber
    public static final class InitializationListener {
        @SubscribeEvent(priority = EventPriority.HIGH)
        public void onClientRegistriesInit(InitializationEvent event) {
            if (event.stage() != InitializationEvent.Stage.CLIENT_REGISTRIES) {
                return;
            }
            event.measure("vertex_layouts", GLVertexLayouts.LAYOUTS::onInitialization);
        }
    }
}
