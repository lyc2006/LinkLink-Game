package io.github.theflysong.client.gl;

import io.github.theflysong.client.render.RenderContext;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * 兼容旧入口的纹理状态代理。
 *
 * 新实现已迁移到 RenderContext；该类仅保留向后兼容。
 */
@SideOnly(Side.CLIENT)
@Deprecated(forRemoval = false)
public final class GLManager {
    private static final GLManager INSTANCE = new GLManager();

    private GLManager() {
    }

    public static GLManager getInstance() {
        return INSTANCE;
    }

    public void pushTextureBindingStack() {
        RenderContext.pushTextureBindingStack();
    }

    public void popTextureBindingStack() {
        RenderContext.popTextureBindingStack();
    }

    public void activateUnit(int unit) {
        RenderContext.activateUnit(unit);
    }

    public void bindTexture(int textureId) {
        RenderContext.bindTexture(textureId);
    }

    public void binding(int unit, int textureId) {
        RenderContext.binding(unit, textureId);
    }

    public int activeUnit() {
        return RenderContext.activeUnit();
    }

    public int textureUnitCnt() {
        return RenderContext.textureUnitCnt();
    }

    public int boundTexture(int unit) {
        return RenderContext.boundTexture(unit);
    }
}