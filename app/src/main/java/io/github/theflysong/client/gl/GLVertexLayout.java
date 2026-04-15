package io.github.theflysong.client.gl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.theflysong.data.ResLoc;
import io.github.theflysong.data.ResType;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;
import io.github.theflysong.util.registry.Deferred;
import io.github.theflysong.util.registry.Registry;
import io.github.theflysong.util.registry.SimpleRegistry;

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.glVertexAttribIPointer;

/**
 * 顶点布局描述（无流版本）。
 *
 * 说明：
 * 1. 本类只描述“一个顶点在内存中的排布方式”，不保存具体顶点数据。
 * 2. 当前架构不引入 stream 概念，因此默认所有 attribute 都来自同一个 VBO。
 * 3. 绘制前通常流程为：绑定 VAO -> 绑定 VBO -> 调用 apply() 配置 attribute 指针。
 */
@SideOnly(Side.CLIENT)
public class GLVertexLayout {
    private final int stride;
    private final List<GLVertexAttribute> attributes;

    public GLVertexLayout(int stride, List<GLVertexAttribute> attributes) {
        if (stride <= 0) {
            throw new IllegalArgumentException("stride must be > 0");
        }
        if (attributes == null || attributes.isEmpty()) {
            throw new IllegalArgumentException("attributes must not be empty");
        }
        this.stride = stride;
        this.attributes = Collections.unmodifiableList(new ArrayList<>(attributes));
    }

    /**
     * 将布局应用到当前 VAO。
     *
     * 注意：
     * - 调用本方法前，需要先 bind 目标 VAO 与对应 VBO。
     * - 本方法不会做 glBindBuffer，只做 attribute 启用与指针配置。
     */
    public void apply() {
        for (GLVertexAttribute attribute : attributes) {
            glEnableVertexAttribArray(attribute.location());
            if (attribute.integerAttribute()) {
                glVertexAttribIPointer(attribute.location(),
                    attribute.componentCount(),
                    attribute.glType(),
                    stride,
                    attribute.offset());
            } else {
                glVertexAttribPointer(attribute.location(),
                    attribute.componentCount(),
                    attribute.glType(),
                    attribute.normalized(),
                    stride,
                    attribute.offset());
            }
        }
    }

    public int stride() {
        return stride;
    }

    public List<GLVertexAttribute> attributes() {
        return attributes;
    }

    /**
     * 判断两个布局是否为同一格式。
     *
     * 不要求对象引用相同，只要 stride 与 attribute 列表一致即可。
     */
    public boolean compatibleWith(GLVertexLayout other) {
        if (other == null) {
            return false;
        }
        return this.stride == other.stride && this.attributes.equals(other.attributes);
    }
}
