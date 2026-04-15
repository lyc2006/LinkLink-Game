#version 330 core
layout (location = 0) in vec2 vertex;
layout (location = 1) in vec2 tex_coord;

out vec2 tex_coords;

uniform mat4 m4_model;
uniform mat4 m4_projection;

void main()
{
    tex_coords = tex_coord;
    gl_Position = m4_projection * m4_model * vec4(vertex, 0.0, 1.0);
}
