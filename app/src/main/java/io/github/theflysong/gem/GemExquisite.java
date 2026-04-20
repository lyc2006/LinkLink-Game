package io.github.theflysong.gem;

import org.jspecify.annotations.NonNull;

import io.github.theflysong.client.render.preprocessor.IPreprocessor;
import io.github.theflysong.client.render.preprocessor.SpriteMetaOverlayPreprocessor;
import io.github.theflysong.client.sprite.Sprite;
import io.github.theflysong.level.GameLevel;
import io.github.theflysong.util.SideOnly;
import io.github.theflysong.util.Side;

/**
 * 精美宝石
 *
 * @author theflysong
 * @date 2026年4月16日
 */
public class GemExquisite extends Gem {
    public GemExquisite() {
        super(50);
    }
    
    @Override
    public void onDestroy(/* Game game, */ GameLevel level, GemInstance instance) {
        // 精美宝石将会带来一定的buff
        // TODO...
        super.onDestroy(/* game, */ level, instance);
    }

    // 精美宝石使用特殊的预处理器来渲染动画
    @Override
    @SideOnly(Side.CLIENT)
    public IPreprocessor getPreprocessor(@NonNull GemInstance instance, @NonNull Sprite sprite) {
        return SpriteMetaOverlayPreprocessor.processor(instance.color().color(), sprite);
    }
}
