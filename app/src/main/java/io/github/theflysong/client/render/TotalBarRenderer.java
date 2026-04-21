package io.github.theflysong.client.render;

import org.joml.Matrix4f;

import io.github.theflysong.bars.EnergyBar;
import io.github.theflysong.bars.TotalBar;
import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceType;
import io.github.theflysong.level.GameLevel;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * 总能量条渲染器。
 * 
 * 渲染位置由 LevelRenderer 通过 modelMatrix 控制。
 * 该渲染器只负责在给定位置绘制条形图，不计算位置。
 *
 * @author theflysong
 * @date 2026年4月21日
 */
@SideOnly(Side.CLIENT)
public class TotalBarRenderer implements IBarRenderer {
    private static TotalBarRenderer INSTANCE = null;

    public static TotalBarRenderer instance() {
        if (INSTANCE == null) {
            INSTANCE = new TotalBarRenderer();
        }
        return INSTANCE;
    }

    private static final ResourceLocation TOTAL_BAR_EMPTY = new ResourceLocation("linklink", ResourceType.TEXTURE,
            "bars/total_bar_empty.png");
    private static final ResourceLocation TOTAL_BAR_FULL = new ResourceLocation("linklink", ResourceType.TEXTURE,
            "bars/total_bar_full.png");

    @Override
    public void render(EnergyBar bar, GameLevel level, LevelRenderer levelRenderer, Renderer renderer,
            Matrix4f modelMatrix) {
        if (bar instanceof TotalBar totalBar) {
            renderTotalBar(totalBar, levelRenderer, modelMatrix);
        } else {
            throw new IllegalArgumentException("Expected TotalBar, got " + bar.getClass().getSimpleName());
        }
    }

    private void renderTotalBar(TotalBar bar, LevelRenderer levelRenderer, Matrix4f modelMatrix) {
        // 计算能量百分比 (0 ~ 1)
        float percent = 0.0f;
        if (bar.maxEnergy() > 0) {
            percent = clamp((float) bar.currentEnergy() / (float) bar.maxEnergy(), 0, 1);
        }

        // 绘制空背景（total_bar_empty）
        levelRenderer.drawTexture(TOTAL_BAR_EMPTY,
                modelMatrix);

        if (percent <= 0.0f) {
            return;
        }

        // 现在绘制 total_bar_full.
        // 我们现在绘制的区域是[0, 1]x[0, 1]
        // 要变换到 [0, 1]x[1 - percent, 1]
        // 我们首先将其y轴缩放 percent, 得到
        // [0, 1]x[0, percent]
        // 然后让 0 => 1 - percent, percent => 1, 也就是在y轴上平移 1 - percent 个单位
        // 此时的 UV 也应当为
        // [0, 1]x[1 - percent, 1]
        Matrix4f fullBarMatrix = new Matrix4f(modelMatrix)
                .translate(0.0f, 1.0f - percent, 0.0f)
                .scale(1.0f, percent, 1.0f);
        levelRenderer.drawTextureRegion(TOTAL_BAR_FULL, fullBarMatrix, 0.0f, 1.0f - percent, 1.0f, 1.0f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
