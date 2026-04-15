package io.github.theflysong.util.event;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 无事件总线。
 *
 * 特性：
 * 1. 显式注册/取消注册监听器。
 * 2. 支持优先级与是否接收已取消事件。
 * 3. post 时按事件类型继承树分发（子类事件会被父类监听器接收）。
 */
public final class EventBus {
    private final Map<Class<? extends Event>, CopyOnWriteArrayList<ListenerEntry<? extends Event>>> listeners = new ConcurrentHashMap<>();

    /**
     * 通过注解注册监听器对象上的监听方法。
     *
     * 要求：
     * 1. 类上存在 {@link EventSubscriber}。
     * 2. 方法上存在 {@link SubscribeEvent}。
     * 3. 方法参数必须且仅有一个，且为 Event 子类。
     */
    public EventSubscription registerAnnotated(Object listenerOwner) {
        Objects.requireNonNull(listenerOwner, "listenerOwner must not be null");

        Class<?> listenerClass = listenerOwner.getClass();
        if (!listenerClass.isAnnotationPresent(EventSubscriber.class)) {
            throw new IllegalArgumentException("Listener class must be annotated with @EventSubscriber: " + listenerClass.getName());
        }

        List<EventSubscription> subscriptions = new ArrayList<>();
        for (Method method : listenerClass.getDeclaredMethods()) {
            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            if (annotation == null) {
                continue;
            }

            validateAnnotatedMethod(listenerClass, method);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType = (Class<? extends Event>) method.getParameterTypes()[0];
            subscriptions.add(registerAnnotatedMethod(eventType, listenerOwner, method, annotation));
        }

        if (subscriptions.isEmpty()) {
            throw new IllegalArgumentException("No @SubscribeEvent methods found in class: " + listenerClass.getName());
        }

        return () -> subscriptions.forEach(EventSubscription::unsubscribe);
    }

    /**
     * 自动扫描包及其子包下所有带 {@link EventSubscriber} 的类，
     * 并实例化后注册其 {@link SubscribeEvent} 方法。
     */
    public EventSubscription registerAnnotatedInPackage(String basePackage) {
        Objects.requireNonNull(basePackage, "basePackage must not be null");
        if (basePackage.isBlank()) {
            throw new IllegalArgumentException("basePackage must not be blank");
        }

        List<EventSubscription> subscriptions = new ArrayList<>();
        for (Class<?> clazz : scanAnnotatedClasses(basePackage)) {
            Object listenerOwner = instantiateListener(clazz);
            subscriptions.add(registerAnnotated(listenerOwner));
        }
        return () -> subscriptions.forEach(EventSubscription::unsubscribe);
    }

    public <E extends Event> EventSubscription register(Class<E> eventType, EventListener<? super E> listener) {
        return register(eventType, listener, EventPriority.NORMAL, false);
    }

    public <E extends Event> EventSubscription register(Class<E> eventType,
                                                        EventListener<? super E> listener,
                                                        EventPriority priority,
                                                        boolean receiveCanceled) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        Objects.requireNonNull(priority, "priority must not be null");

