package io.theflysong.github.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;

import io.theflysong.github.client.gl.Shader;

/**
 * Resource Location，资源位置，表示一个资源在游戏中的唯一标识
 * 分为命名空间 + 类型 + 路径
 *
 * @author theflysong
 * @date 2026年4月14日
 */
public class ResLoader {
    @Nullable
    public static InputStream loadFile(String name) {
        return ResLoader.class.getClassLoader().getResourceAsStream(name);
    }

    @Nullable
    public static InputStream loadFile(ResLoc location) {
        return loadFile(location.toPath());
    }

    public static String loadText(ResLoc location) {
        StringBuilder sb = new StringBuilder();
        InputStream file = Objects.requireNonNull(loadFile(location), "Couldn't load file from " + location);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file, StandardCharsets.UTF_8))) {
            String line = br.readLine();
            if (line != null) {
                sb.append(line);
                while ((line = br.readLine()) != null) {
                    sb.append("\n").append(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load text from " + location, e);
        }
        return sb.toString();
    }

    public static Shader loadShader(ResLoc vertLoc, ResLoc fragLoc) {
        return new Shader(loadText(vertLoc), loadText(fragLoc));
    }
}