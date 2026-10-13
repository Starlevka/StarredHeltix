package set.starlev.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.SubmitNodeCollector

object RenderEngine {
    private val worldRenderers = mutableListOf<WorldRenderer>()
    private val hudRenderers = mutableListOf<HudRenderer>()

    @JvmStatic
    fun registerWorld(renderer: WorldRenderer) = worldRenderers.add(renderer)

    @JvmStatic
    fun registerHud(renderer: HudRenderer) = hudRenderers.add(renderer)

    @JvmStatic
    fun renderWorld(context: RenderContext) {
        worldRenderers.forEach { it.render(context) }
    }

    @JvmStatic
    fun renderHud(graphics: GuiGraphicsExtractor, delta: DeltaTracker) {
        // Здесь мы на потоке рендера — отдаём его ОС как игровой (MMCSS «Games» / nice).
        // Внутри после первого раза остаётся одна volatile-проверка.
        set.starlev.utils.GameModeBoost.onRenderThread()
        hudRenderers.forEach { it.render(graphics, delta.getGameTimeDeltaPartialTick(false)) }
    }

    interface WorldRenderer {
        fun render(context: RenderContext)
    }

    interface HudRenderer {
        fun render(graphics: GuiGraphicsExtractor, delta: Float)
    }
}
