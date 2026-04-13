// ============================================================
// TEXT EFFECTS API
// ============================================================
// Functions to apply text effects in _config.glsl
// ============================================================

// Global state for current effect being processed
vec4 currentVertex;
vec4 currentBaseColor;
bool currentIsShadow = false;
bool currentApplyToShadow = false;

// Effect flags (multiple can be active simultaneously)
bool flagShake = false;
bool flagWavy = false;
bool flagRainbow = false;
bool flagBouncy = false;
bool flagBlinking = false;
bool flagPulse = false;
bool flagSpin = false;
bool flagSequentialSpin = false;
bool flagFade = false;
bool flagIterating = false;
bool flagGlitch = false;
bool flagScale = false;
bool flagGradient = false;
bool flagDynamicGradient = false;

// Effect parameters (overridable via apply_xxx functions)
float paramShakeSpeed = SHAKE_SPEED;
float paramShakeIntensity = SHAKE_INTENSITY;
float paramWaveSpeed = WAVE_SPEED;
float paramWaveAmplitude = WAVE_AMPLITUDE;
float paramWaveXFrequency = WAVE_X_FREQUENCY;
float paramRainbowSpeed = RAINBOW_SPEED;
float paramBounceSpeed = BOUNCE_SPEED;
float paramBounceAmplitude = BOUNCE_AMPLITUDE;
float paramBlinkSpeed = BLINK_SPEED;
float paramPulseSpeed = PULSE_SPEED;
float paramPulseSize = PULSE_SIZE;
float paramSpinSpeed = SPIN_SPEED;
float paramFadeSpeed = FADE_SPEED;
float paramIteratingSpeed = ITERATING_SPEED;
float paramIteratingSpace = ITERATING_SPACE;
float paramGlitchSpeed = GLITCH_SPEED;
float paramGlitchIntensity = GLITCH_INTENSITY;
float paramScaleFactor = SCALE_FACTOR;
float paramScaleOffsetX = SCALE_OFFSET_X;
float paramScaleOffsetY = SCALE_OFFSET_Y;
vec3 paramGradientStart = GRADIENT_START;
vec3 paramGradientEnd = GRADIENT_END;
float paramGradientDirection = GRADIENT_DIRECTION;
vec3 paramDynGradientStart = DYN_GRADIENT_START;
vec3 paramDynGradientEnd = DYN_GRADIENT_END;
float paramDynGradientDirection = DYN_GRADIENT_DIRECTION;
float paramDynGradientSpeed = DYN_GRADIENT_SPEED;

// Helper function: rgb from 0-255 values
vec3 rgb(float r, float g, float b) {
    return vec3(r / 255.0, g / 255.0, b / 255.0);
}

vec4 rgba(float r, float g, float b, float a) {
    return vec4(r / 255.0, g / 255.0, b / 255.0, a);
}

// Set display color (different from trigger color)
void apply_color(vec3 color) {
    currentBaseColor.rgb = color;
}

// Check if any effect flag is active
bool hasAnyEffect() {
    return flagShake || flagWavy || flagRainbow || flagBouncy || flagBlinking ||
           flagPulse || flagSpin || flagSequentialSpin || flagFade ||
           flagIterating || flagGlitch || flagScale || flagGradient || flagDynamicGradient;
}

// --- Shake Effect ---
void apply_shake() {
    flagShake = true;
}

void apply_shake(float speed, float intensity) {
    flagShake = true;
    paramShakeSpeed = speed;
    paramShakeIntensity = intensity;
}

// --- Wavy Effect ---
void apply_wavy() {
    flagWavy = true;
}

void apply_wavy(float speed) {
    flagWavy = true;
    paramWaveSpeed = speed;
}

void apply_wavy(float speed, float amplitude) {
    flagWavy = true;
    paramWaveSpeed = speed;
    paramWaveAmplitude = amplitude;
}

void apply_wavy(float speed, float amplitude, float xFrequency) {
    flagWavy = true;
    paramWaveSpeed = speed;
    paramWaveAmplitude = amplitude;
    paramWaveXFrequency = xFrequency;
}

// --- Rainbow Effect ---
void apply_rainbow() {
    flagRainbow = true;
}

void apply_rainbow(float speed) {
    flagRainbow = true;
    paramRainbowSpeed = speed;
}

