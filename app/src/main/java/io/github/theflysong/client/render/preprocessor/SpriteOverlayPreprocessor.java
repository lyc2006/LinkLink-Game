package io.github.theflysong.client.render.preprocessor;

import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

import io.github.theflysong.client.render.RenderContext;
import io.github.theflysong.client.render.RenderInfo;
import io.github.theflysong.client.sprite.Sprite;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * 
 *
 * @author theflysong
 * @date 2026年4月16日
 */
@SideOnly(Side.CLIENT)
public class SpriteOverlayPreprocessor {
    public static void preprocess(@NonNull RenderInfo info, @NonNull RenderContext ctx, @NonNull Vector4f color, @NonNull Sprite sprite) {
        // 先上传 sprite 通用 uniform
        SpritePreprocessor.preprocess(info, ctx, color, sprite);

        // overlay 专属 uniform（与基础层共用 sam_atlas）
        ctx.shader().getUniform("uv_overlay").ifPresent(u -> {
            var uv = sprite.uvForLayer("overlay");
            u.set(uv.u(), uv.v(), uv.width(), uv.height());
        });
        ctx.shader().getUniform("f_overlay_intensity").ifPresent(u -> u.set(0.7f));
    }

    public static IPreprocessor processor(@NonNull Vector4f color, @NonNull Sprite sprite) {
        return (info, ctx) -> preprocess(info, ctx, color, sprite);
    }

    public static IPreprocessor processor(@NonNull Sprite sprite) {
        return (info, ctx) -> preprocess(info, ctx, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), sprite);
    }
}
