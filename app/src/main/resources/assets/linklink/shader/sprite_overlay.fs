#version 330 core
in vec2 TexCoords;
out vec4 color;

uniform sampler2D sam_texture;
uniform sampler2D sam_overlay;

uniform vec4 v4_spriteColor;
// 高光强度
uniform float f_overlayIntensity;

void main()
{
    vec4 base = texture(sam_texture, TexCoords);
    vec3 baseColor = base.rgb * v4_spriteColor.rgb;

    vec4 overlay = texture(sam_overlay, TexCoords);
    float overlayAlpha = overlay.a;

    // 高光贡献
    float overlayStrength
        = f_overlayIntensity * (1.0 - overlayAlpha);
    vec3 overlayColor = overlay.rgb * overlayStrength;

    // 最终颜色 = 基础颜色 + 高光颜色
    vec3 finalColor = baseColor + overlayColor;
    // 将其限制在 [0, 1] 范围内
    finalColor = clamp(finalColor, vec3(0.0), vec3(1.0));
    // 使用基础材质的 alpha 通道
    color = vec4(finalColor, base.a);
}