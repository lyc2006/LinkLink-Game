package io.github.theflysong.client.sprite;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.github.theflysong.client.gl.shader.Shader;
import io.github.theflysong.data.Identifier;
import io.github.theflysong.data.ResourceLocation;

/**
 * 支持 mcmeta 动画帧的 Sprite 变体。
 */
public final class MetaSprite extends Sprite {
    private final Map<String, TextureAnimation> layerAnimations;

    MetaSprite(Identifier id,
               Model model,
               Shader shader,
               Map<String, ResourceLocation> textureLocations,
               Map<String, TextureAnimation> layerAnimations) {
        super(id, model, shader, textureLocations);
        this.layerAnimations = Map.copyOf(new LinkedHashMap<>(layerAnimations));
    }

    public Optional<TextureAnimation> animation(String layer) {
        return Optional.ofNullable(layerAnimations.get(layer));
    }

    @Override
    public boolean isAnimated() {
        return !layerAnimations.isEmpty();
    }

    @Override
    public int frameIndexAt(double renderTimeSeconds) {
        TextureAnimation animation = layerAnimations.get("layer0");
        if (animation == null && !layerAnimations.isEmpty()) {
            animation = layerAnimations.values().iterator().next();
        }
        if (animation == null) {
            return 0;
        }
        return animation.frameIndexAt(renderTimeSeconds);
    }
}
