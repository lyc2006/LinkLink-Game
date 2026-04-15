package io.github.theflysong.level;

import org.joml.Vector2i;

import io.github.theflysong.gem.GemColor;
import io.github.theflysong.gem.GemInstance;
import io.github.theflysong.gem.Gems;
import reactor.util.annotation.NonNull;
import io.github.theflysong.gem.Gem;

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

    public GameMap(int width, int height) {
        if (width * height % 2 != 0) {
            throw new IllegalArgumentException("地图的格子数量必须为偶数");
        }
        this.width = width;
        this.height = height;
        gems = new GemInstance[width][height];
        generateMap();
    }

    private void generateMap()
    {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (gems[x][y] != null) {
                    continue; // 已经有宝石了，跳过
                }
                GemInstance instance = randomGemInstance();
                gems[x][y] = instance;
                Vector2i coord;
                do {
                    coord = randomCoord();
                } while (gems[coord.x][coord.y] != null);
                gems[coord.x][coord.y] = instance;
            }
        }
    }

    private Vector2i randomCoord()
    {
        int x = (int) (Math.random() * width);
        int y = (int) (Math.random() * height);
        return new Vector2i(x, y);
    }

    private GemInstance randomGemInstance()
    {
        return new GemInstance(randomGem(), randomGemColor());
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

    @NonNull
    private GemColor randomGemColor()
    {
        // 选择一个随机的颜色
        final int colorCount = GemColor.values().length;
        int index = (int) (Math.random() * colorCount);
        return GemColor.values()[index];
    }

    @NonNull
    private Gem randomGem()
    {
        // 选择一个随机的宝石
        // 首先在 [0, 7) 中随机一个整数，分别对应 7 种宝石类型
        final int gemTypeCount = 7;
        return switch ((int) (Math.random() * gemTypeCount)) {
            case 0 -> Gems.DIAMOND.get();
            case 1 -> Gems.JADE.get();
            case 2 -> Gems.SAPPHIRE.get();
            case 3 -> Gems.CHIPPED.get();
            case 4 -> Gems.FLAWED.get();
            case 5 -> Gems.FLAWLESS.get();
            case 6 -> Gems.EXQUISITE.get();
            default -> throw new IllegalStateException("Unexpected random gem type index");
        };
    }
}
