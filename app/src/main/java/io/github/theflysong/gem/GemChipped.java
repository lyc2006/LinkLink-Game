package io.github.theflysong.gem;

import io.github.theflysong.level.GameLevel;

/**
 * 碎裂宝石
 *
 * @author theflysong
 * @date 2026年4月16日
 */
public class GemChipped extends Gem {
    public GemChipped() {
        super(5);
    }
    
    @Override
    public void onDestroy(/* Game game, */ GameLevel level, GemInstance instance) {
        // 碎裂宝石将会带来一定的debuff
        // TODO...
        super.onDestroy(/* game, */ level, instance);
    }
}
