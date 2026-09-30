package set.starlev.features.skyblock

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.Identifier
import set.starlev.config.ConfigManager
import set.starlev.injections.accessors.ContainerScreenAccessor
import set.starlev.render.HudElement
import set.starlev.render.drawString
import set.starlev.utils.detectors.DungeonDetector
import java.util.regex.Pattern

/**
 * Оверлей диалогов с NPC — визуальный порт 1:1 DialogueOverlay из SkyCubed (MIT):
 * девятисекционный фон-панель (dialogue_background.png = их background.png),
 * плашка имени NPC внахлёст с верхним краем слева (отступ 5), текст с отступом 15
 * по левому краю, панель опций всплывает над панелью справа после окончания
 * сообщения, тайминги на основе WPM (180 слов/мин, минимум 0.75с, опции — 10с).
 * В контейнере — прижат к левому краю, ширина = левый край контейнера - 30.
 */
object NpcDialogueOverlay : HudElement("NpcDialogueOverlay") {
    private val messageRegex = Pattern.compile(
        "^(?:(?:(?:\\[NPC]|\\[Персонаж]) )(?:\\[(?<pName1>[^\\]}]+)[\\]}]|(?<pName2>[^:\\[>]+))|(?:\\[(?<bName>[^\\]}]+)\\])): (?<message>.+)",
        Pattern.CASE_INSENSITIVE
    )

    // Системные каналы чата — не NPC-диалоги (англ. и рус. форматы Heltix, см. PartyCommands)
    private val ignoredNames = setOf(
        "To", "From", "Chat", "System", "Guild", "Party", "Officer", "Co-op",
        "Кому", "От", "Чат", "Система", "Гильдия", "Пати", "Офицер", "Кооп", "Ко-оп"
    )
    private val optionHeaderRegex = Pattern.compile(".*(?:Select|Click|Выберите|Нажмите) (?:an option|вариант|опцию): (?<options>.+)", Pattern.CASE_INSENSITIVE)
    private val optionRegex = Pattern.compile("\\[(?<option>.*?)]")
    private val wordRegex = Regex("\\s+")

    // Спрайт панели скопирован из SkyCubed (textures/gui/sprites/background.png, MIT)
    private val BOX_SPRITE = Identifier.fromNamespaceAndPath("starredheltix", "dialogue_background")
    private val SPRITE_WHITE = 0xFFFFFFFF.toInt()

    private var currentDialogue: Dialogue? = null
    private var lastShown: Dialogue? = null
    private val queue = mutableListOf<Dialogue>()
    private var nextCheck = 0L
    private var displayedOptions = false
    private var lastInputTime = 0L
    private var calculatedHeight = 0

    // Тайминги как в NpcOverlayConfig SkyCubed: 180 WPM, 0.75с минимум, 10с на действия
    private const val WORDS_PER_MINUTE = 180
    private const val MIN_DURATION_MS = 750L
    private const val ACTION_DURATION_MS = 10000L

    private val dummyDialogue = Dialogue(
        Component.literal("Житель").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
        Component.literal("Привет! Это пример диалога для настройки расположения и размера оверлея. Ширина меняется Shift+Скроллом."),
        listOf(Option("Да, мне нравится", ""), Option("Нет, измени это", ""))
    )

    data class Dialogue(
        val name: Component,
        val text: Component,
        val options: List<Option> = emptyList()
    )

    data class Option(val text: String, val command: String, val label: Component = Component.literal(text))

    override fun getDefaultX(): Int = 100
    override fun getDefaultY(): Int = 100
    override fun getDefaultScale(): Float = 1.0f

    fun onChat(message: String): Boolean {
        return onChat(Component.literal(message))
    }

    private fun calculateDisplayDuration(message: Component): Long {
        val wordCount = message.string.trim().split(wordRegex).count { it.isNotBlank() }.toLong().coerceAtLeast(1)
        val wpmDuration = (wordCount * 60000) / WORDS_PER_MINUTE
        return wpmDuration.coerceAtLeast(MIN_DURATION_MS)
    }

    private fun splitComponent(root: Component, nameStr: String, msgStr: String): Pair<Component, Component> {
        val fullText = root.getString()
        val nameStart = fullText.indexOf(nameStr)
        val msgStart = if (nameStart != -1) fullText.indexOf(msgStr, nameStart + nameStr.length) else -1

        if (nameStart == -1 || msgStart == -1) {
             return Component.literal(nameStr).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD) to Component.literal(msgStr)
        }

