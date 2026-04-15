package io.github.theflysong.client.sprite;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import io.github.theflysong.client.data.Texture2D;
import io.github.theflysong.client.gl.GLTexture2D;
import io.github.theflysong.client.gl.shader.GLShaders;
import io.github.theflysong.client.gl.shader.Shader;
import io.github.theflysong.data.Identifier;
import io.github.theflysong.data.ResourceLoader;
import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceType;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Sprite 资源。
 *
 * 该类负责：
 * 1. 从 sprite JSON 中解析 model / shader / textures 的引用。
 * 2. 在统一初始化后，从注册表中取出实际资源。
 * 3. 管理 sprite 自己持有的贴图资源生命周期。
 */
@SideOnly(Side.CLIENT)
public final class Sprite implements AutoCloseable {
    private static final Gson GSON = new Gson();

    private final ResourceLocation id;
    private final Model model;
    private final Shader shader;
    private final Map<String, GLTexture2D> textures;

    private static final class SpriteDefinition {
        String model;
        String shader;
        Map<String, String> textures = new LinkedHashMap<>();
    }

    private Sprite(ResourceLocation id, Model model, Shader shader, Map<String, GLTexture2D> textures) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.shader = Objects.requireNonNull(shader, "shader must not be null");
        this.textures = Map.copyOf(textures);
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

        Map<String, GLTexture2D> textures = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : definition.textures.entrySet()) {
            ResourceLocation textureLoc = resolveTextureLocation(spriteConfigLocation, entry.getValue());
            Texture2D texture = Texture2D.fromImage(ResourceLoader.loadBinary(textureLoc), textureLoc.toString());
            textures.put(entry.getKey(), new GLTexture2D.Builder(GLTexture2D.Builder.PIXEL_STYLE).build(texture));
        }

        return new Sprite(spriteConfigLocation, model, shader, textures);
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
        return Optional.ofNullable(textures.get(layer));
    }

    public GLTexture2D textureOrThrow(String layer) {
        return texture(layer).orElseThrow(() -> new IllegalArgumentException("Missing texture layer: " + layer));
    }

    public Map<String, GLTexture2D> textures() {
        return textures;
    }

    @Override
    public void close() {
        for (GLTexture2D texture : textures.values()) {
            texture.close();
        }
    }
}
