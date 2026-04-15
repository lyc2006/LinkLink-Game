package io.github.theflysong.client.gui;

import io.github.theflysong.client.data.Texture2D;
import io.github.theflysong.client.gl.GLTexture2D;
import io.github.theflysong.client.gl.mesh.GLGpuMesh;
import io.github.theflysong.client.gl.mesh.GLMeshBuilder;
import io.github.theflysong.client.gl.mesh.GLMeshData;
import io.github.theflysong.client.gl.mesh.GLVertexLayouts;
import io.github.theflysong.client.gl.shader.GLShaders;
import io.github.theflysong.client.render.RenderContext;
import io.github.theflysong.client.render.RenderItem;
import io.github.theflysong.client.render.RenderableObject;
import io.github.theflysong.client.render.Renderer;
import io.github.theflysong.client.render.preprocessor.IPreprocessor;
import io.github.theflysong.client.sprite.Sprite;
import io.github.theflysong.data.Identifier;
import io.github.theflysong.data.ResourceLoader;
import io.github.theflysong.data.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static io.github.theflysong.App.LOGGER;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL11C.glIsEnabled;

/**
 * GUI 渲染器：提供常用绘制工具函数。
 */
public final class GuiRenderer implements AutoCloseable {
    private static final float GUI_FRONT_Z = -0.95f;

    private final Renderer renderer;
    private final GLGpuMesh quadMesh;
    private final GuiFontManager fontManager;
    private final Map<ResourceLocation, GLTexture2D> textureCache = new HashMap<>();
    private final Map<Identifier, RenderableObject> spriteMeshCache = new HashMap<>();

    private GuiScreenSpace currentScreenSpace;

    public GuiRenderer(@NonNull Renderer renderer) {
        this.renderer = renderer;
        this.quadMesh = createQuadMesh();
        this.fontManager = new GuiFontManager();
    }

    public void renderScreen(@NonNull GuiScreen screen) {
        boolean wasDepthEnabled = glIsEnabled(GL_DEPTH_TEST);
        glDisable(GL_DEPTH_TEST);
        try {
            currentScreenSpace = GuiScreenSpace.fromCurrentViewport();
            renderer.updateProjection(currentScreenSpace.projectionMatrix());
            screen.render(this);
            renderer.flush();
        } finally {
            currentScreenSpace = null;
            if (wasDepthEnabled) {
                glEnable(GL_DEPTH_TEST);
            }
        }
    }

    public void drawTexture(@NonNull ResourceLocation textureLocation,
            @NonNull GuiAnchor anchor,
            float offsetX,
            float offsetY,
            float width,
            float height) {
        GLTexture2D texture = textureCache.get(textureLocation);
        if (texture == null) {
            texture = loadTexture(textureLocation);
            textureCache.put(textureLocation, texture);
        }
        drawTexture(texture, anchor, offsetX, offsetY, width, height);
    }

    public void drawTexture(@NonNull GLTexture2D texture,
            @NonNull GuiAnchor anchor,
            float offsetX,
            float offsetY,
            float width,
            float height) {
        if (currentScreenSpace == null) {
            throw new IllegalStateException("drawTexture must be called during renderScreen");
        }

        Matrix4f model = calcModel(anchor, offsetX, offsetY, width, height);

        renderer.submit(new RenderItem(
                quadMesh,
                GLShaders.TEXTURE.get(),
                model,
                (info, ctx) -> {
                    RenderContext.activateUnit(0);
                    texture.bind();

                    ctx.shader().getUniform("sam_texture").ifPresent(u -> u.set(0));
                    ctx.shader().getUniform("uv_rect").ifPresent(u -> u.set(0.0f, 0.0f, 1.0f, 1.0f));
                    ctx.shader().getUniform("m4_model").ifPresent(u -> u.set(ctx.modelMatrix()));
                    ctx.shader().getUniform("m4_projection").ifPresent(u -> u.set(info.projectionMatrix()));
                }));
    }

    /**
     * 在 GUI 中绘制文本。
     *
     * 当 {@code font} 为 null 时，使用字体管理器中的默认字体。
     */
    public void drawText(@NonNull String text,
            @Nullable GuiFont font,
            @NonNull GuiAnchor anchor,
            float offsetX,
            float offsetY) {
        if (currentScreenSpace == null) {
            throw new IllegalStateException("drawText must be called during renderScreen");
        }
        if (text.isEmpty()) {
            return;
        }

        GuiFont resolvedFont = fontManager.resolve(font);
        GuiFont.TextBounds bounds = resolvedFont.measureText(text);
        Vector2f topLeft = currentScreenSpace.resolveTopLeft(anchor, offsetX, offsetY, bounds.width(), bounds.height());

        float originX = topLeft.x;
        float penX = originX;
        float baselineY = topLeft.y + resolvedFont.ascentPx();

        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i);
            int step = Character.charCount(cp);
            if (cp == '\n') {
                penX = originX;
                baselineY += resolvedFont.lineHeightPx();
                i += step;
                continue;
            }

