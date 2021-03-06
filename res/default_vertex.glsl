#version 150 core

in vec2 position;
in vec3 color;
in vec2 texcoord;

out vec3 vertexColor;
out vec2 textureCoord;

uniform mat4 ortho;

void main() {
    vertexColor = color;
    textureCoord = texcoord;
    mat4 mvp = ortho;
    gl_Position = mvp * vec4(position, 0.0, 1.0);
}
