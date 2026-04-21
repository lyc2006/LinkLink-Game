package io.github.theflysong.input;

import io.github.theflysong.client.render.MapRenderer;
import io.github.theflysong.level.GameMap;
import io.github.theflysong.level.GameLevel;
import io.github.theflysong.level.MatchResult;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Supplier;

import static io.github.theflysong.App.LOGGER;

/**
 * 地图输入处理器：将屏幕点击映射为地图格子坐标。
 *
 * @author theflysong
 * @date 2026年4月19日
 */
public class GameMapInputHandler {
	private static final long MATCH_PATH_DURATION_NANOS = 350_000_000L;

	private final Supplier<GameLevel> gameLevelSupplier;
	private final MapRenderer mapRenderer;
	private Vector2i firstSelection;
	private List<Vector2i> activeMatchPath;
	private long activeMatchPathExpireAtNanos;

	public Optional<Vector2i> currentSelection() {
		if (firstSelection == null) {
			return Optional.empty();
		}
		return Optional.of(new Vector2i(firstSelection));
	}

	public Optional<MatchPathEffect> currentMatchPath() {
		if (activeMatchPath == null || activeMatchPath.isEmpty()) {
			return Optional.empty();
		}
		long now = System.nanoTime();
		if (now >= activeMatchPathExpireAtNanos) {
			activeMatchPath = null;
			activeMatchPathExpireAtNanos = 0L;
			return Optional.empty();
		}
		float alpha = (float) (activeMatchPathExpireAtNanos - now) / MATCH_PATH_DURATION_NANOS;
		alpha = Math.max(0.0f, Math.min(1.0f, alpha));
		return Optional.of(new MatchPathEffect(copyPoints(activeMatchPath), alpha));
	}

	public GameMapInputHandler(Supplier<GameLevel> gameLevelSupplier, MapRenderer mapRenderer) {
		this.gameLevelSupplier = Objects.requireNonNull(gameLevelSupplier, "gameLevelSupplier must not be null");
		this.mapRenderer = Objects.requireNonNull(mapRenderer, "mapRenderer must not be null");
	}

	public boolean handleClick(Vector2f nc, MouseInputContext context) {
		if (!context.isLeftPress()) {
			return false;
		}

		GameLevel gameLevel = gameLevelSupplier.get();
		if (gameLevel == null) {
			return false;
		}
		GameMap gameMap = gameLevel.gameMap();

		LOGGER.info("Handle map click at normalized coordinates ({}, {})", nc.x, nc.y);
		Optional<Vector2i> cell = mapRenderer.pickMapCell(gameMap, nc);
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
			LOGGER.info("First select cell=({}, {}), cursor=({}, {})",
					coord.x,
					coord.y,
					String.format("%.1f", context.cursorX()),
					String.format("%.1f", context.cursorY()));
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
			cacheMatchPath(previous, coord, result);
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

	private void cacheMatchPath(Vector2i start, Vector2i end, MatchResult result) {
		List<Vector2i> points = new ArrayList<>();
		points.add(new Vector2i(start));
		for (Vector2i corner : result.getCorners()) {
			points.add(new Vector2i(corner));
		}
		points.add(new Vector2i(end));
		activeMatchPath = points;
		activeMatchPathExpireAtNanos = System.nanoTime() + MATCH_PATH_DURATION_NANOS;
	}

	private static List<Vector2i> copyPoints(List<Vector2i> points) {
		List<Vector2i> copy = new ArrayList<>(points.size());
		for (Vector2i point : points) {
			copy.add(new Vector2i(point));
		}
		return copy;
	}

	public record MatchPathEffect(List<Vector2i> points, float alpha) {
	}
}
