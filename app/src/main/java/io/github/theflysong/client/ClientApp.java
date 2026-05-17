package io.github.theflysong.client;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static io.github.theflysong.App.LOGGER;

import io.github.theflysong.client.gl.mesh.GLGpuMesh;
import io.github.theflysong.client.gl.shader.GLShaders;
import io.github.theflysong.client.gui.GuiScreen;
import io.github.theflysong.client.gui.GuiScreenSpace;
import io.github.theflysong.client.gui.AuthMenuScreen;
import io.github.theflysong.client.gui.ChoseLevelScreen;
import io.github.theflysong.client.gui.LoginScreen;
import io.github.theflysong.client.gui.RegisterScreen;
import io.github.theflysong.client.gui.LevelScreen;
import io.github.theflysong.client.gui.MainMenuScreen;
import io.github.theflysong.client.gui.PauseMenuScreen;
import io.github.theflysong.client.render.GemRenderer;
import io.github.theflysong.client.render.LevelRenderer;
import io.github.theflysong.client.render.MapRenderer;
import io.github.theflysong.client.render.Renderer;
import io.github.theflysong.client.sprite.Models;
import io.github.theflysong.client.sprite.Sprites;
import io.github.theflysong.client.audio.AudioManager;
import io.github.theflysong.client.window.Window;
import io.github.theflysong.event.InitializationEvent;
import io.github.theflysong.input.GameMapInputHandler;
import io.github.theflysong.input.InputDispatcher;
import io.github.theflysong.input.MouseInputContext;
import io.github.theflysong.init.InitializationPipeline;
import io.github.theflysong.level.GameMap;
import io.github.theflysong.level.GameLevel;
import io.github.theflysong.level.MapGenerator;
import io.github.theflysong.user.UserSystem;

import static org.lwjgl.opengl.GL11C.*;

/**
 * 客户端程序主体，持有窗口和渲染生命周期状态。
 */
public final class ClientApp {
    private static final float WINDOW_WIDTH = 960.0f;
    private static final float WINDOW_HEIGHT = 540.0f;
    private static final String WINDOW_TITLE = "linklink - Gem3 Overlay Demo";
    private static final float ATLAS_DEBUG_VIEWPORT_FILL = 0.92f;

    private enum ScreenState {
        MAIN_MENU,
        PLAYING,
        PAUSED,
        AUTH,
        LEVEL_SELECT
    }

    @NonNull
    private final Renderer renderer = new Renderer();
    @NonNull
    private final MapRenderer mapRenderer = new MapRenderer();
    private @Nullable LevelRenderer levelRenderer;
    private @Nullable MainMenuScreen mainMenuScreen;
    private @Nullable PauseMenuScreen pauseMenuScreen;
    private @Nullable LevelScreen levelScreen;
    private @Nullable GameLevel gameLevel;
    private @Nullable GameLevel savedGameLevel;
    private @Nullable GuiScreen activeScreen;
    private @Nullable AuthMenuScreen authMenuScreen;
    private @Nullable LoginScreen loginScreen;
    private @Nullable RegisterScreen registerScreen;
    private @Nullable ChoseLevelScreen choseLevelScreen;
    private boolean levelCompletionMarked;
    private @Nullable GLGpuMesh atlasDebugMesh;
    private final InputDispatcher inputDispatcher = new InputDispatcher();
    private final GameMapInputHandler gameMapInputHandler = new GameMapInputHandler(() -> gameLevel, mapRenderer);
    private UserSystem userSystem;
    private ScreenState screenState = ScreenState.MAIN_MENU;
    private String selectedLevelId = "preset";

    public void run() {
        LOGGER.info("Creating window: {}x{}, title={}", (int) WINDOW_WIDTH, (int) WINDOW_HEIGHT, WINDOW_TITLE);
        new Window((int) WINDOW_WIDTH, (int) WINDOW_HEIGHT, WINDOW_TITLE)
                .onInit(this::init)
                .onRender(this::render)
                .onWindowSize(this::onWindowSize)
                .onMouseButton(this::onMouseButton)
                .onKey(this::onKey)
                .onChar(this::onChar)
                .onCleanup(this::cleanup)
                .run();
    }