        val nameEnd = nameStart + nameStr.length

        val nameComp = Component.empty()
        val msgComp = Component.empty()

        var currentPos = 0

        fun processTree(c: Component) {
            val node = c.copy()
            node.siblings.clear()
            val text = node.getString()
            val len = text.length

            if (len > 0) {
                val start = currentPos
                val end = currentPos + len

                if (start < nameEnd && end > nameStart) {
                    val s = kotlin.math.max(start, nameStart) - start
                    val e = kotlin.math.min(end, nameEnd) - start
                    if (s < e) {
                        nameComp.append(Component.literal(text.substring(s, e)).withStyle(node.style))
                    }
                }

                if (end > msgStart) {
                    val s = kotlin.math.max(start, msgStart) - start
                    val e = len
                    if (s < e) {
                        msgComp.append(Component.literal(text.substring(s, e)).withStyle(node.style))
                    }
                }

                currentPos += len
            }

            c.siblings.forEach { processTree(it) }
        }

        processTree(root)

        if (nameComp.siblings.isEmpty() && nameComp.string.isEmpty()) {
             nameComp.append(Component.literal(nameStr).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
        }

        return nameComp to msgComp
    }

    fun onChat(message: Component): Boolean {
        if (!ConfigManager.features.skyblock.npcDialogue.enabled) return false
        val text = message.getString()
        val cleanText = ChatFormatting.stripFormatting(text) ?: text

        val matcher = messageRegex.matcher(cleanText)
        if (matcher.matches()) {
            var name = matcher.group("pName1") ?: matcher.group("pName2") ?: matcher.group("bName")
            name = name?.trim() ?: ""
            val msg = matcher.group("message")?.trim() ?: ""

            if (name.isEmpty() || msg.isEmpty()) return false
            if (ignoredNames.contains(name)) return false
            if (name.contains(" > ")) return false

            val (nameComp, msgComp) = splitComponent(message, name, msg)

            // Очередь как в SkyCubed: сообщения только в очередь, снимает цикл рендера по WPM
            queue.add(Dialogue(nameComp, msgComp))

            // Чат пошёл дальше — опции прошлого диалога больше НЕ АКТУАЛЬНЫ:
            // снимаем их со всего (текущий + очередь) и не даём 10-секундному
            // таймеру действий задерживать показ нового сообщения.
            if (displayedOptions || currentDialogue?.options?.isNotEmpty() == true || queue.any { it.options.isNotEmpty() }) {
                displayedOptions = false
                currentDialogue = currentDialogue?.copy(options = emptyList())
                queue.replaceAll { if (it.options.isNotEmpty()) it.copy(options = emptyList()) else it }
                nextCheck = minOf(nextCheck, System.currentTimeMillis() + MIN_DURATION_MS)
            }

            val shouldHide = ConfigManager.features.skyblock.npcDialogue.hideMessages
            if (shouldHide && DungeonDetector.isInDungeon() && cleanText.startsWith("[Персонаж] ")) return false
            return shouldHide
        }

        val optionMatcher = optionHeaderRegex.matcher(cleanText)
        if (optionMatcher.matches()) {
            val options = mutableListOf<Option>()

            fun findOptions(comp: Component) {
                val s = comp.string
                val m = optionRegex.matcher(s)
                if (m.find()) {
                    val label = m.group("option")
                    val style = comp.style
                    val clickEvent = style.clickEvent
                    if (clickEvent != null) {
                        val cmd = when (clickEvent) {
                            is ClickEvent.RunCommand -> clickEvent.command()
                            is ClickEvent.SuggestCommand -> clickEvent.command()
                            else -> null
                        }
                        if (cmd != null) {
                            options.add(Option(label.trim(), cmd, Component.literal(label.trim()).withStyle(style)))
                        }
                    }
                }
                comp.siblings.forEach { findOptions(it) }
            }
            findOptions(message)

            if (options.isEmpty()) {
                 val optionsStr = optionMatcher.group("options")
                 val optM = optionRegex.matcher(optionsStr)
                 while (optM.find()) {
                     val label = optM.group("option")
                     options.add(Option(label, ""))
                 }
            }

            val nowMs = System.currentTimeMillis()
            if (currentDialogue != null) {
                 currentDialogue = currentDialogue?.copy(options = options)
                 // Опции уже на экране и пришла новая строка опций — обновляем таймер,
                 // панель перерисуется с новым списком в тот же кадр.
                 if (displayedOptions) nextCheck = nowMs + ACTION_DURATION_MS
            } else if (queue.isNotEmpty()) {
                val last = queue.lastOrNull()
                if (last != null) {
                    queue.removeAt(queue.lastIndex)
                    queue.add(last.copy(options = options))
                }
            } else {
                // Диалог уже отрендерен, а строка опций пришла следом —
                // поднимаем последнюю реплику и сразу показываем опции
                // (раньше они молча терялись).
                val base = lastShown
                if (base != null) {
                    currentDialogue = base.copy(options = options)
                    displayedOptions = true
                    nextCheck = nowMs + ACTION_DURATION_MS
                }
            }
            return ConfigManager.features.skyblock.npcDialogue.hideMessages
        }

        return false
    }

