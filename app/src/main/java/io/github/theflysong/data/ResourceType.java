package io.github.theflysong.data;

/**
 * 
 *
 * @author theflysong
 * @date 2026年4月14日
 */
public class ResourceType {
    public String category;
    public String type;

    public ResourceType(String category, String type) {
        this.category = category;
        this.type = type;
    }

    public static ResourceType of(String category, String type) {
        return new ResourceType(category, type);
    }

    public static final ResourceType SHADER = of("assets", "shader");
    public static final ResourceType TEXTURE = of("assets", "texture");
    public static final ResourceType VERTEX_LAYOUT = of("assets", "vertexlayout");
    public static final ResourceType MODEL = of("assets", "model");
    public static final ResourceType SPRITE = of("assets", "sprite");
    public static final ResourceType SOUND = of("assets", "sound");
    public static final ResourceType MUSIC = of("assets", "music");
    public static final ResourceType TEXT = of("data", "text");
    public static final ResourceType LEVEL = of("data", "level");

    @Override
    public String toString() {
        return category + "/" + type;
    }
}
