package io.theflysong.github.data;

/**
 * 
 *
 * @author theflysong
 * @date 2026年4月14日
 */
public class ResType {
    public String category;
    public String type;

    public ResType(String category, String type) {
        this.category = category;
        this.type = type;
    }

    public static ResType of(String category, String type) {
        return new ResType(category, type);
    }

    public static final ResType SHADER  = of("assets", "shader");
    public static final ResType TEXTURE = of("assets", "texture");
    public static final ResType TEXT    = of("data", "text");

    @Override
    public String toString() {
        return category + "/" + type;
    }
}