    /** State machine как onTick в SkyCubed: сним из очереди по WPM, опции — после конца сообщения. */
    private fun advance(now: Long, config: set.starlev.config.categories.SkyblockConfig.NpcDialogueConfig) {
        if (now < nextCheck) return
        if (queue.isEmpty()) {
            val d = currentDialogue
            if (d != null && !displayedOptions && d.options.isNotEmpty() && config.showOptions) {
                displayedOptions = true
                nextCheck = now + ACTION_DURATION_MS
            } else {
                currentDialogue = null
                displayedOptions = false
                nextCheck = now + MIN_DURATION_MS
            }
        } else {
            val next = queue.removeAt(0)
            currentDialogue = next
            lastShown = next
            displayedOptions = false
            val timeoutCap = (config.timeoutSeconds * 1000).toLong().coerceAtLeast(MIN_DURATION_MS)
            nextCheck = now + calculateDisplayDuration(next.text).coerceAtMost(timeoutCap)
        }
    }

    override fun getWidth(): Int {
        return if (customWidth > 0) customWidth else 300
    }

    override fun getHeight(): Int {
        return if (calculatedHeight > 0) calculatedHeight else 60
    }

    override fun render() {
        val config = ConfigManager.features.skyblock.npcDialogue
        val graphics = cachedGraphics ?: return
        val mc = Minecraft.getInstance()
        val font = mc.font

        if (isEditing) {
            drawSkyCubed(graphics, font, dummyDialogue, showOptions = true, config, x, y, getWidth())
            return
        }

        if (!config.enabled) return
        val now = System.currentTimeMillis()
        advance(now, config)
        handleInput()

        val dialogue = currentDialogue ?: return
        if (dialogue.name.string.isEmpty() || dialogue.text.string.isEmpty()) return

        val screen = mc.gui.screen()
        if (screen is AbstractContainerScreen<*>) {
            // Как в SkyCubed: в контейнере — слева, ширина = левый край контейнера - 30, по центру по вертикали
            val accessor = screen as ContainerScreenAccessor
            val maxWidth = (accessor.leftPos - 30).coerceAtLeast(90)
            val boxH = measureBoxHeight(font, dialogue, maxWidth, config)
            val drawY = mc.window.guiScaledHeight / 2 - boxH / 2
            drawSkyCubed(graphics, font, dialogue, displayedOptions, config, 5, drawY, maxWidth)
        } else {
            drawSkyCubed(graphics, font, dialogue, displayedOptions, config, x, y, getWidth())
        }
    }

    private fun measureBoxHeight(font: net.minecraft.client.gui.Font, dialogue: Dialogue, boxWidth: Int, config: set.starlev.config.categories.SkyblockConfig.NpcDialogueConfig): Int {
        val pad = if (config.compactMode) 8 else 15
        return font.split(dialogue.text, boxWidth - pad * 2).size * font.lineHeight + pad * 2
    }

