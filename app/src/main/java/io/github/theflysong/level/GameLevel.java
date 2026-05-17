package io.github.theflysong.level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import org.joml.Vector4i;
import org.joml.Vector2i;
import org.jspecify.annotations.NonNull;

import io.github.theflysong.bars.Bars;
import io.github.theflysong.bars.EnergyBar;
import io.github.theflysong.gem.GemInstance;
import io.github.theflysong.level.MatchResult;
import io.github.theflysong.level.TipResult;

/**
 * 游戏关卡
 *
 * @author theflysong
 * @date 2026年4月16日
 */
public class GameLevel {
    private static final long COMBO_WINDOW_NANOS = 1_500_000_000L;

    private final GameMap gameMap;
    private final EnergyBar energyBar;
    private TipResult lastAllTipResult = TipResult.noTip();
    private Vector4i lastTippedCell = null;
    private int score = 0;
    private long levelStartTime = 0;
    private long lastMatchTime = 0;
    private int comboCount = 0;
    private long pauseStartNanos = 0;

    public GameLevel(GameMap gameMap) {
        this.gameMap = Objects.requireNonNull(gameMap, "gameMap must not be null");
        this.energyBar = Objects.requireNonNull(Bars.TOTAL.get(), "energyBar must not be null");
    }

    public void startLevel() {
        levelStartTime = System.nanoTime();
        lastMatchTime = 0;
        comboCount = 0;
        pauseStartNanos = 0;
    }

    public void pauseLevel() {
        if (pauseStartNanos == 0) {
            pauseStartNanos = System.nanoTime();
        }
    }

    public void resumeLevel() {
        if (pauseStartNanos != 0) {
            long pauseDuration = System.nanoTime() - pauseStartNanos;
            levelStartTime += pauseDuration;
            if (lastMatchTime > 0) {
                lastMatchTime += pauseDuration;
            }
            pauseStartNanos = 0;
        }
    }

    public int score() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
    
    public void setElapsedSeconds(long seconds) {
        this.levelStartTime = System.nanoTime() - seconds * 1_000_000_000L;
    }

    public long elapsedSeconds() {
        if (levelStartTime == 0) {
            return 0;
        }
        long now = pauseStartNanos != 0 ? pauseStartNanos : System.nanoTime();
        return (now - levelStartTime) / 1_000_000_000;
    }

    public int comboCount() {
        return comboCount;
    }

    public GameMap gameMap() {
        return gameMap;
    }

    public @NonNull EnergyBar energyBar() {
        return energyBar;
    }

    public boolean isGameOver() {
        return isGameOver(gameMap);
    }

