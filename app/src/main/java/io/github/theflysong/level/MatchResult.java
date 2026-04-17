package io.github.theflysong.level;

import org.joml.Vector2i;
import java.util.List;

public class MatchResult {
    private boolean isMatch;
    private List<Vector2i> corners;
    public MatchResult(boolean isMatch, List<Vector2i> corners) {
        this.isMatch = isMatch;
        this.corners = corners;
    }
    public boolean isMatch() {
        return isMatch;
    }
    public List<Vector2i> getCorners() {
        return corners;
    }
    public static MatchResult fail() {
        return new MatchResult(false, List.of());
    } 

    public static MatchResult success(List<Vector2i> corners) {
        return new MatchResult(true, corners);
    }
}