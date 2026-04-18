package io.github.theflysong.client.render;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import io.github.theflysong.client.render.preprocessor.IPreprocessor;
import io.github.theflysong.client.render.preprocessor.SpriteOverlayPreprocessor;
import io.github.theflysong.client.sprite.Sprite;
import io.github.theflysong.client.sprite.Sprites;
import io.github.theflysong.data.Identifier;
import io.github.theflysong.gem.GemInstance;
import io.github.theflysong.gem.Gems;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * 宝石渲染器
 *
 * @author theflysong
 * @date 2026年4月16日
 */
@SideOnly(Side.CLIENT)
public class GemRenderer {
    @Nullable
    protected static GemRenderer INSTANCE;

    public static GemRenderer instance() {
        if (INSTANCE == null) {
            INSTANCE = new GemRenderer();
        }
        return INSTANCE;
    }

    protected Map<GemInstance, RenderableObject> cache = new HashMap<>();

    public RenderableObject lookupCache(@NonNull GemInstance instance) {
        RenderableObject obj = cache.get(instance);
        if (obj == null) {
            Sprite sprite = Sprites.SPRITES.getOrThrow(instance.gem().getSprite(instance));
            obj = new RenderableObject(
                    sprite.model().createGpuMesh(),
                    sprite.shader(),
                    instance.gem().getPreprocessor(instance, sprite));
            cache.put(instance, obj);
        }
        return obj;
    }

    public void renderGem(@NonNull Renderer renderer, @NonNull GemInstance instance, @NonNull Matrix4f modelMatrix) {
        RenderableObject obj = lookupCache(instance);
        renderer.submit(obj.createRenderItem(modelMatrix));
    }

    public void closeAll() {
        for (RenderableObject obj : cache.values()) {
            obj.close();
        }
        cache.clear();
    }
}
