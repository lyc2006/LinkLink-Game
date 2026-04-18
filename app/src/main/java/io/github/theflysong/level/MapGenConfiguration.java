package io.github.theflysong.level;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import io.github.theflysong.App;
import io.github.theflysong.data.Identifier;
import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceLoader;
import io.github.theflysong.data.ResourceType;
import io.github.theflysong.gem.Gem;
import io.github.theflysong.gem.GemColor;
import io.github.theflysong.gem.GemInstance;
import io.github.theflysong.gem.Gems;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 地图宝石生成配置。
 */
public class MapGenConfiguration {
    private static final Gson GSON = new Gson();

    private final String name;
    private final int width;
    private final int height;
    private final int gemColors;
    private final List<Gem> gemTypes;
    private final boolean preset;
    private final int[][] presetMap;
    private final Map<Integer, GemInstance> presetGemById;

    protected MapGenConfiguration(String name,
                                  int width,
                                  int height,
                                  int gemColors,
                                  List<Gem> gemTypes,
                                  boolean preset,
                                  int[][] presetMap,
                                  Map<Integer, GemInstance> presetGemById) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.width = width;
        this.height = height;
        this.gemColors = gemColors;
        this.gemTypes = List.copyOf(gemTypes);
        this.preset = preset;
        this.presetMap = presetMap;
        this.presetGemById = presetGemById == null ? Map.of() : Map.copyOf(presetGemById);
    }

    public static MapGenConfiguration load(String levelPathOrName) {
        Objects.requireNonNull(levelPathOrName, "levelPathOrName must not be null");
        String normalized = normalizeLevelPath(levelPathOrName);
        ResourceLocation resourceLocation = new ResourceLocation(App.APPID, ResourceType.LEVEL, normalized + ".json");
        String resourcePath = resourceLocation.toPath();

        String json;
        try (InputStream stream = ResourceLoader.loadFile(resourceLocation)) {
            if (stream == null) {
                throw new IllegalArgumentException("Level config not found: " + resourcePath);
            }
            json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read level config: " + resourcePath, ex);
        }

        RawConfig raw;
        try {
            raw = GSON.fromJson(json, RawConfig.class);
        } catch (JsonParseException ex) {
            throw new IllegalArgumentException("Invalid level config json: " + resourcePath, ex);
        }
        if (raw == null) {
            throw new IllegalArgumentException("Empty level config json: " + resourcePath);
        }

        validateBasic(raw, resourcePath);
        List<Gem> parsedGemTypes = parseGemTypes(raw.gemTypes, resourcePath);

        if (!raw.preset) {
            return new MapGenConfiguration(
                    raw.name,
                    raw.width,
                    raw.height,
                    raw.gemColors,
                    parsedGemTypes,
                    false,
                    null,
                    null);
        }

        validatePreset(raw, resourcePath);
        Map<Integer, GemInstance> gemById = parsePresetGems(raw.presetGems, resourcePath);
        int[][] map = parsePresetMap(raw.map, raw.width, raw.height, gemById, resourcePath);

        return new MapGenConfiguration(
                raw.name,
                raw.width,
                raw.height,
                raw.gemColors,
                parsedGemTypes,
                true,
                map,
                gemById);
    }

    public static MapGenConfiguration loadSimple() {
        return load("simple");
    }

    public static MapGenConfiguration loadPreset() {
        return load("preset");
    }

    public String name() {
        return name;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int gemColors() {
        return gemColors;
    }

    public List<Gem> gemTypes() {
        return gemTypes;
    }

    public boolean preset() {
        return preset;
    }

    public int[][] presetMap() {
        if (presetMap == null) {
            return null;
        }
        int[][] copy = new int[presetMap.length][];
        for (int i = 0; i < presetMap.length; i++) {
            copy[i] = presetMap[i].clone();
        }
        return copy;
    }

    public Map<Integer, GemInstance> presetGemById() {
        return presetGemById;
    }

    private static void validateBasic(RawConfig raw, String resourcePath) {
        if (raw.name == null || raw.name.isBlank()) {
            throw new IllegalArgumentException("Missing 'name' in level config: " + resourcePath);
        }
        if (raw.width <= 0 || raw.height <= 0) {
            throw new IllegalArgumentException("Invalid map size in level config: " + resourcePath);
        }
        if ((raw.width * raw.height) % 2 != 0) {
            throw new IllegalArgumentException("Map cell count must be even: " + resourcePath);
        }
        if (raw.gemColors <= 0 || raw.gemColors > GemColor.values().length) {
            throw new IllegalArgumentException("Invalid gemColors in level config: " + resourcePath);
        }
        if (raw.gemTypes == null || raw.gemTypes.isEmpty()) {
            throw new IllegalArgumentException("Missing 'gemTypes' in level config: " + resourcePath);
        }
    }

    private static void validatePreset(RawConfig raw, String resourcePath) {
        if (raw.presetGems == null || raw.presetGems.isEmpty()) {
            throw new IllegalArgumentException("Missing 'presetGems' in preset level config: " + resourcePath);
        }
        if (raw.map == null || raw.map.isEmpty()) {
            throw new IllegalArgumentException("Missing 'map' in preset level config: " + resourcePath);
        }
    }

    private static List<Gem> parseGemTypes(List<String> gemTypes, String resourcePath) {
        List<Gem> result = new ArrayList<>(gemTypes.size());
        for (String gemType : gemTypes) {
            Identifier id = parseIdentifier(gemType);
            Gem gem = Gems.GEMS.get(id)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown gem type in level config: " + gemType + ", file=" + resourcePath));
            result.add(gem);
        }
        return result;
    }

    private static Map<Integer, GemInstance> parsePresetGems(List<RawPresetGem> presetGems, String resourcePath) {
        Map<Integer, GemInstance> result = new HashMap<>();
        for (RawPresetGem presetGem : presetGems) {
            if (presetGem == null) {
                throw new IllegalArgumentException("Null preset gem in level config: " + resourcePath);
            }
            if (result.containsKey(presetGem.id)) {
                throw new IllegalArgumentException("Duplicate preset gem id=" + presetGem.id + " in " + resourcePath);
            }
            Gem gem = Gems.GEMS.get(parseIdentifier(presetGem.type))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown preset gem type: " + presetGem.type + ", file=" + resourcePath));
            GemColor color = parseColor(presetGem.color, resourcePath);
            result.put(presetGem.id, new GemInstance(gem, color));
        }
        return result;
    }

    private static int[][] parsePresetMap(List<List<Integer>> mapRows,
                                          int width,
                                          int height,
                                          Map<Integer, GemInstance> gemById,
                                          String resourcePath) {
        if (mapRows.size() != height) {
            throw new IllegalArgumentException("Preset map row count does not match height in " + resourcePath);
        }

        int[][] map = new int[height][width];
        for (int y = 0; y < height; y++) {
            List<Integer> row = mapRows.get(y);
            if (row == null || row.size() != width) {
                throw new IllegalArgumentException("Preset map column count does not match width in " + resourcePath);
            }
            for (int x = 0; x < width; x++) {
                Integer gemId = row.get(x);
                if (gemId == null || !gemById.containsKey(gemId)) {
                    throw new IllegalArgumentException("Unknown preset gem id=" + gemId + " at (" + x + ", " + y + ") in " + resourcePath);
                }
                map[y][x] = gemId;
            }
        }
        return map;
    }

    private static GemColor parseColor(String value, String resourcePath) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing preset gem color in " + resourcePath);
        }
        try {
            return GemColor.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown gem color: " + value + ", file=" + resourcePath, ex);
        }
    }

    private static Identifier parseIdentifier(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Invalid empty identifier");
        }
        int sep = value.indexOf(':');
        if (sep > 0 && sep < value.length() - 1) {
            return new Identifier(value.substring(0, sep), value.substring(sep + 1));
        }
        return new Identifier(value);
    }

    private static String normalizeLevelPath(String raw) {
        String value = raw.trim();
        if (value.endsWith(".json")) {
            value = value.substring(0, value.length() - 5);
        }
        if (value.startsWith("data/")) {
            String prefix = "data/" + App.APPID + "/level/";
            if (value.startsWith(prefix)) {
                return value.substring(prefix.length());
            }
        }
        return value;
    }

    private static final class RawConfig {
        String name;
        int width;
        int height;
        int gemColors;
        List<String> gemTypes;
        boolean preset;
        List<RawPresetColor> presetColors;
        List<RawPresetType> presetTypes;
        List<RawPresetGem> presetGems;
        List<List<Integer>> map;
    }

    private static final class RawPresetColor {
        String color;
        int id;
    }

    private static final class RawPresetType {
        String type;
        int id;
    }

    private static final class RawPresetGem {
        String color;
        String type;
        int id;
    }
}
