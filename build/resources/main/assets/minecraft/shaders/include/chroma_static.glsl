// Статичный хром эффект (без переливания цветов, как металл)
void applyChromaStatic(inout vec4 vertex, vec4 baseColor) {
    applyProjection(vertex);
    
    vec4 texColor = texelFetch(Sampler2, UV2 / 16, 0);
    
    // Металлический хром — используем светлый серебристый цвет
    vec3 chromaColor = vec3(0.9, 0.95, 1.0); // Яркий серебристо-голубой
    
    // Слегка меняем по позиции для глубины
    chromaColor += vec3(0.05) * sin(vertex.x * 0.3) * cos(vertex.y * 0.3);
    
    vertexColor = vec4(chromaColor, 1.0) * texColor;
    finalize();
}
