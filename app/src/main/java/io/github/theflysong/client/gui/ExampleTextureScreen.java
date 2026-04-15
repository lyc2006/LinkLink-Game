package io.github.theflysong.client.gui;

import io.github.theflysong.client.render.preprocessor.SpriteOverlayPreprocessor;
import io.github.theflysong.client.sprite.Sprites;
import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceType;
import io.github.theflysong.gem.GemColor;

/**
 * 示例屏幕：在 GUI 中渲染一张材质。
 */
public final class ExampleTextureScreen extends GuiScreen {
    private static final ResourceLocation DEMO_TEXTURE =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "items/chipped.png");
    private GuiFont demoFont;

    @Override
    protected void onInit(GuiRenderer renderer) {
        // 显式系统字体示例（Windows）。
        demoFont = renderer.fonts().loadSystemFont("segoeui.ttf", 32.0f);
    }

    @Override
    protected void renderScreen(GuiRenderer renderer) {
        renderer.drawTexture(DEMO_TEXTURE, GuiAnchor.CENTER, 0.0f, 0.0f, 96.0f, 96.0f);
        renderer.drawSprite(
                Sprites.EXQUISITE_GEM.get(),
                GuiAnchor.LEFT, 
                SpriteOverlayPreprocessor.processor(GemColor.ICE.color(), Sprites.EXQUISITE_GEM.get()),
            100.0f, 0.0f, 96.0f, 96.0f);

            // 传入字体对象。
            renderer.drawText("The quick brown fox jumps over the lazy dog(Segoeui Font)", demoFont, GuiAnchor.TOP, 0.0f, 64.0f);
            // 传入 null 时使用默认字体。
            renderer.drawText("The quick brown fox jumps over the lazy dog(font=null)", null, GuiAnchor.TOP, 0.0f, 100.0f);
            // 测试中文
            renderer.drawText("永和九年，岁在癸丑，暮春之初，会于会稽山阴之兰亭，修禊事也。(font=null)", null, GuiAnchor.TOP, 0.0f, 172.0f);
        }
}
