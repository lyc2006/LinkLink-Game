package io.github.theflysong.client.gl;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import io.github.theflysong.data.ResLoader;
import io.github.theflysong.data.ResLoc;
import io.github.theflysong.data.ResType;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import static org.lwjgl.opengl.GL11C.GL_FALSE;
import static org.lwjgl.opengl.GL20C.*;

/**
 * Shader
 * @author theflysong
 */
@SideOnly(Side.CLIENT)
public class Shader implements AutoCloseable {
    private final int programId;
    private final Map<CharSequence, GLUniform> uniformMap = new HashMap<>();
    private final GLVertexLayout vertexLayout;

    private static final Gson GSON = new Gson();

    private static final class ShaderDefinition {
        String vertex;
        String fragment;
        String vertlayout;
        Map<String, String> uniforms = new LinkedHashMap<>();
    }

    public Shader(CharSequence vertexShader, CharSequence fragmentShader) {
        this(vertexShader, fragmentShader, GLVertexLayouts.SPRITE.get());
    }

    public Shader(CharSequence vertexShader, CharSequence fragmentShader, GLVertexLayout vertexLayout) {
        this.vertexLayout = Objects.requireNonNull(vertexLayout, "vertexLayout must not be null");
        programId = glCreateProgram();
        int vsh = compileShader(GL_VERTEX_SHADER, "vertex", vertexShader);
        int fsh = compileShader(GL_FRAGMENT_SHADER, "fragment", fragmentShader);
        glAttachShader(programId, vsh);
        glAttachShader(programId, fsh);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Failed to link program " +
                                            programId +
                                            ". Log: " +
                                            glGetProgramInfoLog(programId));
        }
        glDetachShader(programId, vsh);
        glDetachShader(programId, fsh);
        glDeleteShader(vsh);
        glDeleteShader(fsh);

        // 兼容默认颜色调制 uniform（如果 shader 中不存在则忽略）。
        createUniform("ColorModulator", UniformType.F4).ifPresent(uniform -> uniform.set(1.0f, 1.0f, 1.0f, 1.0f));
    }

    /**
     * 从配置文件创建 Shader。
     *
     * 配置流程：
     * 1. 读取 JSON 配置。
     * 2. 根据配置加载顶点/片段 shader 源码。
     * 3. 解析并创建 uniform。
     * 4. 解析顶点布局并挂到 Shader 上，供渲染阶段校验。
     */
    public static Shader fromConfig(ResLoc shaderConfigLocation) {
        ShaderDefinition definition = parseConfig(shaderConfigLocation);

        String vertexSource = ResLoader.loadText(parseShaderSourceLocation(definition.vertex));
        String fragmentSource = ResLoader.loadText(parseShaderSourceLocation(definition.fragment));
        GLVertexLayout layout = GLVertexLayouts.resolve(parseVertexLayoutLocation(definition.vertlayout));

        Shader shader = new Shader(vertexSource, fragmentSource, layout);
        for (Map.Entry<String, String> entry : definition.uniforms.entrySet()) {
            UniformType uniformType = parseUniformType(entry.getValue());
            shader.createUniform(entry.getKey(), uniformType);
        }
        return shader;
    }

    private static ShaderDefinition parseConfig(ResLoc shaderConfigLocation) {
        String json = ResLoader.loadText(shaderConfigLocation);
        @Nullable
        ShaderDefinition definition;
        try {
            definition = GSON.fromJson(json, ShaderDefinition.class);
        } catch (JsonParseException ex) {
            throw new IllegalArgumentException("Invalid shader config json: " + shaderConfigLocation, ex);
        }

        if (definition.vertex == null || definition.vertex.isBlank()) {
            throw new IllegalArgumentException("Missing 'vertex' in shader config: " + shaderConfigLocation);
        }
        if (definition.fragment == null || definition.fragment.isBlank()) {
            throw new IllegalArgumentException("Missing 'fragment' in shader config: " + shaderConfigLocation);
        }
        if (definition.vertlayout == null || definition.vertlayout.isBlank()) {
            throw new IllegalArgumentException("Missing 'vertlayout' in shader config: " + shaderConfigLocation);
        }
        if (definition.uniforms == null) {
            definition.uniforms = new LinkedHashMap<>();
        }
        return definition;
    }

    /**
     * 解析配置中的 shader 路径，如 linklink:shader/sprite.vert。
     *
     * 兼容策略：
     * 1. 自动去掉前缀 shader/，避免与 ResType.SHADER 重复。
     * 2. 当 .vert/.frag 文件不存在时，回退到 .vs/.fs。
     */
    private static ResLoc parseShaderSourceLocation(String value) {
        int sep = value.indexOf(':');
        if (sep <= 0 || sep >= value.length() - 1) {
            throw new IllegalArgumentException("Invalid shader resource location: " + value);
        }
        String namespace = value.substring(0, sep);
        String path = value.substring(sep + 1);
        if (path.startsWith("shader/")) {
            path = path.substring("shader/".length());
        }

        ResLoc exact = new ResLoc(namespace, ResType.SHADER, path);
        if (ResLoader.loadFile(exact) != null) {
            return exact;
        }

        String fallbackPath = path;
        if (path.endsWith(".vert")) {
            fallbackPath = path.substring(0, path.length() - 5) + ".vs";
        } else if (path.endsWith(".frag")) {
            fallbackPath = path.substring(0, path.length() - 5) + ".fs";
        }

        ResLoc fallback = new ResLoc(namespace, ResType.SHADER, fallbackPath);
        if (ResLoader.loadFile(fallback) != null) {
            return fallback;
        }
        throw new IllegalArgumentException("Cannot resolve shader source from config value: " + value);
    }

    private static ResLoc parseVertexLayoutLocation(String value) {
        int sep = value.indexOf(':');
        if (sep <= 0 || sep >= value.length() - 1) {
            throw new IllegalArgumentException("Invalid vertex layout location: " + value);
        }
        String namespace = value.substring(0, sep);
        String path = value.substring(sep + 1);
        if (path.startsWith("vertexlayout/")) {
            path = path.substring("vertexlayout/".length());
        }
        return new ResLoc(namespace, ResType.VERTEX_LAYOUT, path);
    }

    private static UniformType parseUniformType(String value) {
        return switch (value) {
            case "mat4" -> UniformType.MAT_F4;
            case "vec4" -> UniformType.F4;
            case "sampler2D", "int" -> UniformType.I1;
            case "float" -> UniformType.F1;
            default -> throw new IllegalArgumentException("Unsupported uniform type: " + value);
        };
    }

    private static int compileShader(int type, String typeStr, CharSequence src) {
        int shader = glCreateShader(type);
        glShaderSource(shader, src);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Failed to compile " +
                                            typeStr +
                                            " shader. Log: " +
                                            glGetShaderInfoLog(shader));
        }
        return shader;
    }

    public Optional<GLUniform> createUniform(CharSequence name,
                                             UniformType type) {
        if (uniformMap.containsKey(name)) {
            return Optional.of(uniformMap.get(name));
        }
        int loc = glGetUniformLocation(programId, name);
        if (loc != -1) {
            GLUniform uniform = new GLUniform(loc, type);
            uniformMap.put(name, uniform);
            return Optional.of(uniform);
        }
        return Optional.empty();
    }

    public Optional<GLUniform> getUniform(CharSequence name) {
        return Optional.ofNullable(uniformMap.get(name));
    }

    public GLVertexLayout vertexLayout() {
        return vertexLayout;
    }

    public void uploadUniforms() {
        for (GLUniform uniform : uniformMap.values()) {
            uniform.upload();
        }
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void delete() {
        glDeleteProgram(programId);
    }

    @Override
    public void close() {
        for (GLUniform uniform : uniformMap.values()) {
            uniform.close();
        }
        delete();
    }

    public int programId() {
        return programId;
    }
}