// --- Bouncy Effect ---
void apply_bouncy() {
    flagBouncy = true;
}

void apply_bouncy(float speed) {
    flagBouncy = true;
    paramBounceSpeed = speed;
}

void apply_bouncy(float speed, float amplitude) {
    flagBouncy = true;
    paramBounceSpeed = speed;
    paramBounceAmplitude = amplitude;
}

// --- Blinking Effect ---
void apply_blinking() {
    flagBlinking = true;
}

void apply_blinking(float speed) {
    flagBlinking = true;
    paramBlinkSpeed = speed;
}

// --- Pulse Effect ---
void apply_pulse() {
    flagPulse = true;
}

void apply_pulse(float speed) {
    flagPulse = true;
    paramPulseSpeed = speed;
}

void apply_pulse(float speed, float size) {
    flagPulse = true;
    paramPulseSpeed = speed;
    paramPulseSize = size;
}

// --- Spin Effect ---
void apply_spin() {
    flagSpin = true;
}

void apply_spin(float speed) {
    flagSpin = true;
    paramSpinSpeed = speed;
}

// --- Sequential Spin Effect ---
void apply_sequential_spin() {
    flagSequentialSpin = true;
}

void apply_sequential_spin(float speed) {
    flagSequentialSpin = true;
    paramSpinSpeed = speed;
}

// --- Fade Effect ---
void apply_fade() {
    flagFade = true;
}

void apply_fade(float speed) {
    flagFade = true;
    paramFadeSpeed = speed;
}

// --- Iterating Effect ---
void apply_iterating() {
    flagIterating = true;
}

void apply_iterating(float speed) {
    flagIterating = true;
    paramIteratingSpeed = speed;
}

void apply_iterating(float speed, float space) {
    flagIterating = true;
    paramIteratingSpeed = speed;
    paramIteratingSpace = space;
}

// --- Scale Effect ---
void apply_scale(float scale) {
    flagScale = true;
    paramScaleFactor = scale;
}

void apply_scale(float scale, float offsetX, float offsetY) {
    flagScale = true;
    paramScaleFactor = scale;
    paramScaleOffsetX = offsetX;
    paramScaleOffsetY = offsetY;
}

void apply_offset(float offsetX, float offsetY) {
    flagScale = true;
    paramScaleFactor = 0.0;
    paramScaleOffsetX = offsetX;
    paramScaleOffsetY = offsetY;
}

// --- Glitch Effect ---
void apply_glitch() {
    flagGlitch = true;
}

void apply_glitch(float speed, float intensity) {
    flagGlitch = true;
    paramGlitchSpeed = speed;
    paramGlitchIntensity = intensity;
}

// --- Gradient Effect ---
void apply_gradient(vec3 start, vec3 end, float direction) {
    flagGradient = true;
    paramGradientStart = start;
    paramGradientEnd = end;
    paramGradientDirection = direction;
}

void apply_gradient(vec3 start, vec3 end) {
    apply_gradient(start, end, GRADIENT_DIRECTION);
}

void apply_gradient() {
    flagGradient = true;
}

// --- Dynamic Gradient Effect ---
void apply_dynamic_gradient(vec3 start, vec3 end, float direction, float speed) {
    flagDynamicGradient = true;
    paramDynGradientStart = start;
    paramDynGradientEnd = end;
    paramDynGradientDirection = direction;
    paramDynGradientSpeed = speed;
}

void apply_dynamic_gradient(vec3 start, vec3 end, float direction) {
    apply_dynamic_gradient(start, end, direction, DYN_GRADIENT_SPEED);
}

void apply_dynamic_gradient(vec3 start, vec3 end) {
    apply_dynamic_gradient(start, end, DYN_GRADIENT_DIRECTION, DYN_GRADIENT_SPEED);
}

void apply_dynamic_gradient() {
    flagDynamicGradient = true;
}

void apply_lava(float speed) {
    apply_dynamic_gradient(rgb(255, 20, 0), rgb(255, 200, 0), 2.0, speed);
}

void apply_lava() {
    apply_lava(300.0);
}

// NOTE: To apply effect to both text and shadow, use TEXT_EFFECT_WITH_SHADOW(R, G, B) in _config.glsl
// Shadow color is automatically calculated as RGB * 0.25
