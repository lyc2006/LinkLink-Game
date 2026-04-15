#version 330 core
in vec2 tex_coords;
out vec4 color;

uniform sampler2D sam_atlas;
uniform vec4 uv_texture;
uniform vec4 uv_overlay;
uniform int i_frame; // 当前帧数(从 0 开始)

uniform vec4 v4_sprite_color;
// 高光强度
uniform float f_overlay_intensity;

vec2 to_atlas_uv(vec4 atlas_uv, vec2 local_uv) {
    return atlas_uv.xy + local_uv * atlas_uv.zw;
}

// 这里假设每帧图形在 atlas 中是竖直排列的
vec2 to_frame_local_uv(vec2 local_uv) {
    return local_uv + vec2(0.0, i_frame);
}

void main()
{
    vec2 frame_coords = to_frame_local_uv(tex_coords);
    vec2 base_uv = to_atlas_uv(uv_texture, frame_coords);
    vec4 base = texture(sam_atlas, base_uv);
    vec3 base_color = base.rgb * v4_sprite_color.rgb;

    vec2 overlay_uv = to_atlas_uv(uv_overlay, frame_coords);
    vec4 overlay = texture(sam_atlas, overlay_uv);
    float overlay_alpha = overlay.a;

    // 高光贡献
    float overlay_strength
        = f_overlay_intensity * (1.0 - overlay_alpha);
    vec3 overlay_color = overlay.rgb * overlay_strength;

    // 最终颜色 = 基础颜色 + 高光颜色
    vec3 final_color = base_color + overlay_color;
    // 将其限制在 [0, 1] 范围内
    final_color = clamp(final_color, vec3(0.0), vec3(1.0));
    // 使用基础材质的 alpha 通道
    color = vec4(final_color, base.a);
}