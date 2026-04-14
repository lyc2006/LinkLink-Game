package io.theflysong.github.data;

/**
 * Resource Location，资源位置，表示一个资源在游戏中的唯一标识
 * 分为命名空间 + 类型 + 路径
 *
 * @author theflysong
 * @date 2026年4月14日
 */
public record ResLoc(String namespace, ResType type, String path) {
    public ResLoc(String namespace, ResType type, String path) {
        this.namespace = namespace;
        this.type = type;
        this.path = path;
    }

    @Override
    public String toString() {
        return namespace + ":" + type + "/" + path;
    }

    public String toPath() {
        return type.category + "/" + namespace + "/" + type.type + "/" + path;
    }
}
