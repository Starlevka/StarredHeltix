package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.client.gui.components.toasts.ToastManager
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence

/**
 * JS API: Всплывающее уведомление (toast) в правом верхнем углу.
 * Аналог Toast из ctjs.
 */
class JSToast {
    /**
     * Показать уведомление в стиле системного тоста Minecraft.
     * @param title заголовок (поддерживает &-цвета)
     * @param message текст уведомления (необязательно)
     * @param durationMs длительность показа в миллисекундах (по умолчанию 4000)
     */
    fun show(title: String, message: String = "", durationMs: Long = 4000) {
        val client = Minecraft.getInstance()
        client.execute {
            client.gui.toastManager().addToast(
                ScriptToast(
                    Component.literal(title.replace('&', '§')),
                    if (message.isBlank()) null else Component.literal(message.replace('&', '§')),
                    durationMs
                )
            )
        }
    }

    /** Скрыть все активные toast-уведомления (включая системные). */
    fun clearAll() {
        val client = Minecraft.getInstance()
        client.execute {
            client.gui.toastManager().clear()
        }
    }
}

/**
 * Кастомный toast в стиле SystemToast 26.2: фон "toast/system",
 * до 2 строк заголовка и произвольное количество строк сообщения.
 */
class ScriptToast(
    private val title: Component,
    private val message: Component?,
    private val durationMs: Long = 4000
) : Toast {
    private var firstFrameAt: Long = -1L
    private var wantedVisibility: Toast.Visibility = Toast.Visibility.SHOW
    private val titleLines: List<FormattedCharSequence> = splitToLength(title)
    private val messageLines: List<FormattedCharSequence> = if (message != null) splitToLength(message) else emptyList()

    override fun width(): Int = 160

    override fun height(): Int {
        val titleHeight = (titleLines.size - 1) * LINE_SPACING
        val messageHeight = maxOf(messageLines.size, 1) * LINE_SPACING
        return 20 + titleHeight + messageHeight
    }

    override fun getWantedVisibility(): Toast.Visibility = wantedVisibility

    override fun update(manager: ToastManager, fullyVisibleForMs: Long) {
        if (firstFrameAt < 0) {
            firstFrameAt = fullyVisibleForMs
        }
        val elapsed = fullyVisibleForMs - firstFrameAt
        wantedVisibility = if (elapsed < durationMs) Toast.Visibility.SHOW else Toast.Visibility.HIDE
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, font: Font, fullyVisibleForMs: Long) {
        graphics.blitSprite(
            net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            net.minecraft.resources.Identifier.withDefaultNamespace("toast/system"),
            0, 0, width(), height()
        )
        val titleColor = if (message == null) -1 else -256
        val titleStartY = if (message == null) 12 else 7
        drawLines(graphics, font, titleLines, titleStartY, titleColor)
        if (message != null) {
            drawLines(graphics, font, messageLines, 7 + titleLines.size * LINE_SPACING, -1)
        }
    }

    private fun drawLines(graphics: GuiGraphicsExtractor, font: Font, lines: List<FormattedCharSequence>, yStart: Int, color: Int) {
        for (i in lines.indices) {
            graphics.text(font, lines[i], TEXT_X, yStart + i * LINE_SPACING, color, false)
        }
    }

    companion object {
        private const val LINE_SPACING = 12
        private const val TEXT_X = 18

        private fun splitToLength(text: Component): List<FormattedCharSequence> {
            return Minecraft.getInstance().font.split(text, 150)
        }
    }
}
