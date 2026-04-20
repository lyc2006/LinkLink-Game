package io.github.theflysong.client.gui;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL11C.GL_VIEWPORT;
import static org.lwjgl.opengl.GL11C.glGetIntegerv;

/**
 * GUI 虚拟屏幕空间。
 *
 * 规则：
 * 1. 默认屏幕空间高度固定为 1024。
 * 2. 默认宽高比 3:2，对应默认宽度 1536。
 * 3. 窗口变化时仅扩展/压缩可见宽度，不改变组件给定尺寸。
 */
public final class GuiScreenSpace {
    public static final float DEFAULT_WIDTH = 1536.0f;
    public static final float DEFAULT_HEIGHT = 1024.0f;
    public static final float DEFAULT_ASPECT = DEFAULT_WIDTH / DEFAULT_HEIGHT;

    private final float width;
    private final float height;

    private GuiScreenSpace(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public static GuiScreenSpace fromViewportSize(int viewportWidth, int viewportHeight) {
        int safeWidth = Math.max(1, viewportWidth);
        int safeHeight = Math.max(1, viewportHeight);
        float currentAspect = (float) safeWidth / (float) safeHeight;
        float width = DEFAULT_HEIGHT * currentAspect;
        return new GuiScreenSpace(width, DEFAULT_HEIGHT);
    }

    public static GuiScreenSpace fromCurrentViewport() {
        int viewportWidth;
        int viewportHeight;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var viewport = stack.mallocInt(4);
            glGetIntegerv(GL_VIEWPORT, viewport);
            viewportWidth = Math.max(1, viewport.get(2));
            viewportHeight = Math.max(1, viewport.get(3));
        }
        return fromViewportSize(viewportWidth, viewportHeight);
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public Matrix4f projectionMatrix() {
        return new Matrix4f().ortho(0.0f, width, height, 0.0f, -1.0f, 1.0f);
    }

    public Vector2f toGuiPosition(double cursorX, double cursorY, int viewportWidth, int viewportHeight) {
        float safeWidth = Math.max(1, viewportWidth);
        float safeHeight = Math.max(1, viewportHeight);
        float guiX = (float) (cursorX / safeWidth) * width;
        float guiY = (float) (cursorY / safeHeight) * height;
        return new Vector2f(guiX, guiY);
    }

    /**
     * 输入为“相对锚点的中心偏移 + 组件尺寸”，返回组件左上角。
     */
    public Vector2f resolveTopLeft(GuiAnchor anchor, float offsetX, float offsetY, float elementWidth, float elementHeight) {
        float anchorX;
        float anchorY;
        switch (anchor) {
            case CENTER -> {
                anchorX = width * 0.5f;
                anchorY = height * 0.5f;
            }
            case LEFT -> {
                anchorX = 0.0f;
                anchorY = height * 0.5f;
            }
            case RIGHT -> {
                anchorX = width;
                anchorY = height * 0.5f;
            }
            case TOP -> {
                anchorX = width * 0.5f;
                anchorY = 0.0f;
            }
            case BOTTOM -> {
                anchorX = width * 0.5f;
                anchorY = height;
            }
            default -> throw new IllegalStateException("Unhandled anchor mode: " + anchor);
        }

        float centerX = anchorX + offsetX;
        float centerY = anchorY + offsetY;
        return new Vector2f(centerX - elementWidth * 0.5f, centerY - elementHeight * 0.5f);
    }
}
