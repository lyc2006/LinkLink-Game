package io.github.theflysong.client.gui;

import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceType;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

public final class MainMenuScreen extends GuiScreen {
    private static final ResourceLocation BUTTON_DISABLED =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button_disabled.png");
    private static final ResourceLocation BUTTON_READY =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button_ready.png");
    private static final ResourceLocation BUTTON_NORMAL =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button.png");

    private final Runnable onStart;
    private final Runnable onContinue;
    private final Runnable onBackLogin;
    private final Runnable onPK;
    private final Runnable onSelectLevel;
    private final Supplier<String> selectedLevelLabelSupplier;

    private GuiButtonComponent continueButton;
    private GuiTextComponent selectedLevelText;
    private GuiTextComponent userStatusText;
    private String currentUser = "";

    public MainMenuScreen(@NonNull Runnable onStart,
                          @NonNull Runnable onContinue,
                          @NonNull Runnable onBackLogin,
                          @NonNull Runnable onPK,
                          @NonNull Runnable onSelectLevel,
                          @NonNull Supplier<String> selectedLevelLabelSupplier) {
        this.onStart = onStart;
        this.onContinue = onContinue;
        this.onBackLogin = onBackLogin;
        this.onPK = onPK;
        this.onSelectLevel = onSelectLevel;
        this.selectedLevelLabelSupplier = selectedLevelLabelSupplier;
    }

    @Override
    protected void onInit(GuiRenderer renderer) {

        selectedLevelText = addComponent(new GuiTextComponent(
                "当前关卡：简单",
                null,
                GuiAnchor.CENTER,
                0.0f,
                -200.0f,
                TextStyle.normal().withColor(new Vector4f(0.84f, 0.94f, 0.98f, 1.0f))));

        addComponent(createMenuButton("开始游戏", -140.0f, () -> onStart.run()));
        continueButton = addComponent(createMenuButton("继续游戏", -60.0f, () -> onContinue.run()));
        addComponent(createMenuButton("关卡选择", 20.0f, () -> onSelectLevel.run()));
        addComponent(createMenuButton("对战模式", 100.0f, () -> onPK.run()));
        addComponent(createMenuButton("返回登录", 180.0f, () -> onBackLogin.run()));

        userStatusText = addComponent(new GuiTextComponent(
                "当前用户：" + currentUser,
                null,
                GuiAnchor.CENTER,
                0.0f,
                -260.0f,
                TextStyle.normal().withColor(new Vector4f(0.7f, 0.75f, 0.8f, 1.0f))));
    }

    @Override
    protected void renderScreen(GuiRenderer renderer) {
        selectedLevelText.setText("当前关卡：" + selectedLevelLabelSupplier.get());
    }

    public void setContinueEnabled(boolean enabled) {
        if (continueButton != null) {
            continueButton.setDisabled(!enabled);
        }
    }

    public void setCurrentUser(String userName) {
        currentUser = userName;
        if (userStatusText != null) {
            userStatusText.setText("当前用户：" + userName);
        }
    }

    private GuiButtonComponent createMenuButton(String label, float offsetY, Runnable onClick) {
        GuiButtonComponent button = new GuiButtonComponent(
                BUTTON_DISABLED,
                BUTTON_READY,
                BUTTON_NORMAL,
                GuiAnchor.CENTER,
                0.0f,
                offsetY,
                420.0f,
                80.0f);
        button.setOnClick((component, context) -> {
            onClick.run();
            return true;
        });
        button.setOverlayRenderer((renderer, component, modelMatrix, localZ) ->
            renderer.drawText(label, null, modelMatrix, component.width(), component.height(), localZ + 0.001f,
                TextStyle.normal().withBold(true).withColor(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f))));
        return button;
    }
}