    public boolean isGameOver(GameMap gameMap) {
        for (int i = 0; i < gameMap.width(); i++) {
            for (int j = 0; j < gameMap.height(); j++) {
                if (gameMap.gems[i][j] != null) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean noCorner(Vector2i srcPos, Vector2i dstPos) {
        boolean flag = true;
        if (srcPos.x != dstPos.x && srcPos.y != dstPos.y) {
            return false;
        }
        if (srcPos.x == dstPos.x) {
            for (int i = Math.min(srcPos.y, dstPos.y) + 1; i < Math.max(srcPos.y, dstPos.y); i++) {
                if (gameMap.gemAt(new Vector2i(srcPos.x, i)) != null) {
                    flag = false;
                    break;
                }
            }
        }
        if (srcPos.y == dstPos.y) {
            for (int i = Math.min(srcPos.x, dstPos.x) + 1; i < Math.max(srcPos.x, dstPos.x); i++) {
                if (gameMap.gemAt(new Vector2i(i, srcPos.y)) != null) {
                    flag = false;
                    break;
                }
            }
        }
        return flag;
    }

    public Optional<List<Vector2i>> oneCorner(Vector2i srcPos, Vector2i dstPos) {
        Vector2i corner1 = new Vector2i(srcPos.x, dstPos.y);
        Vector2i corner2 = new Vector2i(dstPos.x, srcPos.y);

        if (gameMap.gemAt(corner1) == null) {
            if (noCorner(srcPos, corner1) && noCorner(corner1, dstPos)) {
                return Optional.of(List.of(corner1));
            }
        }

        if (gameMap.gemAt(corner2) == null) {
            if (noCorner(srcPos, corner2) && noCorner(corner2, dstPos)) {
                return Optional.of(List.of(corner2));
            }
        }
        return Optional.empty();
    }

    public Optional<List<Vector2i>> twoCorners(Vector2i srcPos, Vector2i dstPos) {
        int midX = (srcPos.x + dstPos.x) / 2;
        for (int i = midX; i >= -1; i--) {
            Vector2i corner1 = new Vector2i(i, srcPos.y);
            Vector2i corner2 = new Vector2i(i, dstPos.y);
            if (gameMap.gemAt(corner1) == null && gameMap.gemAt(corner2) == null) {
                if (noCorner(srcPos, corner1) && noCorner(corner1, corner2) && noCorner(corner2, dstPos)) {
                    return Optional.of(List.of(corner1, corner2));
                }
            }
        }
        for(int i = midX + 1; i <= gameMap.width(); i++) {
            Vector2i corner1 = new Vector2i(i, srcPos.y);
            Vector2i corner2 = new Vector2i(i, dstPos.y);
            if (gameMap.gemAt(corner1) == null && gameMap.gemAt(corner2) == null) {
                if (noCorner(srcPos, corner1) && noCorner(corner1, corner2) && noCorner(corner2, dstPos)) {
                    return Optional.of(List.of(corner1, corner2));
                }
            }
        }
        int midY = (srcPos.y + dstPos.y) / 2;
        for (int j = midY; j >= -1; j--) {
            Vector2i corner1 = new Vector2i(srcPos.x, j);
            Vector2i corner2 = new Vector2i(dstPos.x, j);
            if (gameMap.gemAt(corner1) == null && gameMap.gemAt(corner2) == null) {
                if (noCorner(srcPos, corner1) && noCorner(corner1, corner2) && noCorner(corner2, dstPos)) {
                    return Optional.of(List.of(corner1, corner2));
                }
            }
        }
        for(int j = midY + 1; j <= gameMap.height(); j++) {
            Vector2i corner1 = new Vector2i(srcPos.x, j);
            Vector2i corner2 = new Vector2i(dstPos.x, j);
            if (gameMap.gemAt(corner1) == null && gameMap.gemAt(corner2) == null) {
                if (noCorner(srcPos, corner1) && noCorner(corner1, corner2) && noCorner(corner2, dstPos)) {
                    return Optional.of(List.of(corner1, corner2));
                }
            }
        }

        return Optional.empty();
    }

    public MatchResult isMatch(Vector2i srcPos, Vector2i dstPos) {
        GemInstance src = gameMap.gemAt(srcPos);
        GemInstance drc = gameMap.gemAt(dstPos);
        if (src == null || drc == null || srcPos.equals(dstPos)) {
            return MatchResult.fail();
        }
        if (!src.equals(drc)) {
            return MatchResult.fail();
        }
        if (noCorner(srcPos, dstPos)) {
            return MatchResult.success(new ArrayList<>());
        }
        Optional<List<Vector2i>> corner = oneCorner(srcPos, dstPos);
        if (corner.isPresent()) {
            return MatchResult.success(corner.get());
        }
        Optional<List<Vector2i>> corners = twoCorners(srcPos, dstPos);
        if (corners.isPresent()) {
            return MatchResult.success(corners.get());
        }
        return MatchResult.fail();
    }

    public boolean isCurrentTip(Vector4i tip, Vector2i pos) {
        return tip != null && ((tip.x == pos.x && tip.y == pos.y) || (tip.z == pos.x && tip.w == pos.y));
    }

    public MatchResult tryMatch(Vector2i srcPos, Vector2i dstPos) {
        MatchResult result = isMatch(srcPos, dstPos);
        if (result.isMatch()) {
            GemInstance srcGem = gameMap.gemAt(srcPos);
            GemInstance dstGem = gameMap.gemAt(dstPos);
            int baseScore = 0;
            if (srcGem != null) baseScore += srcGem.gem().scoreValue();
            if (dstGem != null) baseScore += dstGem.gem().scoreValue();

            long now = System.nanoTime();
            if (lastMatchTime > 0 && (now - lastMatchTime) < COMBO_WINDOW_NANOS) {
                comboCount++;
            } else {
                comboCount = 0;
            }
            lastMatchTime = now;

            score += baseScore;
            if (comboCount > 0) {
                score += (int) (comboCount * baseScore * 0.7);
            }

            destroyGemAt(srcPos);
            destroyGemAt(dstPos);
            if (isCurrentTip(lastTippedCell, srcPos) || isCurrentTip(lastTippedCell, dstPos)) {
                lastTippedCell = null;
            }
        }
        return result;
    }

    public void destroyGemAt(Vector2i pos) {
        GemInstance gem = gameMap.gemAt(pos);
        if (gem != null) {
            gem.gem().onDestroy(this, gem);
            gameMap.gems[pos.x][pos.y] = null;
        }
    }

    public TipResult tip() {
        Set<Vector4i> tips = new HashSet<>();

        gameMap.foreach((srcGem, srcCoord) -> {
            gameMap.foreach((dstGem, dstCoord) -> {
                if (isMatch(srcCoord, dstCoord).isMatch()) {
                    tips.add(new Vector4i(srcCoord.x, srcCoord.y, dstCoord.x, dstCoord.y));
                }
            });
        });
        tips.removeIf(tip -> (tip.x > tip.z || (tip.x == tip.z && tip.y > tip.w)));
        return (tips.isEmpty() ? TipResult.noTip() : TipResult.withTip(tips));
    }

    public TipResult lastTipResult() {
        return lastAllTipResult;
    }

    public void setLastTipResult(TipResult lastTipResult) {
        this.lastAllTipResult = lastTipResult;
    }

    public Vector4i lastTippedCell() {
        return lastTippedCell;
    }

    public void setLastTippedCell(Vector4i lastTippedCell) {
        this.lastTippedCell = lastTippedCell;
    }

    public void updateTips(int energyCost) {
        if (!energyBar.useEnergy(energyCost)) {
            return;
        }
        setLastTipResult(tip());
        // choose a random tip from the available tips
        if (!lastAllTipResult.tips().isEmpty()) {
            List<Vector4i> tips = new ArrayList<>(lastAllTipResult.tips());
            if (tips.contains(lastTippedCell)) {
                tips.remove(lastTippedCell);
            }
            Collections.shuffle(tips);
            setLastTippedCell(tips.get(0));
        } else {
            setLastTippedCell(null);
        }
    }

    public void refreshMap(int energyCost) {
        if(!energyBar.useEnergy(energyCost)) {
            return;
        }
        gameMap.refreshMap();
        setLastTipResult(TipResult.noTip());
        setLastTippedCell(null);
    }

}
