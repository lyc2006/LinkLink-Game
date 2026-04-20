package io.github.theflysong.gem;

import org.jspecify.annotations.NonNull;

import io.github.theflysong.client.render.preprocessor.IPreprocessor;
import io.github.theflysong.client.render.preprocessor.SpriteOverlayPreprocessor;
import io.github.theflysong.client.sprite.Sprite;
import io.github.theflysong.data.Identifier;
import io.github.theflysong.level.GameLevel;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * 宝石
 *
 * @author theflysong
 * @date 2026年4月16日
 */
public class Gem {
    protected int energyValue = 0;

    public Gem(int energyValue) {
        this.energyValue = energyValue;
    }

    public Gem() {
        this(20);
    }

    public void onSpawn(/* Game game, */ GameLevel level, GemInstance instance) {}
    public void onDestroy(/* Game game, */ GameLevel level, GemInstance instance) {
        // 默认销毁效果：为总能量条充能
        level.energyBar("total").chargeEnergy(energyValue);
    }

    /**
     * 获取宝石的精灵ID。
     * 
     * @return 宝石的精灵ID
     */
    @SideOnly(Side.CLIENT)
    public Identifier getSprite(@NonNull GemInstance instance) {
        Identifier this_id = Gems.GEMS.getKey(instance.gem());
        return new Identifier(this_id.namespace(), "gem." + this_id.path());
    }

    /**
     * 获取宝石的渲染预处理器。
     * 
     * @param instance 宝石实例
     * @param sprite   宝石的精灵
     * @return 宝石的渲染预处理器
     */
    @SideOnly(Side.CLIENT)
    public IPreprocessor getPreprocessor(@NonNull GemInstance instance, @NonNull Sprite sprite) {
        return SpriteOverlayPreprocessor.processor(instance.color().color(), sprite);
    }
}
