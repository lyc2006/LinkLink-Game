package io.github.theflysong.client.render;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;

import io.github.theflysong.client.gl.mesh.GLVertexLayout;
import io.github.theflysong.client.gl.shader.Shader;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * 最小渲染器骨架（无流版本）。
 *
 * 目标：
 * 1. 提供 submit/flush 的基本提交流程。
 * 2. 保持实现简单，便于后续引入排序、批处理、状态缓存。
 *
 * 当前策略：
 * - 按提交顺序直接绘制。
 * - 每个 item 都会 bind shader 并上传 uniform。
 *
 * 后续优化方向（预留）：
 * - 按 shader/material 分桶减少状态切换。
 * - 引入透明物体排序与不同 RenderPass。
 */
@SideOnly(Side.CLIENT)
public class Renderer {
    /**
     * 按 Shader 分桶的提交队列。
     *
     * 选择 LinkedHashMap 的原因：
     * 1. 保留 Shader 首次出现顺序，便于维持可预测的绘制顺序。
     * 2. 同一个 Shader 下的 item 仍按提交顺序追加。
     */
    private final Map<Shader, List<RenderItem>> shaderBuckets = new LinkedHashMap<>();
    private final RenderInfo renderInfo = new RenderInfo(new Matrix4f().identity());
    private final long renderStartNano = System.nanoTime();
    private long frameCounter = 0L;

    /**
     * 提交一个渲染项到当前帧队列。
     */
    public void submit(RenderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("item must not be null");
        }
        shaderBuckets.computeIfAbsent(item.shader(), shader -> new ArrayList<>()).add(item);
    }

    /**
     * 执行本帧渲染队列。
     *
     * 说明：
     * - flush 结束后会清空队列。
     * - 如果希望跨帧保留队列，请改为双缓冲队列或手动控制 clear。
     */
    public void flush() {
        double elapsedSeconds = (System.nanoTime() - renderStartNano) / 1_000_000_000.0;
        renderInfo.beginFrame(frameCounter, elapsedSeconds);
        frameCounter++;

        for (Map.Entry<Shader, List<RenderItem>> bucket : shaderBuckets.entrySet()) {
            Shader shader = bucket.getKey();
            shader.bind();
            for (RenderItem item : bucket.getValue()) {
                RenderContext renderContext = new RenderContext(shader, item.modelMatrix(), item);
                renderContext.pushModelMatrixStack();
                try {
                    // 预处理调用时机：bind 与 uploadUniforms 之间。
                    if (item.preprocessor() != null) {
                        item.preprocessor().preprocess(renderInfo, renderContext);
                    }

                    shader.uploadUniforms();
                    GLVertexLayout meshLayout = item.mesh().vertexLayout();
                    if (meshLayout == null) {
                        throw new IllegalStateException("Mesh vertex layout is null. Ensure mesh.upload(...) is called before submit.");
                    }
                    if (!shader.vertexLayout().compatibleWith(meshLayout)) {
                        throw new IllegalStateException("Vertex layout mismatch. Shader expects " +
                                                        shader.vertexLayout() +
                                                        " but mesh provides " +
                                                        meshLayout);
                    }
                    item.mesh().draw();
                } finally {
                    renderContext.popModelMatrixStack();
                }
            }
        }
        shaderBuckets.clear();
    }

    /**
     * 更新投影矩阵，供预处理器上传 m4_projection。
     */
    public void updateProjection(Matrix4f projectionMatrix) {
        renderInfo.updateProjection(projectionMatrix);
    }

    /**
     * 主动清空队列。
     * 适用于切场景、切 pass 或异常中断后的状态重置。
     */
    public void clear() {
        shaderBuckets.clear();
    }
}
