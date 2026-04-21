package io.github.theflysong.client.gui;

import io.github.theflysong.client.render.LevelRenderer;
import io.github.theflysong.input.GameMapInputHandler;
import io.github.theflysong.level.GameLevel;

/**
 * 关卡屏幕。
 */
public final class LevelScreen extends GuiScreen {
    private final GameLevel gameLevel;
    private final LevelRenderer levelRenderer;
    private final GameMapInputHandler gameMapInputHandler;
    private GameMapComponent gameMapComponent;
    private EnergyBarComponent energyBarComponent;

    public LevelScreen(GameLevel gameLevel,
            LevelRenderer levelRenderer,
            GameMapInputHandler gameMapInputHandler) {
        this.gameLevel = gameLevel;
        this.levelRenderer = levelRenderer;
        this.gameMapInputHandler = gameMapInputHandler;
    }

    @Override
    protected void onInit(GuiRenderer renderer) {
        GuiScreenSpace screenSpace = GuiScreenSpace.fromCurrentViewport();
        gameMapComponent = addComponent(new GameMapComponent(gameLevel, levelRenderer, gameMapInputHandler, screenSpace));
        energyBarComponent = addComponent(new EnergyBarComponent(gameLevel, levelRenderer));
    }

    @Override
    protected void renderScreen(GuiRenderer renderer) {
    }
}