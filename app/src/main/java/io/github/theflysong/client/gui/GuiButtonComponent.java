package io.github.theflysong.client.gui;

import io.github.theflysong.client.audio.AudioManager;
import io.github.theflysong.client.window.CursorPosition;
import io.github.theflysong.client.window.Window;
import io.github.theflysong.client.window.WindowSize;
import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.input.MouseInputContext;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 按钮组件：支持 normal/ready/disabled 三态贴图。
 *
 * 状态规则：
 * 1. 当按钮禁用时，始终渲染 disabled。
 * 2. 当鼠标悬浮在按钮上方时，渲染 ready。
 * 3. 点击按钮后会在短时间内保持 ready（默认 140ms）。
 */
public final class GuiButtonComponent extends GuiComponent {
    private static final long DEFAULT_READY_HOLD_NANOS = 140_000_000L;
    private static final float Z_LAYER_STEP = 0.0001f;

    private enum VisualState {
        NORMAL,
        READY,
        DISABLED
    }

    private ResourceLocation disabledTexture;
    private ResourceLocation readyTexture;
    private ResourceLocation normalTexture;
    private Supplier<@Nullable ResourceLocation> overlayTextureSupplier = () -> null;
    private @Nullable OverlayRenderer overlayRenderer;
    private float baseZ = GuiRenderer.DEFAULT_GUI_Z;
    private long readyHoldNanos = DEFAULT_READY_HOLD_NANOS;
    private long readyUntilNanos;

    private @Nullable GuiClickCallback userOnClick;

    public GuiButtonComponent(@Nullable ResourceLocation disabledTexture,
                              @NonNull ResourceLocation readyTexture,
                              @NonNull ResourceLocation normalTexture,
                              @NonNull GuiAnchor anchor,
                              float offsetX,
                              float offsetY,
                              float width,
                              float height) {
        super(anchor, offsetX, offsetY, width, height);
        this.disabledTexture = disabledTexture;
        this.readyTexture = readyTexture;
        this.normalTexture = normalTexture;
        super.setOnClick(this::handleInternalClick);
    }

    @Override
    protected void renderComponent(@NonNull GuiRenderer renderer, @NonNull Matrix4f modelMatrix) {
        VisualState state = resolveVisualState(renderer);
        ResourceLocation texture = resolveTexture(state);
        renderer.drawTexture(texture, renderer.withLocalZ(modelMatrix, baseZ));

        ResourceLocation overlayTexture = overlayTextureSupplier.get();
        if (overlayTexture != null) {
            renderer.drawTexture(overlayTexture, renderer.withLocalZ(modelMatrix, overlayTextureZ()));
        }
        if (overlayRenderer != null) {
            overlayRenderer.render(renderer, this, modelMatrix, overlayRendererZ());
        }
    }

    private ResourceLocation disabledTexture() {
        return disabledTexture == null ? normalTexture : disabledTexture;
    }

    private VisualState resolveVisualState(@NonNull GuiRenderer renderer) {
        if (!enabled()) {
            return VisualState.DISABLED;
        }
        boolean holdingReady = System.nanoTime() <= readyUntilNanos;
        if (holdingReady || isHovered(renderer)) {
            return VisualState.READY;
        }
        return VisualState.NORMAL;
    }

    private ResourceLocation resolveTexture(@NonNull VisualState state) {
        return switch (state) {
            case DISABLED -> disabledTexture();
            case READY -> readyTexture;
            case NORMAL -> normalTexture;
        };
    }

    private boolean isHovered(@NonNull GuiRenderer renderer) {
        long windowHandle = Window.currentHandle();
        if (windowHandle == 0L) {
            return false;
        }

        WindowSize size = Window.windowSize(windowHandle);
        if (size.width() <= 0 || size.height() <= 0) {
            return false;
        }

        CursorPosition cursor = Window.cursorPosition(windowHandle);
        GuiScreenSpace screenSpace = renderer.currentScreenSpace();
        Vector2f guiPos = screenSpace.toGuiPosition(cursor.x(), cursor.y(), size.width(), size.height());
        return hitTest(screenSpace, guiPos.x, guiPos.y);
    }

    private boolean handleInternalClick(@NonNull GuiComponent component, @NonNull MouseInputContext context) {
        readyUntilNanos = System.nanoTime() + readyHoldNanos;
        AudioManager.playSfx(AudioManager.BUTTON_UP);
        if (userOnClick == null) {
            return true;
        }
        return userOnClick.onClick(component, context);
    }

    @Override
    public void setOnClick(@Nullable GuiClickCallback onClick) {
        this.userOnClick = onClick;
    }

    public void setDisabledTexture(@NonNull ResourceLocation disabledTexture) {
        this.disabledTexture = disabledTexture;
    }

    public @NonNull ResourceLocation readyTexture() {
        return readyTexture;
    }

    public void setReadyTexture(@NonNull ResourceLocation readyTexture) {
        this.readyTexture = readyTexture;
    }

    public @NonNull ResourceLocation normalTexture() {
        return normalTexture;
    }

    public void setNormalTexture(@NonNull ResourceLocation normalTexture) {
        this.normalTexture = normalTexture;
    }

    public @NonNull Supplier<@Nullable ResourceLocation> overlayTextureSupplier() {
        return overlayTextureSupplier;
    }

    public void setOverlayTextureSupplier(@Nullable Supplier<@Nullable ResourceLocation> overlayTextureSupplier) {
        this.overlayTextureSupplier = overlayTextureSupplier == null ? () -> null : overlayTextureSupplier;
    }

    public void setOverlayTexture(@Nullable ResourceLocation overlayTexture) {
        this.overlayTextureSupplier = () -> overlayTexture;
    }

    public float baseZ() {
        return baseZ;
    }

    public void setBaseZ(float baseZ) {
        this.baseZ = baseZ;
    }

    public @Nullable OverlayRenderer overlayRenderer() {
        return overlayRenderer;
    }

    public void setOverlayRenderer(@Nullable OverlayRenderer overlayRenderer) {
        this.overlayRenderer = overlayRenderer;
    }

    public long readyHoldNanos() {
        return readyHoldNanos;
    }

    public void setReadyHoldMillis(long millis) {
        setReadyHoldNanos(millis * 1_000_000L);
    }

    public void setReadyHoldNanos(long readyHoldNanos) {
        this.readyHoldNanos = Math.max(0L, readyHoldNanos);
    }

    public void setDisabled(boolean disabled) {
        setEnabled(!disabled);
    }

    public boolean disabled() {
        return !enabled();
    }

    private float overlayTextureZ() {
        return baseZ + Z_LAYER_STEP;
    }

    private float overlayRendererZ() {
        return baseZ + Z_LAYER_STEP * 2.0f;
    }

    @FunctionalInterface
    public interface OverlayRenderer {
        void render(@NonNull GuiRenderer renderer,
                @NonNull GuiButtonComponent button,
                @NonNull Matrix4f modelMatrix,
                float localZ);
    }
}
