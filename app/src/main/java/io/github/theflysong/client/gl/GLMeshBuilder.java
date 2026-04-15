package io.github.theflysong.client.gl;

import java.nio.ByteBuffer;

import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;

/**
 * 网格构建器骨架。
 *
 * 职责：
 * 1. 接收逻辑层顶点数据（position/normal/uv 等）。
 * 2. 按 GLVertexLayout 进行“无流、单 VBO、交错存储”打包。
 * 3. 产出可直接上传的 GLMeshData。
 *
 * 当前文件为骨架实现：
 * - 保留了核心方法签名与参数说明。
 * - 具体打包细节（类型转换、对齐、归一化）由后续业务填充。
 */
@SideOnly(Side.CLIENT)
public final class GLMeshBuilder {
    private GLMeshBuilder() {
    }

    /**
     * 从已经打包好的顶点字节与索引字节直接创建 GLMeshData。
     *
     * 使用场景：
     * - 你已经在外部模块完成了 interleaved 打包。
     * - 只需要统一封装为渲染层可识别的数据对象。
     */
    public static GLMeshData fromPacked(GLVertexLayout layout,
                                        ByteBuffer vertexBytes,
                                        int vertexCount,
                                        ByteBuffer indexBytes,
                                        int indexCount,
                                        int indexType) {
        return new GLMeshData(layout, vertexBytes, indexBytes, vertexCount, indexCount, indexType);
    }

    /**
     * 常用便捷重载：默认索引类型为 GL_UNSIGNED_INT。
     */
    public static GLMeshData fromPacked(GLVertexLayout layout,
                                        ByteBuffer vertexBytes,
                                        int vertexCount,
                                        ByteBuffer indexBytes,
                                        int indexCount) {
        return fromPacked(layout, vertexBytes, vertexCount, indexBytes, indexCount, GL_UNSIGNED_INT);
    }

    /**
     * 仅顶点、无索引的便捷重载。
     */
    public static GLMeshData fromPacked(GLVertexLayout layout,
                                        ByteBuffer vertexBytes,
                                        int vertexCount) {
        return fromPacked(layout, vertexBytes, vertexCount, null, 0, GL_UNSIGNED_INT);
    }

    /**
     * 预留：从“分离的逻辑数组”打包为交错顶点字节。
     *
     * 建议实现步骤：
     * 1. 依据 layout.stride() 分配 vertexCount * stride 字节。
     * 2. 遍历顶点 i，并按 attribute.offset 写入对应字段。
     * 3. 对 normalized 整型字段做压缩编码（如颜色 float->UNORM8）。
     *
     * 这里先抛出异常，明确提醒调用方该路径尚未落地。
     */
    public static GLMeshData buildInterleavedPlaceholder() {
        throw new UnsupportedOperationException("TODO: implement interleaved packing logic");
    }
}
