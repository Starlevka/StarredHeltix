void processScaleEffect(inout vec4 vertex, float expansion, float offsetX, float offsetY) {
    float vertexId = mod(float(gl_VertexID), 4.0);
    vec2 dir;
    if      (vertexId < 0.5) dir = vec2(-1.0, -1.0);
    else if (vertexId < 1.5) dir = vec2(-1.0,  1.0);
    else if (vertexId < 2.5) dir = vec2( 1.0,  1.0);
    else                     dir = vec2( 1.0, -1.0);

    float actualExpansion = (expansion - 1.0) * 4.0;

    dir *= vec2(0.7, 1.0);
    vertex.xy += dir * actualExpansion + vec2(offsetX, offsetY);

    applyProjection(vertex);
    finalize();
}
