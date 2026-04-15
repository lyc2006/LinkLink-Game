package io.github.theflysong.gem;

import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

/**
 * 宝石颜色
 *
 * @author theflysong
 * @date 2026年4月16日
 */
public enum GemColor {
    DIAMOND(0.35f, 0.90f, 1.00f, 1.0f);

    @NonNull 
    private final Vector4f color;
    
    GemColor(float r, float g, float b, float a) {
        this.color = new Vector4f(r, g, b, a);
    }

    public @NonNull Vector4f color() {
        return color;
    }
}
