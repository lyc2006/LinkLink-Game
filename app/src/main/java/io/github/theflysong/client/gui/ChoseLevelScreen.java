package io.github.theflysong.client.gui;

import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceType;
import org.joml.Vector4f;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ChoseLevelScreen extends GuiScreen {

    private record LevelDef(String id, String displayName) {}

    private static final List<LevelDef> LEVELS = List.of(
            new LevelDef("simple", "简单"),
            new LevelDef("hard", "困难"),
            new LevelDef("bridge", "桥梁"),
            new LevelDef("cell", "细胞"),
            new LevelDef("island", "岛屿")
    );

    private static final ResourceLocation BUTTON_DISABLED =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button_disabled.png");
    private static final ResourceLocation BUTTON_READY =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button_ready.png");
    private static final ResourceLocation BUTTON_NORMAL =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button.png");
    private static final ResourceLocation ARROW_LEFT =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/overlay/arrow_green_left.png");
    private static final ResourceLocation ARROW_RIGHT =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/overlay/arrow_green_right.png");

    private static final float PREVIEW_WIDTH = 600.0f;
    private static final float PREVIEW_HEIGHT = 420.0f;
    private static final float LEVEL_BUTTON_WIDTH = 300.0f;
    private static final float LEVEL_BUTTON_HEIGHT = 64.0f;
    private static final float ARROW_SIZE = 48.0f;
    private static final float BACK_BUTTON_WIDTH = 140.0f;
    private static final float BACK_BUTTON_HEIGHT = 50.0f;

    private int currentPage;
    private final Consumer<String> onSelectLevel;
    private final Runnable onBack;
    private final Predicate<String> isLevelCompleted;

    private GuiTextureComponent previewImage;
    private GuiButtonComponent levelButton;
    private GuiTextComponent completedHint;
    private GuiButtonComponent leftArrow;
    private GuiButtonComponent rightArrow;

    public ChoseLevelScreen(Consumer<String> onSelectLevel,
                            Runnable onBack,
                            Predicate<String> isLevelCompleted) {
        this.onSelectLevel = onSelectLevel;
        this.onBack = onBack;
        this.isLevelCompleted = isLevelCompleted;
        this.currentPage = 0;
    }

    @Override
    protected void onInit(GuiRenderer renderer) {
        currentPage = 0;

        addComponent(createBackButton());

        previewImage = addComponent(new GuiTextureComponent(
                levelPreviewTexture(LEVELS.get(currentPage).id),
                GuiAnchor.CENTER,
                0.0f,
                60.0f,
                PREVIEW_WIDTH,
                PREVIEW_HEIGHT));

        levelButton = addComponent(createLevelButton(LEVELS.get(currentPage).displayName));

        completedHint = addComponent(new GuiTextComponent(
                "此关卡已通关",
                null,
                GuiAnchor.CENTER,
                0.0f,
                -320.0f,
                TextStyle.normal().withColor(new Vector4f(0.3f, 0.9f, 0.3f, 1.0f)).withBold(true)), 90);
        completedHint.setVisible(isLevelCompleted.test(LEVELS.get(currentPage).id));

        rightArrow = addComponent(createArrowButton(ARROW_RIGHT, -50.0f, this::nextPage));
        leftArrow = addComponent(createArrowButton(ARROW_LEFT, -110.0f, this::prevPage));

        updateArrowStates();
    }

    @Override
    protected void renderScreen(GuiRenderer renderer) {
    }

    private GuiButtonComponent createBackButton() {
        GuiButtonComponent button = new GuiButtonComponent(
                BUTTON_DISABLED,
                BUTTON_READY,
                BUTTON_NORMAL,
                GuiAnchor.LEFT,
                100.0f,
                460.0f,
                BACK_BUTTON_WIDTH,
                BACK_BUTTON_HEIGHT);
        button.setOnClick((component, context) -> {
            onBack.run();
            return true;
        });
        button.setOverlayRenderer((renderer, btn, modelMatrix, localZ) ->
                renderer.drawText("返回", null, modelMatrix, btn.width(), btn.height(), localZ + 0.001f,
                        TextStyle.normal().withBold(true).withColor(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f))));
        return button;
    }

    private GuiButtonComponent createLevelButton(String label) {
        GuiButtonComponent button = new GuiButtonComponent(
                BUTTON_DISABLED,
                BUTTON_READY,
                BUTTON_NORMAL,
                GuiAnchor.CENTER,
                0.0f,
                -220.0f,
                LEVEL_BUTTON_WIDTH,
                LEVEL_BUTTON_HEIGHT);
        button.setOnClick((component, context) -> {
            String levelId = LEVELS.get(currentPage).id;
            onSelectLevel.accept(levelId);
            onBack.run();
            return true;
        });
        button.setOverlayRenderer((renderer, btn, modelMatrix, localZ) ->
                renderer.drawText(label, null, modelMatrix, btn.width(), btn.height(), localZ + 0.001f,
                        TextStyle.normal().withBold(true).withColor(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f))));
        return button;
    }

    private GuiButtonComponent createArrowButton(ResourceLocation arrowTexture, float offsetX, Runnable action) {
        GuiButtonComponent button = new GuiButtonComponent(
                BUTTON_DISABLED,
                BUTTON_READY,
                BUTTON_NORMAL,
                GuiAnchor.RIGHT,
                offsetX,
                -420.0f,
                ARROW_SIZE,
                ARROW_SIZE);
        button.setOverlayTexture(arrowTexture);
        button.setOnClick((component, context) -> {
            action.run();
            return true;
        });
        return button;
    }

    private void nextPage() {
        if (currentPage < LEVELS.size() - 1) {
            currentPage++;
            updatePage();
        }
    }

    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePage();
        }
    }

    private void updatePage() {
        LevelDef level = LEVELS.get(currentPage);
        previewImage.setTexture(levelPreviewTexture(level.id));

        levelButton.setOverlayRenderer((renderer, btn, modelMatrix, localZ) ->
                renderer.drawText(level.displayName, null, modelMatrix, btn.width(), btn.height(), localZ + 0.001f,
                        TextStyle.normal().withBold(true).withColor(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f))));

        completedHint.setVisible(isLevelCompleted.test(level.id));
        updateArrowStates();
    }

    private void updateArrowStates() {
        leftArrow.setDisabled(currentPage <= 0);
        rightArrow.setDisabled(currentPage >= LEVELS.size() - 1);
    }

    private static ResourceLocation levelPreviewTexture(String levelId) {
        return new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/level/" + levelId + ".png");
    }
}
