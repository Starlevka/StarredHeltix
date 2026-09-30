package set.starlev.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.PrimitiveTopology
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.LayeringTransform
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.resources.Identifier

/**
 * Свой world RenderType для заливок через submitCustomGeometry (паттерн Skysoft):
 * ванильный RenderTypes.debugQuads() без VIEW_OFFSET_Z_LAYERING — заливка
 * на поверхности блока z-fighting'ится и пропадает под углами.
 * (Линии хитбоксов рисуем проверенным submitShapeOutline — кастомная геометрия
 * с lines() из уровневой коллекции не видна.)
 */
object SHRenderTypes {

    /** Заливка боксов/граней (аналог DEBUG_QUADS + z-смещение, как FILLED_SHAPE у Skysoft). */
    val FILLED: RenderType by lazy {
        val pipeline = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(com.mojang.blaze3d.pipeline.ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .withLocation(Identifier.fromNamespaceAndPath("starredheltix", "pipeline/filled_offset"))
            .build()
        val registered = RenderPipelines.register(pipeline)
        RenderType.create(
            "starredheltix_filled_offset",
            RenderSetup.builder(registered)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .sortOnUpload()
                .createRenderSetup()
        )
    }
}
