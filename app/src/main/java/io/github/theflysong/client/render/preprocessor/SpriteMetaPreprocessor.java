package io.github.theflysong.client.render.preprocessor;

import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

import io.github.theflysong.client.render.RenderContext;
import io.github.theflysong.client.render.RenderInfo;
import io.github.theflysong.client.sprite.Sprite;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * Meta Sprite 预处理器：在基础 Sprite uniform 之上补充动画帧信息。
 */
@SideOnly(Side.CLIENT)
public class SpriteMetaPreprocessor {
    public static void preprocess(@NonNull RenderInfo info, @NonNull RenderContext ctx, @NonNull Vector4f color, @NonNull Sprite sprite) {
        SpritePreprocessor.preprocess(info, ctx, color, sprite);
        ctx.shader().getUniform("i_frame").ifPresent(u -> u.set(sprite.frameIndexAt(info.renderTimeSeconds())));
    }

    public static IPreprocessor processor(@NonNull Vector4f color, @NonNull Sprite sprite) {
        return (info, ctx) -> preprocess(info, ctx, color, sprite);
    }

    public static IPreprocessor processor(@NonNull Sprite sprite) {
        return (info, ctx) -> preprocess(info, ctx, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), sprite);
    }
}
