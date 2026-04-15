#version 330 core
layout (location = 0) in vec2 vertex;   // vertex
layout (location = 1) in vec2 texCoord; // texture coordinate

out vec2 TexCoords;

uniform mat4 m4_model;
uniform mat4 m4_projection;

void main()
{
    TexCoords = texCoord;
    gl_Position = m4_projection * m4_model * vec4(vertex, 0.0, 1.0);
}