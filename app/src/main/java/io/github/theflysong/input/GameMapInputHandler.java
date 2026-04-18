package io.github.theflysong.input;

import io.github.theflysong.client.render.MapRenderer;
import io.github.theflysong.level.GameMap;
import io.github.theflysong.level.GameLevel;
import io.github.theflysong.level.MatchResult;
import org.joml.Vector2i;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static io.github.theflysong.App.LOGGER;

/**
 * 地图输入处理器：将屏幕点击映射为地图格子坐标。
 *
 * @author theflysong
 * @date 2026年4月19日
 */
public class GameMapInputHandler {
	private final Supplier<GameLevel> gameLevelSupplier;
	private final MapRenderer mapRenderer;
	private Vector2i firstSelection;

	public GameMapInputHandler(Supplier<GameLevel> gameLevelSupplier, MapRenderer mapRenderer) {
		this.gameLevelSupplier = Objects.requireNonNull(gameLevelSupplier, "gameLevelSupplier must not be null");
		this.mapRenderer = Objects.requireNonNull(mapRenderer, "mapRenderer must not be null");
	}

	/**
	 * @return true 表示点击命中了地图格子并已消费输入。
	 */
	public boolean handle(MouseInputContext context) {
		if (!context.isLeftPress()) {
			return false;
		}

		GameLevel gameLevel = gameLevelSupplier.get();
		if (gameLevel == null) {
			return false;
		}
		GameMap gameMap = gameLevel.gameMap();

		float aspect = context.windowHeight() > 0
				? (float) context.windowWidth() / (float) context.windowHeight()
				: 1.0f;
		float mapSpaceX = context.ndcX() * aspect;
		float mapSpaceY = context.ndcY();

		Optional<Vector2i> cell = mapRenderer.pickMapCell(gameMap, mapSpaceX, mapSpaceY);
		if (cell.isEmpty()) {
			return false;
		}

		Vector2i coord = cell.get();
		if (gameMap.gemAt(coord) == null) {
			firstSelection = null;
			LOGGER.info("Click empty cell=({}, {}), reset selection", coord.x, coord.y);
			return true;
		}

		if (firstSelection == null) {
			firstSelection = new Vector2i(coord);
			LOGGER.info("First select cell=({}, {}), cursor=({}, {}), mapSpace=({}, {})",
					coord.x,
					coord.y,
					String.format("%.1f", context.cursorX()),
					String.format("%.1f", context.cursorY()),
					String.format("%.3f", mapSpaceX),
					String.format("%.3f", mapSpaceY));
			return true;
		}

		if (firstSelection.equals(coord)) {
			firstSelection = null;
			LOGGER.info("Cancel selection at same cell=({}, {})", coord.x, coord.y);
			return true;
		}

		Vector2i previous = new Vector2i(firstSelection);
		MatchResult result = gameLevel.tryMatch(previous, coord);
		if (result.isMatch()) {
			LOGGER.info("Match success: ({}, {}) <-> ({}, {}), corners={} ",
					previous.x,
					previous.y,
					coord.x,
					coord.y,
					result.getCorners().size());
			firstSelection = null;
			if (gameLevel.isGameOver()) {
				LOGGER.info("Game over: all gems removed.");
			}
			return true;
		}

		firstSelection = new Vector2i(coord);
		LOGGER.info("Match failed: ({}, {}) -> ({}, {}), switch selection to second cell",
				previous.x,
				previous.y,
				coord.x,
				coord.y);
		return true;
	}
}
