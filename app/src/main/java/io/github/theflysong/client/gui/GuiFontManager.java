package io.github.theflysong.client.gui;

import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * GUI 字体管理器：负责系统字体加载、缓存和默认字体。
 */
public final class GuiFontManager implements AutoCloseable {
    private static final String[] DEFAULT_WINDOWS_FONT_CANDIDATES = new String[] {
            "msyh.ttc",
            "simhei.ttf",
            "simsun.ttc",
            "segoeui.ttf",
            "arial.ttf"
    };

    private final Map<String, GuiFont> cache = new HashMap<>();
    private @Nullable GuiFont defaultFont;

    public GuiFontManager() {
        this.defaultFont = loadDefaultSystemFont(28.0f);
    }

    public GuiFont loadSystemFont(String systemFontFileName, float sizePx) {
        Path fontPath = resolveSystemFontPath(systemFontFileName);
        return loadFontFromPath(fontPath, sizePx);
    }

    public GuiFont loadFontFromPath(Path fontPath, float sizePx) {
        Path normalized = fontPath.toAbsolutePath().normalize();
        String key = normalized + "#" + sizePx;
        return cache.computeIfAbsent(key, ignored -> GuiFont.fromFile(normalized, sizePx));
    }

    public void setDefaultFont(GuiFont font) {
        if (font == null) {
            throw new IllegalArgumentException("font must not be null");
        }
        this.defaultFont = font;
    }

    public @Nullable GuiFont defaultFont() {
        return defaultFont;
    }

    public GuiFont resolve(@Nullable GuiFont font) {
        if (font != null) {
            return font;
        }
        if (defaultFont == null) {
            defaultFont = loadDefaultSystemFont(28.0f);
        }
        return defaultFont;
    }

    private GuiFont loadDefaultSystemFont(float sizePx) {
        for (String candidate : DEFAULT_WINDOWS_FONT_CANDIDATES) {
            try {
                return loadSystemFont(candidate, sizePx);
            } catch (Exception ignored) {
            }
        }
        throw new IllegalStateException("No usable system font found in Windows font directory");
    }

    private static Path resolveSystemFontPath(String systemFontFileName) {
        Path direct = Paths.get(systemFontFileName);
        if (direct.isAbsolute() && Files.exists(direct)) {
            return direct;
        }

        String windir = System.getenv("WINDIR");
        if (windir == null || windir.isBlank()) {
            windir = "C:/Windows";
        }

        Path underWindowsFonts = Paths.get(windir, "Fonts", systemFontFileName);
        if (Files.exists(underWindowsFonts)) {
            return underWindowsFonts;
        }

        throw new IllegalArgumentException("System font file not found: " + systemFontFileName);
    }

    @Override
    public void close() {
        for (GuiFont font : cache.values()) {
            font.close();
        }
        cache.clear();
        defaultFont = null;
    }
}
