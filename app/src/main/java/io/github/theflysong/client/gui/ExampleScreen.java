package io.github.theflysong.client.gui;

import org.joml.Vector2f;
import org.joml.Vector4f;

import io.github.theflysong.client.render.preprocessor.SpriteOverlayPreprocessor;
import io.github.theflysong.client.sprite.Sprites;
import io.github.theflysong.client.window.CursorPosition;
import io.github.theflysong.client.window.Window;
import io.github.theflysong.client.window.WindowSize;
import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceType;
import io.github.theflysong.gem.GemColor;

/**
 * 示例屏幕：在 GUI 中渲染一张材质。
 */
public final class ExampleScreen extends GuiScreen {
    private static final ResourceLocation DEMO_TEXTURE =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "items/chipped.png");
    private GuiFont demoFont;
    private GuiTextComponent hoverItalicText;
    private boolean hoverItalicActive;

    @Override
    protected void onInit(GuiRenderer renderer) {
        // 显式系统字体示例（Windows）。
        demoFont = renderer.fonts().loadSystemFont("segoeui.ttf", 32.0f);

        addComponent(new GuiTextureComponent(
            DEMO_TEXTURE,
            GuiAnchor.CENTER,
            0.0f,
            0.0f,
            96.0f,
            96.0f));

        addComponent(new GuiSpriteComponent(
            Sprites.EXQUISITE_GEM.get(),
            SpriteOverlayPreprocessor.processor(GemColor.ICE.color(), Sprites.EXQUISITE_GEM.get()),
            GuiAnchor.LEFT,
            100.0f,
            0.0f,
            96.0f,
            96.0f));

        addComponent(new GuiTextComponent(
            "The quick brown fox jumps over the lazy dog(Segoeui Font)",
            demoFont,
            GuiAnchor.TOP,
            0.0f,
            64.0f,
            TextStyle.normal()));

        addComponent(new GuiTextComponent(
            "The quick brown fox jumps over the lazy dog(font=null)",
            null,
            GuiAnchor.TOP,
            0.0f,
            100.0f,
            TextStyle.normal()));

        addComponent(new GuiTextComponent(
            "永和九年，岁在癸丑，暮春之初，会于会稽山阴之兰亭，修禊事也。(font=null)",
            null,
            GuiAnchor.TOP,
            0.0f,
            140.0f,
            TextStyle.normal().withColor(new Vector4f(1.0f, 0.5f, 0.5f, 1.0f))));

        addComponent(new GuiTextComponent(
            "Bold + Italic + Underline + Strike",
            demoFont,
            GuiAnchor.TOP,
            0.0f,
            220.0f,
            TextStyle.normal()
                .withBold(true)
                .withItalic(true)
                .withUnderline(true)
                .withStrikethrough(true)
                .withColor(new Vector4f(0.95f, 0.95f, 0.2f, 1.0f))));

        addComponent(new GuiTextComponent(
            "Bold + Underline + Strike",
            demoFont,
            GuiAnchor.TOP,
            0.0f,
            260.0f,
            TextStyle.normal()
                .withBold(true)
                .withUnderline(true)
                .withStrikethrough(true)
                .withColor(new Vector4f(0.95f, 0.95f, 0.2f, 1.0f))));

        addComponent(new GuiTextComponent(
            "Italic + Underline + Strike",
            demoFont,
            GuiAnchor.TOP,
            0.0f,
            300.0f,
            TextStyle.normal()
                .withItalic(true)
                .withUnderline(true)
                .withStrikethrough(true)
                .withColor(new Vector4f(0.95f, 0.95f, 0.2f, 1.0f))));

        addComponent(new GuiTextComponent(
            "Underline + Strike",
            demoFont,
            GuiAnchor.TOP,
            0.0f,
            340.0f,
            TextStyle.normal()
                .withUnderline(true)
                .withStrikethrough(true)
                .withColor(new Vector4f(0.95f, 0.95f, 0.2f, 1.0f))));

        GuiTextComponent clickableText = addComponent(new GuiTextComponent(
            "点击我切换删除线状态",
            null,
            GuiAnchor.TOP,
            0.0f,
            390.0f,
            TextStyle.normal()
                .withUnderline(true)
                .withColor(new Vector4f(0.5f, 1.0f, 0.8f, 1.0f))));
        clickableText.setOnClick((component, context) -> {
            GuiTextComponent textComponent = (GuiTextComponent) component;
            TextStyle current = textComponent.style();
            textComponent.setStyle(current.withStrikethrough(!current.strikethrough()));
            return true;
        });

        hoverItalicText = addComponent(new GuiTextComponent(
            "鼠标悬浮到这行文字时会变成斜体",
            null,
            GuiAnchor.TOP,
            0.0f,
            440.0f,
            TextStyle.normal().withColor(new Vector4f(0.85f, 0.95f, 1.0f, 1.0f))));
    }

    @Override
    protected void renderScreen(GuiRenderer renderer) {
        if (hoverItalicText == null) {
            return;
        }

		long windowHandle = Window.currentHandle();
		if (windowHandle == 0L) {
			return;
		}

        CursorPosition cursor = Window.cursorPosition(windowHandle);
        WindowSize size = Window.windowSize(windowHandle);

        GuiScreenSpace screenSpace = GuiScreenSpace.fromViewportSize(size.width(), size.height());
        Vector2f guiPos = screenSpace.toGuiPosition(cursor.x(), cursor.y(), size.width(), size.height());
        boolean isHovered = hoverItalicText.hitTest(screenSpace, guiPos.x, guiPos.y);
        if (isHovered == hoverItalicActive) {
            return;
        }

        hoverItalicActive = isHovered;
        hoverItalicText.setStyle(hoverItalicText.style().withItalic(isHovered));
    }
}
