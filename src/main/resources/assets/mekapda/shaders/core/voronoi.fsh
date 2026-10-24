#version 150

in vec2 vUv;

uniform float Time;
uniform vec4 Panel;

out vec4 fragColor;

vec2 hash2(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
    return fract(sin(p) * 43758.5453);
}

void main() {
    vec2 uv = vUv;
    uv.x *= 400.0 / 260.0;
    vec2 p = uv * 5.0;
    p.y += Time * 0.07;
    p.x += sin(Time * 0.11) * 0.5;

    vec2 cell = floor(p);
    vec2 local = fract(p);
    float f1 = 8.0;
    float f2 = 8.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 offset = vec2(float(x), float(y));
            vec2 point = hash2(cell + offset);
            point = 0.5 + 0.5 * sin(Time * 0.45 + 6.2831 * point);
            vec2 delta = offset + point - local;
            float dist = dot(delta, delta);
            if (dist < f1) {
                f2 = f1;
                f1 = dist;
            } else if (dist < f2) {
                f2 = dist;
            }
        }
    }
    f1 = sqrt(f1);
    f2 = sqrt(f2);

    float border = 1.0 - smoothstep(0.0, 0.06, f2 - f1);
    float core = exp(-f1 * 8.0);
    float glow = border * 0.032 + core * 0.009;

    float mask = smoothstep(Panel.x - 0.008, Panel.x + 0.008, vUv.x)
               * (1.0 - smoothstep(Panel.z - 0.008, Panel.z + 0.008, vUv.x))
               * smoothstep(Panel.y - 0.008, Panel.y + 0.008, vUv.y)
               * (1.0 - smoothstep(Panel.w - 0.008, Panel.w + 0.008, vUv.y));

    vec3 green = vec3(0.46, 1.0, 0.54);
    fragColor = vec4(green * glow * mask, 1.0);
}
