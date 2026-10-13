package set.starlev.render

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import set.starlev.features.chat.NotifierCenter
import set.starlev.features.chat.NotifierRule
import set.starlev.features.chat.NotifierSounds
import set.starlev.features.visual.swing.PixelButtonTone
import set.starlev.features.visual.swing.Rect

/**
 * Меню пользовательских уведомлений (`/sh notifiers`).
 *
 * Слева — список правил («если в чате X — показать заголовок»), справа по центру — редактор
 * выбранного правила и живой предпросмотр того, как игрок увидит ванильные титры.
 *
 * Стиль тот же, что у `/sh binds` и `/sh aliases` (SHHeader/SHRow/SHFooter/SHListHelper),
 * но на две колонки: список + карточка настройки.
 */
class NotifierGui(private val parent: Screen? = null) : Screen(Component.literal("Уведомления")) {

    /** Кнопки правой карточки — геометрия считается в одном месте, рисуется и кликается одинаково. */
    private class Btn(val id: String, val rect: Rect, val label: String, val tone: PixelButtonTone = PixelButtonTone.NORMAL)

    private class Geo(
        val top: Int,
        val bottom: Int,
        val listX: Int,
        val listW: Int,
        val panelX: Int,
        val panelW: Int,
        val listTop: Int,
        val listBottom: Int,
    )

    private var selected = 0
    private var scrollOffset = 0

    /** Индекс редактируемого текстового поля (-1 — не редактируем). */
    private var editing = -1
    private var editingBuffer = ""
    private var blinkTicks = 0
    private var status = "§7Клик по строке — править • Enter — сохранить • ESC — назад"

    private val fields = listOf("name", "pattern", "title", "subtitle")

    // ─────────────────────────── геометрия ───────────────────────────

    private fun geo(): Geo {
        val top = 46
        val bottom = height - 28
        val listW = 148
        val gap = 8
        val panelW = (width - listW - gap - 40).coerceIn(170, 320)
        val leftX = (width - (listW + gap + panelW)) / 2
        return Geo(
            top = top,
            bottom = bottom,
            listX = leftX,
            listW = listW,
            panelX = leftX + listW + gap,
            panelW = panelW,
            listTop = top + 26,
            listBottom = bottom,
        )
    }

    private fun fieldRects(g: Geo): List<Rect> {
        val top = previewBottom(g) + 6
        return fields.mapIndexed { i, _ ->
            Rect(g.panelX + 8, top + i * FIELD_H, g.panelW - 16, FIELD_H - 2)
        }
    }

    private fun previewRect(g: Geo): Rect = Rect(g.panelX + 8, g.top + 8, g.panelW - 16, PREVIEW_H)

    private fun previewBottom(g: Geo): Int = g.top + 8 + PREVIEW_H

    private fun buttons(g: Geo): List<Btn> {
        val out = mutableListOf<Btn>()
        val top = fieldRects(g).last().y + FIELD_H + 6
        val half = (g.panelW - 24) / 2
        out += Btn("soundPrev", Rect(g.panelX + 8, top, 16, 18), "<")
        out += Btn("soundNext", Rect(g.panelX + g.panelW - 24, top, 16, 18), ">")
        out += Btn("soundPlay", Rect(g.panelX + 28, top, 40, 18), "§b▶", PixelButtonTone.CONFIRM)
        out += Btn("stayMinus", Rect(g.panelX + 76, top, 16, 18), "-")
        out += Btn("stayPlus", Rect(g.panelX + g.panelW - 24, top, 16, 18), "+")
        out += Btn("toggle", Rect(g.panelX + 8, top + 22, half, 18), "", PixelButtonTone.CONFIRM)
        out += Btn("fire", Rect(g.panelX + 16 + half, top + 22, half - 8, 18), "§aПоказать", PixelButtonTone.CONFIRM)
        return out
    }

    // ─────────────────────────── отрисовка ───────────────────────────

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        blinkTicks++
        val rules = NotifierCenter.snapshot()
        if (selected >= rules.size) selected = (rules.size - 1).coerceAtLeast(0)
        val rule = rules.getOrNull(selected)
        val g = geo()

        SHHeader.draw(graphics, font, title, "§7Свои титры и звук по совпадению текста чата", null, width)

        drawList(graphics, g, mouseX, mouseY, rules)
        drawPanel(graphics, g, rule, mouseX, mouseY)