        ListenerEntry<E> entry = new ListenerEntry<>(eventType, listener, priority, receiveCanceled);
        CopyOnWriteArrayList<ListenerEntry<? extends Event>> bucket =
                listeners.computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>());
        bucket.add(entry);
        bucket.sort(Comparator.comparingInt((ListenerEntry<? extends Event> e) -> e.priority().level()).reversed());

        return () -> bucket.remove(entry);
    }

    private <E extends Event> EventSubscription registerAnnotatedMethod(Class<E> eventType,
                                                                        Object listenerOwner,
                                                                        Method method,
                                                                        SubscribeEvent annotation) {
        return register(
                eventType,
                event -> invokeAnnotatedMethod(listenerOwner, method, event),
                annotation.priority(),
                annotation.receiveCanceled());
    }

    private static void validateAnnotatedMethod(Class<?> listenerClass, Method method) {
        if (method.getParameterCount() != 1) {
            throw new IllegalArgumentException("@SubscribeEvent method must have exactly one parameter: " +
                                               listenerClass.getName() + "#" + method.getName());
        }
        if (!Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
            throw new IllegalArgumentException("@SubscribeEvent parameter must be Event subtype: " +
                                               listenerClass.getName() + "#" + method.getName());
        }
        if (method.getReturnType() != void.class) {
            throw new IllegalArgumentException("@SubscribeEvent method return type must be void: " +
                                               listenerClass.getName() + "#" + method.getName());
        }
    }

    private static void invokeAnnotatedMethod(Object listenerOwner, Method method, Event event) {
        try {
            method.invoke(listenerOwner, event);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot access listener method: " + method, ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw new IllegalStateException("Listener method threw exception: " + method, cause);
        }
    }

    private static Set<Class<?>> scanAnnotatedClasses(String basePackage) {
        Set<Class<?>> result = new LinkedHashSet<>();
        String packagePath = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = EventBus.class.getClassLoader();
        }

        try {
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
                    scanFileSystem(basePackage, new File(filePath), result, classLoader);
                } else if ("jar".equals(protocol)) {
                    scanJar(basePackage, url, result, classLoader);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to scan package for event subscribers: " + basePackage, ex);
        }
        return result;
    }

    private static void scanFileSystem(String basePackage,
                                       File dir,
                                       Set<Class<?>> out,
                                       ClassLoader classLoader) {
        if (dir == null || !dir.exists()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanFileSystem(basePackage + "." + file.getName(), file, out, classLoader);
            } else if (file.getName().endsWith(".class")) {
                String simpleName = file.getName().substring(0, file.getName().length() - 6);
                String className = basePackage + "." + simpleName;
                loadIfAnnotated(className, out, classLoader);
            }
        }
    }

    private static void scanJar(String basePackage,
                                URL url,
                                Set<Class<?>> out,
                                ClassLoader classLoader) {
        try {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                String packagePath = basePackage.replace('.', '/');
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(packagePath) || !name.endsWith(".class")) {
                        continue;
                    }
                    String className = name.substring(0, name.length() - 6).replace('/', '.');
                    loadIfAnnotated(className, out, classLoader);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to scan jar for event subscribers: " + basePackage, ex);
        }
    }

    private static void loadIfAnnotated(String className, Set<Class<?>> out, ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
            if (clazz.isAnnotationPresent(EventSubscriber.class)
                    && !clazz.isInterface()
                    && !Modifier.isAbstract(clazz.getModifiers())) {
                out.add(clazz);
            }
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Failed to load class while scanning event subscribers: " + className, ex);
        }
    }

    private static Object instantiateListener(Class<?> clazz) {
        try {
            var ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Event subscriber class must have no-arg constructor: " + clazz.getName(), ex);
        }
    }

    public <E extends Event> E post(E event) {
        Objects.requireNonNull(event, "event must not be null");

        List<ListenerEntry<? extends Event>> entries = collectApplicable(event.getClass());
        entries.sort(Comparator.comparingInt((ListenerEntry<? extends Event> e) -> e.priority().level()).reversed());

        for (ListenerEntry<? extends Event> raw : entries) {
            @SuppressWarnings("unchecked")
            ListenerEntry<E> entry = (ListenerEntry<E>) raw;
            if (event.isCanceled() && !entry.receiveCanceled()) {
                continue;
            }
            entry.listener().onEvent(event);
        }
        return event;
    }

    public int listenerCount(Class<? extends Event> eventType) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        List<ListenerEntry<? extends Event>> bucket = listeners.get(eventType);
        return bucket == null ? 0 : bucket.size();
    }

    public void clear() {
        listeners.clear();
    }

    private List<ListenerEntry<? extends Event>> collectApplicable(Class<?> eventClass) {
        List<ListenerEntry<? extends Event>> result = new ArrayList<>();
        for (Map.Entry<Class<? extends Event>, CopyOnWriteArrayList<ListenerEntry<? extends Event>>> bucket : listeners.entrySet()) {
            if (bucket.getKey().isAssignableFrom(eventClass)) {
                result.addAll(bucket.getValue());
            }
        }
        return result;
    }

    private record ListenerEntry<E extends Event>(Class<E> eventType,
                                                  EventListener<? super E> listener,
                                                  EventPriority priority,
                                                  boolean receiveCanceled) {
    }
}
