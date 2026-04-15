#version 330 core
in vec2 TexCoords;
out vec4 color;

uniform sampler2D sam_texture;
uniform vec4      v4_spriteColor;

void main()
{
    color = vec4(v4_spriteColor, 1.0) * texture(sam_texture, TexCoords);
}