package io.github.theflysong.client.gui;

import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceType;
import io.github.theflysong.user.User;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class RankingList extends GuiScreen {
    private static final ResourceLocation BUTTON_DISABLED =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button_disabled.png");
    private static final ResourceLocation BUTTON_READY =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button_ready.png");
    private static final ResourceLocation BUTTON_NORMAL =
            new ResourceLocation("linklink", ResourceType.TEXTURE, "gui/button.png");

    private final Runnable onBack;
    private @Nullable List<User> rankingData;
    private final GuiTextComponent[] rankTexts = new GuiTextComponent[10];

    public RankingList(Runnable onBack) {
        this.onBack = onBack;
    }

    public void setRankingData(List<User> data) {
        this.rankingData = data;
    }

    @Override
    protected void onInit(GuiRenderer renderer) {
        addComponent(new GuiTextComponent(
                "排行榜",
                null,
                GuiAnchor.CENTER,
                0.0f,
                -220.0f,
                TextStyle.normal().withBold(true).withColor(new Vector4f(1.0f, 0.84f, 0.0f, 1.0f))));

        for (int i = 0; i < 10; i++) {
            String text;
            Vector4f color;
            if (rankingData != null && i < rankingData.size()) {
                User user = rankingData.get(i);
                text = String.format("第%d名    %s    %d分",
                        i + 1, user.getUsername(), user.getHighestScore());
                if (i == 0) {
                    color = new Vector4f(1.0f, 0.84f, 0.0f, 1.0f);
                } else if (i == 1) {
                    color = new Vector4f(0.75f, 0.75f, 0.85f, 1.0f);
                } else if (i == 2) {
                    color = new Vector4f(0.8f, 0.55f, 0.35f, 1.0f);
                } else {
                    color = new Vector4f(0.9f, 0.92f, 0.95f, 1.0f);
                }
            } else {
                text = String.format("第%d名    ---    ---", i + 1);
                color = new Vector4f(0.45f, 0.48f, 0.52f, 1.0f);
            }
            rankTexts[i] = addComponent(new GuiTextComponent(
                    text,
                    null,
                    GuiAnchor.CENTER,
                    0.0f,
                    -165.0f + i * 35.0f,
                    TextStyle.normal().withColor(color)));
        }

        GuiButtonComponent backButton = new GuiButtonComponent(
                BUTTON_DISABLED,
                BUTTON_READY,
                BUTTON_NORMAL,
                GuiAnchor.CENTER,
                0.0f,
                210.0f,
                300.0f,
                60.0f);
        backButton.setOnClick((component, context) -> {
            onBack.run();
            return true;
        });
        backButton.setOverlayRenderer((renderer1, component, modelMatrix, localZ) ->
                renderer1.drawText("返回", null, modelMatrix, component.width(), component.height(),
                        localZ + 0.001f,
                        TextStyle.normal().withBold(true)
                                .withColor(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f))));
        addComponent(backButton);
    }

    @Override
    protected void renderScreen(GuiRenderer renderer) {
    }
}
