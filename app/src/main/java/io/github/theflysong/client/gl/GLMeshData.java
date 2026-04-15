package io.github.theflysong.client.gl;

import java.nio.ByteBuffer;

import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * CPU 侧网格数据容器。
 *
 * 该类只负责“数据承载”，不涉及 OpenGL 上传。
 * 典型生命周期：
 * 1. 由构建器打包出 interleaved 顶点字节（可选索引字节）。
 * 2. 传入 GLGpuMesh#upload 上传到 GPU。
 * 3. 上传完成后可按策略释放或复用本对象中的数据。
 */
@SideOnly(Side.CLIENT)
public class GLMeshData {
    private final GLVertexLayout layout;
    private final ByteBuffer vertexBytes;
    private final ByteBuffer indexBytes;
    private final int vertexCount;
    private final int indexCount;
    private final int indexType;

    /**
     * @param layout 顶点布局（描述顶点字节如何解析）
     * @param vertexBytes 顶点字节数据（必填）
     * @param indexBytes 索引字节数据（可空；为空表示非索引绘制）
     * @param vertexCount 顶点数量
     * @param indexCount 索引数量（无索引时传 0）
     * @param indexType 索引类型（如 GL_UNSIGNED_SHORT / GL_UNSIGNED_INT）
     */
    public GLMeshData(GLVertexLayout layout,
                      ByteBuffer vertexBytes,
                      ByteBuffer indexBytes,
                      int vertexCount,
                      int indexCount,
                      int indexType) {
        if (layout == null) {
            throw new IllegalArgumentException("layout must not be null");
        }
        if (vertexBytes == null) {
            throw new IllegalArgumentException("vertexBytes must not be null");
        }
        if (vertexCount < 0 || indexCount < 0) {
            throw new IllegalArgumentException("vertexCount/indexCount must be >= 0");
        }
        this.layout = layout;
        this.vertexBytes = vertexBytes;
        this.indexBytes = indexBytes;
        this.vertexCount = vertexCount;
        this.indexCount = indexCount;
        this.indexType = indexType;
    }

    public GLVertexLayout layout() {
        return layout;
    }

    public ByteBuffer vertexBytes() {
        return vertexBytes;
    }

    public ByteBuffer indexBytes() {
        return indexBytes;
    }

    public int vertexCount() {
        return vertexCount;
    }

    public int indexCount() {
        return indexCount;
    }

    public int indexType() {
        return indexType;
    }

    public boolean indexed() {
        return indexBytes != null && indexCount > 0;
    }
}
