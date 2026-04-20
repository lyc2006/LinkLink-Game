package io.github.theflysong.client.render;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glGetInteger;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL20C.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS;

import java.util.Stack;

import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import io.github.theflysong.client.gl.shader.Shader;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

/**
 * 渲染上下文。
 *
 * 1. 作为单次绘制的上下文，持有 shader、item 与当前 modelMatrix。
 * 2. 提供 modelMatrix 栈，允许在局部修改前后保存与恢复矩阵状态。
 * 3. 承接原 GLManager 的纹理绑定状态，供纹理上传与预处理阶段复用。
 */
@SideOnly(Side.CLIENT)
public final class RenderContext {
	private final @NonNull Shader shader;
	private final @NonNull RenderItem item;
	private final @NonNull Matrix4f modelMatrix;
	private final Stack<Matrix4f> modelMatrixStack = new Stack<>();

	/**
	 * 记录一次纹理状态快照：激活单元 + 该单元纹理。
	 */
	private static record TextureBinding(int unit, int textureId) {
	}

	private static final Stack<TextureBinding> textureBindingStack = new Stack<>();
	private static int activeUnit = 0;
	private static int textureUnitCnt = -1;
	private static int[] textureBindings;

	public RenderContext(@NonNull Shader shader, @NonNull Matrix4f modelMatrix, @NonNull RenderItem item) {
		this.shader = shader;
		this.item = item;
		this.modelMatrix = new Matrix4f(modelMatrix);
	}

	public @NonNull Shader shader() {
		return shader;
	}

	public @NonNull RenderItem item() {
		return item;
	}

	public @NonNull Matrix4f modelMatrix() {
		return modelMatrix;
	}

	public void pushModelMatrixStack() {
		modelMatrixStack.push(new Matrix4f(modelMatrix));
	}

	public void popModelMatrixStack() {
		Matrix4f previous = modelMatrixStack.pop();
		modelMatrix.set(previous);
	}

	public void resetModelMatrix(@NonNull Matrix4f matrix) {
		modelMatrix.set(matrix);
	}

	private static void ensureTextureState() {
		if (textureBindings != null) {
			return;
		}
		textureUnitCnt = glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
		textureBindings = new int[textureUnitCnt];
		for (int i = 0; i < textureUnitCnt; i++) {
			textureBindings[i] = -1;
		}
	}

	public static void pushTextureBindingStack() {
		ensureTextureState();
		textureBindingStack.push(new TextureBinding(activeUnit, textureBindings[activeUnit]));
	}

	public static void popTextureBindingStack() {
		ensureTextureState();
		TextureBinding binding = textureBindingStack.pop();
		glActiveTexture(GL_TEXTURE0 + binding.unit);
		if (binding.textureId >= 0) {
			glBindTexture(GL_TEXTURE_2D, binding.textureId);
		}
		activeUnit = binding.unit;
		textureBindings[binding.unit] = binding.textureId;
	}

	public static void activateUnit(int unit) {
		ensureTextureState();
		if (unit != activeUnit) {
			glActiveTexture(GL_TEXTURE0 + unit);
			activeUnit = unit;
		}
	}

	public static void bindTexture(int textureId) {
		ensureTextureState();
		if (textureBindings[activeUnit] != textureId) {
			textureBindings[activeUnit] = textureId;
			glBindTexture(GL_TEXTURE_2D, textureId);
		}
	}

	public static void binding(int unit, int textureId) {
		activateUnit(unit);
		bindTexture(textureId);
	}

	public static int activeUnit() {
		ensureTextureState();
		return activeUnit;
	}

	public static int textureUnitCnt() {
		ensureTextureState();
		return textureUnitCnt;
	}

	public static int boundTexture(int unit) {
		ensureTextureState();
		return textureBindings[unit];
	}
}