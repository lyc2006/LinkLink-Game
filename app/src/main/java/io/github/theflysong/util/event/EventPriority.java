package io.github.theflysong.util.event;

/**
 * 监听器优先级，越高越先执行。
 */
public enum EventPriority {
    LOWEST(0),
    LOW(1),
    NORMAL(2),
    HIGH(3),
    HIGHEST(4);

    private final int level;

    EventPriority(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }
}
