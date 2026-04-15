package io.github.theflysong.client.gui;

import io.github.theflysong.client.data.Texture2D;
import io.github.theflysong.client.gl.GLTexture2D;
import org.jspecify.annotations.Nullable;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.stb.STBTruetype.stbtt_FindGlyphIndex;
import static org.lwjgl.stb.STBTruetype.stbtt_FreeBitmap;
import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointBitmap;
import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointBitmapBox;
import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointHMetrics;
import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointKernAdvance;
import static org.lwjgl.stb.STBTruetype.stbtt_GetFontVMetrics;
import static org.lwjgl.stb.STBTruetype.stbtt_InitFont;
import static org.lwjgl.stb.STBTruetype.stbtt_ScaleForPixelHeight;

/**
 * GUI 字体对象（基于 STB TrueType + 系统字体文件）。
 */
public final class GuiFont implements AutoCloseable {
    private final String name;
    private final Path sourcePath;
    private final float sizePx;
    private final STBTTFontinfo fontInfo;
    private final ByteBuffer fontData;
    private final float scale;
    private final float ascentPx;
    private final float lineHeightPx;

    private final Map<Integer, Glyph> glyphCache = new HashMap<>();
    private final Map<Integer, Float> advanceCache = new HashMap<>();

    public record TextBounds(float width, float height) {
    }

    public static final class Glyph implements AutoCloseable {
        private final @Nullable GLTexture2D texture;
        private final int width;
        private final int height;
        private final int xOffset;
        private final int yOffset;
        private final float advance;

        private Glyph(@Nullable GLTexture2D texture,
                      int width,
                      int height,
                      int xOffset,
                      int yOffset,
                      float advance) {
            this.texture = texture;
            this.width = width;
            this.height = height;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.advance = advance;
        }

        public @Nullable GLTexture2D texture() {
            return texture;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public int xOffset() {
            return xOffset;
        }

        public int yOffset() {
            return yOffset;
        }

        public float advance() {
            return advance;
        }

        @Override
        public void close() {
            if (texture != null) {
                texture.close();
            }
        }
    }

    private GuiFont(String name,
                    Path sourcePath,
                    float sizePx,
                    STBTTFontinfo fontInfo,
                    ByteBuffer fontData,
                    float scale,
                    float ascentPx,
                    float lineHeightPx) {
        this.name = name;
        this.sourcePath = sourcePath;
        this.sizePx = sizePx;
        this.fontInfo = fontInfo;
        this.fontData = fontData;
        this.scale = scale;
        this.ascentPx = ascentPx;
        this.lineHeightPx = lineHeightPx;
    }

    public static GuiFont fromFile(Path fontPath, float sizePx) {
        if (sizePx <= 0.0f) {
            throw new IllegalArgumentException("sizePx must be > 0");
        }
        try {
            byte[] bytes = Files.readAllBytes(fontPath);
            ByteBuffer ttf = MemoryUtil.memAlloc(bytes.length);
            ttf.put(bytes);
            ttf.flip();

            STBTTFontinfo info = STBTTFontinfo.create();
            if (!stbtt_InitFont(info, ttf)) {
                MemoryUtil.memFree(ttf);
                throw new IllegalArgumentException("Failed to init font: " + fontPath);
            }

            float scale = stbtt_ScaleForPixelHeight(info, sizePx);
            int ascent;
            int descent;
            int lineGap;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var pAscent = stack.mallocInt(1);
                var pDescent = stack.mallocInt(1);
                var pLineGap = stack.mallocInt(1);
                stbtt_GetFontVMetrics(info, pAscent, pDescent, pLineGap);
                ascent = pAscent.get(0);
                descent = pDescent.get(0);
                lineGap = pLineGap.get(0);
            }

            float ascentPx = ascent * scale;
            float lineHeightPx = (ascent - descent + lineGap) * scale;

            return new GuiFont(
                    fontPath.getFileName().toString(),
                    fontPath,
                    sizePx,
                    info,
                    ttf,
                    scale,
                    ascentPx,
                    lineHeightPx);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read font file: " + fontPath, ex);
        }
    }

    public String name() {
        return name;
    }

    public Path sourcePath() {
        return sourcePath;
    }

    public float sizePx() {
        return sizePx;
    }

    public float ascentPx() {
        return ascentPx;
    }

    public float lineHeightPx() {
        return lineHeightPx;
    }

    public float kerningAdvance(int currentCodePoint, int nextCodePoint) {
        return stbtt_GetCodepointKernAdvance(fontInfo, currentCodePoint, nextCodePoint) * scale;
    }

