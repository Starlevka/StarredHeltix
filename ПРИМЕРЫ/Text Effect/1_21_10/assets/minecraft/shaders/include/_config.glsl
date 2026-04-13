// ============================================================
// TEXT EFFECTS CONFIGURATION
// ============================================================
// Define color-to-effect mappings here.
// Usage: TEXT_EFFECT(R, G, B) { apply_effect(); }
// ============================================================

// --- 1. Basic Effects ---

// Shake (#FCFC54) - Red
TEXT_EFFECT_WITH_SHADOW(252, 252, 84) {
    apply_shake();
    apply_color(rgb(255, 80, 80));
}

// Wavy (#FCFC58) - Green
TEXT_EFFECT_WITH_SHADOW(252, 252, 88) {
    apply_wavy();
    apply_color(rgb(80, 255, 80));
}

// Rainbow (#FCFC5C)
TEXT_EFFECT(252, 252, 92) {
    apply_rainbow();
}

// Bouncy (#FCFC60) - Orange
TEXT_EFFECT_WITH_SHADOW(252, 252, 96) {
    apply_bouncy();
    apply_color(rgb(255, 170, 0));
}

// Blinking (#FCFC64) - Blue
TEXT_EFFECT_WITH_SHADOW(252, 252, 100) {
    apply_blinking();
    apply_color(rgb(80, 80, 255));
}

// Pulse (#FCFC68) - Cyan
TEXT_EFFECT_WITH_SHADOW(252, 252, 104) {
    apply_pulse();
    apply_color(rgb(80, 255, 255));
}

// --- 2. Motion Effects ---

// Spin (#FCFC6C) - Pink
TEXT_EFFECT_WITH_SHADOW(252, 252, 108) {
    apply_spin();
    apply_color(rgb(255, 80, 255));
}

// Sequential Spin (#FCFC70) - White
TEXT_EFFECT_WITH_SHADOW(252, 252, 112) {
    apply_sequential_spin();
    apply_color(rgb(255, 255, 255));
}

// Fade (#FCFC74) - Yellow
TEXT_EFFECT_WITH_SHADOW(252, 252, 116) {
    apply_fade();
    apply_color(rgb(255, 255, 80));
}

// Iterating Jump (#FCFC78) - Purple
TEXT_EFFECT_WITH_SHADOW(252, 252, 120) {
    apply_iterating();
    apply_color(rgb(170, 0, 255));
}

// --- 3. Special Effects ---

// Glitch (#FCFC7C) - Red
TEXT_EFFECT_WITH_SHADOW(252, 252, 124) {
    apply_glitch();
    apply_color(rgb(255, 80, 80));
}

// --- 4. Scale / Offset Effects ---

// Scale x1.5 (#FCFC80) - Cyan
TEXT_EFFECT_WITH_SHADOW(252, 252, 128) {
    apply_scale(1.5);
    apply_color(rgb(80, 255, 255));
}

// Offset up (#FCFC84) - Orange
TEXT_EFFECT_WITH_SHADOW(252, 252, 132) {
    apply_offset(0.0, 40.0);
    apply_color(rgb(255, 170, 0));
}

// --- 5. Gradient Effects ---

// Gradient: Green → Yellow, Down (#FCFC88)
TEXT_EFFECT(252, 252, 136) {
    apply_gradient(rgb(0, 200, 0), rgb(255, 255, 0), 4.0);
}

// Dynamic Gradient: Red → Blue, Right (#FCFC8C)
TEXT_EFFECT(252, 252, 140) {
    apply_dynamic_gradient(rgb(255, 0, 0), rgb(0, 0, 255), 2.0, 500.0);
}

// Dynamic Gradient: Green → Yellow, Down (#FCFC90)
TEXT_EFFECT(252, 252, 144) {
    apply_dynamic_gradient(rgb(0, 200, 0), rgb(255, 255, 0), 4.0, 500.0);
}

// Lava (#FCFC94)
TEXT_EFFECT(252, 252, 148) {
    apply_lava();
}

// --- 5. Combinations ---

// Wavy + Rainbow (#FCFC98)
TEXT_EFFECT(252, 252, 152) {
    apply_wavy();
    apply_rainbow();
}

// Bouncy + Rainbow (#FCFC9C)
TEXT_EFFECT(252, 252, 156) {
    apply_bouncy();
    apply_rainbow();
}

// Custom Parameters (Fast Shake) (#C86432)
TEXT_EFFECT(200, 100, 50) {
    apply_shake(2.0, 1.5);
    apply_color(rgb(255, 255, 85));
}

// Shadow Support Example (Text + Shadow)
TEXT_EFFECT_WITH_SHADOW(255, 200, 200) {
    apply_wavy(5000.0, 0.5);
}
