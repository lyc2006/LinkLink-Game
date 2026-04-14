package io.theflysong.github.client.gl;

import io.theflysong.github.util.SideOnly;
import io.theflysong.github.util.Side;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glGetInteger;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL20C.*;

import java.util.Stack;

@SideOnly(Side.CLIENT)
public class GLMgr {
    private static GLMgr INSTANCE;

    private static record TextureBinding(int unit, int textureId) {
    };

    // 纹理绑定栈
    private final Stack<TextureBinding> textureBindingStack = new Stack<>();
    // 当前激活的纹理单元
    private int activeUnit = 0;
    // 当前纹理单元数量
    private int textureUnitCnt = glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
    // 纹理单元绑定的纹理ID列表
    private int[] textureBindings = new int[textureUnitCnt];

    private GLMgr() {
    }

    public static GLMgr getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GLMgr();
        }
        return INSTANCE;
    }

    // 将当前纹理绑定状态压入栈中
    public void pushTextureBindingStack() {
        textureBindingStack.push(new TextureBinding(activeUnit, textureBindings[activeUnit]));
    }

    // 恢复栈顶的纹理绑定状态
    public void popTextureBindingStack() {
        TextureBinding binding = textureBindingStack.pop();
        glActiveTexture(GL_TEXTURE0 + binding.unit);
        glBindTexture(GL_TEXTURE_2D, binding.textureId);
    }

    // 激活指定纹理单元
    public void activateUnit(int unit)
    {
        if (unit != activeUnit) {
            glActiveTexture(GL_TEXTURE0 + unit);
            activeUnit = unit;
        }
    }

    // 绑定纹理到激活单元
    public void bindTexture(int textureId) {
        if (textureBindings[activeUnit] != textureId) {
            textureBindings[activeUnit] = textureId;
            glBindTexture(GL_TEXTURE_2D, textureId);
        }
    }

    // 绑定纹理到指定单元
    public void binding(int unit, int textureId) {
        activateUnit(unit);
        bindTexture(textureId);
    }

    public int activeUnit() {
        return activeUnit;
    }

    public int textureUnitCnt() {
        return textureUnitCnt;
    }

    public int boundTexture(int unit) {
        return textureBindings[unit];
    }
}