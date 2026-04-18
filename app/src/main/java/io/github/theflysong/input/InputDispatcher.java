package io.github.theflysong.input;

import io.github.theflysong.event.GameEvents;
import io.github.theflysong.event.MouseClickEvent;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * 输入分发层：先发布事件，再按条件路由到不同处理器。
 */
public final class InputDispatcher {
    private final List<Route> routes = new CopyOnWriteArrayList<>();

    public void register(@NonNull String name,
                         @NonNull Predicate<MouseInputContext> condition,
                         @NonNull MouseInputHandler handler) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        routes.add(new Route(name, condition, handler));
    }

    public void clear() {
        routes.clear();
    }

    /**
     * @return true 表示输入已被某个处理器消费，false 表示未消费或事件被取消。
     */
    public boolean dispatch(MouseInputContext context) {
        Objects.requireNonNull(context, "context must not be null");

        MouseClickEvent clickEvent = new MouseClickEvent(context);

        GameEvents.BUS.post(clickEvent);
        if (clickEvent.isCanceled()) {
            return false;
        }

        for (Route route : routes) {
            if (!route.condition().test(context)) {
                continue;
            }
            if (route.handler().handle(context)) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    public interface MouseInputHandler {
        /**
         * @return true 表示消费当前输入，Dispatcher 将停止后续路由。
         */
        boolean handle(MouseInputContext context);
    }

    private record Route(String name, Predicate<MouseInputContext> condition, MouseInputHandler handler) {
    }
}
