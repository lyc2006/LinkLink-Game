package io.github.theflysong.bars;

import io.github.theflysong.client.render.IBarRenderer;
import io.github.theflysong.client.render.TotalBarRenderer;
import io.github.theflysong.level.GameLevel;

/**
 * 
 *
 * @author theflysong
 * @date 2026年4月21日
 */
public class TotalBar extends EnergyBar {
    public TotalBar() {
        super(300);
    }

    @Override
    protected ExecuteResult on_execute(GameLevel level) {
        System.out.println("TotalBar executed with effect count: " + effectCount);
        return new ExecuteResult(ExecuteResult.Type.SUCCESS);
    }

    @Override
    public IBarRenderer renderer() {
        return TotalBarRenderer.instance();
    }
}
