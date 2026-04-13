// Эффект переключения слоёв: мигание между основным слоем и тенью (задним слоем)
void applyLayerToggle(inout vec4 vertex, vec4 baseColor) {
    applyProjection(vertex);
    
    vec4 texColor = texelFetch(Sampler2, UV2 / 16, 0);
    
    // Переключение между полной видимостью и тенью (0.25 от яркости)
    // Скорость мигания 2 раза в секунду
    float toggle = step(0.5, fract(GameTime * 2.0)); // 0 или 1
    
    // toggle = 1 => полная видимость (передний слой)
    // toggle = 0 => тень, затемнённый вид (задний слой)
    float brightness = mix(0.25, 1.0, toggle);
    
    vertexColor = vec4(baseColor.rgb * brightness, 1.0) * texColor;
    finalize();
}
