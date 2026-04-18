package io.github.theflysong.event;

import io.github.theflysong.input.MouseInputContext;
import io.github.theflysong.util.event.Event;

import java.util.Objects;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

/**
 * 鼠标点击事件，支持取消后阻断输入分发。
 */
public final class MouseClickEvent extends Event {
    private final MouseInputContext context;

    public MouseClickEvent(MouseInputContext context) {
        this.context = Objects.requireNonNull(context, "context must not be null");
    }

    public MouseInputContext context() {
        return context;
    }

    public long windowHandle() {
        return context.windowHandle();
    }

    public double cursorX() {
        return context.cursorX();
    }

    public double cursorY() {
        return context.cursorY();
    }

    public float ndcX() {
        return context.ndcX();
    }

    public float ndcY() {
        return context.ndcY();
    }

    public int windowWidth() {
        return context.windowWidth();
    }

    public int windowHeight() {
        return context.windowHeight();
    }

    public int button() {
        return context.button();
    }

    public int action() {
        return context.action();
    }

    public int mods() {
        return context.mods();
    }

    public boolean isPress() {
        return action() == GLFW_PRESS;
    }

    public boolean isRelease() {
        return action() == GLFW_RELEASE;
    }
}
