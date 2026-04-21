package io.github.theflysong.client;

import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static io.github.theflysong.App.LOGGER;

import io.github.theflysong.client.gl.Window;
import io.github.theflysong.client.gl.mesh.GLGpuMesh;
import io.github.theflysong.client.gl.shader.GLShaders;
import io.github.theflysong.client.gui.GuiScreenSpace;
import io.github.theflysong.client.gui.LevelScreen;
import io.github.theflysong.client.render.GemRenderer;
import io.github.theflysong.client.render.LevelRenderer;
import io.github.theflysong.client.render.MapRenderer;
import io.github.theflysong.client.render.Renderer;
import io.github.theflysong.client.sprite.Models;
import io.github.theflysong.client.sprite.Sprites;
import io.github.theflysong.event.InitializationEvent;
import io.github.theflysong.input.GameMapInputHandler;
import io.github.theflysong.input.InputDispatcher;
import io.github.theflysong.input.MouseInputContext;
import io.github.theflysong.init.InitializationPipeline;
import io.github.theflysong.level.GameMap;
import io.github.theflysong.level.GameLevel;
import io.github.theflysong.level.MapGenerator;

import static org.lwjgl.opengl.GL11C.*;

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
    private @Nullable LevelRenderer levelRenderer;
    private @Nullable LevelScreen levelScreen;
    private GameLevel gameLevel;
    private @Nullable GLGpuMesh atlasDebugMesh;
    private final InputDispatcher inputDispatcher = new InputDispatcher();
    private final GameMapInputHandler gameMapInputHandler = new GameMapInputHandler(() -> gameLevel, mapRenderer);

    public void run() {
        LOGGER.info("Creating window: {}x{}, title={}", (int) WINDOW_WIDTH, (int) WINDOW_HEIGHT, WINDOW_TITLE);
        new Window((int) WINDOW_WIDTH, (int) WINDOW_HEIGHT, WINDOW_TITLE)
                .onInit(this::init)
                .onRender(this::render)
                .onWindowSize(this::onWindowSize)
            .onMouseButton(this::onMouseButton)
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

        gameLevel = new GameLevel(new MapGenerator().generateHard());
        levelRenderer = new LevelRenderer(renderer);
        levelScreen = new LevelScreen(gameLevel, levelRenderer, gameMapInputHandler);
        atlasDebugMesh = Sprites.CHIPPED_GEM.get().model().createGpuMesh();
        setupInputDispatcher();
        GameMap map = gameLevel.gameMap();
        LOGGER.info("Client initialization completed: map={}x{}", map.width(), map.height());
    }

    private void render() {
        if (levelRenderer != null && levelScreen != null) {
            levelRenderer.renderScreen(levelScreen);
        }
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
        if (levelScreen != null) {
            levelScreen.close();
            levelScreen = null;
        }
        if (levelRenderer != null) {
            levelRenderer.close();
            levelRenderer = null;
        }
        GemRenderer.instance().closeAll();
        Sprites.closeAll();
        Models.closeAll();
        GLShaders.closeAll();
        LOGGER.info("Client cleanup finished");
    }

    private void setupInputDispatcher() {
        inputDispatcher.clear();
        inputDispatcher.register(
            "gui-left-click",
            MouseInputContext::isLeftPress,
            this::handleGuiLeftClick);
    }

    private boolean handleGuiLeftClick(MouseInputContext context) {
        if (levelScreen == null) {
            return false;
        }
        return levelScreen.handleMouseClick(context);
    }

    private void onWindowSize(long windowHandle, int windowWidth, int windowHeight) {
        if (levelScreen == null) {
            return;
        }
        levelScreen.refreshLayout(GuiScreenSpace.fromViewportSize(windowWidth, windowHeight));
    }

    private void onMouseButton(long windowHandle,
                               double cursorX,
                               double cursorY,
                               int windowWidth,
                               int windowHeight,
                               int button,
                               int action,
                               int mods) {
        int safeWidth = Math.max(1, windowWidth);
        int safeHeight = Math.max(1, windowHeight);

        float ndcX = (float) ((cursorX / safeWidth) * 2.0 - 1.0);
        float ndcY = (float) (1.0 - (cursorY / safeHeight) * 2.0);

        MouseInputContext context = new MouseInputContext(
                windowHandle,
                cursorX,
                cursorY,
                ndcX,
                ndcY,
                safeWidth,
                safeHeight,
                button,
                action,
                mods);

        inputDispatcher.dispatch(context);
    }

}