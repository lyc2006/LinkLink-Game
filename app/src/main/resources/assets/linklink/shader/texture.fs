#version 330 core
in vec2 tex_coords;
out vec4 color;

uniform sampler2D sam_texture;
uniform vec4 uv_rect;

vec2 to_uv(vec4 rect, vec2 local_uv) {
    return rect.xy + local_uv * rect.zw;
}

void main()
{
    vec2 uv = to_uv(uv_rect, tex_coords);
    color = texture(sam_texture, uv);
}
