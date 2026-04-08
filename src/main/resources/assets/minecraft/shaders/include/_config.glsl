// ============================================================
// TEXT EFFECTS CONFIGURATION
// ============================================================
// Define color-to-effect mappings here.
// Usage: TEXT_EFFECT(R, G, B) { apply_effect(); }
// ============================================================

// --- &z Formatting Effects ---

// Rainbow trigger (255, 255, 252)
TEXT_EFFECT_WITH_SHADOW(255, 255, 252) {
    apply_rainbow();
}

// Spin trigger (255, 255, 247)
TEXT_EFFECT_WITH_SHADOW(255, 255, 247) {
    apply_sequential_spin();
}

// --- Player Nick Effects ---

// Starlev (255, 255, 245) - Rainbow + Wavy (Special ID 10)
TEXT_EFFECT_WITH_SHADOW(255, 255, 245) {
    apply_wavy();
    apply_rainbow();
}

// MegaChromeX (170, 0, 243) - Dark Red Fade + Shake
TEXT_EFFECT_WITH_SHADOW(170, 0, 243) {
    apply_fade();
    apply_shake();
    apply_color(rgb(170, 0, 0));
}

// maksimwain (51, 224, 255) - Cyan Wavy (ID 2)
TEXT_EFFECT_WITH_SHADOW(51, 224, 255) {
    apply_wavy();
}

// ridar (100, 50, 200) - Vivid Purple-Blue Dynamic Gradient
TEXT_EFFECT_WITH_SHADOW(100, 50, 200) {
    apply_dynamic_gradient(rgb(180, 50, 255), rgb(50, 150, 255), 2.0, 300.0);
}

// zinanel0 (50, 150, 200) - Cyan-Blue Dynamic Gradient
TEXT_EFFECT_WITH_SHADOW(50, 150, 200) {
    apply_dynamic_gradient(rgb(50, 200, 255), rgb(30, 100, 200), 2.0, 300.0);
}

// Apostol312 (170, 170, 170) - Static Gray
TEXT_EFFECT_WITH_SHADOW(170, 170, 170) {
    apply_color(rgb(170, 170, 170));
}

// Timyr12 (30, 144, 255) - Dodger Blue Wavy
TEXT_EFFECT_WITH_SHADOW(30, 144, 255) {
    apply_wavy();
}

// ZurGames (128, 96, 255) - Cyan-Purple Gradient + Wavy
TEXT_EFFECT_WITH_SHADOW(128, 96, 255) {
    apply_wavy();
    apply_dynamic_gradient(rgb(80, 200, 255), rgb(180, 80, 255), 2.0, 300.0);
}

// NiKoMao (255, 128, 192) - Pink Wavy
TEXT_EFFECT_WITH_SHADOW(255, 128, 192) {
    apply_wavy();
}
