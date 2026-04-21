package io.github.theflysong.client.window;

/**
 * 
 *
 * @author theflysong
 * @date 2026年4月21日
 */
@FunctionalInterface
public interface MouseButtonCallback {
    void onMouseButton(long window,
            double cursorX,
            double cursorY,
            int windowWidth,
            int windowHeight,
            int button,
            int action,
            int mods);
}