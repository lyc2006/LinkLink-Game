package io.github.theflysong.client.gl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.*;
import static org.lwjgl.opengl.GL30C.*;
import org.lwjgl.opengl.GL30C;

import io.github.theflysong.client.data.Texture2D;

/**
 * 2D 纹理对象封装。
 *
 * 说明：
 * 1. 构造时立即创建并上传纹理。
 * 2. 参数由 Builder.Settings 提供。
 */
public class GLTexture2D implements AutoCloseable {
    private final int width;
    private final int height;
    private final ByteBuffer data;
    private final int glId;
    private final Map<Integer, Integer> params;

    private GLTexture2D(Texture2D texture, Map<Integer, Integer> params) {
        this.width = texture.width();
        this.height = texture.height();
        this.data = texture.data();
        this.glId = glGenTextures();
        this.params = params;

        build();
    }

    /**
     * 创建 GL 纹理并上传像素数据。
     */
    private void build() {
        GLMgr.getInstance().pushTextureBindingStack();
        GLMgr.getInstance().binding(0, glId);

        for (var entry : params.entrySet()) {
            glTexParameteri(GL_TEXTURE_2D, entry.getKey(), entry.getValue());
        }

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA,
                width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);

        GLMgr.getInstance().popTextureBindingStack();
    }

    public void bind() {
        GLMgr.getInstance().bindTexture(glId);
    }

    public void unbind() {
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

    public int param(int param) {
        return params.get(param);
    }

    @Override
    public void close() {
        glDeleteTextures(glId);
    }

    public static class Builder {
        /**
         * 纹理参数集合。
         */
        public static class Settings {
            private Map<Integer, Integer> params = new HashMap<>();

            public Settings param(int param, int value) {
                params.put(param, value);
                return this;
            }

            public Settings mipmap(int mipmap_base, int mipmap_max) {
                param(GL_TEXTURE_BASE_LEVEL, mipmap_base);
                param(GL_TEXTURE_MAX_LEVEL, mipmap_max);
                return this;
            }

            public Settings filter(int filter) {
                param(GL_TEXTURE_MIN_FILTER, filter);
                param(GL_TEXTURE_MAG_FILTER, filter);
                return this;
            }

            public Settings wrap(int wrap) {
                param(GL_TEXTURE_WRAP_S, wrap);
                param(GL_TEXTURE_WRAP_T, wrap);
                return this;
            }
        };

        Settings settings;

        public Builder(@Nullable Settings preset) {
            if (preset != null) {
                this.settings = preset;
            } else {
                this.settings = new Settings();
            }
        }

        public Builder param(int param, int value) {
            settings.param(param, value);
            return this;
        }

        public Builder mipmap(int mipmap_base, int mipmap_max) {
            settings.mipmap(mipmap_base, mipmap_max);
            return this;
        }

        public Builder filter(int filter) {
            settings.filter(filter);
            return this;
        }

        public Builder wrap(int wrap) {
            settings.wrap(wrap);
            return this;
        }

        public GLTexture2D build(Texture2D texture) {
            return new GLTexture2D(texture, settings.params);
        }
        
        /**
         * 像素风格预设：近邻采样 + clamp。
         */
        public static final Settings PIXEL_STYLE = new Settings()
                .filter(GL_NEAREST)
                .wrap(GL_CLAMP_TO_EDGE);
    }
}