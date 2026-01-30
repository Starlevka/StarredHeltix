#version 150

void applyEffect(inout vec4 vertex, int effectID, vec4 baseColor, bool isShadow) {
    vec4 displayColor = isShadow ? vec4(baseColor.rgb * 0.25, 1.0) : baseColor;

    if (effectID == 1) {
        float speed = SHAKE_SPEED;
        float intensity = SHAKE_INTENSITY;
        vertexColor = displayColor * texelFetch(Sampler2, UV2 / 16, 0);
        processShakeEffect(vertex, speed, intensity);
        return;
    }

    if (effectID == 2) {
        float speed = WAVE_SPEED;
        applyProjection(vertex);
        vertexColor = displayColor * texelFetch(Sampler2, UV2 / 16, 0);
        applyWaveEffect(speed);
        finalize();
        return;
    }

    if (effectID == 3) {
        float speed = RAINBOW_SPEED;
        processRainbowEffect(vertex, speed);
        return;
    }

    if (effectID == 4) {
        float speed = BOUNCE_SPEED;
        float amplitude = BOUNCE_AMPLITUDE;
        vertexColor = displayColor * texelFetch(Sampler2, UV2 / 16, 0);
        float vertexId = mod(float(gl_VertexID), 4.0);
        float time = GameTime * speed;
        if (vertex.z <= 0.0) {
            if (vertexId == 3.0 || vertexId == 0.0) {
                vertex.y += cos(time) * amplitude + max(cos(time) * amplitude, 0.0);
            }
        } else {
            if (vertexId == 3.0 || vertexId == 0.0) {
                vertex.y -= cos(time) * (amplitude * 30.0) + max(cos(time) * (amplitude * 30.0), 0.0);
            }
        }
        applyProjection(vertex);
        finalize();
        return;
    }

    if (effectID == 5) {
        float speed = BLINK_SPEED;
        vertexColor = displayColor * texelFetch(Sampler2, UV2 / 16, 0);
        float blink = step(0.5, fract(GameTime * speed * 1200.0));
        if (blink < 0.5) {
            gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
            finalize();
            return;
        }
        applyProjection(vertex);
        finalize();
        return;
    }

    if (effectID == 6) {
        float speed = PULSE_SPEED;
        float size = PULSE_SIZE;
        vertexColor = displayColor * texelFetch(Sampler2, UV2 / 16, 0);
        processPulse(vertex, speed, size);
        return;
    }

    if (effectID == 7) {
        float speed = SPIN_SPEED;
        float axis = 0.0;
        vertexColor = displayColor * texelFetch(Sampler2, UV2 / 16, 0);
        processSpin(vertex, speed, axis);
        finalize();
        return;
    }

    if (effectID == 8) {
        float speed = SPIN_SPEED;
        float axis = 0.0;
        vertexColor = displayColor * texelFetch(Sampler2, UV2 / 16, 0);
        processDelayedSpin(vertex, speed, axis);
        finalize();
        return;
    }

    if (effectID == 9) {
        float speed = FADE_SPEED;
        vertexColor = displayColor * texelFetch(Sampler2, UV2 / 16, 0);
        processFadeEffect(vertex, speed);
        return;
    }

    if (effectID == 10) {
        float waveSpeed = WAVE_SPEED;
        float rainbowSpeed = RAINBOW_SPEED;
        float xPos = vertex.x;
        float yPos = vertex.y;
        applyProjection(vertex);
        applyWaveEffect(waveSpeed);
        applyHueColor(rainbowSpeed, xPos, yPos);
        finalize();
        return;
    }

    if (effectID == 11) {
        float bounceSpeed = BOUNCE_SPEED;
        float bounceAmp = BOUNCE_AMPLITUDE;
        float rainbowSpeed = RAINBOW_SPEED;
        float xPos = vertex.x;
        float yPos = vertex.y;
        float vertexId = mod(float(gl_VertexID), 4.0);
        float time = GameTime * bounceSpeed;
        if (vertex.z <= 0.0) {
            if (vertexId == 3.0 || vertexId == 0.0) {
                vertex.y += cos(time) * bounceAmp + max(cos(time) * bounceAmp, 0.0);
            }
        } else {
            if (vertexId == 3.0 || vertexId == 0.0) {
                vertex.y -= cos(time) * (bounceAmp * 30.0) + max(cos(time) * (bounceAmp * 30.0), 0.0);
            }
        }
        applyHueColor(rainbowSpeed, xPos, yPos);
        applyProjection(vertex);
        finalize();
        return;
    }

    if (effectID == 12) {
        float shakeSpeed = SHAKE_SPEED;
        float shakeIntensity = SHAKE_INTENSITY;
        float fadeSpeed = FADE_SPEED;
        
        float timeShake = GameTime * 1200.0 * (shakeSpeed <= 0.0 ? 8.0 : shakeSpeed);
        float charSeed = hashShake(floor(vertex.x / 8.0) * 127.1);
        float shakeX = noiseShake(timeShake * 1.0 + charSeed * 100.0) * 2.0 - 1.0;
        float shakeY = noiseShake(timeShake * 1.3 + charSeed * 200.0 + 50.0) * 2.0 - 1.0;
        shakeX += sin(timeShake * 0.7 + charSeed * 6.28) * 0.3;
        shakeY += cos(timeShake * 0.9 + charSeed * 6.28 + 1.57) * 0.3;
        float burstPhase = noiseShake(timeShake * 0.15 + charSeed * 50.0);
        float burstIntensity = smoothstep(0.6, 0.8, burstPhase) * (1.0 - smoothstep(0.8, 1.0, burstPhase));
        float currentIntensity = (shakeIntensity <= 0.0 ? 1.0 : shakeIntensity) * (0.6 + burstIntensity * 0.8);
        vertex.x += shakeX * currentIntensity;
        vertex.y += shakeY * currentIntensity;
        
        applyProjection(vertex);
        
        float alphaFade = sin(GameTime * 3000.0 * (fadeSpeed <= 0.0 ? 0.5 : fadeSpeed));
        alphaFade = (alphaFade + 1.0) * 0.5;
        
        vertexColor = displayColor * texelFetch(Sampler2, UV2 / 16, 0);
        vertexColor.a *= alphaFade;
        
        finalize();
        return;
    }
}
