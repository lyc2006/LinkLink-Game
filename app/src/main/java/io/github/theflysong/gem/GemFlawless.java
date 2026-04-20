package io.github.theflysong.gem;

import io.github.theflysong.level.GameLevel;

/**
 * 无暇宝石
 *
 * @author theflysong
 * @date 2026年4月16日
 */
public class GemFlawless extends Gem {
    public GemFlawless() {
        super(30);
    }
    
    @Override
    public void onDestroy(/* Game game, */ GameLevel level, GemInstance instance) {
        // 无暇宝石将会带来一定的buff
        super.onDestroy(/* game, */ level, instance);
    }
}
