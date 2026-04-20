package io.github.theflysong.client.gui;

import io.github.theflysong.data.ResourceLocation;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

/**
 * 贴图组件。
 */
public final class GuiTextureComponent extends GuiComponent {
    private static final Vector4f WHITE = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

    private ResourceLocation texture;
    private Vector4f tintColor = new Vector4f(WHITE);

    public GuiTextureComponent(@NonNull ResourceLocation texture,
                               @NonNull GuiAnchor anchor,
                               float offsetX,
                               float offsetY,
                               float width,
                               float height) {
        super(anchor, offsetX, offsetY, width, height);
        this.texture = texture;
    }

    @Override
    protected void renderComponent(@NonNull GuiRenderer renderer) {
        renderer.drawTexture(texture, anchor(), offsetX(), offsetY(), width(), height(), tintColor);
    }

    public @NonNull ResourceLocation texture() {
        return texture;
    }

    public void setTexture(@NonNull ResourceLocation texture) {
        this.texture = texture;
    }

    public @NonNull Vector4f tintColor() {
        return new Vector4f(tintColor);
    }

    public void setTintColor(@NonNull Vector4f tintColor) {
        this.tintColor = new Vector4f(tintColor);
    }

    public void setTintColor(float r, float g, float b, float a) {
        this.tintColor = new Vector4f(r, g, b, a);
    }
}
