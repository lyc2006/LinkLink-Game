package io.github.theflysong.client.gui;

import io.github.theflysong.input.MouseInputContext;
import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GUI 屏幕基类。
 *
 * 使用方式：
 * 1. 继承此类。
 * 2. 在 renderScreen(...) 中调用 GuiRenderer 的工具函数绘制组件。
 */
public abstract class GuiScreen implements AutoCloseable {
    private boolean initialized;
    private final List<GuiComponent> components = new ArrayList<>();
    private @Nullable GuiScreenSpace currentScreenSpace;

    public final void render(GuiRenderer renderer) {
        if (renderer == null) {
            throw new IllegalArgumentException("renderer must not be null");
        }
        if (!initialized) {
            onInit(renderer);
            initialized = true;
        }
        if (currentScreenSpace == null) {
            refreshLayout(GuiScreenSpace.fromCurrentViewport());
        }
        renderScreen(renderer);
        renderComponents(renderer);
    }

    public boolean handleMouseClick(@NonNull MouseInputContext context) {
        GuiScreenSpace screenSpace = currentScreenSpace != null
                ? currentScreenSpace
                : GuiScreenSpace.fromViewportSize(context.windowWidth(), context.windowHeight());
        Vector2f guiPosition = screenSpace.toGuiPosition(
                context.cursorX(),
                context.cursorY(),
                context.windowWidth(),
                context.windowHeight());

        for (int i = components.size() - 1; i >= 0; i--) {
            GuiComponent component = components.get(i);
            if (!component.hitTest(screenSpace, guiPosition.x, guiPosition.y)) {
                continue;
            }
            if (component.handleClick(context)) {
                return true;
            }
        }
        return false;
    }

    public final void refreshLayout(@NonNull GuiScreenSpace screenSpace) {
        currentScreenSpace = screenSpace;
        onRefreshLayout(screenSpace);
        for (GuiComponent component : components) {
            component.refreshLayout(screenSpace);
        }
    }

    /**
     * 首次渲染前调用一次。
     */
    protected void onInit(GuiRenderer renderer) {
    }

    protected void onRefreshLayout(@NonNull GuiScreenSpace screenSpace) {
    }

    /**
     * 每帧渲染入口：在这里调用 renderer 的工具函数绘制组件。
     */
    protected abstract void renderScreen(GuiRenderer renderer);

    protected final <T extends GuiComponent> T addComponent(@NonNull T component) {
        components.add(component);
        return component;
    }

    protected final void clearComponents() {
        components.clear();
    }

    protected final List<GuiComponent> components() {
        return Collections.unmodifiableList(components);
    }

    private void renderComponents(GuiRenderer renderer) {
        for (GuiComponent component : components) {
            component.render(renderer);
        }
    }

    @Override
    public void close() {
        clearComponents();
    }
}
