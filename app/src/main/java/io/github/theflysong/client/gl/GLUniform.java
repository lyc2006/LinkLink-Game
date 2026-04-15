package io.github.theflysong.client.gl;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

import org.joml.Matrix4fc;
import org.joml.Vector4fc;
import static org.lwjgl.opengl.GL20C.*;

/**
 * OpenGL Uniform，表示一个Uniform变量，包含Uniform的位置、类型等信息
 *
 * @author theflysong
 * @date 2026年4月14日
 */
@SideOnly(Side.CLIENT)
public class GLUniform implements AutoCloseable {
    private final int location;
    private final ByteBuffer buffer;
    private final UniformType type;
    // only update data when value is changed.
    private boolean isDirty = true;

    public GLUniform(int loc, UniformType type) {
        this.location = loc;
        buffer = MemoryUtil.memCalloc(type.length);
        this.type = type;
        if (type == UniformType.MAT_F4) {
            putFloats(1, 0, 0, 0);
            putFloats(0, 1, 0, 0);
            putFloats(0, 0, 1, 0);
            putFloats(0, 0, 0, 1);
            buffer.flip();
        }
    }

    private void putFloats(float... values) {
        for (float v : values) {
            buffer.putFloat(v);
        }
    }

    public int getLocation() {
        return location;
    }

    public void set(int value) {
        buffer.position(0)
            .putInt(value);
        markDirty();
    }

    public void set(Matrix4fc value) {
        buffer.position(0);
        value.get(buffer);
        markDirty();
    }

    public void set(float[] value) {
        buffer.position(0);
        for (float v : value) {
            buffer.putFloat(v);
        }
        markDirty();
    }

    public void set(Vector4fc value) {
        set(value.x(), value.y(), value.z(), value.w());
    }

    public void set(float x) {
        buffer.position(0)
            .putFloat(x);
        markDirty();
    }

    public void set(float x, float y, float z, float w) {
        buffer.position(0)
            .putFloat(x)
            .putFloat(y)
            .putFloat(z)
            .putFloat(w);
        markDirty();
    }

    private void markDirty() {
        isDirty = true;
    }

    public void upload() {
        if (!isDirty) {
            return;
        }
        switch (type) {
            case I1 -> glUniform1i(location, buffer.getInt(0));
            case F4 -> glUniform4f(location, buffer.getFloat(0),
                buffer.getFloat(4),
                buffer.getFloat(8),
                buffer.getFloat(12));
            case F1 -> glUniform1f(location, buffer.getFloat(0));
            case MAT_F4 -> nglUniformMatrix4fv(location, 1, false, MemoryUtil.memAddress(buffer));
        }
        isDirty = false;
    }

    @Override
    public void close() {
        MemoryUtil.memFree(buffer);
    }
}
