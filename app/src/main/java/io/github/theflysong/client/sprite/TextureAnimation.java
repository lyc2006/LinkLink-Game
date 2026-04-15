package io.github.theflysong.client.sprite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.theflysong.data.ResourceLoader;
import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceType;

/**
 * 从 .mcmeta 读取到的纹理动画信息。
 */
public final class TextureAnimation {
    public record FrameStep(int index, double durationSeconds) {
    }

    private static final int DEFAULT_FPS = 20;

    private final List<FrameStep> sequence;
    private final double totalDurationSeconds;
    private final int maxFrameIndex;
    private final int frameHeightHint;

    private TextureAnimation(List<FrameStep> sequence, int maxFrameIndex, int frameHeightHint) {
        if (sequence == null || sequence.isEmpty()) {
            throw new IllegalArgumentException("sequence must not be empty");
        }
        this.sequence = List.copyOf(sequence);
        this.maxFrameIndex = Math.max(0, maxFrameIndex);
        this.frameHeightHint = Math.max(0, frameHeightHint);

        double total = 0.0;
        for (FrameStep step : sequence) {
            total += Math.max(1e-6, step.durationSeconds());
        }
        this.totalDurationSeconds = total;
    }

    public static Optional<TextureAnimation> fromTexture(ResourceLocation textureLocation) throws IOException {
        ResourceLocation mcmetaLocation = resolveMcmetaLocation(textureLocation).orElse(null);
        if (mcmetaLocation == null) {
            return Optional.empty();
        }

        JsonObject root = JsonParser.parseString(ResourceLoader.loadText(mcmetaLocation)).getAsJsonObject();
        if (!root.has("animation") || !root.get("animation").isJsonObject()) {
            return Optional.empty();
        }

        JsonObject animation = root.getAsJsonObject("animation");
        int fps = readInt(animation, "vframespersecond")
                .or(() -> readInt(animation, "framespersecond"))
                .orElse(DEFAULT_FPS);
        fps = Math.max(1, fps);

        int defaultFrameTime = readInt(animation, "vframetime")
                .or(() -> readInt(animation, "frametime"))
                .orElse(1);
        defaultFrameTime = Math.max(1, defaultFrameTime);

        int frameHeightHint = readInt(animation, "vframeheight")
                .or(() -> readInt(animation, "frameheight"))
                .or(() -> readInt(animation, "height"))
                .orElse(0);

        List<FrameStep> sequence = new ArrayList<>();
        int maxFrameIndex = 0;

        JsonArray frames = animation.has("frames") && animation.get("frames").isJsonArray()
                ? animation.getAsJsonArray("frames")
                : null;
        if (frames != null) {
            for (JsonElement element : frames) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                    int index = Math.max(0, element.getAsInt());
                    sequence.add(new FrameStep(index, (double) defaultFrameTime / (double) fps));
                    maxFrameIndex = Math.max(maxFrameIndex, index);
                    continue;
                }
                if (element.isJsonObject()) {
                    JsonObject frameObject = element.getAsJsonObject();
                    int index = readInt(frameObject, "index").orElse(0);
                    int time = readInt(frameObject, "time").orElse(defaultFrameTime);
                    index = Math.max(0, index);
                    time = Math.max(1, time);
                    sequence.add(new FrameStep(index, (double) time / (double) fps));
                    maxFrameIndex = Math.max(maxFrameIndex, index);
                }
            }
        }

        if (sequence.isEmpty()) {
            int frameCount = readInt(animation, "vframecount")
                    .or(() -> readInt(animation, "framecount"))
                    .orElse(1);
            frameCount = Math.max(1, frameCount);
            for (int i = 0; i < frameCount; i++) {
                sequence.add(new FrameStep(i, (double) defaultFrameTime / (double) fps));
                maxFrameIndex = i;
            }
        }

        return Optional.of(new TextureAnimation(sequence, maxFrameIndex, frameHeightHint));
    }

    private static Optional<ResourceLocation> resolveMcmetaLocation(ResourceLocation textureLocation) {
        ResourceLocation direct = new ResourceLocation(
                textureLocation.namespace(),
                ResourceType.TEXTURE,
                textureLocation.path() + ".mcmeta");
        if (ResourceLoader.loadFile(direct) != null) {
            return Optional.of(direct);
        }

        String path = textureLocation.path();
        if (path.endsWith(".png")) {
            ResourceLocation stripped = new ResourceLocation(
                    textureLocation.namespace(),
                    ResourceType.TEXTURE,
                    path.substring(0, path.length() - 4) + ".mcmeta");
            if (ResourceLoader.loadFile(stripped) != null) {
                return Optional.of(stripped);
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> readInt(JsonObject object, String key) {
        if (!object.has(key)) {
            return Optional.empty();
        }
        JsonElement element = object.get(key);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return Optional.empty();
        }
        return Optional.of(element.getAsInt());
    }

    public int frameIndexAt(double renderTimeSeconds) {
        if (sequence.size() == 1) {
            return sequence.get(0).index();
        }
        double local = renderTimeSeconds % totalDurationSeconds;
        if (local < 0.0) {
            local += totalDurationSeconds;
        }

        double cursor = 0.0;
        for (FrameStep step : sequence) {
            cursor += Math.max(1e-6, step.durationSeconds());
            if (local < cursor) {
                return step.index();
            }
        }
        return sequence.get(sequence.size() - 1).index();
    }

    public int maxFrameIndex() {
        return maxFrameIndex;
    }

    public int frameHeightHint() {
        return frameHeightHint;
    }

    public List<FrameStep> sequence() {
        return sequence;
    }
}
