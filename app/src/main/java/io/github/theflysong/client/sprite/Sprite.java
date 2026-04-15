package io.github.theflysong.client.sprite;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import io.github.theflysong.client.gl.GLTextureAtlas;
import io.github.theflysong.client.gl.GLTexture2D;
import io.github.theflysong.client.gl.mesh.GLGpuMesh;
import io.github.theflysong.client.gl.mesh.GLMeshData;
import io.github.theflysong.client.gl.mesh.GLVertexAttribute;
import io.github.theflysong.client.gl.shader.GLShaders;
import io.github.theflysong.client.gl.shader.Shader;
import io.github.theflysong.data.Identifier;
import io.github.theflysong.data.ResourceLoader;
import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceType;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.lwjgl.system.MemoryUtil;

import static io.github.theflysong.App.LOGGER;

import static org.lwjgl.opengl.GL11C.GL_FLOAT;

/**
 * Sprite 资源。
 *
 * 该类负责：
 * 1. 从 sprite JSON 中解析 model / shader / textures 的引用。
 * 2. 在统一初始化后，从注册表中取出实际资源。
 * 3. 管理 sprite 自己持有的贴图资源生命周期。
 */
@SideOnly(Side.CLIENT)
public class Sprite implements AutoCloseable {
    private static final Gson GSON = new Gson();

    private final ResourceLocation id;
    private final Model model;
    private final Shader shader;
    private final Map<String, ResourceLocation> textureLocations;
    private GLTextureAtlas atlas;

    private static final class SpriteDefinition {
        String model;
        String shader;
        Map<String, String> textures = new LinkedHashMap<>();
    }

