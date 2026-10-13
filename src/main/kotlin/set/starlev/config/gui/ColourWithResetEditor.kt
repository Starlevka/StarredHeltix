package set.starlev.config.gui

import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.GuiTextures
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiComponent
import io.github.notenoughupdates.moulconfig.gui.GuiImmediateContext
import io.github.notenoughupdates.moulconfig.gui.KeyboardEvent
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.gui.component.ColorSelectComponent
import io.github.notenoughupdates.moulconfig.gui.editors.ComponentEditor
import io.github.notenoughupdates.moulconfig.internal.ColourUtil
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import set.starlev.render.SHTokens

/**
 * Редактор цвета, у которого кнопка «Сброс» стоит **справа от образца**, в одной строке.
 *
 * Раньше сброс был отдельной синтетической опцией под цветом (см. `addColourResetButtons`
 * в `Config.kt`) — занимал целую строку и визуально отрывался от того, что сбрасывает.
 * Теперь это один компонент: образец 48×16 (на него кликается пипетка цвета, как в
 * ванильном `GuiOptionEditorColour`) и рядом кнопка «Сброс».
 *
 * Нарисовано вручную, а не через `PanelComponent`, потому что панель умеет только одного
 * ребёнка — здесь их два, и они должны стоять в линию по центру слота.
 */
class ColourWithResetEditor(
    option: ProcessedOption,
    private val defaultValue: String,
    private val onReset: Runnable,
) : ComponentEditor(option) {

    /** Поля цветов в конфиге — обычные String; ChromaColour поддерживаем на всякий случай. */
    private val usesString: Boolean = runCatching { option.type == String::class.java }.getOrDefault(true)

    private val component: GuiComponent = wrapComponent(RowComponent())

    override fun getDelegate(): GuiComponent = component

    @Suppress("DEPRECATION")
    private fun currentColour(): ChromaColour {
        val value = try {
            option.get()
        } catch (_: Throwable) {
            return ChromaColour.forLegacyString(defaultValue)
        }
        return try {
            if (usesString) ChromaColour.forLegacyString(value as String) else value as ChromaColour
        } catch (_: Throwable) {
            ChromaColour.forLegacyString(defaultValue)
        }
    }

    /** Текущее значение строкой — так оно лежит в конфиге («B:G:R:A:Chroma»). */
    @Suppress("DEPRECATION")
    private fun currentString(): String {
        if (!usesString) return currentColour().toLegacyString()
        return try {
            option.get() as? String ?: defaultValue
        } catch (_: Throwable) {
            defaultValue
        }
    }

    @Suppress("DEPRECATION")
    private fun apply(newColour: String) {
        if (usesString) option.set(newColour) else option.set(ChromaColour.forLegacyString(newColour))
        option.explicitNotifyChange()
    }

    private inner class RowComponent : GuiComponent() {

        override fun getWidth(): Int = SWATCH_W + GAP + RESET_W

        override fun getHeight(): Int = ROW_H

        override fun render(context: GuiImmediateContext) {
            val rc = context.renderContext

            // Образец цвета — 1в1 как в ванильном GuiOptionEditorColour
            rc.drawComplexTexture(
                GuiTextures.BUTTON_WHITE, 0f, 0f, SWATCH_W.toFloat(), ROW_H.toFloat()
            ) { it.color(ColourUtil.makeOpaque(currentColour().getEffectiveColour().rgb)) }

            // Кнопка сброса. Раньше тут была ванильная GuiTextures.BUTTON — светлая и
            // выбивается из тёмного меню. Теперь рисуется тем же стилем, что и панель
            // вокруг опции (SHPanel/SHRow), но на тон-два светлее и с цветной подписью,
            // чтобы её нельзя было спутать с самим значением цвета.
            val hovered = isOverReset(context)
            val rx = SWATCH_W + GAP
            val buttonColour = when {
                hovered -> 0xFF2E2E36.toInt()
                currentString() == defaultValue -> 0xFF1B1B21.toInt()
                else -> 0xFF24242B.toInt()
            }
            rc.drawColoredRect(rx.toFloat(), 0f, (rx + RESET_W).toFloat(), ROW_H.toFloat(), buttonColour)
            outlineRect(rc, rx, 0, RESET_W, ROW_H, SHTokens.PANEL_OUTLINE.toInt())

            val textColour = when {
                currentString() == defaultValue -> SHTokens.TEXT_DIM.toInt()
                hovered -> 0xFFFF9A9A.toInt()
                else -> RESET_COLOR
            }
            rc.drawStringCenteredScaledMaxWidth(
                RESET_TEXT,
                rc.minecraft.defaultFontRenderer,
                (rx + RESET_W / 2).toFloat(), (ROW_H / 2).toFloat(),
                true, RESET_W - 6,
                textColour
            )
        }

        override fun mouseEvent(mouseEvent: MouseEvent, context: GuiImmediateContext): Boolean {
            val click = mouseEvent as? MouseEvent.Click ?: return super.mouseEvent(mouseEvent, context)
            if (!click.mouseState || click.mouseButton != 0) return super.mouseEvent(mouseEvent, context)

            val mx = context.mouseX
            val my = context.mouseY
            if (mx < 0 || my < 0 || my >= ROW_H) return false

            // Правая часть — сброс к дефолту
            if (mx >= SWATCH_W + GAP && mx < getWidth()) {
                onReset.run()
                return true
            }
            // Левая часть — пипетка цвета (копия поведения ванильного редактора)
            if (mx < SWATCH_W) {
                openColourPicker(context)
                return true
            }
            return false
        }

        override fun keyboardEvent(event: KeyboardEvent, context: GuiImmediateContext): Boolean = false

        private fun isOverReset(context: GuiImmediateContext): Boolean {
            val mx = context.mouseX
            val my = context.mouseY
            return mx >= SWATCH_W + GAP && mx < getWidth() && my in 0 until ROW_H
        }

        private fun openColourPicker(context: GuiImmediateContext) {
            val picker = ColorSelectComponent(
                0, 0, currentString(),
                { colour -> apply(colour) },
                { closeOverlay() }
            )
            // Пипетка не должна уезжать за нижний край экрана
            val scaledHeight = context.renderContext.minecraft.scaledHeight
            val y = if (context.absoluteMouseY + picker.height > scaledHeight) {
                scaledHeight - picker.height
            } else {
                context.absoluteMouseY
            }
            openOverlay(picker, context.absoluteMouseX, y)
        }
    }

    private companion object {
        private val RESET_TEXT = StructuredText.of("Сброс")

        /** У RenderContext нет готового `outline` — рамка рисуется четырьмя полосками. */
        private fun outlineRect(
            rc: io.github.notenoughupdates.moulconfig.common.RenderContext,
            x: Int, y: Int, w: Int, h: Int, colour: Int,
        ) {
            rc.drawColoredRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + 1).toFloat(), colour)
            rc.drawColoredRect(x.toFloat(), (y + h - 1).toFloat(), (x + w).toFloat(), (y + h).toFloat(), colour)
            rc.drawColoredRect(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + h).toFloat(), colour)
            rc.drawColoredRect((x + w - 1).toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), colour)
        }
        private const val SWATCH_W = 48
        private const val GAP = 5
        private const val RESET_W = 40
        private const val ROW_H = 16
        private const val RESET_COLOR = 0xFFE05050.toInt()
    }
}