    private fun drawSkyCubed(
        graphics: GuiGraphicsExtractor,
        font: net.minecraft.client.gui.Font,
        dialogue: Dialogue,
        showOptions: Boolean,
        config: set.starlev.config.categories.SkyblockConfig.NpcDialogueConfig,
        drawX: Int,
        drawY: Int,
        boxWidth: Int
    ) {
        val pad = if (config.compactMode) 8 else 15
        val namePad = if (config.compactMode) 3 else 5
        val lh = font.lineHeight

        val textLines = font.split(dialogue.text, boxWidth - pad * 2)
        val nameLines = font.split(dialogue.name, boxWidth - namePad * 2)

        var boxHeight = textLines.size * lh + pad * 2
        if (customHeight > 0) boxHeight = maxOf(customHeight, boxHeight)
        calculatedHeight = boxHeight

        // Основная панель сообщения
        if (config.showBackground && showBackground) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BOX_SPRITE, drawX, drawY, boxWidth, boxHeight, SPRITE_WHITE)
        }

        // Плашка имени — внахлёст с верхним краем слева (как SkyCubed: x+5, y - h/2)
        val nameW = nameLines.maxOf { font.width(it) }
        val nameBoxW = nameW + namePad * 2
        val nameBoxH = nameLines.size * lh + namePad * 2
        val nameX = drawX + 5
        val nameY = drawY - nameBoxH / 2
        if (config.showBackground && showBackground) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BOX_SPRITE, nameX, nameY, nameBoxW, nameBoxH, SPRITE_WHITE)
        }
        nameLines.forEachIndexed { i, line ->
            graphics.drawString(font, line, nameX + namePad, nameY + namePad + i * lh, 0xFFFFFFFF.toInt())
        }

        // Текст сообщения — по левому краю с отступом pad
        textLines.forEachIndexed { i, line ->
            graphics.drawString(font, line, drawX + pad, drawY + pad + i * lh, 0xFFFFFFFF.toInt())
        }

        // Панель опций — над основной панелью, выровнена по правому краю (как createOptionsDisplay в SkyCubed)
        if (showOptions && dialogue.options.isNotEmpty() && config.showOptions) {
            val rows = dialogue.options.mapIndexed { i, opt ->
                Component.empty()
                    .append(Component.literal("${i + 1}. ").withStyle(ChatFormatting.GRAY))
                    .append(opt.label)
            }
            val rowW = rows.maxOf { font.width(it) } + namePad * 2
            val rowH = lh + namePad * 2
            val panelH = rows.size * rowH + (rows.size - 1) * 5
            val px = drawX + boxWidth - rowW - 10
            var py = drawY - panelH - 10
            for (row in rows) {
                if (config.showBackground && showBackground) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BOX_SPRITE, px, py, rowW, rowH, SPRITE_WHITE)
                }
                graphics.drawString(font, row, px + namePad, py + namePad, 0xFFFFFFFF.toInt())
                py += rowH + 5
            }
        }

        if (config.closeOnEsc) {
            val escText = "§7[ESC] — пропустить"
            graphics.drawString(font, escText, drawX + (boxWidth - font.width(escText)) / 2, drawY + boxHeight + 2, 0xFFFFFFFF.toInt())
        }
    }

    fun isActive(): Boolean {
        return currentDialogue != null || queue.isNotEmpty()
    }

    fun close() {
        currentDialogue = null
        lastShown = null
        queue.clear()
        displayedOptions = false
        nextCheck = 0
    }

    fun clearOnServerChange() {
        queue.clear()
        currentDialogue = null
        lastShown = null
        displayedOptions = false
        nextCheck = 0
    }

    private fun handleInput() {
        val dialogue = currentDialogue ?: return
        val config = ConfigManager.features.skyblock.npcDialogue
        val mc = Minecraft.getInstance()
        val window = mc.window
        val time = System.currentTimeMillis()

        if (config.closeOnEsc && InputConstants.isKeyDown(window, 256)) {
            // ESC — пропустить текущий диалог, очередь продолжает сниматься
            currentDialogue = null
            displayedOptions = false
            nextCheck = time
            return
        }

        if (dialogue.options.isEmpty()) return

        if (time - lastInputTime < 200) return

        for (i in 0 until dialogue.options.size.coerceAtMost(9)) {
            if (InputConstants.isKeyDown(window, 49 + i)) {
                val cmd = dialogue.options[i].command
                if (cmd.isNotEmpty()) {
                    if (cmd.startsWith("/")) {
                        mc.player?.connection?.sendCommand(cmd.removePrefix("/"))
                    } else {
                        mc.player?.connection?.sendChat(cmd)
                    }
                }
                currentDialogue = null
                displayedOptions = false
                nextCheck = time
                lastInputTime = time
                return
            }
        }
    }
}
