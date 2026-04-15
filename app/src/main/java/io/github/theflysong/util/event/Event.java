package io.github.theflysong.util.event;

/**
 * 事件基类。
 *
 * 说明：
 * 1. 每个事件都有创建时间戳。
 * 2. 支持取消语义，事件总线可据此中止后续监听器（除非监听器声明接收已取消事件）。
 */
public class Event {
    private boolean canceled;

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        this.canceled = true;
    }

    public void uncancel() {
        this.canceled = false;
    }
}
