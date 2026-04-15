package io.theflysong.github.client.gl;

import io.theflysong.github.util.Side;
import io.theflysong.github.util.SideOnly;

/**
 * 顶点属性描述。
 *
 * 设计目标：
 * 1. 与具体网格数据解耦，只描述“某个 attribute 应该如何从顶点字节块中读取”。
 * 2. 本项目当前采用“无流”架构，即所有 attribute 都来自同一个 VBO。
 *
 * 字段含义：
 * - location: 着色器中的 attribute location。
 * - componentCount: 分量个数（例如 vec3 => 3）。
 * - glType: 底层 OpenGL 类型（如 GL_FLOAT / GL_UNSIGNED_BYTE）。
 * - normalized: 对整型输入是否做归一化转换（常用于颜色）。
 * - offset: 相对于单个顶点起始地址的字节偏移。
 * - integerAttribute: true 表示使用 glVertexAttribIPointer（整型语义，不做浮点转换）。
 */
@SideOnly(Side.CLIENT)
public record GLVertexAttribute(
    int location,
    int componentCount,
    int glType,
    boolean normalized,
    int offset,
    boolean integerAttribute
) {
    /**
     * 创建一个“浮点语义”的 attribute。
     * OpenGL 绑定时会走 glVertexAttribPointer。
     */
    public static GLVertexAttribute floating(int location,
                                             int componentCount,
                                             int glType,
                                             boolean normalized,
                                             int offset) {
        return new GLVertexAttribute(location, componentCount, glType, normalized, offset, false);
    }

    /**
     * 创建一个“整型语义”的 attribute。
     * OpenGL 绑定时会走 glVertexAttribIPointer。
     */
    public static GLVertexAttribute integer(int location,
                                            int componentCount,
                                            int glType,
                                            int offset) {
        return new GLVertexAttribute(location, componentCount, glType, false, offset, true);
    }
}
