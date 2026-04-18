#version 330 core

in vec4 frag_vertex_color;
out vec4 final_color;

uniform vec4 v4_color_multiplier;

void main()
{
    final_color = frag_vertex_color * v4_color_multiplier;
}