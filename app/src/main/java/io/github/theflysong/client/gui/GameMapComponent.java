package io.github.theflysong.client.gui;

import static io.github.theflysong.App.LOGGER;

import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;

import io.github.theflysong.client.render.LevelRenderer;
import io.github.theflysong.client.render.MapRenderer;
import io.github.theflysong.input.GameMapInputHandler;
import io.github.theflysong.input.MouseInputContext;
import io.github.theflysong.level.GameLevel;

/**
 * 地图组件：负责地图点击命中与输入归一化。
 */
public final class GameMapComponent extends GuiComponent {
    private static final boolean SHOW_HIT_RANGE = true;

    private final GameLevel gameLevel;
    private final LevelRenderer levelRenderer;
    private final GameMapInputHandler gameMapInputHandler;
    private GuiScreenSpace currentScreenSpace;

    public GameMapComponent(@NonNull GameLevel gameLevel,
            @NonNull LevelRenderer levelRenderer,
            @NonNull GameMapInputHandler gameMapInputHandler,
            @NonNull GuiScreenSpace screenSpace) {
        super(GuiAnchor.CENTER, 0.0f, 0.0f, 0.0f, 0.0f);
        this.gameLevel = gameLevel;
        this.levelRenderer = levelRenderer;
        this.gameMapInputHandler = gameMapInputHandler;
        setOnClick(this::handleClick);
        refreshLayout(screenSpace);
    }

    @Override
    protected void renderComponent(@NonNull GuiRenderer renderer) {
        Vector2i selectedCell = gameMapInputHandler.currentSelection().orElse(null);
        GameMapInputHandler.MatchPathEffect matchPath = gameMapInputHandler.currentMatchPath().orElse(null);
        levelRenderer.renderMap(
                gameLevel,
                new Matrix4f().identity(),
                selectedCell,
                matchPath == null ? null : matchPath.points(),
                matchPath == null ? 1.0f : matchPath.alpha(),
                SHOW_HIT_RANGE);
    }

    private boolean handleClick(@NonNull GuiComponent component, @NonNull MouseInputContext context) {
        GuiScreenSpace screenSpace = currentScreenSpace != null
                ? currentScreenSpace
                : GuiScreenSpace.fromViewportSize(context.windowWidth(), context.windowHeight());
        Vector2f guiPosition = screenSpace.toGuiPosition(
                context.cursorX(),
                context.cursorY(),
                context.windowWidth(),
                context.windowHeight());
        Vector2f topLeft = screenSpace.resolveTopLeft(anchor(), offsetX(), offsetY(), width(), height());
        if (width() <= 0.0f || height() <= 0.0f) {
            return false;
        }

        float normalizedX = (guiPosition.x - topLeft.x) / width();
        float normalizedY = (guiPosition.y - topLeft.y) / height();
        Vector2f nc = new Vector2f(normalizedX, normalizedY);
        LOGGER.info("Map click at GUI position ({}, {}), normalized to ({}, {})",
                guiPosition.x, guiPosition.y, normalizedX, normalizedY);
        return gameMapInputHandler.handleClick(nc, context);
    }

    @Override
    public void refreshLayout(@NonNull GuiScreenSpace screenSpace) {
        currentScreenSpace = screenSpace;
        MapRenderer.MapBounds bounds = levelRenderer.mapRenderer().mapCanvasBounds(gameLevel.gameMap());
        if (bounds.width() <= 0.0f || bounds.height() <= 0.0f) {
            setSize(0.0f, 0.0f);
            return;
        }

        float aspect = screenSpace.width() / screenSpace.height();
        float leftPx = mapSpaceXToGui(bounds.left(), screenSpace.width(), aspect);
        float rightPx = mapSpaceXToGui(bounds.right(), screenSpace.width(), aspect);
        float topPx = mapSpaceYToGui(bounds.top(), screenSpace.height());
        float bottomPx = mapSpaceYToGui(bounds.bottom(), screenSpace.height());

        float centerX = (leftPx + rightPx) * 0.5f;
        float centerY = (topPx + bottomPx) * 0.5f;

        setAnchor(GuiAnchor.CENTER);
        setOffsetX(centerX - screenSpace.width() * 0.5f);
        setOffsetY(centerY - screenSpace.height() * 0.5f);
        setSize(rightPx - leftPx, bottomPx - topPx);
    }

    private static float mapSpaceXToGui(float mapSpaceX, float screenWidth, float aspect) {
        float ndcX = mapSpaceX / aspect;
        return (ndcX * 0.5f + 0.5f) * screenWidth;
    }

    private static float mapSpaceYToGui(float mapSpaceY, float screenHeight) {
        return (1.0f - mapSpaceY) * 0.5f * screenHeight;
    }
}