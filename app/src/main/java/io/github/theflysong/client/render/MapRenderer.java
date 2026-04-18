package io.github.theflysong.client.render;

import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector4f;

import io.github.theflysong.gem.GemInstance;
import io.github.theflysong.level.GameMap;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

import java.util.List;
import java.util.Optional;

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

	// 地图渲染颜色配置
	public static final int SLOT_COLOR = 0xADB0C4;
	public static final int SLOT_SHADOW_COLOR = 0x9A9FB4;

	// 减少 90% 的暗度
	public static final float HIGHLIGHT_MASK = 0.1f;

	public static float applyHighlight(float colorComponent) {
		return 1 - (1 - colorComponent) * HIGHLIGHT_MASK;
	}

	public static Vector4f applyHighlight(Vector4f color) {
		return new Vector4f(
				applyHighlight(color.x),
				applyHighlight(color.y),
				applyHighlight(color.z),
				color.w
		);
	}

	public static final int CANVAS_COLOR = 0xCBCCD4;
	public static final int MATCH_PATH_COLOR = 0xFFE45A;

	private static Vector4f rgba(int hex) {
		float r = ((hex >> 16) & 0xFF) / 255.0f;
		float g = ((hex >> 8) & 0xFF) / 255.0f;
		float b = (hex & 0xFF) / 255.0f;
		return new Vector4f(r, g, b, 1.0f);
	}

	public MapRenderer() {
	}

	private GemRenderer gemRenderer() {
		return GemRenderer.instance();
	}

	private GeometryRenderer geometryRenderer() {
		return GeometryRenderer.instance();
	}

	public void renderSlot(Renderer renderer, Matrix4f modelMatrix, Layout layout) {
		renderSlot(renderer, modelMatrix, layout, false);
	}

	public void renderSlot(Renderer renderer, Matrix4f modelMatrix, Layout layout, boolean selected) {
		// 在此处看来, 槽位占据了[-1, 1]的整个区域
		// 绘制槽位底色
		Vector4f slotColor = rgba(SLOT_COLOR);
		Vector4f shadowColor = rgba(SLOT_SHADOW_COLOR);
		if (selected) {
			slotColor = applyHighlight(slotColor);
			shadowColor = applyHighlight(shadowColor);
		}
		geometryRenderer().renderRectangle(renderer, modelMatrix, slotColor);
		// 在该矩形的上1/16部分绘制阴影
		float shadowSize = 1 / 16.0f;
		// 位置位于矩形的上边界, 水平居中
		float shadowCenterY = 7.5f * shadowSize;
		geometryRenderer().renderRectangle(renderer, modelMatrix,
				0.0f, shadowCenterY, 1, shadowSize, shadowColor);
	}

	private void renderSelectionOverlay(Renderer renderer, Matrix4f modelMatrix) {
		geometryRenderer().renderRectangle(
				renderer,
				modelMatrix,
				0.0f,
				0.0f,
				0.92f,
				0.92f,
				new Vector4f(1.0f, 1.0f, 1.0f, 0.22f));
	}

	public void renderCanvas(Renderer renderer, Matrix4f modelMatrix) {
		geometryRenderer().renderRectangle(renderer, modelMatrix, rgba(CANVAS_COLOR));
	}

	public void renderMap(Renderer renderer, Matrix4f modelMatrix, GameMap map) {
		renderMap(renderer, modelMatrix, map, null);
	}

	public void renderMap(Renderer renderer, Matrix4f modelMatrix, GameMap map, Vector2i selectedCell) {
		renderMap(renderer, modelMatrix, map, selectedCell, null, 1.0f);
	}

	public void renderMap(Renderer renderer,
						 Matrix4f modelMatrix,
						 GameMap map,
						 Vector2i selectedCell,
						 List<Vector2i> matchPathPoints,
						 float matchPathAlpha) {
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
		renderCanvas(renderer, new Matrix4f(modelMatrix)
				.scale(layout.canvasWidth, layout.canvasHeight, 1.0f));
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float centerX = layout.startX + x * layout.step;
				float centerY = layout.startY - y * layout.step;
				boolean selected = selectedCell != null && selectedCell.x == x && selectedCell.y == y;
				Matrix4f newMatrix = new Matrix4f(modelMatrix)
						.translate(centerX, centerY, 0.0f)
						.scale(layout.gemSize, layout.gemSize, 1.0f);
				// 先绘制槽位背景
				renderSlot(renderer, newMatrix, layout, selected);

				// 然后绘制宝石
				GemInstance gem = map.gemAt(x, y);
				if (gem != null) {
					gemRenderer().renderGem(renderer, gem, newMatrix);
				}

				if (selected) {
					renderSelectionOverlay(renderer, newMatrix);
				}
			}
		}

		renderMatchPath(renderer, modelMatrix, layout, matchPathPoints, matchPathAlpha);
	}

	private void renderMatchPath(Renderer renderer,
							 Matrix4f modelMatrix,
							 Layout layout,
							 List<Vector2i> points,
							 float alpha) {
		if (points == null || points.size() < 2 || alpha <= 0.0f) {
			return;
		}

		float clampedAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
		Vector4f lineColor = rgba(MATCH_PATH_COLOR);
		lineColor.w = 0.15f + 0.85f * clampedAlpha;
		float thickness = layout.gemSize * 0.18f;

		for (int i = 0; i < points.size() - 1; i++) {
			Vector2i a = points.get(i);
			Vector2i b = points.get(i + 1);

			float ax = layout.startX + a.x * layout.step;
			float ay = layout.startY - a.y * layout.step;
			float bx = layout.startX + b.x * layout.step;
			float by = layout.startY - b.y * layout.step;

			float dx = bx - ax;
			float dy = by - ay;
			float length = (float) Math.sqrt(dx * dx + dy * dy);
			if (length <= 1.0e-6f) {
				continue;
			}

			float midX = (ax + bx) * 0.5f;
			float midY = (ay + by) * 0.5f;
			float angle = (float) Math.atan2(dy, dx);

			Matrix4f segment = new Matrix4f(modelMatrix)
					.translate(midX, midY, 0.0f)
					.rotateZ(angle)
					.scale(length + thickness * 0.35f, thickness, 1.0f);
			geometryRenderer().renderRectangle(renderer, segment, lineColor);
		}
	}

	/**
	 * 将一次 NDC 点击坐标映射到地图格子。
	 */
	public Optional<Vector2i> pickMapCell(GameMap map, float ndcX, float ndcY) {
		if (map == null) {
			throw new IllegalArgumentException("map must not be null");
		}

		int width = map.width();
		int height = map.height();
		if (width <= 0 || height <= 0) {
			return Optional.empty();
		}

		Layout layout = computeLayout(width, height);
		float half = layout.gemSize * 0.5f;

		float left = layout.startX - half;
		float right = layout.startX + (width - 1) * layout.step + half;
		float top = layout.startY + half;
		float bottom = layout.startY - (height - 1) * layout.step - half;

		if (ndcX < left || ndcX > right || ndcY > top || ndcY < bottom) {
			return Optional.empty();
		}

		int x = (int) Math.floor((ndcX - left) / layout.step);
		int y = (int) Math.floor((top - ndcY) / layout.step);
		if (x < 0 || x >= width || y < 0 || y >= height) {
			return Optional.empty();
		}

		float centerX = layout.startX + x * layout.step;
		float centerY = layout.startY - y * layout.step;
		if (Math.abs(ndcX - centerX) > half || Math.abs(ndcY - centerY) > half) {
			return Optional.empty();
		}

		return Optional.of(new Vector2i(x, y));
	}

	private Layout computeLayout(int mapWidth, int mapHeight) {
		// n * s + (n + 1) * (s / 8) = 2 => s = 16 / (9n + 1)
		float gemSizeByWidth = 16.0f / (9.0f * mapWidth + 1.0f);
		float gemSizeByHeight = 16.0f / (9.0f * mapHeight + 1.0f);
		float gemSize = Math.min(gemSizeByWidth, gemSizeByHeight);
		float gap = gemSize * GAP_RATIO;
		float step = gemSize + gap;

		float usedWidth = mapWidth * gemSize + (mapWidth + 1) * gap;
		float usedHeight = mapHeight * gemSize + (mapHeight + 1) * gap;

		// 在左右额外留出 1/2 gemSize 的空间, 用于绘制连接线贴图
		// 即一共再留出一个 gemSize 的空间, 水平居中分布在两侧
		float canvasWidth = usedWidth + gemSize;
		float canvasHeight = usedHeight + gemSize;

		float offsetX = (2.0f - usedWidth) * 0.5f;
		float offsetY = (2.0f - usedHeight) * 0.5f;

		float startX = NDC_MIN + offsetX + gap + gemSize * 0.5f;
		float startY = NDC_MAX - offsetY - gap - gemSize * 0.5f;
		return new Layout(gemSize, step, startX, startY, canvasWidth, canvasHeight);
	}

	private record Layout(float gemSize,
			float step,
			float startX,
			float startY,
			float canvasWidth,
			float canvasHeight) {
	}
}
