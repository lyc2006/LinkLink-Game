package io.github.theflysong.client.gui;

import io.github.theflysong.client.render.preprocessor.IPreprocessor;
import io.github.theflysong.client.sprite.Sprite;
import org.jspecify.annotations.NonNull;

/**
 * Sprite 组件。
 */
public final class GuiSpriteComponent extends GuiComponent {
    private Sprite sprite;
    private IPreprocessor preprocessor;

    public GuiSpriteComponent(@NonNull Sprite sprite,
                              @NonNull IPreprocessor preprocessor,
                              @NonNull GuiAnchor anchor,
                              float offsetX,
                              float offsetY,
                              float width,
                              float height) {
        super(anchor, offsetX, offsetY, width, height);
        this.sprite = sprite;
        this.preprocessor = preprocessor;
    }

    @Override
    protected void renderComponent(@NonNull GuiRenderer renderer) {
        renderer.drawSprite(sprite, anchor(), preprocessor, offsetX(), offsetY(), width(), height());
    }

    public @NonNull Sprite sprite() {
        return sprite;
    }

    public void setSprite(@NonNull Sprite sprite) {
        this.sprite = sprite;
    }

    public @NonNull IPreprocessor preprocessor() {
        return preprocessor;
    }

    public void setPreprocessor(@NonNull IPreprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }
}