    public TextBounds measureText(String text) {
        if (text == null || text.isEmpty()) {
            return new TextBounds(0.0f, 0.0f);
        }

        float maxWidth = 0.0f;
        float lineWidth = 0.0f;
        int lineCount = 1;

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int step = Character.charCount(cp);
            if (cp == '\n') {
                maxWidth = Math.max(maxWidth, lineWidth);
                lineWidth = 0.0f;
                lineCount++;
                i += step;
                continue;
            }

            lineWidth += advance(cp);
            int nextIndex = i + step;
            if (nextIndex < text.length()) {
                int nextCp = text.codePointAt(nextIndex);
                if (nextCp != '\n') {
                    lineWidth += kerningAdvance(cp, nextCp);
                }
            }
            i = nextIndex;
        }
        maxWidth = Math.max(maxWidth, lineWidth);
        return new TextBounds(maxWidth, lineCount * lineHeightPx);
    }

    public Glyph glyph(int codePoint) {
        int resolved = resolveCodePoint(codePoint);
        return glyphCache.computeIfAbsent(resolved, this::buildGlyph);
    }

    public float advance(int codePoint) {
        int resolved = resolveCodePoint(codePoint);
        return advanceCache.computeIfAbsent(resolved, this::computeAdvance);
    }

    private int resolveCodePoint(int codePoint) {
        if (codePoint == '\n' || codePoint == '\r' || codePoint == '\t') {
            return codePoint;
        }
        if (stbtt_FindGlyphIndex(fontInfo, codePoint) != 0) {
            return codePoint;
        }
        return '?';
    }

    private float computeAdvance(int codePoint) {
        if (codePoint == '\n' || codePoint == '\r') {
            return 0.0f;
        }
        if (codePoint == '\t') {
            return computeAdvance(' ') * 4.0f;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var pAdvance = stack.mallocInt(1);
            var pBearing = stack.mallocInt(1);
            stbtt_GetCodepointHMetrics(fontInfo, codePoint, pAdvance, pBearing);
            return pAdvance.get(0) * scale;
        }
    }

    private Glyph buildGlyph(int codePoint) {
        if (codePoint == '\n' || codePoint == '\r') {
            return new Glyph(null, 0, 0, 0, 0, 0.0f);
        }
        if (codePoint == '\t') {
            return new Glyph(null, 0, 0, 0, 0, computeAdvance(codePoint));
        }

        int width;
        int height;
        int xOffset;
        int yOffset;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var pWidth = stack.mallocInt(1);
            var pHeight = stack.mallocInt(1);
            var pXOffset = stack.mallocInt(1);
            var pYOffset = stack.mallocInt(1);
            ByteBuffer bitmap = stbtt_GetCodepointBitmap(
                    fontInfo,
                    scale,
                    scale,
                    codePoint,
                    pWidth,
                    pHeight,
                    pXOffset,
                    pYOffset);

            width = pWidth.get(0);
            height = pHeight.get(0);
            xOffset = pXOffset.get(0);
            yOffset = pYOffset.get(0);

            if (bitmap == null || width <= 0 || height <= 0) {
                if (bitmap != null) {
                    stbtt_FreeBitmap(bitmap, 0);
                }
                return new Glyph(null, 0, 0, xOffset, yOffset, advance(codePoint));
            }

            ByteBuffer rgba = MemoryUtil.memAlloc(width * height * 4);
            for (int i = 0; i < width * height; i++) {
                int alpha = bitmap.get(i) & 0xFF;
                rgba.put((byte) 0xFF);
                rgba.put((byte) 0xFF);
                rgba.put((byte) 0xFF);
                rgba.put((byte) alpha);
            }
            rgba.flip();

            GLTexture2D texture;
            try {
                Texture2D textureData = Texture2D.fromRaw(width, height, rgba);
                texture = new GLTexture2D.Builder(new GLTexture2D.Builder.Settings()
                        .filter(GL_LINEAR)
                        .wrap(GL_CLAMP_TO_EDGE))
                        .build(textureData);
            } finally {
                MemoryUtil.memFree(rgba);
                stbtt_FreeBitmap(bitmap, 0);
            }

            return new Glyph(texture, width, height, xOffset, yOffset, advance(codePoint));
        }
    }

    @Override
    public void close() {
        for (Glyph glyph : glyphCache.values()) {
            glyph.close();
        }
        glyphCache.clear();
        advanceCache.clear();
        MemoryUtil.memFree(fontData);
    }
}
