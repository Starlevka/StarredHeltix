package set.starlev.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.MultiBufferSource

object RenderEngine {
    private val worldRenderers = mutableListOf<WorldRenderer>()
    private val hudRenderers = mutableListOf<HudRenderer>()

    @JvmStatic
    fun registerWorld(renderer: WorldRenderer) = worldRenderers.add(renderer)

    @JvmStatic
    fun registerHud(renderer: HudRenderer) = hudRenderers.add(renderer)

    @JvmStatic
    fun renderWorld(stack: PoseStack, buffer: MultiBufferSource, delta: Float) {
        worldRenderers.forEach { it.render(stack, buffer, delta) }
    }

    @JvmStatic
    fun renderHud(graphics: GuiGraphics, delta: Float) {
        hudRenderers.forEach { it.render(graphics, delta) }
    }

    interface WorldRenderer {
        fun render(stack: PoseStack, buffer: MultiBufferSource, delta: Float)
    }

    interface HudRenderer {
        fun render(graphics: GuiGraphics, delta: Float)
    }
}
