#moj_import <minecraft:common.glsl>
#moj_import <minecraft:config.glsl>
#moj_import <minecraft:rainbow.glsl>
#moj_import <minecraft:wavy.glsl>
#moj_import <minecraft:bouncy.glsl>
#moj_import <minecraft:blinking.glsl>
#moj_import <minecraft:pulse.glsl>

#moj_import <minecraft:spin.glsl>
#moj_import <minecraft:shake.glsl>
#moj_import <minecraft:fade.glsl>

bool colorMatchMain(ivec3 c, int r, int g) {
    return abs(c.r - r) <= 2 && abs(c.g - g) <= 2;
}

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

#moj_import <minecraft:apply_effect.glsl>

void applyTextEffects() {
    vec4 vertex = vec4(Position, 1.0);
    ivec3 c = ivec3(Color.rgb * 255.0 + 0.5);

    int effectID = 0;
    vec4 baseColor = vec4(1.0);

    int alpha = int(Color.a * 255.0 + 0.5);
    if (alpha >= 242 && alpha <= 254) {
        effectID = 255 - alpha;
        baseColor = vec4(Color.rgb * 4.0, 1.0);
        applyEffect(vertex, effectID, baseColor, true);
        return;
    }

    if (colorMatchMain(c, 0, 0)) {
        effectID = getEffectOffset(c.b, 0);
        if (effectID > 0) {
            baseColor = vec4(0.0, 0.0, 0.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }
        
        effectID = getEffectOffset(c.b, 170);
        if (effectID > 0) {
            baseColor = vec4(0.0, 0.0, 170.0/255.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }
    }

    if (colorMatchMain(c, 0, 170)) {
        effectID = getEffectOffset(c.b, 0);
        if (effectID > 0) {
            baseColor = vec4(0.0, 170.0/255.0, 0.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }
    }

    if (colorMatchMain(c, 0, 170)) {
        effectID = getEffectOffset(c.b, 170);
        if (effectID > 0) {
            baseColor = vec4(0.0, 170.0/255.0, 170.0/255.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }
    }

    if (colorMatchMain(c, 170, 0)) {
        effectID = getEffectOffset(c.b, 0);
        if (effectID > 0) {
            baseColor = vec4(170.0/255.0, 0.0, 0.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }
    }

    if (colorMatchMain(c, 170, 0)) {
        effectID = getEffectOffset(c.b, 170);
        if (effectID > 0) {
            baseColor = vec4(170.0/255.0, 0.0, 170.0/255.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }
    }

    if (colorMatchMain(c, 255, 170)) {
        effectID = getEffectOffset(c.b, 0);
        if (effectID > 0) {
            baseColor = vec4(1.0, 170.0/255.0, 0.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }
    }

    if (colorMatchMain(c, 170, 170)) {
        effectID = getEffectOffset(c.b, 170);
        if (effectID > 0) {
            baseColor = vec4(170.0/255.0, 170.0/255.0, 170.0/255.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }
    }

    if (colorMatchMain(c, 85, 85)) {
        effectID = getEffectOffset(c.b, 85);
        if (effectID > 0) {
            baseColor = vec4(85.0/255.0, 85.0/255.0, 85.0/255.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }

        // Добавляем поддержку &9 (85, 85, 255) через отрицательный оффсет
        effectID = getEffectOffsetNegative(c.b, 255);
        if (effectID > 0) {
            baseColor = vec4(85.0/255.0, 85.0/255.0, 1.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }
    }

    if (colorMatchMain(c, 85, 255)) {
        effectID = getEffectOffset(c.b, 85);
        if (effectID > 0) {
            baseColor = vec4(85.0/255.0, 1.0, 85.0/255.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }

        // Добавляем поддержку &b (85, 255, 255) через отрицательный оффсет
        effectID = getEffectOffsetNegative(c.b, 255);
        if (effectID > 0) {
            baseColor = vec4(85.0/255.0, 1.0, 1.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }
    }

    if (colorMatchMain(c, 255, 85)) {
        effectID = getEffectOffset(c.b, 85);
        if (effectID > 0) {
            baseColor = vec4(1.0, 85.0/255.0, 85.0/255.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }
        
        // Добавляем поддержку &d (255, 85, 255) через отрицательный оффсет
        effectID = getEffectOffsetNegative(c.b, 255);
        if (effectID > 0) {
            baseColor = vec4(1.0, 85.0/255.0, 1.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }
    }

    if (colorMatchMain(c, 255, 255)) {
        // Добавляем поддержку &e (255, 255, 85) через положительный оффсет
        effectID = getEffectOffset(c.b, 85);
        if (effectID > 0) {
            baseColor = vec4(1.0, 1.0, 85.0/255.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }

        // Добавляем поддержку &f (255, 255, 255) через отрицательный оффсет
        effectID = getEffectOffsetNegative(c.b, 255);
        if (effectID > 0 && effectID != 10) { // Пропускаем ID 10, так как это Starlev
            baseColor = vec4(1.0, 1.0, 1.0, Color.a);
            applyEffect(vertex, effectID, baseColor, false);
            return;
        }
    }

    // Starlev эффект: используем ОЧЕНЬ строгие условия (255, 255, 245)
    if (c.r == 255 && c.g == 255 && c.b == 245) {
        effectID = 10;
        baseColor = vec4(1.0, 1.0, 1.0, Color.a);
        applyEffect(vertex, effectID, baseColor, false);
        return;
    }

    // Тень Starlev (63, 63, 61)
    if (c.r == 63 && c.g == 63 && c.b == 61) {
        // Эффект тени отключен, чтобы тень не переливалась вместе с текстом
        applyProjection(vertex);
        applyColorTexture();
        finalize();
        return;
    }

    // MegaChromeX эффект: используем ОЧЕНЬ строгие условия, чтобы не задеть питомцев
    // Проверяем точное соответствие R=170, G=0 и специфическое B=243
    if (c.r == 170 && c.g == 0 && c.b == 243) {
        effectID = 12;
        baseColor = vec4(170.0/255.0, 0.0, 0.0, Color.a);
        applyEffect(vertex, effectID, baseColor, false);
        return;
    }

    // Тень MegaChromeX (42, 0, 61)
    if (c.r == 42 && c.g == 0 && c.b == 61) {
        // Эффект тени отключен
        applyProjection(vertex);
        applyColorTexture();
        finalize();
        return;
    }

    applyProjection(vertex);
    applyColorTexture();
    finalize();
}
