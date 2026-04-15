package io.github.theflysong.client.gl;

import static org.lwjgl.opengl.GL11C.GL_NEAREST;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.stb.STBImage.stbi_image_free;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import io.github.theflysong.client.data.Texture2D;
import io.github.theflysong.client.gl.mesh.GLGpuMesh;
import io.github.theflysong.client.gl.shader.Shader;
import io.github.theflysong.client.render.RenderContext;
import io.github.theflysong.client.render.RenderItem;
import io.github.theflysong.client.render.Renderer;
import io.github.theflysong.client.sprite.Sprite;
import io.github.theflysong.client.sprite.TextureAnimation;
import io.github.theflysong.data.ResourceLoader;
import io.github.theflysong.data.ResourceLocation;

/**
 * Atlas 纹理, 即将多个小纹理打包到一个大纹理中以减少绑定次数。
 *
 * @author theflysong
 * @date 2026年4月16日
 */
public class GLTextureAtlas extends GLTexture2D {
	public record UvRect(float u, float v, float width, float height) {
		public static UvRect full() {
			return new UvRect(0.0f, 0.0f, 1.0f, 1.0f);
		}
	}

	private final Map<ResourceLocation, UvRect> uvMap;
    private final int cellWidth;
    private final int cellHeight;
    private final int columns;
    private final int rows;

	private GLTextureAtlas(Texture2D atlasTexture, Map<ResourceLocation, UvRect> uvMap, int cellWidth, int cellHeight, int columns, int rows) {
		super(atlasTexture, defaultParams());
		this.uvMap = Map.copyOf(uvMap);
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.columns = columns;
        this.rows = rows;
	}

	public Optional<UvRect> uv(ResourceLocation textureLocation) {
		return Optional.ofNullable(uvMap.get(textureLocation));
	}

	/**
	 * 调试渲染 atlas：将整张图集作为普通精灵提交到渲染器。
	 */
	public void renderDebug(Renderer renderer,
							GLGpuMesh mesh,
							Shader shader,
							Matrix4f modelMatrix) {
		if (renderer == null || mesh == null || shader == null || modelMatrix == null) {
			throw new IllegalArgumentException("renderer/mesh/shader/modelMatrix must not be null");
		}

		renderer.submit(new RenderItem(
				mesh,
				shader,
				new Matrix4f(modelMatrix),
				(info, ctx) -> {
					RenderContext.activateUnit(0);
					bind();
					ctx.shader().getUniform("sam_atlas").ifPresent(u -> u.set(0));
					ctx.shader().getUniform("v4_sprite_color").ifPresent(u -> u.set(1.0f, 1.0f, 1.0f, 1.0f));
					ctx.shader().getUniform("uv_texture").ifPresent(u -> u.set(0.0f, 0.0f, 1.0f, 1.0f));
					ctx.shader().getUniform("m4_model").ifPresent(u -> u.set(ctx.modelMatrix()));
					ctx.shader().getUniform("m4_projection").ifPresent(u -> u.set(info.projectionMatrix()));
				}));
	}

