package io.github.theflysong.client.gui;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 文本组件。
 */
public final class GuiTextComponent extends GuiComponent {
    private String text;
    private @Nullable GuiFont font;
    private TextStyle style;

    public GuiTextComponent(@NonNull String text,
                            @Nullable GuiFont font,
                            @NonNull GuiAnchor anchor,
                            float offsetX,
                            float offsetY,
                            @NonNull TextStyle style) {
        super(anchor, offsetX, offsetY, 0.0f, 0.0f);
        this.text = text;
        this.font = font;
        this.style = style;
    }

    @Override
    protected void renderComponent(@NonNull GuiRenderer renderer) {
        GuiFont resolvedFont = renderer.fonts().resolve(font);
        GuiFont.TextBounds bounds = resolvedFont.measureText(text);
        setSize(bounds.width(), bounds.height());

        if (text.isEmpty()) {
            return;
        }

        renderer.drawText(text, font, anchor(), offsetX(), offsetY(), style);
    }

    public @NonNull String text() {
        return text;
    }

    public void setText(@NonNull String text) {
        this.text = text;
    }

    public @Nullable GuiFont font() {
        return font;
    }

    public void setFont(@Nullable GuiFont font) {
        this.font = font;
    }

    public @NonNull TextStyle style() {
        return style;
    }

    public void setStyle(@NonNull TextStyle style) {
        this.style = style;
    }
}
