package io.github.theflysong.client.window;

@FunctionalInterface
public interface WindowSizeCallback {
    void onWindowSize(long window, int width, int height);
}
