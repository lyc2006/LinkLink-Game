package io.github.theflysong.client.gl;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;

import static io.github.theflysong.App.LOGGER;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
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
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
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
	private MouseButtonCallback onMouseButton;
	private WindowSizeCallback onWindowSize;

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

	public Window onMouseButton(MouseButtonCallback onMouseButton) {
		this.onMouseButton = onMouseButton;
		return this;
	}

	public Window onWindowSize(WindowSizeCallback onWindowSize) {
		this.onWindowSize = onWindowSize;
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
			LOGGER.error("Unable to initialize GLFW");
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
			LOGGER.error("Failed to create GLFW window, size={}x{}, title={}", width, height, title);
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
		glfwSetWindowSizeCallback(handle, (window, windowWidth, windowHeight) -> {
			if (onWindowSize != null) {
				onWindowSize.onWindowSize(window, windowWidth, windowHeight);
			}
		});

		glfwSetMouseButtonCallback(handle, (window, button, action, mods) -> {
			if (onMouseButton == null) {
				return;
			}
			double[] xPos = new double[1];
			double[] yPos = new double[1];
			int[] windowWidth = new int[1];
			int[] windowHeight = new int[1];
			glfwGetCursorPos(window, xPos, yPos);
			glfwGetWindowSize(window, windowWidth, windowHeight);
			onMouseButton.onMouseButton(window, xPos[0], yPos[0], windowWidth[0], windowHeight[0], button, action, mods);
		});

		glfwMakeContextCurrent(handle);
		glfwSwapInterval(1);
		glfwShowWindow(handle);

		GL.createCapabilities();
		glViewport(0, 0, width, height);

		if (onInit != null) {
			onInit.run();
		}
		if (onWindowSize != null) {
			onWindowSize.onWindowSize(handle, width, height);
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

	@FunctionalInterface
	public interface MouseButtonCallback {
		void onMouseButton(long window,
							 double cursorX,
							 double cursorY,
							 int windowWidth,
							 int windowHeight,
							 int button,
							 int action,
							 int mods);
	}

	@FunctionalInterface
	public interface WindowSizeCallback {
		void onWindowSize(long window, int width, int height);
	}
}
