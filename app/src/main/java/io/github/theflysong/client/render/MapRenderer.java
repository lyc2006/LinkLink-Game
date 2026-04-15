package io.github.theflysong.client.render;

import org.joml.Matrix4f;

import io.github.theflysong.gem.GemInstance;
import io.github.theflysong.level.GameMap;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * 地图渲染器。
 *
 * 布局规则：
 * 1. 将整张地图映射到 NDC 的 [-1, 1]^2。
 * 2. 宝石之间与地图边界的间隙均为宝石尺寸的 1/8。
 *
 * @author theflysong
 * @date 2026年4月16日
 */
@SideOnly(Side.CLIENT)
public class MapRenderer {
	private static final float NDC_MIN = -1.0f;
	private static final float NDC_MAX = 1.0f;
	private static final float GAP_RATIO = 1.0f / 8.0f;

	private final GemRenderer gemRenderer;

	public MapRenderer() {
		this(GemRenderer.instance());
	}

	public MapRenderer(GemRenderer gemRenderer) {
		if (gemRenderer == null) {
			throw new IllegalArgumentException("gemRenderer must not be null");
		}
		this.gemRenderer = gemRenderer;
	}

	public void renderMap(Renderer renderer, GameMap map) {
		if (renderer == null) {
			throw new IllegalArgumentException("renderer must not be null");
		}
		if (map == null) {
			throw new IllegalArgumentException("map must not be null");
		}

		int width = map.width();
		int height = map.height();
		if (width <= 0 || height <= 0) {
			return;
		}

		Layout layout = computeLayout(width, height);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				GemInstance gem = map.gemAt(x, y);
				if (gem == null) {
					continue;
				}

				float centerX = layout.startX + x * layout.step;
				float centerY = layout.startY - y * layout.step;
				Matrix4f modelMatrix = new Matrix4f()
						.identity()
						.translate(centerX, centerY, 0.0f)
						.scale(layout.gemSize, layout.gemSize, 1.0f);
				gemRenderer.renderGem(renderer, gem, modelMatrix);
			}
		}
	}

	private Layout computeLayout(int mapWidth, int mapHeight) {
		// n * s + (n + 1) * (s / 8) = 2  =>  s = 16 / (9n + 1)
		float gemSizeByWidth = 16.0f / (9.0f * mapWidth + 1.0f);
		float gemSizeByHeight = 16.0f / (9.0f * mapHeight + 1.0f);
		float gemSize = Math.min(gemSizeByWidth, gemSizeByHeight);
		float gap = gemSize * GAP_RATIO;
		float step = gemSize + gap;

		float usedWidth = mapWidth * gemSize + (mapWidth + 1) * gap;
		float usedHeight = mapHeight * gemSize + (mapHeight + 1) * gap;
		float offsetX = (2.0f - usedWidth) * 0.5f;
		float offsetY = (2.0f - usedHeight) * 0.5f;

		float startX = NDC_MIN + offsetX + gap + gemSize * 0.5f;
		float startY = NDC_MAX - offsetY - gap - gemSize * 0.5f;
		return new Layout(gemSize, step, startX, startY);
	}

	private record Layout(float gemSize, float step, float startX, float startY) {
	}
}
