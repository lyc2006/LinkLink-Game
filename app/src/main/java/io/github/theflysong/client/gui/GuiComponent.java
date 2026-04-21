package io.github.theflysong.client.gui;

import io.github.theflysong.input.MouseInputContext;
import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * GUI 组件基类：负责布局参数、命中检测和点击回调。
 */
public abstract class GuiComponent {
    private GuiAnchor anchor;
    private float offsetX;
    private float offsetY;
    private float width;
    private float height;
    private boolean visible = true;
    private boolean enabled = true;
    private @Nullable GuiClickCallback onClick;

    protected GuiComponent(@NonNull GuiAnchor anchor,
                           float offsetX,
                           float offsetY,
                           float width,
                           float height) {
        this.anchor = anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = Math.max(0.0f, width);
        this.height = Math.max(0.0f, height);
    }

    public final void render(@NonNull GuiRenderer renderer) {
        if (!visible) {
            return;
        }
        renderComponent(renderer);
    }

    public final boolean hitTest(@NonNull GuiScreenSpace screenSpace, float guiX, float guiY) {
        if (!visible || !enabled || width <= 0.0f || height <= 0.0f) {
            return false;
        }
        Vector2f topLeft = screenSpace.resolveTopLeft(anchor, offsetX, offsetY, width, height);
        return guiX >= topLeft.x
                && guiX <= topLeft.x + width
                && guiY >= topLeft.y
                && guiY <= topLeft.y + height;
    }

    public final boolean handleClick(@NonNull MouseInputContext context) {
        if (!enabled || onClick == null) {
            return false;
        }
        return onClick.onClick(this, context);
    }

    public void refreshLayout(@NonNull GuiScreenSpace screenSpace) {
    }

    protected abstract void renderComponent(@NonNull GuiRenderer renderer);

    public @NonNull GuiAnchor anchor() {
        return anchor;
    }

    public void setAnchor(@NonNull GuiAnchor anchor) {
        this.anchor = anchor;
    }

    public float offsetX() {
        return offsetX;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public float offsetY() {
        return offsetY;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public void setSize(float width, float height) {
        this.width = Math.max(0.0f, width);
        this.height = Math.max(0.0f, height);
    }

    public boolean visible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public @Nullable GuiClickCallback onClick() {
        return onClick;
    }

    public void setOnClick(@Nullable GuiClickCallback onClick) {
        this.onClick = onClick;
    }

    @FunctionalInterface
    public interface GuiClickCallback {
        boolean onClick(@NonNull GuiComponent component, @NonNull MouseInputContext context);
    }
}
