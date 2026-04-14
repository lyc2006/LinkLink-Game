package io.theflysong.github.client.gl;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11C.glDeleteTextures;
import static org.lwjgl.opengl.GL11C.glGenTextures;
import static org.lwjgl.opengl.GL11C.glTexImage2D;
import static org.lwjgl.opengl.GL11C.glTexParameteri;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_BASE_LEVEL;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL30C.*;
import org.lwjgl.opengl.GL30C;

import io.theflysong.github.client.data.Texture2D;

public class GLTexture2D implements AutoCloseable {
    private final int width;
    private final int height;
    private final ByteBuffer data;
    private final int glId;
    @Nullable
    private IntConsumer param;
    @Nullable
    private IntConsumer mipmap = GL30C::glGenerateMipmap;

    public GLTexture2D(Texture2D texture) {
        this.width = texture.width();
        this.height = texture.height();
        this.data = texture.data();
        this.glId = glGenTextures();

        build();
    }
    private void build()
    {
        GLMgr.getInstance().pushTextureBindingStack();
        GLMgr.getInstance().binding(0, glId);

        if (mipmap != null)
        {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
        }

        if (param != null)
        {
            param.accept(GL_TEXTURE_2D);
        }

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 
            width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);

        if (mipmap != null)
        {
            mipmap.accept(GL_TEXTURE_2D);
        }

        GLMgr.getInstance().popTextureBindingStack();
    }

    public void bind()
    {
        GLMgr.getInstance().bindTexture(glId);
    }

    public void unbind()
    {
        GLMgr.getInstance().bindTexture(0);
    }

    public int glId() {
        return glId;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public void close() {
        glDeleteTextures(glId);
    }

    public void setParam(@Nullable IntConsumer param) {
        this.param = param;
    }

    public Optional<IntConsumer> getParam() {
        return Optional.ofNullable(param);
    }

    public void setMipmap(IntConsumer mipmap) {
        this.mipmap = mipmap;
    }

    public Optional<IntConsumer> getMipmap() {
        return Optional.ofNullable(mipmap);
    }
}