    private void init() {
        LOGGER.info("Client initialization started");
        InitializationEvent initEvent = InitializationPipeline.initializeClientRegistries();
        userSystem = new UserSystem();
        initEvent.initializeNanos().forEach((name, nanos) -> {
            double millis = nanos / 1_000_000.0;
            LOGGER.info("[init] {} initialized in {} ms", name, String.format("%.3f", millis));
        });

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        float aspect = WINDOW_WIDTH / WINDOW_HEIGHT;
        Matrix4f projection = new Matrix4f().ortho(-aspect, aspect, -1.0f, 1.0f, -1.0f, 1.0f);
        renderer.updateProjection(projection);

        levelRenderer = new LevelRenderer(renderer);
        authMenuScreen = new AuthMenuScreen(
                this::openLogin,
                this::openRegister,
                this::loginAsGuest,
                this::onExit);
        if (userSystem.isLoadCorrupted()) {
            authMenuScreen.setWarningMessage("存档数据已损坏");
        }
        activeScreen = authMenuScreen;
        screenState = ScreenState.AUTH;
        setupInputDispatcher();
        AudioManager.playMusic(AudioManager.TITLE_MUSIC);
        LOGGER.info("Client initialization completed: auth screen ready");
    }

    private void render() {
        AudioManager.update();
        if (levelRenderer == null) {
            return;
        }

        if (screenState == ScreenState.MAIN_MENU) {
            if (mainMenuScreen != null) {
                levelRenderer.renderScreen(mainMenuScreen);
            }
        } else if (screenState == ScreenState.PLAYING) {
            if (levelScreen != null) {
                levelRenderer.renderScreen(levelScreen);
            }
        } else if (screenState == ScreenState.PAUSED) {
            if (levelScreen != null) {
                levelRenderer.renderScreen(levelScreen);
            }
            if (pauseMenuScreen != null) {
                levelRenderer.renderScreen(pauseMenuScreen);
            }
        } else if (screenState == ScreenState.AUTH) {
            if (activeScreen != null) {
                levelRenderer.renderScreen(activeScreen);
            }
        } else if (screenState == ScreenState.LEVEL_SELECT) {
            if (choseLevelScreen != null) {
                levelRenderer.renderScreen(choseLevelScreen);
            }
        }

        if (screenState == ScreenState.PLAYING && gameLevel != null && gameLevel.isGameOver() && !levelCompletionMarked) {
            levelCompletionMarked = true;
            if (!userSystem.isGuest()) {
                userSystem.markLevelCompleted(selectedLevelId);
            }
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
        if (mainMenuScreen != null) {
            mainMenuScreen.close();
            mainMenuScreen = null;
        }
        if (pauseMenuScreen != null) {
            pauseMenuScreen.close();
            pauseMenuScreen = null;
        }
        if (authMenuScreen != null) {
            authMenuScreen.close();
            authMenuScreen = null;
        }
        if (loginScreen != null) {
            loginScreen.close();
            loginScreen = null;
        }
        if (registerScreen != null) {
            registerScreen.close();
            registerScreen = null;
        }
        if (choseLevelScreen != null) {
            choseLevelScreen.close();
            choseLevelScreen = null;
        }
        if (levelScreen != null) {
            levelScreen.close();
            levelScreen = null;
        }
        AudioManager.shutdown();
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
        if (screenState == ScreenState.LEVEL_SELECT && choseLevelScreen != null) {
            return choseLevelScreen.handleMouseClick(context);
        }
        if (activeScreen != null) {
            return activeScreen.handleMouseClick(context);
        }
        return false;
    }

    private void onWindowSize(long windowHandle, int windowWidth, int windowHeight) {
        GuiScreenSpace screenSpace = GuiScreenSpace.fromViewportSize(windowWidth, windowHeight);
        if (mainMenuScreen != null) {
            mainMenuScreen.refreshLayout(screenSpace);
        }
        if (pauseMenuScreen != null) {
            pauseMenuScreen.refreshLayout(screenSpace);
        }
        if (levelScreen != null) {
            levelScreen.refreshLayout(screenSpace);
        }
        if (authMenuScreen != null) {
            authMenuScreen.refreshLayout(screenSpace);
        }
        if (loginScreen != null) {
            loginScreen.refreshLayout(screenSpace);
        }
        if (registerScreen != null) {
            registerScreen.refreshLayout(screenSpace);
        }
        if (choseLevelScreen != null) {
            choseLevelScreen.refreshLayout(screenSpace);
        }
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

    private void onKey(long window, int key, int scancode, int action, int mods) {
        if (action != GLFW_PRESS) {
            return;
        }
        if (screenState == ScreenState.AUTH) {
            if (key == GLFW_KEY_ESCAPE) {
                closeAuth();
                return;
            }
            if (key == 259 /* GLFW_KEY_BACKSPACE */) {
                if (activeScreen instanceof LoginScreen login) {
                    login.handleBackspace();
                } else if (activeScreen instanceof RegisterScreen register) {
                    register.handleBackspace();
                }
                return;
            }
            if (key == 257 /* GLFW_KEY_ENTER */) {
                return;
            }
        }
        if (key == GLFW_KEY_ESCAPE) {
            if (screenState == ScreenState.PLAYING) {
                pauseGame();
            } else if (screenState == ScreenState.PAUSED) {
                resumeGame();
            } else if (screenState == ScreenState.LEVEL_SELECT) {
                backToMainMenu();
            }
        }
    }

    private void onChar(long window, int codepoint) {
        if (activeScreen instanceof LoginScreen login) {
            login.handleChar(codepoint);
        } else if (activeScreen instanceof RegisterScreen register) {
            register.handleChar(codepoint);
        }
    }

    private void startGame() {
        userSystem.clearSave();
        levelCompletionMarked = false;
        createGameLevel(selectedLevelId);
        gameLevel.startLevel();
        resumeGame();
    }

    private void loginAsGuest() {
        userSystem.loginAsGuest();
        gotoMainMenu();
    }

    private void openLogin() {
        if (loginScreen == null) {
            loginScreen = new LoginScreen(userSystem, this::gotoMainMenu, this::backToAuthMenu);
        }
        loginScreen.resetInit();
        screenState = ScreenState.AUTH;
        activeScreen = loginScreen;
    }

    private void onExit() {
        long handle = Window.currentHandle();
        if(handle != 0){
            glfwSetWindowShouldClose(handle, true);
        }
    }   

    private void openRegister() {
        if (registerScreen == null) {
            registerScreen = new RegisterScreen(userSystem, this::gotoMainMenu, this::backToAuthMenu);
        }
        registerScreen.resetInit();
        screenState = ScreenState.AUTH;
        activeScreen = registerScreen;
    }

    private void openLevelSelect() {
        if (choseLevelScreen == null) {
            choseLevelScreen = new ChoseLevelScreen(
                    this::selectLevel,
                    this::backToMainMenu,
                    userSystem::isLevelCompleted);
        }
        choseLevelScreen.resetInit();
        screenState = ScreenState.LEVEL_SELECT;
        activeScreen = null;
    }

    private void backToMainMenu() {
        screenState = ScreenState.MAIN_MENU;
        activeScreen = mainMenuScreen;
    }

    private void backToAuthMenu() {
        screenState = ScreenState.AUTH;
        activeScreen = authMenuScreen;
    }

    private void gotoMainMenu() {
        if (mainMenuScreen == null) {
            mainMenuScreen = new MainMenuScreen(
                    this::startGame,
                    this::continueGame,
                    this::backToLogin,
                    this::PKGame,
                    this::openLevelSelect,
                    this::selectedLevelLabel);
            pauseMenuScreen = new PauseMenuScreen(
                    this::saveGame,
                    this::resumeGame,
                    this::quitToMainMenu);
        }
        screenState = ScreenState.MAIN_MENU;
        activeScreen = mainMenuScreen;
        updateUserDisplay();
        updateMenuState();
    }

    private void closeAuth() {
        if (activeScreen instanceof LoginScreen || activeScreen instanceof RegisterScreen) {
            backToAuthMenu();
            return;
        }
        // AuthMenuScreen: ESC exits game
        backToLogin();
    }

    private void updateUserDisplay() {
        if (mainMenuScreen != null && userSystem.getCurrentUser() != null) {
            mainMenuScreen.setCurrentUser(userSystem.getCurrentUser().getUsername());
        }
    }

    private void PKGame() {
        //TODO Auto-generated method stub
        }

    private void continueGame() {
        if (screenState == ScreenState.PAUSED) {
            resumeGame();
            return;
        }
        if (gameLevel != null) {
            gameLevel.resumeLevel();
            screenState = ScreenState.PLAYING;
            activeScreen = levelScreen;
            return;
        }
        if (userSystem.hasSave()) {
            GameMap savedMap = userSystem.getSavedGameMap();
            if (savedMap != null) {
                gameLevel = new GameLevel(savedMap);
                gameLevel.energyBar().setEnergy(userSystem.getSavedEnergy());
                gameLevel.startLevel();
                gameLevel.setScore(userSystem.getSavedScore());
                gameLevel.setElapsedSeconds(userSystem.getSavedTime());
                selectedLevelId = userSystem.getSavedLevelId();
                levelScreen = new LevelScreen(gameLevel, levelRenderer, gameMapInputHandler);
                screenState = ScreenState.PLAYING;
                activeScreen = levelScreen;
                updateMenuState();
                return;
            }
        }
        if (savedGameLevel != null) {
            gameLevel = cloneGameLevel(savedGameLevel);
            gameLevel.startLevel();
            levelScreen = new LevelScreen(gameLevel, levelRenderer, gameMapInputHandler);
            screenState = ScreenState.PLAYING;
            activeScreen = levelScreen;
            return;
        }
    }

    private void pauseGame() {
        if (screenState != ScreenState.PLAYING) {
            return;
        }
        screenState = ScreenState.PAUSED;
        activeScreen = pauseMenuScreen;
        gameLevel.pauseLevel();
    }

    private void resumeGame() {
        if (gameLevel == null || levelScreen == null || savedGameLevel != null) {
            return;
        }
        gameLevel.resumeLevel();
        screenState = ScreenState.PLAYING;
        activeScreen = levelScreen;
    }

    private void backToLogin() {
        userSystem.logout();
        if (gameLevel != null) {
            gameLevel = null;
        }
        if (levelScreen != null) {
            levelScreen.close();
            levelScreen = null;
        }
        if (mainMenuScreen != null) {
            mainMenuScreen.setContinueEnabled(false);
            mainMenuScreen.setCurrentUser("游客");
        }
        screenState = ScreenState.AUTH;
        activeScreen = authMenuScreen;
    }

    private void quitToMainMenu() {
        screenState = ScreenState.MAIN_MENU;
        activeScreen = mainMenuScreen;
        updateMenuState();
    }

    private void createGameLevel(String levelId) {
        if (levelRenderer == null) {
            return;
        }
        gameLevel = new GameLevel(MapGenerator.generate(levelId));
        gameLevel.energyBar().setEnergy(0);
        levelScreen = new LevelScreen(gameLevel, levelRenderer, gameMapInputHandler);
        selectedLevelId = levelId;
        updateMenuState();
    }

    private void saveGame() {
        if (gameLevel == null) {
            return;
        }
        savedGameLevel = cloneGameLevel(gameLevel);
        if (!userSystem.isGuest()) {
            userSystem.saveGame(
                gameLevel.gameMap(),
                selectedLevelId,
                gameLevel.energyBar().currentEnergy(),
                gameLevel.score(),
                gameLevel.elapsedSeconds()
            );
        }
        updateMenuState();
        LOGGER.info("Game saved for level {}", selectedLevelId);
    }

    private void selectLevel(String levelId) {
        selectedLevelId = levelId;
        if (mainMenuScreen != null) {
            // mainMenuScreen.setSelectedLevelId(levelId);
            mainMenuScreen.setContinueEnabled(
                gameLevel != null || savedGameLevel != null || userSystem.hasSave());
        }
    }

    private String selectedLevelLabel() {
        return switch (selectedLevelId) {
            case "simple" -> "简单";
            case "hard" -> "困难";
            case "preset" -> "预设";
            case "bridge" -> "桥梁";
            case "cell" -> "细胞";
            case "island" -> "岛屿";
            case "foolish" -> "愚者";
            default -> selectedLevelId;
        };
    }

    private void updateMenuState() {
        if (mainMenuScreen == null) {
            return;
        }
        mainMenuScreen.setContinueEnabled(
            gameLevel != null || savedGameLevel != null || userSystem.hasSave());
    }

    private @Nullable GameLevel cloneGameLevel(@NonNull GameLevel source) {
        return new GameLevel(source.gameMap().copy());
    }

}