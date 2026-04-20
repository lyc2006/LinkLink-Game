#version 330 core
in vec2 tex_coords;
out vec4 color;

uniform sampler2D sam_texture;
uniform vec4 uv_rect;
uniform vec4 v4_tint_color;

vec2 to_uv(vec4 rect, vec2 local_uv) {
    return rect.xy + local_uv * (rect.zw - rect.xy);
}

void main()
{
    vec2 uv = to_uv(uv_rect, tex_coords);
    vec4 base = texture(sam_texture, uv);
    color = vec4(base.rgb * v4_tint_color.rgb, base.a * v4_tint_color.a);
}