        SHFooter.draw(graphics, font, width, height, "§7ESC — Назад • §7клик по строке — править • §7Enter — сохранить")
    }

    private fun drawList(
        graphics: GuiGraphicsExtractor,
        g: Geo,
        mouseX: Int,
        mouseY: Int,
        rules: List<NotifierRule>,
    ) {
        val addRect = Rect(g.listX, g.top, g.listW, 20)
        val addHover = addRect.contains(mouseX, mouseY)
        graphics.drawString(font, "§a+ Добавить правило", addRect.x + 6, addRect.y + 6, if (addHover) 0xFFFFFFFF.toInt() else SHTokens.TEXT_PRIMARY.toInt())
        graphics.outline(addRect.x, addRect.y, addRect.width, addRect.height, SHTokens.PANEL_OUTLINE.toInt())

        val itemH = 20
        val total = rules.size * itemH
        scrollOffset = SHListHelper.clampScroll(scrollOffset, total, g.listBottom - g.listTop)
        graphics.enableScissor(g.listX, g.listTop, g.listX + g.listW, g.listBottom)
        var y = g.listTop - scrollOffset
        rules.forEachIndexed { index, rule ->
            if (y + itemH >= g.listTop && y <= g.listBottom) {
                val rect = Rect(g.listX, y, g.listW, itemH - 2)
                val hovered = rect.contains(mouseX, mouseY)
                SHRow.draw(graphics, rect, hovered, index == selected)
                graphics.drawString(font, if (rule.enabled) "§a●" else "§8●", rect.x + 6, rect.y + 5, -1)
                val name = rule.name.ifBlank { "Без названия" }
                val shown = font.plainSubstrByWidth(name, rect.width - 46).let {
                    if (it.length < name.length) "$it…" else it
                }
                graphics.drawString(
                    font,
                    (if (rule.enabled) "§f" else "§8") + shown,
                    rect.x + 18, rect.y + 5, -1
                )
                graphics.drawString(
                    font,
                    if (hovered && mouseX > rect.x + rect.width - 16) "§cX" else "§7x",
                    rect.x + rect.width - 12, rect.y + 5, -1
                )
            }
            y += itemH
        }
        graphics.disableScissor()
        SHListHelper.drawScrollbar(
            graphics, Rect(g.listX, g.listTop, g.listW, g.listBottom - g.listTop),
            total, scrollOffset, g.listBottom - g.listTop
        )
        if (rules.isEmpty()) {
            graphics.drawCenteredString(font, Component.literal("§8правил нет"), g.listX + g.listW / 2, g.listTop + 20, SHTokens.TEXT_DIM.toInt())
        }
    }

    private fun drawPanel(
        graphics: GuiGraphicsExtractor,
        g: Geo,
        rule: NotifierRule?,
        mouseX: Int,
        mouseY: Int,
    ) {
        SHPanel.draw(graphics, Rect(g.panelX, g.top, g.panelW, g.bottom - g.top))
        val preview = previewRect(g)
        graphics.fill(preview.x, preview.y, preview.x + preview.width, preview.y + preview.height, 0xFF101018.toInt())
        graphics.outline(preview.x, preview.y, preview.width, preview.height, SHTokens.PANEL_OUTLINE.toInt())

        if (rule == null) {
            graphics.drawCenteredString(font, Component.literal("§8выберите или добавьте правило"), preview.x + preview.width / 2, preview.y + preview.height / 2 - 4, SHTokens.TEXT_DIM.toInt())
            return
        }

        // Предпросмотр: ванильный заголовок по центру, крупно
        val title = rule.title.ifBlank { rule.name }.ifBlank { "§8Заголовок" }
        val subtitle = rule.subtitle
        graphics.pose().pushMatrix()
        val scale = 2f
        val titleX = ((preview.x + preview.width / 2) - font.width(title) * scale / 2f).toInt()
        graphics.pose().translate(titleX.toFloat(), (preview.y + 16).toFloat())
        graphics.pose().scale(scale, scale)
        graphics.text(font, Component.literal(title), 0, 0, SHTokens.TEXT_PRIMARY.toInt(), true)
        graphics.pose().popMatrix()
        if (subtitle.isNotBlank()) {
            graphics.drawCenteredString(
                font, Component.literal(subtitle),
                preview.x + preview.width / 2, preview.y + preview.height - 18, SHTokens.TEXT_MUTED.toInt()
            )
        }
        graphics.drawCenteredString(
            font, Component.literal("§8предпросмотр"),
            preview.x + preview.width / 2, preview.y + preview.height - 10, SHTokens.TEXT_DIM.toInt()
        )

        // Поля
        val rects = fieldRects(g)
        val values = listOf(rule.name, rule.pattern, rule.title, rule.subtitle)
        val labels = listOf("§7Название", "§7Текст или §a/регекс/", "§7Заголовок", "§7Подзаголовок")
        fields.forEachIndexed { i, _ ->
            val rect = rects[i]
            val focused = editing == i
            if (focused || rect.contains(mouseX, mouseY)) {
                graphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, SHTokens.ROW_HOVER.toInt())
            }
            graphics.drawString(font, labels[i], rect.x + 4, rect.y + 5, SHTokens.TEXT_MUTED.toInt())
            val raw = if (focused) editingBuffer else values[i]
            val shown = font.plainSubstrByWidth(raw, rect.width / 2 - 6).let {
                if (it.length < raw.length) "$it…" else it
            }
            val cursor = if (focused && (blinkTicks / 6) % 2 == 0) "§b_" else ""
            graphics.drawString(font, if (raw.isEmpty()) "§8—" else "§f$shown$cursor", rect.x + rect.width / 2, rect.y + 5, -1)
        }

        // Звук / длительность / действия
        val sound = NotifierSounds.byId(rule.sound)
        val soundY = rects.last().y + FIELD_H + 6
        graphics.drawString(font, "§7Звук", g.panelX + 46, soundY + 5, SHTokens.TEXT_MUTED.toInt())
        graphics.drawString(font, "§f${sound.label}", g.panelX + 28 + 44, soundY + 5, -1)
        graphics.drawString(font, "§7Длительность §f${rule.stay}§7т", g.panelX + 96, soundY + 5, SHTokens.TEXT_MUTED.toInt())

        val btns = buttons(g)
        btns.forEach { btn ->
            val label = when (btn.id) {
                "toggle" -> if (rule.enabled) "§aВключено" else "§8Выключено"
                else -> btn.label
            }
            val hovered = btn.rect.contains(mouseX, mouseY)
            val tone = when {
                btn.id == "soundPlay" -> PixelButtonTone.CONFIRM
                btn.id == "toggle" && !rule.enabled -> PixelButtonTone.DANGER
                else -> btn.tone
            }
            SHRow.drawButton(
                graphics, font, btn.rect, label,
                selected = btn.id == "toggle" && rule.enabled,
                hovered = hovered, enabled = true, tone = tone
            )
        }
        graphics.drawCenteredString(
            font, Component.literal(status),
            g.panelX + g.panelW / 2, g.bottom - 12, SHTokens.TEXT_DIM.toInt()
        )
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        SHBackdrop.render(graphics, width, height)
    }

    // ─────────────────────────── клик ───────────────────────────

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, isDoubleClick)
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        val g = geo()
        val rules = NotifierCenter.snapshot()

        // Кнопки карточки
        val rule = rules.getOrNull(selected)
        if (rule != null) {
            buttons(g).firstOrNull { it.rect.contains(mouseX, mouseY) }?.let { btn ->
                editing = -1
                handleButton(btn.id)
                return true
            }
            // Поля ввода
            fieldRects(g).forEachIndexed { i, rect ->
                if (rect.contains(mouseX, mouseY)) {
                    editing = i
                    editingBuffer = fieldValue(rule, fields[i])
                    return true
                }
            }
        }

        // Список слева
        val addRect = Rect(g.listX, g.top, g.listW, 20)
        if (addRect.contains(mouseX, mouseY)) {
            editing = -1
            selected = NotifierCenter.add()
            status = "§aПравило добавлено — заполни поля"
            scrollOffset = Int.MAX_VALUE
            return true
        }
        if (mouseY in g.listTop..g.listBottom) {
            val index = (mouseY - g.listTop + scrollOffset) / ITEM_H
            if (index in rules.indices) {
                editing = -1
                selected = index
                if (mouseX > g.listX + g.listW - 16) {
                    NotifierCenter.remove(index)
                    selected = selected.coerceAtMost((NotifierCenter.snapshot().size - 1).coerceAtLeast(0))
                    status = "§cПравило удалено"
                } else if (mouseX < g.listX + 16) {
                    val on = NotifierCenter.toggle(index)
                    status = if (on) "§aПравило включено" else "§8Правило выключено"
                }
                return true
            }
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    private fun handleButton(id: String) {
        val rule = NotifierCenter.snapshot().getOrNull(selected) ?: return
        when (id) {
            "soundPrev", "soundNext" -> {
                val size = NotifierSounds.ALL.size
                val idx = NotifierSounds.ALL.indexOfFirst { it.id == rule.sound }.coerceAtLeast(0)
                val next = if (id == "soundNext") (idx + 1) % size else (idx - 1 + size) % size
                rule.sound = NotifierSounds.ALL[next].id
                NotifierCenter.commit("notifier-sound")
                status = "§7Звук: §f${NotifierSounds.ALL[next].label}"
            }
            "soundPlay" -> NotifierSounds.play(rule.sound)
            "stayMinus", "stayPlus" -> {
                val step = if (id == "stayPlus") 10 else -10
                rule.stay = (rule.stay + step).coerceIn(10, 400)
                NotifierCenter.commit("notifier-stay")
            }
            "toggle" -> {
                val on = NotifierCenter.toggle(selected)
                status = if (on) "§aПравило включено" else "§8Правило выключено"
            }
            "fire" -> {
                NotifierCenter.fire(rule)
                status = "§aПоказано в игре"
            }
        }
    }

    // ─────────────────────────── клавиатура ───────────────────────────

    override fun keyPressed(event: KeyEvent): Boolean {
        val key = event.input()
        if (editing >= 0) {
            when (key) {
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    commitField()
                    editing = if (editing < fields.lastIndex) editing + 1 else -1
                    if (editing >= 0) startEditing(NotifierCenter.snapshot().getOrNull(selected))
                    status = "§aСохранено"
                    return true
                }
                GLFW.GLFW_KEY_ESCAPE -> {
                    editing = -1
                    status = "§8Правка отменена"
                    return true
                }
                GLFW.GLFW_KEY_BACKSPACE -> {
                    if (editingBuffer.isNotEmpty()) editingBuffer = editingBuffer.dropLast(1)
                    return true
                }
                GLFW.GLFW_KEY_DELETE -> {
                    editingBuffer = ""
                    return true
                }
            }
            return true
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            NotifierCenter.snapshot().getOrNull(selected)?.let {
                NotifierCenter.remove(selected)
                selected = selected.coerceAtMost((NotifierCenter.snapshot().size - 1).coerceAtLeast(0))
                status = "§cПравило удалено"
            }
            return true
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (editing >= 0) {
            val chr = event.codepoint().toChar()
            if (chr.code >= 32 && editingBuffer.length < MAX_FIELD) editingBuffer += chr
            return true
        }
        return super.charTyped(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val g = geo()
        if (mouseY.toInt() in g.listTop..g.listBottom) {
            val total = NotifierCenter.snapshot().size * ITEM_H
            scrollOffset = SHListHelper.clampScroll(
                (scrollOffset - verticalAmount * 20).toInt(), total, g.listBottom - g.listTop
            )
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun onClose() {
        // Незакрытая правка не должна теряться по ESC из родительского экрана
        if (editing >= 0) commitField()
        editing = -1
        if (parent != null) minecraft.gui.setScreen(parent) else super.onClose()
    }

    // ─────────────────────────── правка полей ───────────────────────────

    private fun startEditing(rule: NotifierRule?) {
        editingBuffer = rule?.let { fieldValue(it, fields.getOrElse(editing) { "name" }) } ?: ""
    }

    private fun fieldValue(rule: NotifierRule, field: String): String = when (field) {
        "name" -> rule.name
        "pattern" -> rule.pattern
        "title" -> rule.title
        "subtitle" -> rule.subtitle
        else -> ""
    }

    private fun commitField() {
        val rule = NotifierCenter.snapshot().getOrNull(selected) ?: return
        when (fields.getOrElse(editing) { "name" }) {
            "name" -> rule.name = editingBuffer.take(40)
            "pattern" -> rule.pattern = editingBuffer.take(MAX_FIELD)
            "title" -> rule.title = editingBuffer.take(MAX_FIELD)
            "subtitle" -> rule.subtitle = editingBuffer.take(MAX_FIELD)
        }
        NotifierCenter.commit("notifier-field")
    }

    private companion object {
        const val ITEM_H = 20
        const val FIELD_H = 18
        const val PREVIEW_H = 66
        const val MAX_FIELD = 120
    }
}

private fun Rect.contains(x: Int, y: Int): Boolean = x >= this.x && x < this.x + this.width && y >= this.y && y < this.y + this.height