package io.github.theflysong.input;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

/**
 * 一次鼠标按钮输入的上下文。
 */
public record MouseInputContext(
        long windowHandle,
        double cursorX,
        double cursorY,
        float ndcX,
        float ndcY,
        int windowWidth,
        int windowHeight,
        int button,
        int action,
        int mods
) {
    public boolean isPress() {
        return action == GLFW_PRESS;
    }

    public boolean isRelease() {
        return action == GLFW_RELEASE;
    }

    public boolean isLeftButton() {
        return button == GLFW_MOUSE_BUTTON_LEFT;
    }

    public boolean isLeftPress() {
        return isLeftButton() && isPress();
    }
}
