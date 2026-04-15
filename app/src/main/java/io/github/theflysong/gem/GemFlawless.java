package io.github.theflysong.gem;

/**
 * 无暇宝石
 *
 * @author theflysong
 * @date 2026年4月16日
 */
public class GemFlawless extends Gem {
    public GemFlawless() {
    }
    
    @Override
    public void onDestroy(/* Game game, GameLevel level, */ GemInstance instance) {
        // 无暇宝石将会带来一定的buff
        // TODO...
        super.onDestroy(/* game, level, */ instance);
    }
}
