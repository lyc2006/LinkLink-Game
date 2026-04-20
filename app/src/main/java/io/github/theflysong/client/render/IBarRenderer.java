package io.github.theflysong.client.render;

import org.joml.Matrix4f;

import io.github.theflysong.bars.EnergyBar;
import io.github.theflysong.level.GameLevel;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * 
 *
 * @author theflysong
 * @date 2026年4月21日
 */
@SideOnly(Side.CLIENT)
public interface IBarRenderer {
    void render(EnergyBar bar, GameLevel level, LevelRenderer levelRenderer, Renderer renderer, Matrix4f modelMatrix);
}
