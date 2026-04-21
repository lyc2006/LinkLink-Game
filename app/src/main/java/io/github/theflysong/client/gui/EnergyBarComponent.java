package io.github.theflysong.client.gui;

import org.jspecify.annotations.NonNull;

import io.github.theflysong.client.render.LevelRenderer;
import io.github.theflysong.level.GameLevel;

/**
 * 能量条组件。
 */
public final class EnergyBarComponent extends GuiComponent {
    private final GameLevel gameLevel;
    private final LevelRenderer levelRenderer;

    public EnergyBarComponent(@NonNull GameLevel gameLevel, @NonNull LevelRenderer levelRenderer) {
        super(GuiAnchor.CENTER, 0.0f, 0.0f, 0.0f, 0.0f);
        this.gameLevel = gameLevel;
        this.levelRenderer = levelRenderer;
        setEnabled(false);
    }

    @Override
    public void refreshLayout(@NonNull GuiScreenSpace screenSpace) {
    }

    @Override
    protected void renderComponent(@NonNull GuiRenderer renderer) {
        levelRenderer.renderBars(gameLevel);
    }
}