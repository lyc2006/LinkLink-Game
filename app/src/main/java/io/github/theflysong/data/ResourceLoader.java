package io.github.theflysong.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Objects;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;

import io.github.theflysong.data.audio.AudioData;

/**
 * Resource Location，资源位置，表示一个资源在游戏中的唯一标识
 * 分为命名空间 + 类型 + 路径
 *
 * @author theflysong
 * @date 2026年4月14日
 */
public class ResourceLoader {
    @Nullable
    private static InputStream loadFile(String name) {
        return ResourceLoader.class.getClassLoader().getResourceAsStream(name);
    }

    @Nullable
    public static InputStream loadFile(ResourceLocation location) {
        return loadFile(location.toPath());
    }

    public static String loadText(ResourceLocation location) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream file = Objects.requireNonNull(loadFile(location), "Couldn't load file from " + location);
        BufferedReader br = new BufferedReader(new InputStreamReader(file, StandardCharsets.UTF_8));
        String line = br.readLine();
        if (line != null) {
            sb.append(line);
            while ((line = br.readLine()) != null) {
                sb.append("\n").append(line);
            }
        }
        return sb.toString();
    }

    public static ByteBuffer loadBinary(ResourceLocation location) throws IOException {
        InputStream stream = Objects.requireNonNull(ResourceLoader.loadFile(location),
                "Couldn't load file from " + location);
        byte[] bytes = stream.readAllBytes();
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    public static AudioData loadAudio(ResourceLocation location) throws IOException {
        String path = location.toPath();
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".ogg")) {
            return AudioData.fromOgg(loadBinary(location), location.toString());
        }
        if (lower.endsWith(".wav")) {
            try (InputStream stream = Objects.requireNonNull(loadFile(location),
                    "Couldn't load file from " + location)) {
                return AudioData.fromWav(stream, location.toString());
            }
        }
        throw new IOException("Unsupported audio format: " + location);
    }
}