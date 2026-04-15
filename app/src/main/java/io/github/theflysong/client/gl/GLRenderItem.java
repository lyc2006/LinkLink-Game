package io.github.theflysong.client.gl;

import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * 渲染提交单元。
 *
 * 这是渲染器最小工作单元：
 * - mesh: 几何体（VAO/VBO/EBO）
 * - shader: 着色器程序
 *
 * 后续可扩展字段：
 * - 材质参数
 * - 纹理集合
 * - 模型矩阵
 * - 排序键（前后向、材质分桶等）
 */
@SideOnly(Side.CLIENT)
public record GLRenderItem(
    GLGpuMesh mesh,
    Shader shader
) {
    public GLRenderItem {
        if (mesh == null) {
            throw new IllegalArgumentException("mesh must not be null");
        }
        if (shader == null) {
            throw new IllegalArgumentException("shader must not be null");
        }
    }
}
