// Textured Chroma Fragment Shader
// Modified with Marker Detection

#version 120

uniform float chromaSize;
uniform float timeOffset;
uniform float saturation;
uniform bool forwardDirection;

uniform sampler2D outTexture;

varying vec2 outTextureCoords;
varying vec4 outColor;

float rgb2b(vec3 rgb) {
    return max(max(rgb.r, rgb.g), rgb.b);
}

vec3 hsb2rgb_smooth(vec3 c) {
    vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    rgb = rgb * rgb * (3.0 - 2.0 * rgb); // Cubic smoothing
    return c.z * mix(vec3(1.0), rgb, c.y);
}

bool isMarker(vec3 color) {
    // Check for 0xCAFE01 (Text) or 0x323F00 (Shadow) or 0x2B333B (Shadow Alt)
    // We use a small epsilon for float comparison
    bool textMarker = (abs(color.r - 202.0/255.0) < 0.01 && abs(color.g - 254.0/255.0) < 0.01 && abs(color.b - 1.0/255.0) < 0.01);
    bool shadowMarker = (abs(color.r - 50.0/255.0) < 0.01 && abs(color.g - 63.0/255.0) < 0.01 && abs(color.b - 0.0/255.0) < 0.01);
    bool shadowMarkerAlt = (abs(color.r - 43.0/255.0) < 0.01 && abs(color.g - 51.0/255.0) < 0.01 && abs(color.b - 59.0/255.0) < 0.01);
    return textMarker || shadowMarker || shadowMarkerAlt;
}

void main() {
    vec4 originalColor = texture2D(outTexture, outTextureCoords) * outColor;

    if (isMarker(outColor.rgb)) {
        // Determine the direction chroma moves
        float fragCoord;
        if (forwardDirection) {
            fragCoord = gl_FragCoord.x - gl_FragCoord.y;
        } else {
            fragCoord = gl_FragCoord.x + gl_FragCoord.y;
        }

        // The hue takes in account the position, chroma settings, and time
        float hue = mod(((fragCoord) / chromaSize) - timeOffset, 1.0);

        // Set the color to use the new hue & original saturation/value/alpha values
        gl_FragColor = vec4(hsb2rgb_smooth(vec3(hue, saturation, rgb2b(originalColor.rgb))), originalColor.a);
    } else {
        gl_FragColor = originalColor;
    }
}
