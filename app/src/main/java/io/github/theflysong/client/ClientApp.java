package io.github.theflysong.client;

import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import io.github.theflysong.client.gl.Window;
import io.github.theflysong.client.gl.mesh.GLVertexLayouts;
import io.github.theflysong.client.gl.shader.GLShaders;
import io.github.theflysong.client.render.GemRenderer;
import io.github.theflysong.client.render.Renderer;
import io.github.theflysong.client.sprite.Models;
import io.github.theflysong.client.sprite.Sprites;
import io.github.theflysong.gem.GemColor;
import io.github.theflysong.gem.GemInstance;
import io.github.theflysong.gem.Gems;

import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL11C.glEnable;

/**
 * 客户端程序主体，持有窗口和渲染生命周期状态。
 */
public final class ClientApp {
    private static final float WINDOW_WIDTH = 960.0f;
    private static final float WINDOW_HEIGHT = 540.0f;
    private static final String WINDOW_TITLE = "linklink - Gem3 Overlay Demo";

    @NonNull
    private Renderer renderer = new Renderer();
    private GemInstance chippedGem;

    public void run() {
        new Window((int) WINDOW_WIDTH, (int) WINDOW_HEIGHT, WINDOW_TITLE)
                .onInit(this::init)
                .onRender(this::render)
                .onCleanup(this::cleanup)
                .run();
    }

    private void init() {
        Gems.initialize();
        GLVertexLayouts.LAYOUTS.onInitialization();
        Models.initialize();
        GLShaders.SHADERS.onInitialization();
        Sprites.initialize();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        float aspect = WINDOW_WIDTH / WINDOW_HEIGHT;
        Matrix4f projection = new Matrix4f().ortho(-aspect, aspect, -1.0f, 1.0f, -1.0f, 1.0f);
        renderer.updateProjection(projection);

        chippedGem = new GemInstance(Gems.CHIPPED_GEM.get(), GemColor.DIAMOND);
    }

    private void render() {
        if (chippedGem == null) {
            return;
        }
        GemRenderer.instance().renderGem(renderer, chippedGem, new Matrix4f().identity());
        renderer.flush();
    }

    private void cleanup() {
        GemRenderer.instance().closeAll();
        Sprites.closeAll();
        Models.closeAll();
        GLShaders.closeAll();
    }
}