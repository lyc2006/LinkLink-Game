package io.github.theflysong.gem;

import io.github.theflysong.level.GameLevel;

/**
 * 瑕疵宝石
 *
 * @author theflysong
 * @date 2026年4月16日
 */
public class GemFlawed extends Gem {
    public GemFlawed() {
        super(10);
    }
    
    @Override
    public void onDestroy(/* Game game, */ GameLevel level, GemInstance instance) {
        // 瑕疵宝石将会带来一定的debuff
        // TODO...
        super.onDestroy(/* game, */ level, instance);
    }
}
