package io.github.theflysong.client.gui;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

import io.github.theflysong.client.render.LevelRenderer;
import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceType;
import io.github.theflysong.input.GameMapInputHandler;
import io.github.theflysong.level.GameLevel;

/**
 * 关卡屏幕。
 */
public final class LevelScreen extends GuiScreen {
    private static final ResourceLocation PASS_TEXTURE = new ResourceLocation("linklink", ResourceType.TEXTURE,
            "gui/pass.png");
    private static final float PASS_TEXTURE_ASPECT = 829.0f / 771.0f;
    private static final float PASS_ANIMATION_SECONDS = 1.0f;

    private static final int ENERGY_COST_REFRESH = 10;
    private static final int ENERGY_COST_TIPS = 5;

    private final GameLevel gameLevel;
    private final LevelRenderer levelRenderer;
    private final GameMapInputHandler gameMapInputHandler;
    private GameMapComponent gameMapComponent;
    private EnergyBarComponent energyBarComponent;
    private GuiButtonComponent refreshButton;
    private GuiButtonComponent tipsButton;
    private long passOverlayStartNanos = -1L;

    private static final float ENDGAME_OVERLAY_Z = 0.990f;
    private static final float ENDGAME_MARK_Z = 0.995f;

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
        gameMapComponent = addComponent(
                new GameMapComponent(gameLevel, levelRenderer, gameMapInputHandler, screenSpace, 0, 0, 500, 500), 100
            );
        energyBarComponent = addComponent(new EnergyBarComponent(gameLevel, levelRenderer), 90);

        refreshButton = addComponent(new GuiButtonComponent(
                new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button_disabled.png"),
                new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button_ready.png"),
                new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button.png"),
                GuiAnchor.RIGHT,
                -100.0f,
                400.0f,
                50.0f,
                50.0f), 100);
        refreshButton.setOverlayTexture(new ResourceLocation(ResourceType.TEXTURE, "gui/overlay/shuffle.png"));
        refreshButton.setOnClick((component, context) -> {
            gameLevel.refreshMap(ENERGY_COST_REFRESH);
            return true;
        });

        tipsButton = addComponent(new GuiButtonComponent(
                new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button_disabled.png"),
                new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button_ready.png"),
                new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button.png"),
                GuiAnchor.RIGHT,
                -100.0f,
                320.0f,
                50.0f,
                50.0f), 100);
        tipsButton.setOverlayTexture(new ResourceLocation(ResourceType.TEXTURE, "gui/overlay/tips.png"));
        tipsButton.setOnClick((component, context) -> {
            gameLevel.updateTips(ENERGY_COST_TIPS);
            return true;
        });

        addComponent(new GuiComponent(GuiAnchor.CENTER, 0.0f, 0.0f, 0.0f, 0.0f) {
            @Override
            public void refreshLayout(@NonNull GuiScreenSpace screenSpace) {
                setSize(screenSpace.width(), screenSpace.height());
            }

            @Override
            protected void renderComponent(GuiRenderer renderer, Matrix4f modelMatrix) {
                if (!gameLevel.isGameOver()) {
                    passOverlayStartNanos = -1L;
                    return;
                }

                GuiScreenSpace screenSpace = renderer.currentScreenSpace();

                renderer.drawRectangle(
                        GuiAnchor.CENTER,
                        0.0f,
                        0.0f,
                        screenSpace.width(),
                        screenSpace.height(),
                        ENDGAME_OVERLAY_Z,
                        new Vector4f(0.0f, 0.0f, 0.0f, 0.35f));
                renderer.renderer().flush();

                if (passOverlayStartNanos < 0L) {
                    passOverlayStartNanos = System.nanoTime();
                }

                float elapsedSeconds = (System.nanoTime() - passOverlayStartNanos) / 1_000_000_000.0f;
                float progress = Math.min(1.0f, elapsedSeconds / PASS_ANIMATION_SECONDS);
                float maxHeight = Math.min(screenSpace.height() * 0.85f,
                        screenSpace.width() * 0.85f / PASS_TEXTURE_ASPECT);
                float passHeight = maxHeight * progress;
                float passWidth = passHeight * PASS_TEXTURE_ASPECT;

                Matrix4f passModel = renderer.componentChildMatrix(
                        modelMatrix,
                        width(),
                        height(),
                        0.0f,
                        -100.0f,
                        passWidth,
                        passHeight,
                        ENDGAME_MARK_Z);
                renderer.drawTexture(PASS_TEXTURE, passModel);
            }
        }, 1);

        addComponent(new GuiComponent(GuiAnchor.RIGHT, -100.0f, -450.0f, 260.0f, 80.0f) {
            @Override
            protected void renderComponent(GuiRenderer renderer, Matrix4f modelMatrix) {
                long elapsed = gameLevel.elapsedSeconds();
                String timeStr = String.format("%02d:%02d", elapsed / 60, elapsed % 60);
                String scoreStr = "得分: " + gameLevel.score();

                Matrix4f scoreModel = renderer.componentChildMatrix(modelMatrix, width(), height(),
                        0.0f, -height() * 0.22f, width(), height() * 0.38f, 0.0f);
                renderer.drawText(scoreStr, null, scoreModel, width(), height() * 0.38f, 0.0f,
                        TextStyle.normal().withColor(1.0f, 0.85f, 0.2f, 1.0f));

                Matrix4f timeModel = renderer.componentChildMatrix(modelMatrix, width(), height(),
                        0.0f, height() * 0.22f, width(), height() * 0.38f, 0.0f);
                renderer.drawText(timeStr, null, timeModel, width(), height() * 0.38f, 0.0f,
                        TextStyle.normal().withColor(1.0f, 1.0f, 1.0f, 1.0f));
            }
        }, 200);
    }

    @Override
    protected void renderScreen(GuiRenderer renderer) {
    }
}
