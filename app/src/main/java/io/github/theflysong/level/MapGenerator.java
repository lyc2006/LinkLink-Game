package io.github.theflysong.level;

import io.github.theflysong.gem.Gem;
import io.github.theflysong.gem.GemColor;
import io.github.theflysong.gem.GemInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 关卡生成器，负责根据一定的规则生成游戏地图。
 *
 * @author theflysong
 * @date 2026年4月19日
 */
public class MapGenerator {
	public GameMap generate(MapGenConfiguration configuration) {
		Objects.requireNonNull(configuration, "configuration must not be null");
		return configuration.preset()
				? generatePreset(configuration)
				: generateRandom(configuration);
	}

	public GameMap generate(String levelPathOrName) {
		return generate(MapGenConfiguration.load(levelPathOrName));
	}

	public GameMap generateSimple() {
		return generate(MapGenConfiguration.loadSimple());
	}

	public GameMap generatePreset() {
		return generate(MapGenConfiguration.loadPreset());
	}

	private GameMap generateRandom(MapGenConfiguration configuration) {
		int width = configuration.width();
		int height = configuration.height();
		int cells = width * height;

		List<Gem> gemTypes = configuration.gemTypes();
		if (gemTypes.isEmpty()) {
			throw new IllegalArgumentException("Random level requires non-empty gemTypes");
		}

		int colorLimit = configuration.gemColors();
		GemColor[] allColors = GemColor.values();
		if (colorLimit <= 0 || colorLimit > allColors.length) {
			throw new IllegalArgumentException("Invalid gemColors: " + colorLimit);
		}

		List<GemColor> availableColors = new ArrayList<>(colorLimit);
		for (int i = 0; i < colorLimit; i++) {
			availableColors.add(allColors[i]);
		}

		List<GemInstance> pool = new ArrayList<>(cells);
		int pairCount = cells / 2;
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i < pairCount; i++) {
			Gem gem = gemTypes.get(random.nextInt(gemTypes.size()));
			GemColor color = availableColors.get(random.nextInt(availableColors.size()));
			GemInstance instance = new GemInstance(gem, color);
			pool.add(instance);
			pool.add(instance);
		}
		Collections.shuffle(pool, random);

		GemInstance[][] gems = new GemInstance[width][height];
		int index = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				gems[x][y] = pool.get(index++);
			}
		}
		return new GameMap(gems, width, height);
	}

	private GameMap generatePreset(MapGenConfiguration configuration) {
		int width = configuration.width();
		int height = configuration.height();
		int[][] presetMap = configuration.presetMap();
		Map<Integer, GemInstance> gemById = configuration.presetGemById();

		if (presetMap == null) {
			throw new IllegalArgumentException("Preset level requires map data");
		}
		if (gemById.isEmpty()) {
			throw new IllegalArgumentException("Preset level requires preset gem definitions");
		}

		GemInstance[][] gems = new GemInstance[width][height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int id = presetMap[y][x];
				GemInstance instance = gemById.get(id);
				if (instance == null) {
					throw new IllegalArgumentException("Preset map references unknown gem id: " + id);
				}
				gems[x][y] = instance;
			}
		}
		return new GameMap(gems, width, height);
	}
}
