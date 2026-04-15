package io.github.theflysong.client;

import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static io.github.theflysong.App.LOGGER;

import io.github.theflysong.client.gl.Window;
import io.github.theflysong.client.gl.mesh.GLGpuMesh;
import io.github.theflysong.client.gl.shader.GLShaders;
import io.github.theflysong.client.render.GemRenderer;
import io.github.theflysong.client.render.MapRenderer;
import io.github.theflysong.client.render.Renderer;
import io.github.theflysong.client.sprite.Models;
import io.github.theflysong.client.sprite.Sprites;
import io.github.theflysong.event.InitializationEvent;
import io.github.theflysong.init.InitializationPipeline;
import io.github.theflysong.level.GameMap;

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
    private static final float ATLAS_DEBUG_VIEWPORT_FILL = 0.92f;

    @NonNull
    private Renderer renderer = new Renderer();
    @NonNull
    private final MapRenderer mapRenderer = new MapRenderer();
    private GameMap gameMap;
    private @Nullable GLGpuMesh atlasDebugMesh;

    public void run() {
        LOGGER.info("Creating window: {}x{}, title={}", (int) WINDOW_WIDTH, (int) WINDOW_HEIGHT, WINDOW_TITLE);
        new Window((int) WINDOW_WIDTH, (int) WINDOW_HEIGHT, WINDOW_TITLE)
                .onInit(this::init)
                .onRender(this::render)
                .onCleanup(this::cleanup)
                .run();
    }

    private void init() {
        LOGGER.info("Client initialization started");
        InitializationEvent initEvent = InitializationPipeline.initializeClientRegistries();
        initEvent.initializeNanos().forEach((name, nanos) -> {
            double millis = nanos / 1_000_000.0;
            LOGGER.info("[init] {} initialized in {} ms", name, String.format("%.3f", millis));
        });

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        float aspect = WINDOW_WIDTH / WINDOW_HEIGHT;
        Matrix4f projection = new Matrix4f().ortho(-aspect, aspect, -1.0f, 1.0f, -1.0f, 1.0f);
        renderer.updateProjection(projection);

        gameMap = new GameMap(12, 8);
        atlasDebugMesh = Sprites.CHIPPED_GEM.get().model().createGpuMesh();
        LOGGER.info("Client initialization completed: map={}x{}", gameMap.width(), gameMap.height());
    }

    private void render() {
        if (gameMap == null) {
            return;
        }
        mapRenderer.renderMap(renderer, gameMap);
        // renderAtlasDebug();
        renderer.flush();
    }

    private void renderAtlasDebug() {
        if (atlasDebugMesh == null) {
            return;
        }

        Sprites.textureAtlas().ifPresent(atlas -> {
            float atlasWidth = Math.max(1.0f, atlas.width());
            float atlasHeight = Math.max(1.0f, atlas.height());
            float atlasAspect = atlasWidth / atlasHeight;

            float viewHalfWidth = WINDOW_WIDTH / WINDOW_HEIGHT;
            float maxWidth = viewHalfWidth * 2.0f * ATLAS_DEBUG_VIEWPORT_FILL;
            float maxHeight = 2.0f * ATLAS_DEBUG_VIEWPORT_FILL;

            float scaledWidth = Math.min(maxWidth, maxHeight * atlasAspect);
            float scaledHeight = scaledWidth / atlasAspect;

            Matrix4f model = new Matrix4f()
                    .identity()
                    .scale(scaledWidth, scaledHeight, 1.0f);
            atlas.renderDebug(renderer, atlasDebugMesh, GLShaders.SPRITE.get(), model);
        });
    }

    private void cleanup() {
        LOGGER.info("Client cleanup started");
        if (atlasDebugMesh != null) {
            atlasDebugMesh.close();
            atlasDebugMesh = null;
        }
        GemRenderer.instance().closeAll();
        Sprites.closeAll();
        Models.closeAll();
        GLShaders.closeAll();
        LOGGER.info("Client cleanup finished");
    }
}