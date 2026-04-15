package io.github.theflysong.client.gui;

/**
 * GUI 屏幕基类。
 *
 * 使用方式：
 * 1. 继承此类。
 * 2. 在 renderScreen(...) 中调用 GuiRenderer 的工具函数绘制组件。
 */
public abstract class GuiScreen implements AutoCloseable {
    private boolean initialized;

    public final void render(GuiRenderer renderer) {
        if (renderer == null) {
            throw new IllegalArgumentException("renderer must not be null");
        }
        if (!initialized) {
            onInit(renderer);
            initialized = true;
        }
        renderScreen(renderer);
    }

    /**
     * 首次渲染前调用一次。
     */
    protected void onInit(GuiRenderer renderer) {
    }

    /**
     * 每帧渲染入口：在这里调用 renderer 的工具函数绘制组件。
     */
    protected abstract void renderScreen(GuiRenderer renderer);

    @Override
    public void close() {
    }
}
