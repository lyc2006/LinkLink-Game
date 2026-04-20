package io.github.theflysong.client.render.preprocessor;

import org.jspecify.annotations.NonNull;

import io.github.theflysong.client.render.RenderContext;
import io.github.theflysong.client.render.RenderInfo;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * 渲染预处理器
 *
 * @author theflysong
 * @date 2026年4月16日
 */
@SideOnly(Side.CLIENT)
public interface IPreprocessor {
    /**
     * 预处理方法
     *
     * @param info 渲染信息载体
     * @param ctx  渲染上下文，包含当前RenderItem、Shader、模型矩阵等信息
     */
    void preprocess(@NonNull RenderInfo info, @NonNull RenderContext ctx);
}