            GuiFont.Glyph glyph = resolvedFont.glyph(cp);
            GLTexture2D glyphTexture = glyph.texture();
            if (glyphTexture != null && glyph.width() > 0 && glyph.height() > 0) {
                Matrix4f model = new Matrix4f()
                        .identity()
                        .translate(penX + glyph.xOffset(), baselineY + glyph.yOffset(), GUI_FRONT_Z)
                        .scale(glyph.width(), glyph.height(), 1.0f);

                renderer.submit(new RenderItem(
                        quadMesh,
                        GLShaders.TEXTURE.get(),
                        model,
                        (info, ctx) -> {
                            RenderContext.activateUnit(0);
                            glyphTexture.bind();
                            ctx.shader().getUniform("sam_texture").ifPresent(u -> u.set(0));
                            ctx.shader().getUniform("uv_rect").ifPresent(u -> u.set(0.0f, 0.0f, 1.0f, 1.0f));
                            ctx.shader().getUniform("m4_model").ifPresent(u -> u.set(ctx.modelMatrix()));
                            ctx.shader().getUniform("m4_projection").ifPresent(u -> u.set(info.projectionMatrix()));
                        }));
            }

            penX += resolvedFont.advance(cp);
            int nextIndex = i + step;
            if (nextIndex < text.length()) {
                int nextCp = text.codePointAt(nextIndex);
                if (nextCp != '\n') {
                    penX += resolvedFont.kerningAdvance(cp, nextCp);
                }
            }
            i = nextIndex;
        }
    }

    public GuiFontManager fonts() {
        return fontManager;
    }

    public void drawSprite(@NonNull Sprite sprite,
            @NonNull GuiAnchor anchor,
            @NonNull IPreprocessor preprocessor,
            float offsetX,
            float offsetY,
            float width,
            float height) {
        if (currentScreenSpace == null) {
            throw new IllegalStateException("drawSprite must be called during renderScreen");
        }
        // 从 sprite 中构造一个RenderItem，提交给renderer
        RenderableObject renderable = lookupSpriteCache(sprite, preprocessor);
        Matrix4f model = calcModel(anchor, offsetX, offsetY, width, height);
        // 此处需要进行上下翻转以适配 GUI 坐标系(y向下)
        model.scale(1.0f, -1.0f, 1.0f);
        // 偏移到中心, 因为GUI渲染时定位位于左上角
        // 而Sprite的顶点坐标是以中心为原点的
        model.translate(0.5f, 0.5f, 0.0f);
        renderer.submit(new RenderItem(
                renderable.mesh(),
                renderable.shader(),
                model,
                renderable.preprocessor()));
    }

    public RenderableObject lookupSpriteCache(@NonNull Sprite sprite, @NonNull IPreprocessor preprocessor) {
        return spriteMeshCache.computeIfAbsent(sprite.id(), id -> {
            GLGpuMesh mesh = sprite.model().createGpuMesh();
            return new RenderableObject(mesh, sprite.shader(), preprocessor);
        });
    }

    @Override
    public void close() {
        for (GLTexture2D texture : textureCache.values()) {
            texture.close();
        }
        textureCache.clear();
        for (RenderableObject renderable : spriteMeshCache.values()) {
            renderable.close();
        }
        spriteMeshCache.clear();
        fontManager.close();
        quadMesh.close();
    }

    private Matrix4f calcModel(
            @NonNull GuiAnchor anchor, float offsetX, float offsetY, float width, float height) {
        Vector2f topLeft = currentScreenSpace.resolveTopLeft(anchor, offsetX, offsetY, width, height);
        Matrix4f model = new Matrix4f()
                .identity()
                .translate(topLeft.x, topLeft.y, GUI_FRONT_Z)
                .scale(width, height, 1.0f);
        return model;
    }

    private static GLTexture2D loadTexture(ResourceLocation textureLocation) {
        try {
            Texture2D textureData = Texture2D.fromImage(
                    ResourceLoader.loadBinary(textureLocation),
                    textureLocation.toString(),
                    false);
            return new GLTexture2D.Builder(GLTexture2D.Builder.PIXEL_STYLE).build(textureData);
        } catch (IOException ex) {
            LOGGER.error("Failed to load gui texture: {}", textureLocation, ex);
            throw new IllegalStateException("Failed to load gui texture: " + textureLocation, ex);
        }
    }

    private static GLGpuMesh createQuadMesh() {
        ByteBuffer vertices = MemoryUtil.memAlloc(4 * 4 * Float.BYTES);
        vertices.putFloat(0.0f).putFloat(0.0f).putFloat(0.0f).putFloat(0.0f);
        vertices.putFloat(1.0f).putFloat(0.0f).putFloat(1.0f).putFloat(0.0f);
        vertices.putFloat(1.0f).putFloat(1.0f).putFloat(1.0f).putFloat(1.0f);
        vertices.putFloat(0.0f).putFloat(1.0f).putFloat(0.0f).putFloat(1.0f);
        vertices.flip();

        ByteBuffer indices = MemoryUtil.memAlloc(6 * Integer.BYTES);
        indices.putInt(0).putInt(1).putInt(2);
        indices.putInt(2).putInt(3).putInt(0);
        indices.flip();

        GLMeshData data = GLMeshBuilder.fromPacked(
                GLVertexLayouts.SPRITE.get(),
                vertices,
                4,
                indices,
                6,
                GL_UNSIGNED_INT);

        GLGpuMesh mesh = new GLGpuMesh();
        try {
            mesh.upload(data);
            return mesh;
        } finally {
            MemoryUtil.memFree(vertices);
            MemoryUtil.memFree(indices);
        }
    }
}
