package io.github.theflysong.client.render;

import java.nio.ByteBuffer;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import io.github.theflysong.client.gl.mesh.GLGpuMesh;
import io.github.theflysong.client.gl.mesh.GLMeshBuilder;
import io.github.theflysong.client.gl.mesh.GLMeshData;
import io.github.theflysong.client.gl.mesh.GLVertexLayouts;
import io.github.theflysong.client.gl.shader.GLShaders;
import io.github.theflysong.util.SideOnly;
import io.github.theflysong.util.Side;

import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;

/**
 * 几何渲染器
 *
 * @author theflysong
 * @date 2026年4月16日
 */
@SideOnly(Side.CLIENT)
public class GeometryRenderer implements AutoCloseable {
    private static final int FLOATS_PER_VERTEX = 6; // xy + rgba
    private static final Vector4f WHITE = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

    private final GLGpuMesh triangleMesh;
    private final GLGpuMesh rectangleMesh;

    private static GeometryRenderer INSTANCE;

    public static GeometryRenderer instance() {
        if (INSTANCE == null) {
            INSTANCE = new GeometryRenderer();
        }
        return INSTANCE;
    }

    private GeometryRenderer() {
        this.triangleMesh = createTriangleMesh();
        this.rectangleMesh = createRectangleMesh();
    }

    public void renderTriangle(@NonNull Renderer renderer, @NonNull Matrix4f modelMatrix) {
        renderTriangle(renderer, modelMatrix, WHITE);
    }

    public void renderTriangle(@NonNull Renderer renderer, @NonNull Matrix4f modelMatrix, @NonNull Vector4f color) {
        submit(renderer, triangleMesh, modelMatrix, color);
    }

    public void renderTriangle(@NonNull Renderer renderer,
            @Nullable Matrix4f modelMatrix,
            float centerX,
            float centerY,
            float width,
            float height,
            @NonNull Vector4f color) {
        if (modelMatrix == null) {
            modelMatrix = new Matrix4f().identity();
        }
        Matrix4f newMatrix = new Matrix4f(modelMatrix)
                .translate(centerX, centerY, 0.0f)
                .scale(width, height, 1.0f);
        renderTriangle(renderer, newMatrix, color);
    }

    public void renderRectangle(@NonNull Renderer renderer, @NonNull Matrix4f modelMatrix) {
        renderRectangle(renderer, modelMatrix, WHITE);
    }

    public void renderRectangle(@NonNull Renderer renderer, @NonNull Matrix4f modelMatrix, @NonNull Vector4f color) {
        submit(renderer, rectangleMesh, modelMatrix, color);
    }

    public void renderRectangle(@NonNull Renderer renderer,
            @Nullable Matrix4f modelMatrix,
            float centerX,
            float centerY,
            float width,
            float height,
            @NonNull Vector4f color) {
        if (modelMatrix == null) {
            modelMatrix = new Matrix4f().identity();
        }
        Matrix4f newMatrix = new Matrix4f(modelMatrix)
                .translate(centerX, centerY, 0.0f)
                .scale(width, height, 1.0f);
        renderRectangle(renderer, newMatrix, color);
    }

    private static void submit(@NonNull Renderer renderer,
            @NonNull GLGpuMesh mesh,
            @NonNull Matrix4f modelMatrix,
            @NonNull Vector4f color) {
        if (renderer == null) {
            throw new IllegalArgumentException("renderer must not be null");
        }
        if (mesh == null) {
            throw new IllegalArgumentException("mesh must not be null");
        }
        if (modelMatrix == null) {
            throw new IllegalArgumentException("modelMatrix must not be null");
        }
        if (color == null) {
            throw new IllegalArgumentException("color must not be null");
        }

        Vector4f colorCopy = new Vector4f(color);
        renderer.submit(new RenderItem(
                mesh,
                GLShaders.GEOMETRY.get(),
                new Matrix4f(modelMatrix),
                (info, ctx) -> {
                    ctx.shader().getUniform("m4_model").ifPresent(u -> u.set(ctx.modelMatrix()));
                    ctx.shader().getUniform("m4_projection").ifPresent(u -> u.set(info.projectionMatrix()));
                    ctx.shader().getUniform("v4_color_multiplier").ifPresent(u -> u.set(colorCopy));
                }));
    }

    private static GLGpuMesh createTriangleMesh() {
        // 局部空间：以原点为中心，尺寸约 1x1。
        float[] vertices = new float[] {
                -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 1.0f,
                0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 1.0f,
                0.0f, 0.5f, 1.0f, 1.0f, 1.0f, 1.0f
        };
        int[] indices = new int[] { 0, 1, 2 };
        return createMesh(vertices, 3, indices, 3);
    }

    private static GLGpuMesh createRectangleMesh() {
        // 局部空间：以原点为中心，尺寸 1x1。
        float[] vertices = new float[] {
                -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 1.0f,
                0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 1.0f,
                0.5f, 0.5f, 1.0f, 1.0f, 1.0f, 1.0f,
                -0.5f, 0.5f, 1.0f, 1.0f, 1.0f, 1.0f
        };
        int[] indices = new int[] { 0, 1, 2, 2, 3, 0 };
        return createMesh(vertices, 4, indices, 6);
    }

    private static GLGpuMesh createMesh(float[] vertexData,
            int vertexCount,
            int[] indexData,
            int indexCount) {
        ByteBuffer vertices = MemoryUtil.memAlloc(vertexCount * FLOATS_PER_VERTEX * Float.BYTES);
        ByteBuffer indices = MemoryUtil.memAlloc(indexCount * Integer.BYTES);
        try {
            for (float value : vertexData) {
                vertices.putFloat(value);
            }
            vertices.flip();

            for (int index : indexData) {
                indices.putInt(index);
            }
            indices.flip();

            GLMeshData data = GLMeshBuilder.fromPacked(
                    GLVertexLayouts.GEOMETRY.get(),
                    vertices,
                    vertexCount,
                    indices,
                    indexCount,
                    GL_UNSIGNED_INT);

            GLGpuMesh mesh = new GLGpuMesh();
            mesh.upload(data);
            return mesh;
        } finally {
            MemoryUtil.memFree(vertices);
            MemoryUtil.memFree(indices);
        }
    }

    @Override
    public void close() {
        triangleMesh.close();
        rectangleMesh.close();
    }
}
