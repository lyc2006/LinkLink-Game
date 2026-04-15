#version 330 core
in vec2 tex_coords;
out vec4 color;

uniform sampler2D sam_atlas;
uniform vec4 uv_texture;
uniform vec4 v4_sprite_color;
uniform int i_frame; // 当前帧数(从 0 开始)

vec2 to_atlas_uv(vec4 atlas_uv, vec2 local_uv) {
    return atlas_uv.xy + local_uv * atlas_uv.zw;
}

// 这里假设每帧图形在 atlas 中是竖直排列的
vec2 to_frame_local_uv(vec2 local_uv) {
    return local_uv + vec2(0.0, i_frame);
}

void main()
{
    // 首先计算当前帧的local UV偏移
    vec2 frame_coords = to_frame_local_uv(tex_coords);
    // 然后将其转换为 atlas UV
    vec2 atlas_uv = uv_texture.xy + frame_coords * uv_texture.zw;
    vec4 base = texture(sam_atlas, atlas_uv);
    vec3 base_color = base.rgb * v4_sprite_color.rgb;
    // 使用基础材质的 alpha 通道
    color = vec4(base_color, base.a);
}