    protected Sprite(ResourceLocation id,
                     Model model,
                     Shader shader,
                     Map<String, ResourceLocation> textureLocations) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.shader = Objects.requireNonNull(shader, "shader must not be null");
        this.textureLocations = Map.copyOf(textureLocations);
    }

    /**
     * 从 sprite 配置文件创建 Sprite。
     *
     * 约定：
     * - model 引用会被解析成 linklink:model/<name>
     * - shader 引用会被解析成 linklink:shader/<name>
     * - texture 引用支持若干回退规则，以适配当前资源命名
     */
    public static Sprite fromConfig(ResourceLocation spriteConfigLocation)
            throws IOException, IllegalArgumentException {
        String json = ResourceLoader.loadText(spriteConfigLocation);
        SpriteDefinition definition;
        try {
            definition = GSON.fromJson(json, SpriteDefinition.class);
        } catch (JsonParseException ex) {
            LOGGER.error("Invalid sprite config json: {}", spriteConfigLocation, ex);
            throw new IllegalArgumentException("Invalid sprite config json: " + spriteConfigLocation, ex);
        }
    
        if (definition.model == null || definition.model.isBlank()) {
            throw new IllegalArgumentException("Missing 'model' in sprite config: " + spriteConfigLocation);
        }
        if (definition.shader == null || definition.shader.isBlank()) {
            throw new IllegalArgumentException("Missing 'shader' in sprite config: " + spriteConfigLocation);
        }
        if (definition.textures == null || definition.textures.isEmpty()) {
            throw new IllegalArgumentException("Missing 'textures' in sprite config: " + spriteConfigLocation);
        }

        Identifier modelId = parseModelLocation(spriteConfigLocation, definition.model);
        Identifier shaderId = parseShaderLocation(spriteConfigLocation, definition.shader);
        Model model = Models.getOrThrow(modelId);
        Shader shader = GLShaders.getOrThrow(shaderId);

        Map<String, ResourceLocation> textureLocations = new LinkedHashMap<>();
        Map<String, TextureAnimation> layerAnimations = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : definition.textures.entrySet()) {
            ResourceLocation textureLoc = resolveTextureLocation(spriteConfigLocation, entry.getValue());
            textureLocations.put(entry.getKey(), textureLoc);
            TextureAnimation.fromTexture(textureLoc).ifPresent(animation -> layerAnimations.put(entry.getKey(), animation));
        }

        if (!layerAnimations.isEmpty()) {
            return new MetaSprite(spriteConfigLocation, model, shader, textureLocations, layerAnimations);
        }
        return new Sprite(spriteConfigLocation, model, shader, textureLocations);
    }

    private static Identifier parseModelLocation(ResourceLocation base, String value) {
        int sep = value.indexOf(':');
        if (sep > 0 && sep < value.length() - 1) {
            String namespace = value.substring(0, sep);
            String path = value.substring(sep + 1);
            if (path.startsWith("sprite/")) {
                path = path.substring("sprite/".length());
            }
            return new Identifier(namespace, path);
        }
        return new Identifier(base.namespace(), value);
    }

    private static Identifier parseShaderLocation(ResourceLocation base, String value) {
        int sep = value.indexOf(':');
        if (sep > 0 && sep < value.length() - 1) {
            String namespace = value.substring(0, sep);
            String path = value.substring(sep + 1);
            if (path.startsWith("shader/")) {
                path = path.substring("shader/".length());
            }
            return new Identifier(namespace, path);
        }
        return new Identifier(base.namespace(), value);
    }

    private static ResourceLocation resolveTextureLocation(ResourceLocation base, String value) {
        ResourceLocation exact = parseTextureLocation(base, value);
        if (ResourceLoader.loadFile(exact) != null) {
            return exact;
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(exact.path() + ".png");

        String normalized = exact.path();
        if (normalized.endsWith("_gem_overlay")) {
            normalized = normalized.substring(0, normalized.length() - "_gem_overlay".length()) + ".overlay";
            candidates.add(normalized);
            candidates.add(normalized + ".png");
        } else if (normalized.endsWith("_overlay")) {
            normalized = normalized.substring(0, normalized.length() - "_overlay".length()) + ".overlay";
            candidates.add(normalized);
            candidates.add(normalized + ".png");
        } else if (normalized.endsWith("_gem")) {
            normalized = normalized.substring(0, normalized.length() - "_gem".length());
            candidates.add(normalized);
            candidates.add(normalized + ".png");
        }

        for (String candidate : candidates) {
            ResourceLocation candidateLoc = new ResourceLocation(exact.namespace(), ResourceType.TEXTURE, candidate);
            if (ResourceLoader.loadFile(candidateLoc) != null) {
                return candidateLoc;
            }
        }

        throw new IllegalArgumentException("Cannot resolve texture resource from config value: " + value);
    }

    private static ResourceLocation parseTextureLocation(ResourceLocation base, String value) {
        int sep = value.indexOf(':');
        if (sep > 0 && sep < value.length() - 1) {
            String namespace = value.substring(0, sep);
            String path = value.substring(sep + 1);
            return new ResourceLocation(namespace, ResourceType.TEXTURE, path);
        }
        return new ResourceLocation(base.namespace(), ResourceType.TEXTURE, value);
    }

    public ResourceLocation id() {
        return id;
    }

    public Model model() {
        return model;
    }

    public Shader shader() {
        return shader;
    }

    public Optional<GLTexture2D> texture(String layer) {
        if (atlas == null || !textureLocations.containsKey(layer)) {
            return Optional.empty();
        }
        return Optional.of(atlas);
    }

    public Optional<GLTextureAtlas> textureAtlas() {
        return Optional.ofNullable(atlas);
    }

    public Optional<ResourceLocation> textureLocation(String layer) {
        return Optional.ofNullable(textureLocations.get(layer));
    }

    public Map<String, ResourceLocation> textureLocations() {
        return textureLocations;
    }

    public GLTextureAtlas.UvRect uvByTexturePath(ResourceLocation textureLocation) {
        if (atlas == null) {
            return GLTextureAtlas.UvRect.full();
        }
        return atlas.uv(textureLocation).orElse(GLTextureAtlas.UvRect.full());
    }

    public GLTextureAtlas.UvRect uvForLayer(String layer) {
        ResourceLocation location = textureLocation(layer)
                .orElseThrow(() -> new IllegalArgumentException("Missing texture layer: " + layer));
        return uvByTexturePath(location);
    }

    public GLGpuMesh createGpuMeshForLayer(String layer) {
        GLTextureAtlas.UvRect uvRect = uvForLayer(layer);
        GLMeshData remapped = remapMeshUv(model.meshData(), uvRect);
        GLGpuMesh mesh = new GLGpuMesh();
        try {
            mesh.upload(remapped);
            return mesh;
        } finally {
            MemoryUtil.memFree(remapped.vertexBytes());
            if (remapped.indexBytes() != null) {
                MemoryUtil.memFree(remapped.indexBytes());
            }
        }
    }

    void setTextureAtlas(GLTextureAtlas atlas) {
        this.atlas = atlas;
    }

    private static GLMeshData remapMeshUv(GLMeshData source, GLTextureAtlas.UvRect uvRect) {
        int stride = source.layout().stride();
        int uvOffset = findUvOffset(source);

        ByteBuffer srcVertices = source.vertexBytes().duplicate();
        srcVertices.rewind();
        ByteBuffer dstVertices = MemoryUtil.memAlloc(srcVertices.remaining());
        dstVertices.put(srcVertices);
        dstVertices.flip();

        for (int i = 0; i < source.vertexCount(); i++) {
            int base = i * stride + uvOffset;
            float u = dstVertices.getFloat(base);
            float v = dstVertices.getFloat(base + 4);
            dstVertices.putFloat(base, uvRect.u() + u * uvRect.width());
            dstVertices.putFloat(base + 4, uvRect.v() + v * uvRect.height());
        }

        ByteBuffer dstIndices = null;
        if (source.indexBytes() != null) {
            ByteBuffer srcIndices = source.indexBytes().duplicate();
            srcIndices.rewind();
            dstIndices = MemoryUtil.memAlloc(srcIndices.remaining());
            dstIndices.put(srcIndices);
            dstIndices.flip();
        }

        return new GLMeshData(
                source.layout(),
                dstVertices,
                dstIndices,
                source.vertexCount(),
                source.indexCount(),
                source.indexType());
    }

    private static int findUvOffset(GLMeshData meshData) {
        for (GLVertexAttribute attribute : meshData.layout().attributes()) {
            if (attribute.location() == 1 &&
                    attribute.componentCount() == 2 &&
                    attribute.glType() == GL_FLOAT) {
                return attribute.offset();
            }
        }
        throw new IllegalStateException("Cannot find uv attribute (location=1, vec2 float) in layout");
    }

    public GLTexture2D textureOrThrow(String layer) {
        return texture(layer).orElseThrow(() -> new IllegalArgumentException("Missing texture layer: " + layer));
    }

    public Map<String, GLTexture2D> textures() {
        if (atlas == null) {
            return Map.of();
        }
        Map<String, GLTexture2D> mapped = new LinkedHashMap<>();
        for (String layer : textureLocations.keySet()) {
            mapped.put(layer, atlas);
        }
        return Map.copyOf(mapped);
    }

    public boolean isAnimated() {
        return false;
    }

    public int frameIndexAt(double renderTimeSeconds) {
        return 0;
    }

    @Override
    public void close() {
        // Sprite 不再持有独立 GLTexture2D，资源释放由 atlas 统一管理。
    }
}
