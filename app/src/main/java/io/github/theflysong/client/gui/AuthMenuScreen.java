package io.github.theflysong.client.gui;

import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceType;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

/**
 * 用户入口界面 — 登录/注册/游客/退出
 *
 * @author norbe
 * @date 2026年5月11日
 */
public final class AuthMenuScreen extends GuiScreen {
    private static final ResourceLocation BUTTON_NORMAL =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button.png");
    private static final ResourceLocation BUTTON_READY =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button_ready.png");
    private static final ResourceLocation BUTTON_DISABLED =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button_disabled.png");

    private final Runnable onLogin;
    private final Runnable onRegister;
    private final Runnable onGuest;
    private final Runnable onExit;
    private String warningMessage;

    public AuthMenuScreen(@NonNull Runnable onLogin,
                          @NonNull Runnable onRegister,
                          @NonNull Runnable onGuest,
                          @NonNull Runnable onExit) {
        this.onLogin = onLogin;
        this.onRegister = onRegister;
        this.onGuest = onGuest;
        this.onExit = onExit;
    }

    public void setWarningMessage(String message) {
        this.warningMessage = message;
    }

    @Override
    protected void onInit(GuiRenderer renderer) {
        addComponent(new GuiTextComponent("连连看", null,
                GuiAnchor.CENTER, 0.0f, -240.0f,
                TextStyle.normal().withBold(true)
                        .withColor(new Vector4f(0.95f, 0.95f, 1.0f, 1.0f))), 1);

        addComponent(createMenuButton("登录", -120.0f, onLogin), 1);
        addComponent(createMenuButton("注册", -40.0f, onRegister), 1);
        addComponent(createMenuButton("游客模式", 40.0f, onGuest), 1);
        addComponent(createMenuButton("退出游戏", 120.0f, onExit), 1);

        if (warningMessage != null) {
            addComponent(new GuiTextComponent(warningMessage, null,
                    GuiAnchor.CENTER, 0.0f, 200.0f,
                    TextStyle.normal().withBold(true)
                            .withColor(new Vector4f(1.0f, 0.75f, 0.3f, 1.0f))), 1);
        }

        setMaxLayer(2);
    }

    @Override
    protected void renderScreen(GuiRenderer renderer) {
        renderer.drawRectangle(GuiAnchor.CENTER,
                0.0f, 0.0f, 760.0f, 580.0f, 0.0f,
                new Vector4f(0.04f, 0.06f, 0.10f, 0.88f));
        renderer.drawRectangle(GuiAnchor.CENTER,
                0.0f, 0.0f, 720.0f, 540.0f, 0.001f,
                new Vector4f(0.08f, 0.10f, 0.14f, 0.96f));
    }

    private GuiButtonComponent createMenuButton(String label, float offsetY, Runnable onClick) {
        GuiButtonComponent button = new GuiButtonComponent(
                BUTTON_DISABLED, BUTTON_READY, BUTTON_NORMAL,
                GuiAnchor.CENTER, 0.0f, offsetY, 420.0f, 80.0f);
        button.setOnClick((component, context) -> {
            onClick.run();
            return true;
        });
        button.setOverlayRenderer((renderer, component, modelMatrix, localZ) ->
                renderer.drawText(label, null, modelMatrix,
                        component.width(), component.height(), localZ + 0.002f,
                        TextStyle.normal().withBold(true)
                                .withColor(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f))));
        return button;
    }
}
