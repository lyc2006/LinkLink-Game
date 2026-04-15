package io.theflysong.github.client.gl;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import io.theflysong.github.util.SideOnly;
import io.theflysong.github.util.Side;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glViewport;

/**
 * GLFW 窗口与主循环封装。
 *
 * run() 生命周期：init -> loop -> cleanup。
 */
@SideOnly(Side.CLIENT)
public class Window {
	private final int width;
	private final int height;
	private final String title;
	private long handle;
	private GLFWErrorCallback errorCallback;
	private Runnable onInit;
	private Runnable onRender;
	private Runnable onCleanup;

	public Window(int width, int height, String title) {
		this.width = width;
		this.height = height;
		this.title = title;
	}

	public void run() {
		init();
		loop();
		cleanup();
	}

	public Window onInit(Runnable onInit) {
		this.onInit = onInit;
		return this;
	}

	public Window onRender(Runnable onRender) {
		this.onRender = onRender;
		return this;
	}

	public Window onCleanup(Runnable onCleanup) {
		this.onCleanup = onCleanup;
		return this;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	/**
	 * 初始化 GLFW、窗口与 OpenGL 上下文。
	 */
	private void init() {
		errorCallback = GLFWErrorCallback.createPrint(System.err);
		errorCallback.set();

		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

		handle = glfwCreateWindow(width, height, title, 0, 0);
		if (handle == 0) {
			throw new IllegalStateException("Failed to create GLFW window");
		}

		GLFWVidMode mode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		if (mode != null) {
			int xpos = (mode.width() - width) / 2;
			int ypos = (mode.height() - height) / 2;
			glfwSetWindowPos(handle, xpos, ypos);
		}

		glfwSetKeyCallback(handle, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
				glfwSetWindowShouldClose(window, true);
			}
		});

		glfwSetFramebufferSizeCallback(handle,
				(window, framebufferWidth, framebufferHeight) -> glViewport(0, 0, framebufferWidth, framebufferHeight));

		glfwMakeContextCurrent(handle);
		glfwSwapInterval(1);
		glfwShowWindow(handle);

		GL.createCapabilities();
		glViewport(0, 0, width, height);

		if (onInit != null) {
			onInit.run();
		}
	}

	/**
	 * 主循环：清屏 -> 用户渲染 -> 交换缓冲 -> 事件轮询。
	 */
	private void loop() {
		while (!glfwWindowShouldClose(handle)) {
			glClearColor(0.08f, 0.10f, 0.14f, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT);

			if (onRender != null) {
				onRender.run();
			}

			glfwSwapBuffers(handle);
			glfwPollEvents();
		}
	}

	/**
	 * 释放窗口和 GLFW 相关资源。
	 */
	private void cleanup() {
		if (onCleanup != null) {
			onCleanup.run();
		}

		if (handle != 0) {
			glfwDestroyWindow(handle);
			handle = 0;
		}
		glfwTerminate();
		GLFWErrorCallback callback = glfwSetErrorCallback(null);
		if (callback != null) {
			callback.free();
		}
	}
}
