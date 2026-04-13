# TheSalt's Text Effects

Add dynamic text animations to your Minecraft world! This resource pack provides **customizable text effects**. Use with `/title`, `/tellraw`, and similar text components.

> This resource pack is **not compatible** with other packs that use the `rendertype_text` core shader.

> Text Display entities are **not supported**.

## How It Works

Define color-to-effect mappings in `_config.glsl`. When text uses a matching RGB color, the configured effect is applied.

## Preview

![23](https://github.com/user-attachments/assets/1ed9fd46-a769-4256-bc36-758adc431602)

<img width="1920" height="1009" alt="2026-03-08_22 50 06" src="https://github.com/user-attachments/assets/3c988bdb-a60d-4db7-ba49-a50a3ab8f918" />

<img width="1920" height="1009" alt="2026-03-08_22 49 24" src="https://github.com/user-attachments/assets/a4d76af1-aae6-4132-b3ac-4e39eb6344f9" />

## Can I use this on my map/server?

Yes, provided that you keep the [license file](./LICENSE) within the resource pack and add `Credit. TheSalt's Text Effects` to the description in `pack.mcmeta`.

## Available Effects

| Effect                                                                                                                               | Description               |
| ------------------------------------------------------------------------------------------------------------------------------------ | ------------------------- |
| [`apply_shake(speed, intensity)`](#apply_shakefloat-speed-float-intensity)                                                           | Random shaking            |
| [`apply_wavy(speed, amplitude, xFrequency)`](#apply_wavyfloat-speed-float-amplitude-float-xfrequency)                                | Wave animation            |
| [`apply_rainbow(speed)`](#apply_rainbowfloat-speed)                                                                                  | Rainbow color cycle       |
| [`apply_bouncy(speed, amplitude)`](#apply_bouncyfloat-speed-float-amplitude)                                                         | Bounce animation          |
| [`apply_blinking(speed)`](#apply_blinkingfloat-speed)                                                                                | Blink on/off              |
| [`apply_pulse(speed, size)`](#apply_pulsefloat-speed-float-size)                                                                     | Grow/Shrink animation     |
| [`apply_spin(speed)`](#apply_spinfloat-speed)                                                                                        | Rotation                  |
| [`apply_sequential_spin(speed)`](#apply_sequential_spinfloat-speed)                                                                  | Sequential character spin |
| [`apply_fade(speed)`](#apply_fadefloat-speed)                                                                                        | Fade in/out               |
| [`apply_iterating(speed, space)`](#apply_iteratingfloat-speed-float-space)                                                           | Sequential jump           |
| [`apply_glitch(speed, intensity)`](#apply_glitchfloat-speed-float-intensity)                                                         | Random displacement       |
| [`apply_scale(scale, offsetX, offsetY)`](#apply_scalefloat-scale-float-offsetx-float-offsety)                                        | Scale text                |
| [`apply_offset(offsetX, offsetY)`](#apply_offsetfloat-offsetx-float-offsety)                                                         | Move text position        |
| [`apply_gradient(startColor, endColor, direction)`](#apply_gradientvec3-startcolor-vec3-endcolor-float-direction)                    | Static linear gradient    |
| [`apply_dynamic_gradient(start, end, dir, speed)`](#apply_dynamic_gradientvec3-startcolor-vec3-endcolor-float-direction-float-speed) | Animated moving gradient  |
| [`apply_lava(speed)`](#apply_lavafloat-speed)                                                                                        | Flowing lava effect       |
| [`apply_color(vec3 color)`](#apply_colorvec3-color)                                                                                  | Override display color    |

---

## Configuration

Edit `assets/minecraft/shaders/include/_config.glsl` to customize effects.

### Basic Syntax

```glsl
TEXT_EFFECT(R, G, B) {
    apply_effect();
}
```

### Shadow Support

> [!WARNING]
> The internal check divides each RGB component by 4 (and rounds the result) to detect if a color is a shadow.
> Therefore, it is highly recommended to use RGB values that are **divisible by 4**. If you assign multiple effects with trigger colors that are very close to each other (difference less than 4), **their calculated shadow colors might overlap, causing incorrect effects to be applied.**

To apply the same effect to both text and shadow:

```glsl
TEXT_EFFECT_WITH_SHADOW(255, 255, 86) {
    apply_shake();
}
```

```glsl
// Text effect (255, 255, 86)
TEXT_EFFECT(255, 255, 86) {
    apply_shake();
}

// Shadow effect (255/4, 255/4, 86/4) = (63, 63, 21)
TEXT_EFFECT(63, 63, 21) {
    apply_wavy();
}
```

### Examples

```glsl
// Yellow text (#FFFF56) triggers shake effect
TEXT_EFFECT(255, 255, 86) {
    apply_shake();
}

// Combining Wavy and Rainbow
TEXT_EFFECT(255, 255, 95) {
    apply_wavy();
    apply_rainbow();
}

// Custom color with fast shake
TEXT_EFFECT(200, 100, 50) {
    apply_shake(2.0, 1.5);
}
```

### Display Color

Use `apply_color()` to display a different color than the trigger color:

```glsl
// Trigger color: (200, 100, 50)
// Display color: white (255, 255, 255)
TEXT_EFFECT(200, 100, 50) {
    apply_shake();
    apply_color(255, 255, 255);
}
```

This is useful when you want to use a specific trigger color but display the text in a different color.

---

### Detailed Functions

### `apply_color(vec3 color)`

Changes the display color of the text independently from the trigger color.

- `color`: The RGB color vector, created using `rgb(R, G, B)`.

### `apply_shake(float speed, float intensity)`

Applies a random shaking animation to the text.

- `speed`: Determines how fast the text shakes.
- `intensity`: Determines the distance the text moves while shaking.

### `apply_wavy(float speed, float amplitude, float xFrequency)`

Applies a sine wave animation to the text vertically.

- `speed`: How fast the wave moves horizontally over time.
- `amplitude`: How high/low the wave goes.
- `xFrequency`: Frequency multiplier across the text width.

### `apply_rainbow(float speed)`

Applies a continuous scrolling rainbow color effect.

- `speed`: How fast the colors cycle.

### `apply_bouncy(float speed, float amplitude)`

Makes the text bounce up and down.

- `speed`: The speed of the bouncing motion.
- `amplitude`: The maximum height of the bounce.

### `apply_blinking(float speed)`

Makes the text turn invisible and visible alternately.

- `speed`: How fast the text blinks.

### `apply_pulse(float speed, float size)`

Makes the text continuously grow and shrink.

- `speed`: The speed of the pulsation.
- `size`: The maximum scale factor during the pulse.

### `apply_spin(float speed)`

Rotates the entire text component around its origin continuously.

- `speed`: The speed of rotation.

### `apply_sequential_spin(float speed)`

Rotates each character sequentially.

- `speed`: The speed of rotation.

### `apply_fade(float speed)`

Fades the text in and out smoothly.

- `speed`: The speed of the fade transition.

### `apply_iterating(float speed, float space)`

Makes characters jump sequentially like a wave.

- `speed`: The speed of the iterating wave.
- `space`: The distance between the jumping characters.

### `apply_glitch(float speed, float intensity)`

Displaces random characters rapidly to create a glitch effect.

- `speed`: How frequently the glitch occurs.
- `intensity`: The maximum random displacement distance.

### `apply_scale(float scale, float offsetX, float offsetY)`

Scales the text and optionally offsets its position.

- `scale`: The multiplier for the text size (e.g., `1.5` for 150%).
- `offsetX` / `offsetY`: Positional shift applied after scaling.

### `apply_offset(float offsetX, float offsetY)`

Moves the text without scaling it.

- `offsetX`: The horizontal shift distance.
- `offsetY`: The vertical shift distance.

### `apply_gradient(vec3 startColor, vec3 endColor, float direction)`

Applies a static linear gradient across the text.

- `startColor` / `endColor`: RGB color vectors using `rgb(R, G, B)`.
- `direction`: Angle/direction of the gradient.

### `apply_dynamic_gradient(vec3 startColor, vec3 endColor, float direction, float speed)`

Applies an animated gradient that sweeps across the text.

- `startColor` / `endColor`: RGB color vectors via `rgb(R, G, B)`.
- `direction`: Angle/direction of the gradient.
- `speed`: How fast the gradient moves.

### `apply_lava(float speed)`

A preset dynamic gradient combining red and yellow colors to simulate flowing lava.

- `speed`: How fast the lava flows.

---

## Special Thanks

좌우반전 코드를 제공해 주신 [@베스트견과류](https://github.com/bestnuts) 님에게 감사를 표합니다.

## License

This code is under MIT License

This project includes code from [Text-Effects-by-Akis](https://github.com/YeahAkis/Text-Effects-by-Akis).
