package io.github.theflysong.bars;

import java.util.function.Consumer;

import io.github.theflysong.client.render.IBarRenderer;
import io.github.theflysong.level.GameLevel;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * 能量条
 *
 * @author theflysong
 * @date 2026年4月21日
 */
public abstract class EnergyBar {
    protected final int maxEffectCount;
    protected int effectCount;

    public EnergyBar(int maxEffectCount) {
        this.maxEffectCount = maxEffectCount;
        this.effectCount = 0;
    }

    public void chargeEnergy(int count) {
        effectCount = Math.min(effectCount + count, maxEffectCount);
    }

    public boolean useEnergy(int count) {
        if (effectCount >= count) {
            effectCount = Math.max(effectCount - count, 0);
            return true;
        }
        return false;
    }

    public void setEnergy(int count) {
        effectCount = Math.max(0, Math.min(count, maxEffectCount));
    }

    public int currentEnergy() {
        return effectCount;
    }

    public int maxEnergy() {
        return maxEffectCount;
    }

    public record ExecuteResult(Type type) {
        public static enum Type {
            SUCCESS,
            FAILURE,
            INSUFFICIENT_ENERGY
        };
    }

    public ExecuteResult execute(GameLevel level) {
        if (effectCount < maxEffectCount) {
            return new ExecuteResult(ExecuteResult.Type.INSUFFICIENT_ENERGY);
        }
        var res = on_execute(level);
        if (res.type() == ExecuteResult.Type.SUCCESS) {
            effectCount = 0;
        }
        return res;
    }

    protected abstract ExecuteResult on_execute(GameLevel level);

    @SideOnly(Side.CLIENT)
    public abstract IBarRenderer renderer();
}
