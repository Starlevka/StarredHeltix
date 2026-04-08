#moj_import <minecraft:common.glsl>
#moj_import <minecraft:defaults.glsl>
#moj_import <minecraft:rainbow.glsl>
#moj_import <minecraft:wavy.glsl>
#moj_import <minecraft:bouncy.glsl>
#moj_import <minecraft:blinking.glsl>
#moj_import <minecraft:pulse.glsl>
#moj_import <minecraft:spin.glsl>
#moj_import <minecraft:shake.glsl>
#moj_import <minecraft:fade.glsl>
#moj_import <minecraft:iterating.glsl>
#moj_import <minecraft:glitch.glsl>
#moj_import <minecraft:gradient.glsl>
#moj_import <minecraft:scale.glsl>
#moj_import <minecraft:text_effects_api.glsl>
#moj_import <minecraft:apply_effect.glsl>

// ============================================================
// TEXT EFFECTS - Config-based System
// ============================================================
// Edit _config.glsl to customize color-effect mappings
// ============================================================

// Helper function to check shadow match and update state
bool checkAndSetShadow(ivec3 c, int R, int G, int B) {
    // Check main color (exact match)
    if (c.r == R && c.g == G && c.b == B) {
        return true;
    }
    // Check shadow color (exact 25% of main, no tolerance to avoid false matches with white text shadow)
    int shadowR = int(float(R) * 0.25 + 0.5);
    int shadowG = int(float(G) * 0.25 + 0.5);
    int shadowB = int(float(B) * 0.25 + 0.5);
    if (c.r == shadowR && c.g == shadowG && c.b == shadowB) {
        currentIsShadow = true;
        return currentIsShadow;
    }
    return false;
}

// TEXT_EFFECT macro: matches RGB color only (exact match)
#define TEXT_EFFECT(R, G, B) \
    if (c.r == R && c.g == G && c.b == B)

// TEXT_EFFECT_WITH_SHADOW macro: matches RGB color AND its shadow (exact match)
#define TEXT_EFFECT_WITH_SHADOW(R, G, B) \
    if (checkAndSetShadow(c, R, G, B))

// Helper: check if R,G match with tolerance
bool colorMatchMain(ivec3 c, int r, int g) {
    return abs(c.r - r) <= 2 && abs(c.g - g) <= 2;
}

// Helper: get effect offset from blue channel
int getEffectOffset(int actualB, int baseB) {
    int offset = actualB - baseB;
    if (offset >= 1 && offset <= 11) {
        return offset;
    }
    return 0;
}

int getEffectOffsetNegative(int actualB, int baseB) {
    int offset = baseB - actualB;
    if (offset >= 1 && offset <= 11) {
        return offset;
    }
    return 0;
}

void applyTextEffects() {
    vec4 vertex = vec4(Position, 1.0);
    ivec3 c = ivec3(Color.rgb * 255.0 + 0.5);

    // Initialize global state
    currentVertex = vertex;
    currentBaseColor = Color;
    originalBaseColor = Color; // Save original for shadow rendering
    currentIsShadow = false;
    currentApplyToShadow = false;

    // ============================================
    // Config-based color-effect mappings (player nicks + &z formatting)
    // ============================================
    #moj_import <minecraft:_config.glsl>

    // If any effect was applied, execute it
    if (hasAnyEffect()) {
        applyEffect(currentVertex, currentBaseColor, currentIsShadow);
        return;
    }

    // === No effect matched, render normally ===
    applyProjection(vertex);
    applyColorTexture();
    finalize();
}