	public static GLTextureAtlas buildFromSprites(Collection<Sprite> sprites) throws IOException {
		if (sprites == null || sprites.isEmpty()) {
			throw new IllegalArgumentException("sprites must not be empty");
		}

		record AtlasEntry(ResourceLocation location,
		                  Texture2D texture,
		                  int frameWidth,
		                  int frameHeight,
		                  int frameCount,
		                  TextureAnimation animation) {
		}

		Map<ResourceLocation, AtlasEntry> uniqueEntries = new LinkedHashMap<>();
		for (Sprite sprite : sprites) {
			for (ResourceLocation textureLocation : sprite.textureLocations().values()) {
				if (!uniqueEntries.containsKey(textureLocation)) {
					Texture2D texture = Texture2D.fromImage(
							ResourceLoader.loadBinary(textureLocation),
							textureLocation.toString());
					TextureAnimation animation = TextureAnimation.fromTexture(textureLocation).orElse(null);
					int frameHeight = resolveFrameHeight(texture, animation);
					int frameCount = resolveFrameCount(texture, animation, frameHeight);
					uniqueEntries.put(textureLocation,
							new AtlasEntry(textureLocation, texture, texture.width(), frameHeight, frameCount, animation));
				}
			}
		}
		if (uniqueEntries.isEmpty()) {
			throw new IllegalArgumentException("No textures found in sprites");
		}

		List<AtlasEntry> entries = new ArrayList<>(uniqueEntries.values());
		int cellWidth = 0;
		int cellHeight = 0;
		int totalUnits = 0;
		for (AtlasEntry entry : entries) {
			cellWidth = Math.max(cellWidth, entry.frameWidth());
			cellHeight = Math.max(cellHeight, entry.frameHeight());
			totalUnits += Math.max(1, entry.frameCount());
		}

		int columns = Math.max(1, (int) Math.ceil(Math.sqrt(totalUnits)));
		int[] columnHeights = new int[columns];
		Map<ResourceLocation, int[]> placements = new HashMap<>();
		for (AtlasEntry entry : entries) {
			int bestColumn = 0;
			int minHeight = columnHeights[0];
			for (int col = 1; col < columns; col++) {
				if (columnHeights[col] < minHeight) {
					bestColumn = col;
					minHeight = columnHeights[col];
				}
			}
			int yCell = columnHeights[bestColumn];
			placements.put(entry.location(), new int[] { bestColumn, yCell });
			columnHeights[bestColumn] += Math.max(1, entry.frameCount());
		}

		int rows = 0;
		for (int height : columnHeights) {
			rows = Math.max(rows, height);
		}

		int atlasWidth = columns * cellWidth;
		int atlasHeight = rows * cellHeight;

		ByteBuffer atlasBuffer = MemoryUtil.memAlloc(atlasWidth * atlasHeight * 4);
		for (int i = 0; i < atlasBuffer.capacity(); i++) {
			atlasBuffer.put(i, (byte) 0);
		}

		Map<ResourceLocation, UvRect> uvMap = new HashMap<>();
		for (AtlasEntry entry : entries) {
			Texture2D texture = entry.texture();
			int[] placement = placements.get(entry.location());
			int col = placement[0];
			int yCell = placement[1];
			int x = col * cellWidth;
			int y = yCell * cellHeight;

			ByteBuffer src = texture.data();
			int srcWidth = texture.width();
			int srcHeight = texture.height();
			for (int py = 0; py < srcHeight; py++) {
				for (int px = 0; px < srcWidth; px++) {
					int srcBase = (py * srcWidth + px) * 4;
					int dstBase = ((y + py) * atlasWidth + (x + px)) * 4;
					atlasBuffer.put(dstBase, src.get(srcBase));
					atlasBuffer.put(dstBase + 1, src.get(srcBase + 1));
					atlasBuffer.put(dstBase + 2, src.get(srcBase + 2));
					atlasBuffer.put(dstBase + 3, src.get(srcBase + 3));
				}
			}

			float u = (float) x / (float) atlasWidth;
			float v = (float) y / (float) atlasHeight;
			float w = (float) entry.frameWidth() / (float) atlasWidth;
			float h = (float) entry.frameHeight() / (float) atlasHeight;
			uvMap.put(entry.location(), new UvRect(u, v, w, h));
		}

		for (AtlasEntry entry : uniqueEntries.values()) {
			stbi_image_free(entry.texture().data());
		}

		Texture2D atlasTexture = Texture2D.fromRaw(atlasWidth, atlasHeight, atlasBuffer);
		return new GLTextureAtlas(atlasTexture, uvMap, cellWidth, cellHeight, columns, rows);
	}

	private static int resolveFrameHeight(Texture2D texture, TextureAnimation animation) {
		if (animation == null) {
			return texture.height();
		}

		int hinted = animation.frameHeightHint();
		if (hinted > 0 && texture.height() % hinted == 0) {
			return hinted;
		}

		if (texture.width() > 0 && texture.height() % texture.width() == 0) {
			return texture.width();
		}

		int byIndex = animation.maxFrameIndex() + 1;
		if (byIndex > 1 && texture.height() % byIndex == 0) {
			return texture.height() / byIndex;
		}
		return texture.height();
	}

	private static int resolveFrameCount(Texture2D texture, TextureAnimation animation, int frameHeight) {
		int derivedByHeight = Math.max(1, texture.height() / Math.max(1, frameHeight));
		if (animation == null) {
			return derivedByHeight;
		}
		return Math.max(derivedByHeight, animation.maxFrameIndex() + 1);
	}

	private static Map<Integer, Integer> defaultParams() {
		Map<Integer, Integer> params = new HashMap<>();
		params.put(GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		params.put(GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		params.put(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		params.put(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		return params;
	}

    public int cellWidth() {
        return cellWidth;
    }

    public int cellHeight() {
        return cellHeight;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }
}
