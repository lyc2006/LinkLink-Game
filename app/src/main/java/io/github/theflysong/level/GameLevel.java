package io.github.theflysong.level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.joml.Vector2i;

import io.github.theflysong.gem.GemInstance;
import io.github.theflysong.level.MatchResult;

/**
 * 游戏关卡
 *
 * @author theflysong
 * @date 2026年4月16日
 */
public class GameLevel {
     private GameMap gameMap;

    public GameLevel(int width, int height) {
        gameMap = new GameMap(width, height);
    }
    
    public boolean isGameOver(GameMap gameMap) {
        for(int i  = 0; i < gameMap.width(); i++) {
            for(int j = 0; j < gameMap.height(); j++) {
                if(gameMap.gems[i][j] != null) {
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

    public Optional<List<Vector2i>> oneCorner(Vector2i srcPos, Vector2i dstPos ) {
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

    public Optional<List<Vector2i>> twoCorners(Vector2i srcPos, Vector2i dstPos ) {
        for (int i = 0; i < gameMap.width(); i++) {
            Vector2i corner1 = new Vector2i(i, srcPos.y);
            Vector2i corner2 = new Vector2i(i, dstPos.y);
            if (gameMap.gemAt(corner1) == null && gameMap.gemAt(corner2) == null) {
                if (noCorner(srcPos, corner1) && noCorner(corner1, corner2) && noCorner(corner2, dstPos)) {
                    return Optional.of(List.of(corner1, corner2));
                }
            }
        }

        for (int j = 0; j < gameMap.height(); j++) {
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

    public MatchResult isMatch(Vector2i srcPos, Vector2i dstPos ) {
        GemInstance src = gameMap.gemAt(srcPos);
        GemInstance drc = gameMap.gemAt(dstPos);
        if (src == null || drc == null || !srcPos.equals(dstPos)) {
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
    
    public MatchResult tryMatch(Vector2i srcPos, Vector2i dstPos) {
        MatchResult result = isMatch(srcPos, dstPos);
        if (result.isMatch()) {
            gameMap.gems[srcPos.x][srcPos.y] = null;
            gameMap.gems[dstPos.x][dstPos.y] = null;
        }
        return result;
    }

}
