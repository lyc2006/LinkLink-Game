package io.github.theflysong.client.data;

import java.nio.ByteBuffer;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;
import static org.lwjgl.stb.STBImage.*;

/**
 * 2D纹理数据，包含纹理的宽高、像素数据等信息
 *
 * @author theflysong
 * @date 2026年4月14日
 */
public class Texture2D {
    private final int width;
    private final int height;
    private final ByteBuffer data;

    public Texture2D(int width, int height, ByteBuffer data) {
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public ByteBuffer data() {
        return data;
    }

    public static Texture2D empty(int width, int height) {
        return new Texture2D(width, height, null);
    }

    public static Texture2D fromRaw(int width, int height, ByteBuffer data) {
        return new Texture2D(width, height, data);
    }

    public static Texture2D fromRaw(int width, int height, byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
        buffer.put(data);
        buffer.flip();
        return new Texture2D(width, height, buffer);
    }

    public static Texture2D fromImage(ByteBuffer imageBuf, @Nullable String name) {
        return fromImage(imageBuf, name, true);
    }

    public static Texture2D fromImage(ByteBuffer imageBuf,
                                      @Nullable String name,
                                      boolean flipV) {
        if (imageBuf == null) {
            throw new IllegalArgumentException("Image buffer cannot be null for texture " + name);
        }

        int[] xp = { 0 }, yp = { 0 }, cp = { 0 };
        stbi_set_flip_vertically_on_load(flipV);
        ByteBuffer ret;
        try {
            ret = stbi_load_from_memory(
                    imageBuf,
                    xp,
                    yp,
                    cp,
                    STBI_rgb_alpha);
        } finally {
            stbi_set_flip_vertically_on_load(false);
        }
        if (ret == null) {
            if (name != null) {
                throw new RuntimeException("Failed to load texture " + name + ": " + stbi_failure_reason());
            } else {
                throw new RuntimeException("Failed to load texture: " + stbi_failure_reason());
            }
        }

        return new Texture2D(xp[0], yp[0], ret);
    }
}
