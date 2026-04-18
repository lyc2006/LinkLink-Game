package io.github.theflysong.level;

import org.joml.Vector2i;

import io.github.theflysong.gem.GemInstance;

import java.util.Objects;

/**
 * 游戏地图
 *
 * @author theflysong
 * @date 2026年4月16日
 */
public class GameMap
{
    protected GemInstance[][] gems;
    protected int width;
    protected int height;

    public GameMap(GemInstance[][] gems, int width, int height) {
        Objects.requireNonNull(gems, "gems must not be null");
        if (width * height % 2 != 0) {
            throw new IllegalArgumentException("地图的格子数量必须为偶数");
        }
        if (gems.length != width) {
            throw new IllegalArgumentException("地图数组宽度与 width 不一致");
        }
        for (int x = 0; x < width; x++) {
            if (gems[x] == null || gems[x].length != height) {
                throw new IllegalArgumentException("地图数组高度与 height 不一致");
            }
        }
        this.width = width;
        this.height = height;
        this.gems = new GemInstance[width][height];
        for (int x = 0; x < width; x++) {
            System.arraycopy(gems[x], 0, this.gems[x], 0, height);
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public GemInstance gemAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Invalid map coordinate: (" + x + ", " + y + ")");
        }
        return gems[x][y];
    }

    public GemInstance gemAt(Vector2i pos) {
        if (pos.x < 0 || pos.x >= width || pos.y < 0 || pos.y >= height) {
            throw new IndexOutOfBoundsException("Invalid map coordinate: (" + pos.x + ", " + pos.y + ")");
        }
        return gems[pos.x][pos.y];
    }
}
