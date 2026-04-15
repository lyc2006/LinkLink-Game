package io.github.theflysong.client.gl;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL11C.glDrawElements;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL15C.glBufferData;
import static org.lwjgl.opengl.GL15C.glDeleteBuffers;
import static org.lwjgl.opengl.GL15C.glGenBuffers;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;

import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * GPU 侧网格对象。
 *
 * 设计定位：
 * 1. 持有 VAO/VBO/EBO 的 OpenGL 句柄。
 * 2. 接收 GLMeshData 上传，建立“单 VBO + 单布局”的顶点输入。
 * 3. 提供 draw() 作为最小可用绘制入口。
 */
@SideOnly(Side.CLIENT)
public class GLGpuMesh implements AutoCloseable {
    private final int vao;
    private final int vbo;
    private int ebo;
    private int drawMode = GL_TRIANGLES;
    private int vertexCount;
    private int indexCount;
    private int indexType = GL_UNSIGNED_INT;
    private GLVertexLayout vertexLayout;

    public GLGpuMesh() {
        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();
        this.ebo = 0;
    }

    /**
     * 上传网格数据到 GPU。
     *
     * 说明：
     * - 仅支持无流布局：所有 attribute 均从同一 VBO 读取。
     * - 如果传入索引数据，则自动创建并绑定 EBO。
     */
    public void upload(GLMeshData data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }

        bind();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data.vertexBytes(), GL_STATIC_DRAW);
        data.layout().apply();
        this.vertexLayout = data.layout();
        this.vertexCount = data.vertexCount();

        if (data.indexed()) {
            if (ebo == 0) {
                ebo = glGenBuffers();
            }
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, data.indexBytes(), GL_STATIC_DRAW);
            this.indexCount = data.indexCount();
            this.indexType = data.indexType();
        } else {
            this.indexCount = 0;
        }

        unbind();
    }

    public void setDrawMode(int drawMode) {
        this.drawMode = drawMode;
    }

    public void bind() {
        glBindVertexArray(vao);
    }

    public void unbind() {
        glBindVertexArray(0);
    }

    /**
     * 发起绘制。
     *
     * 规则：
     * - 有索引：glDrawElements
     * - 无索引：glDrawArrays
     */
    public void draw() {
        bind();
        if (indexCount > 0) {
            glDrawElements(drawMode, indexCount, indexType, 0L);
        } else {
            glDrawArrays(drawMode, 0, vertexCount);
        }
    }

    public GLVertexLayout vertexLayout() {
        return vertexLayout;
    }

    @Override
    public void close() {
        glDeleteBuffers(vbo);
        if (ebo != 0) {
            glDeleteBuffers(ebo);
        }
        glDeleteVertexArrays(vao);
    }
}
