package io.github.theflysong.client.sprite;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import io.github.theflysong.client.gl.mesh.GLGpuMesh;
import io.github.theflysong.client.gl.mesh.GLMeshBuilder;
import io.github.theflysong.client.gl.mesh.GLMeshData;
import io.github.theflysong.client.gl.mesh.GLVertexLayout;
import io.github.theflysong.client.gl.mesh.GLVertexLayouts;
import io.github.theflysong.data.Identifier;
import io.github.theflysong.data.ResourceLoader;
import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

import org.jspecify.annotations.NonNull;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;

/**
 * 2D 模型资源。
 *
 * 该类负责：
 * 1. 从 model JSON 中解析顶点/索引数据。
 * 2. 保存 CPU 侧网格数据。
 * 3. 根据需要生成 GPU 侧网格对象。
 */
@SideOnly(Side.CLIENT)
public final class Model implements AutoCloseable {
    @NonNull
    private static final Gson GSON = new Gson();

    private final ResourceLocation id;
    private final String type;
    private final GLVertexLayout layout;
    private final GLMeshData meshData;

    private static final class ModelDefinition {
        String type;
        String layout;
        List<VertexDefinition> vertices = new ArrayList<>();
        List<List<Integer>> indices = new ArrayList<>();
    }

    private static final class VertexDefinition {
        List<Float> position = new ArrayList<>();
        List<Float> uv = new ArrayList<>();
    }

    private Model(ResourceLocation id, String type, GLVertexLayout layout, GLMeshData meshData) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.layout = Objects.requireNonNull(layout, "layout must not be null");
        this.meshData = Objects.requireNonNull(meshData, "meshData must not be null");
    }

    /**
     * 从 model 配置文件创建模型。
     *
     * 当前实现针对 2D 模型：
     * - vertices 需要提供 position[2] 和 uv[2]
     * - indices 需要提供三角形索引列表
     */
    public static Model fromConfig(ResourceLocation modelConfigLocation) throws IOException, IllegalArgumentException {
        String json = ResourceLoader.loadText(modelConfigLocation);

        ModelDefinition definition;
        try {
            definition = GSON.fromJson(json, ModelDefinition.class);
        } catch (JsonParseException ex) {
            throw new IllegalArgumentException("Invalid model config json: " + modelConfigLocation, ex);
        }

        if (definition.type == null || definition.type.isBlank()) {
            throw new IllegalArgumentException("Missing 'type' in model config: " + modelConfigLocation);
        }
        if (definition.layout == null || definition.layout.isBlank()) {
            throw new IllegalArgumentException("Missing 'layout' in model config: " + modelConfigLocation);
        }
        if (definition.vertices == null || definition.vertices.isEmpty()) {
            throw new IllegalArgumentException("Missing 'vertices' in model config: " + modelConfigLocation);
        }

        Identifier layoutId = parseVertexLayoutLocation(modelConfigLocation, definition.layout);
        GLVertexLayout layout = GLVertexLayouts.resolve(layoutId);
        GLMeshData meshData = buildMeshData(layout, definition.vertices, definition.indices);
        return new Model(modelConfigLocation, definition.type, layout, meshData);
    }

    private static Identifier parseVertexLayoutLocation(ResourceLocation base, String value) {
        int sep = value.indexOf(':');
        if (sep > 0 && sep < value.length() - 1) {
            String namespace = value.substring(0, sep);
            String path = value.substring(sep + 1);
            if (path.startsWith("vertexlayout/")) {
                path = path.substring("vertexlayout/".length());
            }
            return new Identifier(namespace, path);
        }
        return new Identifier(base.namespace(), value.toLowerCase());
    }

    private static GLMeshData buildMeshData(GLVertexLayout layout,
            List<VertexDefinition> vertices,
            List<List<Integer>> indices) {
        ByteBuffer vertexBytes = MemoryUtil.memAlloc(vertices.size() * layout.stride());
        for (VertexDefinition vertex : vertices) {
            requireSize(vertex.position, 2, "position");
            requireSize(vertex.uv, 2, "uv");
            // model JSON 使用 0..1 的局部坐标系，这里统一平移到以原点为中心，
            // 使得模型中心位于 (0, 0)，便于后续变换。
            vertexBytes.putFloat(vertex.position.get(0) - 0.5f);
            vertexBytes.putFloat(vertex.position.get(1) - 0.5f);
            vertexBytes.putFloat(vertex.uv.get(0));
            vertexBytes.putFloat(vertex.uv.get(1));
        }
        vertexBytes.flip();

        if (indices == null || indices.isEmpty()) {
            return GLMeshBuilder.fromPacked(layout, vertexBytes, vertices.size());
        }

        List<Integer> flat = new ArrayList<>();
        for (List<Integer> triangle : indices) {
            requireSize(triangle, 3, "indices triangle");
            flat.addAll(triangle);
        }
        ByteBuffer indexBytes = MemoryUtil.memAlloc(flat.size() * Integer.BYTES);
        for (int index : flat) {
            indexBytes.putInt(index);
        }
        indexBytes.flip();
        return GLMeshBuilder.fromPacked(layout, vertexBytes, vertices.size(), indexBytes, flat.size(), GL_UNSIGNED_INT);
    }

    private static void requireSize(List<?> values, int expected, String name) {
        if (values == null || values.size() != expected) {
            throw new IllegalArgumentException("Invalid " + name + " size, expected " + expected);
        }
    }

    public ResourceLocation id() {
        return id;
    }

    public String type() {
        return type;
    }

    public GLVertexLayout layout() {
        return layout;
    }

    public GLMeshData meshData() {
        return meshData;
    }

    public GLGpuMesh createGpuMesh() {
        GLGpuMesh mesh = new GLGpuMesh();
        mesh.upload(meshData);
        return mesh;
    }

    @Override
    public void close() {
        MemoryUtil.memFree(meshData.vertexBytes());
        if (meshData.indexBytes() != null) {
            MemoryUtil.memFree(meshData.indexBytes());
        }
    }
}
