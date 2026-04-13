void applyEffect(inout vec4 vertex, vec4 baseColor, bool isShadow) {
    vec4 displayColor = isShadow ? vec4(baseColor.rgb * 0.25, 1.0) : baseColor;

    // ========================================
    // Phase 1: Blinking (short-circuit)
    // ========================================
    if (flagBlinking) {
        float blink = step(0.5, fract(GameTime * paramBlinkSpeed * 1200.0));
        if (blink < 0.5) {
            gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
            finalize();
            return;
        }
    }

    // ========================================
    // Phase 2: Pre-projection vertex mods
    // ========================================
    if (flagShake) {
        float charId = floor(float(gl_VertexID) / 4.0);
        float shakeTime = GameTime * 32000.0 * paramShakeSpeed;
        float noiseX = noise(charId * 10.0 + shakeTime) - 0.5;
        float noiseY = noise(charId * 10.0 - shakeTime + 100.0) - 0.5;
        vertex.x += noiseX * paramShakeIntensity;
        vertex.y += noiseY * paramShakeIntensity;
    }

    if (flagBouncy) {
        float vertexId = mod(float(gl_VertexID), 4.0);
        float bounceTime = GameTime * paramBounceSpeed;
        if (vertexId == 3.0 || vertexId == 0.0) {
            vertex.y += cos(bounceTime) * paramBounceAmplitude + max(cos(bounceTime) * paramBounceAmplitude, 0.0);
        }
    }

    if (flagPulse) {
        float pulseTime = GameTime * paramPulseSpeed * 1000.0;
        float pulseFactor = (sin(pulseTime) * 0.5 + 0.5);
        float expansion = paramPulseSize * 2.5 * pulseFactor;
        float vertexId = mod(float(gl_VertexID), 4.0);
        vec2 pulseDir = vec2(0.0);
        if (vertexId < 0.5) pulseDir = vec2(-1.0, -1.0);
        else if (vertexId < 1.5) pulseDir = vec2(-1.0, 1.0);
        else if (vertexId < 2.5) pulseDir = vec2(1.0, 1.0);
        else pulseDir = vec2(1.0, -1.0);
        pulseDir *= vec2(0.7, 1.0);
        vertex.xy += pulseDir * expansion;
    }

    if (flagIterating) {
        float iterSpeed = paramIteratingSpeed;
        float iterSpace = paramIteratingSpace;
        if (iterSpeed <= 0.0) iterSpeed = 1.0;
        if (iterSpace <= 0.0) iterSpace = 1.0;
        float iterCharX = floor(vertex.x / 8.0);
        float iterTime = GameTime * 18000.0 * iterSpeed;
        float iterX = mod(iterCharX * 0.4 - iterTime, (5.0 * iterSpace) * TAU);
        if (iterX > TAU) iterX = TAU;
        vertex.y -= (-cos(iterX) * 0.5 + 0.5) * 2.0;
    }

    if (flagGlitch) {
        float gSpeed = paramGlitchSpeed;
        float gIntensity = paramGlitchIntensity;
        if (gSpeed <= 0.0) gSpeed = 1.0;
        if (gIntensity <= 0.0) gIntensity = 2.0;
        float glitchTime = floor(GameTime * 32000.0 * gSpeed);
        float glitchCharX = floor(vertex.x / 8.0);
        float glitchTrigger = random(vec2(glitchTime * 0.1, 0.0));
        if (glitchTrigger > 0.7) {
            vertex.x += (random(vec2(glitchCharX + glitchTime, 1.0)) - 0.5) * gIntensity * 4.0;
        }
        if (glitchTrigger > 0.85) {
            vertex.y += (random(vec2(glitchCharX - glitchTime + 50.0, 2.0)) - 0.5) * gIntensity;
        }
    }

    if (flagScale) {
        float scaleVid = mod(float(gl_VertexID), 4.0);
        vec2 scaleDir;
        if      (scaleVid < 0.5) scaleDir = vec2(-1.0, -1.0);
        else if (scaleVid < 1.5) scaleDir = vec2(-1.0,  1.0);
        else if (scaleVid < 2.5) scaleDir = vec2( 1.0,  1.0);
        else                     scaleDir = vec2( 1.0, -1.0);
        float actualExpansion = (paramScaleFactor - 1.0) * 4.0;
        scaleDir *= vec2(0.7, 1.0);
        vertex.xy += scaleDir * actualExpansion + vec2(paramScaleOffsetX, paramScaleOffsetY);
    }

    // Save pre-projection position for color effects
    float preX = vertex.x;
    float preY = vertex.y;

    // ========================================
    // Phase 3: Projection
    // ========================================
    if (flagSequentialSpin) {
        processSequentialSpin(vertex, paramSpinSpeed, 0.0);
    } else if (flagSpin) {
        processSpin(vertex, paramSpinSpeed, 0.0);
    } else {
        applyProjection(vertex);
    }

    // ========================================
    // Phase 4: Post-projection effects
    // ========================================
    if (flagWavy) {
        gl_Position.y += sin(GameTime * paramWaveSpeed + (Position.x * paramWaveXFrequency)) * (paramWaveAmplitude / 150.0);
    }

    // ========================================
    // Phase 5: Color
    // ========================================
    if (flagRainbow) {
        applyHueColor(paramRainbowSpeed, preX, preY);
    } else if (flagDynamicGradient) {
        float s = isShadow ? 0.25 : 1.0;
        int dynDir = int(paramDynGradientDirection);
        float spatial;
        if      (dynDir == 0) spatial =  preY;
        else if (dynDir == 1) spatial =  preX + preY;
        else if (dynDir == 2) spatial =  preX;
        else if (dynDir == 3) spatial =  preX - preY;
        else if (dynDir == 4) spatial = -preY;
        else if (dynDir == 5) spatial = -preX - preY;
        else if (dynDir == 6) spatial = -preX;
        else                  spatial = -preX + preY;
        float dynT = 1.0 - abs(fract(GameTime * paramDynGradientSpeed + spatial * 0.01) * 2.0 - 1.0);
        vec3 dynColor = mix(paramDynGradientStart * s, paramDynGradientEnd * s, dynT);
        vec4 texColor = texelFetch(Sampler2, UV2 / 16, 0);
        vertexColor = vec4(dynColor, 1.0) * texColor;
    } else if (flagGradient) {
        float s = isShadow ? 0.25 : 1.0;
        float vid = mod(float(gl_VertexID), 4.0);
        float x_t = (vid == 2.0 || vid == 3.0) ? 1.0 : 0.0;
        float y_t = (vid == 1.0 || vid == 2.0) ? 1.0 : 0.0;
        int gradDir = int(paramGradientDirection);
        float gradT;
        if      (gradDir == 0) gradT = 1.0 - y_t;
        else if (gradDir == 1) gradT = (x_t + (1.0 - y_t)) * 0.5;
        else if (gradDir == 2) gradT = x_t;
        else if (gradDir == 3) gradT = (x_t + y_t) * 0.5;
        else if (gradDir == 4) gradT = y_t;
        else if (gradDir == 5) gradT = ((1.0 - x_t) + y_t) * 0.5;
        else if (gradDir == 6) gradT = 1.0 - x_t;
        else                   gradT = ((1.0 - x_t) + (1.0 - y_t)) * 0.5;
        vec3 gradColor = mix(paramGradientStart * s, paramGradientEnd * s, gradT);
        vec4 texColor = texelFetch(Sampler2, UV2 / 16, 0);
        vertexColor = vec4(gradColor, 1.0) * texColor;
    } else {
        vertexColor = displayColor * texelFetch(Sampler2, UV2 / 16, 0);
    }

    // ========================================
    // Phase 6: Alpha modifiers
    // ========================================
    if (flagFade) {
        float fadeAlpha = sin(GameTime * 3000.0 * paramFadeSpeed);
        fadeAlpha = (fadeAlpha + 1.0) * 0.5;
        vertexColor.a *= fadeAlpha;
    }

    // ========================================
    // Phase 7: Finalize
    // ========================================
    finalize();
}
