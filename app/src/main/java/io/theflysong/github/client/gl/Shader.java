package io.theflysong.github.client.gl;

import io.theflysong.github.util.Side;
import io.theflysong.github.util.SideOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.lwjgl.opengl.GL20C.*;

/**
 * Shader
 * @author theflysong
 */
@SideOnly(Side.CLIENT)
public class Shader implements AutoCloseable {
    private final int programId;
    private final Map<CharSequence, GLUniform> uniformMap = new HashMap<>();

    public Shader(CharSequence vertexShader, CharSequence fragmentShader) {
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

        // Initialize uniforms
        createUniform("ColorModulator", UniformType.F4).ifPresent(uniform -> uniform.set(1.0f, 1.0f, 1.0f, 1.0f));
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
