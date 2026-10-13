package set.starlev.config.gui

import io.github.notenoughupdates.moulconfig.common.IFontRenderer
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiComponent
import io.github.notenoughupdates.moulconfig.gui.GuiImmediateContext
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorInfoText
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption

/**
 * Заголовок-хлебная крошка для категорий, у которых нет своих опций.
 *
 * Рисует крупный (x2) заголовок текущей подкатегории и под ним две серые строки —
 * «Родитель → Подкатегория» и подсказку, что настройки лежат в подкатегориях слева.
 *
 * Почему не [GuiOptionEditorInfoText]: он рисует `new TextComponent(infoTitle, 100, CENTER)`,
 * то есть ширина жёстко 100 px, и длинный текст молча сжимается через
 * `drawStringScaledMaxWidth` до нечитаемых 3-4 px (а высота бокса остаётся большой).
 * Свой компонент переносит строки по словам и умеет увеличивать шрифт.
 */
class SubcategoryHintComponent(
    private val title: StructuredText,
    private val subtitle: StructuredText,
    private val footnote: StructuredText,
    private val preferredWidth: Int = 340
) : GuiComponent() {

    private val font: IFontRenderer get() = IMinecraft.INSTANCE.defaultFontRenderer

    override fun getWidth(): Int = preferredWidth

    // Высота считается по заведомо узкой ширине, чтобы текст не обрезался в маленьком окне
    override fun getHeight(): Int {
        val width = HEIGHT_REFERENCE_WIDTH
        val subLines = font.splitText(subtitle, width).size + font.splitText(footnote, width).size
        return TITLE_SCALE * font.height + PADDING + subLines * (font.height + LINE_GAP) + PADDING / 2
    }

    override fun render(context: GuiImmediateContext) {
        val width = minOf(getWidth(), context.width)
        val context2d = context.renderContext

        // Заголовок x2 — так он читается как «вы находитесь здесь», а не как мелкая сноска
        val titleWidth = font.getStringWidth(title) * TITLE_SCALE
        context2d.pushMatrix()
        context2d.translate((width - titleWidth) / 2f, 0f)
        context2d.scale(TITLE_SCALE.toFloat(), TITLE_SCALE.toFloat())
        context2d.drawString(font, title, 0, PADDING / 2, TITLE_COLOR, true)
        context2d.popMatrix()

        var y = PADDING / 2 + TITLE_SCALE * font.height + 3
        for (line in font.splitText(subtitle, width)) {
            val lineWidth = font.getStringWidth(line)
            context2d.drawString(font, line, (width - lineWidth) / 2, y, SUBTITLE_COLOR, false)
            y += font.height + LINE_GAP
        }
        for (line in font.splitText(footnote, width)) {
            val lineWidth = font.getStringWidth(line)
            context2d.drawString(font, line, (width - lineWidth) / 2, y, FOOTNOTE_COLOR, false)
            y += font.height + LINE_GAP
        }
    }

    companion object {
        private const val TITLE_SCALE = 2
        private const val HEIGHT_REFERENCE_WIDTH = 240
        private const val LINE_GAP = 2
        private const val PADDING = 8
        private const val TITLE_COLOR = 0xE0E0E8
        private const val SUBTITLE_COLOR = 0x8A8A96
        private const val FOOTNOTE_COLOR = 0x6E6E7A
    }
}

/** Редактор-обёртка: та же семантика, что у GuiOptionEditorInfoText, но со своим компонентом. */
class SubcategoryHintEditor(
    option: ProcessedOption,
    title: StructuredText,
    subtitle: StructuredText,
    footnote: StructuredText
) : GuiOptionEditorInfoText(option, title) {

    private val wrapped = SubcategoryHintComponent(title, subtitle, footnote)

    override fun getDelegate(): GuiComponent = wrapped
}
