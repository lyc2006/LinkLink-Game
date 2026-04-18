#version 330 core
layout (location = 0) in vec2 vertex;
layout (location = 1) in vec4 in_vertex_color;

out vec4 frag_vertex_color;

uniform mat4 m4_model;
uniform mat4 m4_projection;

void main()
{
    frag_vertex_color = in_vertex_color;
    gl_Position = m4_projection * m4_model * vec4(vertex, 0.0, 1.0);
}