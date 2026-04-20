package io.github.theflysong.client.render;

import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * 渲染相关信息载体
 * 用于向各类渲染器传递必要的上下文信息
 * 比如Projection Matrix等 
 *
 * @author theflysong
 * @date 2026年4月16日
 */
@SideOnly(Side.CLIENT)
public class RenderInfo {
    private Matrix4f projectionMatrix;
    private long frameIndex;
    private double renderTimeSeconds;

    public RenderInfo(Matrix4f projectionMatrix) {
        this.projectionMatrix = projectionMatrix;
        this.frameIndex = 0L;
        this.renderTimeSeconds = 0.0;
    }

    public void updateProjection(Matrix4f newProjection) {
        this.projectionMatrix = newProjection;
    }

    public @NonNull Matrix4f projectionMatrix() {
        assert projectionMatrix != null : "Projection matrix must not be null";
        return projectionMatrix;
    }

    public void beginFrame(long frameIndex, double renderTimeSeconds) {
        this.frameIndex = frameIndex;
        this.renderTimeSeconds = renderTimeSeconds;
    }

    public long frameIndex() {
        return frameIndex;
    }

    public double renderTimeSeconds() {
        return renderTimeSeconds;
    }
}
