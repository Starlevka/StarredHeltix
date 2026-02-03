package set.starlev.features.overlays

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import set.starlev.config.ConfigManager
import set.starlev.hud.HudElement
import java.util.regex.Pattern
import java.awt.Color
import com.mojang.blaze3d.platform.InputConstants

object NpcDialogueOverlay : HudElement("NpcDialogueOverlay") {
    // Поддержка форматов:
    // 1. [NPC] Имя: Сообщение (или [Персонаж])
    // 2. [NPC] [Имя]: Сообщение
    // 3. [Имя}: Сообщение (ТОЛЬКО с закрывающей фигурной скобкой, если нет префикса)
    // 4. [МЕГА-ЯЩИК]: Сообщение (Специально для MegaChestNPC)
    // Обычные сообщения вида "[Имя]: Сообщение" (с квадратными скобками без префикса) игнорируются, чтобы не ловить чат каналов (Guild, Party, Auction и т.д.).
    private val messageRegex = Pattern.compile(
        "^(?:(?:(?:\\[NPC]|\\[Персонаж]) )(?:\\[(?<pName1>[^\\]}]+)[\\]}]|(?<pName2>[^:\\[>]+))|(?:\\[(?<bName>[^\\]}]+)\\})|(?:\\[(?<megaName>МЕГА-ЯЩИК)\\])): (?<message>.+)",
        Pattern.CASE_INSENSITIVE
    )
    
    // Игнорируемые имена (системные сообщения)
    private val ignoredNames = setOf("To", "From", "Chat", "System", "Guild", "Party", "Officer", "Co-op")
    
    // Поддержка английских и русских заголовков опций
    private val optionHeaderRegex = Pattern.compile(".*(?:Select|Click|Выберите|Нажмите) (?:an option|вариант|опцию): (?<options>.+)", Pattern.CASE_INSENSITIVE)
    private val optionRegex = Pattern.compile("\\[(?<option>.*?)]")

    private var currentDialogue: Dialogue? = null
    private var lastActivity = 0L
    private var lastInputTime = 0L
    private var calculatedHeight = 0

    // Dummy dialogue for editing mode
    private val dummyDialogue = Dialogue(
        Component.literal("Житель").withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.BOLD),
        Component.literal("Привет! Это пример диалога для настройки расположения и размера оверлея. Вы можете изменить ширину с помощью Shift+Скролл и высоту с помощью Ctrl+Скролл."),
        listOf(Option("Да, мне нравится", ""), Option("Нет, измени это", ""))
    )

    data class Dialogue(
        val name: Component, 
        val text: Component, 
        val options: List<Option> = emptyList()
    )

    data class Option(val text: String, val command: String)

    override fun getDefaultX(): Int = 100
    override fun getDefaultY(): Int = 100
    override fun getDefaultScale(): Float = 1.0f

    fun onChat(message: String): Boolean {
        return onChat(Component.literal(message))
    }

    // Вспомогательная функция для разделения компонента на имя и сообщение с сохранением стилей
    private fun splitComponent(root: Component, nameStr: String, msgStr: String): Pair<Component, Component> {
        val fullText = root.getString()
        val nameStart = fullText.indexOf(nameStr)
        // Ищем сообщение после имени
        val msgStart = if (nameStart != -1) fullText.indexOf(msgStr, nameStart + nameStr.length) else -1

        if (nameStart == -1 || msgStart == -1) {
             return Component.literal(nameStr).withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.BOLD) to Component.literal(msgStr)
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
                
                // Пересечение с Именем
                if (start < nameEnd && end > nameStart) {
                    val s = kotlin.math.max(start, nameStart) - start
                    val e = kotlin.math.min(end, nameEnd) - start
                    if (s < e) {
                        nameComp.append(Component.literal(text.substring(s, e)).withStyle(node.style))
                    }
                }
                
                // Пересечение с Сообщением
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
             nameComp.append(Component.literal(nameStr).withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.BOLD))
        }
        
        return nameComp to msgComp
    }

    fun onChat(message: Component): Boolean {
        if (!ConfigManager.features.skyblock.npcDialogue.enabled) return false
        val text = message.getString()
        val cleanText = net.minecraft.ChatFormatting.stripFormatting(text) ?: text
        
        // Сообщение NPC
        val matcher = messageRegex.matcher(cleanText)
        if (matcher.matches()) {
            val isMegaChest = matcher.group("megaName") != null
            var name = matcher.group("pName1") ?: matcher.group("pName2") ?: matcher.group("bName") ?: matcher.group("megaName")
            name = name?.trim() ?: ""
            val msg = matcher.group("message")?.trim() ?: ""

            if (name.isEmpty() || msg.isEmpty()) return false
            if (ignoredNames.contains(name)) return false
            if (name.contains(" > ")) return false

            val (nameComp, msgComp) = splitComponent(message, name, msg)
            
            val finalName = if (isMegaChest) {
                Component.literal(name).withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.BOLD)
            } else {
                nameComp
            }

            currentDialogue = Dialogue(finalName, msgComp)
            lastActivity = System.currentTimeMillis()
            return ConfigManager.features.skyblock.npcDialogue.hideMessages
        }

        // Опции
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
                            options.add(Option(label.trim(), cmd))
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

            if (currentDialogue != null) {
                 currentDialogue = currentDialogue?.copy(options = options)
                 lastActivity = System.currentTimeMillis()
            }
            return ConfigManager.features.skyblock.npcDialogue.hideMessages
        }

        return false
    }

    override fun getWidth(): Int {
        // Если customWidth установлен (через редактор), используем его.
        // Иначе 300 по умолчанию.
        return if (customWidth > 0) customWidth else 300
    }

    override fun getHeight(): Int {
        // Возвращаем рассчитанную высоту
        // Если calculatedHeight 0 (еще не рендерилось), возвращаем минимальную высоту или высоту dummy dialogue (примерно 100)
        val h = if (calculatedHeight > 0) calculatedHeight else 100
        return h.coerceAtLeast(if (customHeight > 0) customHeight else 50)
    }

    override fun render() {
        val config = ConfigManager.features.skyblock.npcDialogue
        
        // В режиме редактирования показываем пример
        val dialogue = if (isEditing) dummyDialogue else currentDialogue
        
        if (dialogue == null && !isEditing) return
        
        if (!isEditing) {
            if (System.currentTimeMillis() - lastActivity > (config.timeoutSeconds * 1000).toLong()) {
                currentDialogue = null
                return
            }
            
            if (dialogue!!.name.string.isEmpty() || dialogue.text.string.isEmpty()) return
            if (!config.enabled) return
            
            val mc = Minecraft.getInstance()
            // Не рендерим поверх других экранов (инвентарей), кроме чата
            if (mc.screen != null && mc.screen !is net.minecraft.client.gui.screens.ChatScreen) return
            
            handleInput()
        }

        val graphics = cachedGraphics ?: return
        val font = Minecraft.getInstance().font

        // Компактный режим
        val isCompact = config.compactMode
        val padding = if (isCompact) 6 else 10
        val lineSpacing = if (isCompact) 2 else 5
        val nameGap = if (isCompact) 10 else 15
        
        // Ширина окна - используем customWidth или дефолт
        val boxWidth = getWidth()
        
        // Разбивка текста на строки
        val wrappedText = font.split(dialogue!!.text, boxWidth - padding * 2)
        
        var contentHeight = padding * 2 + wrappedText.size * font.lineHeight + nameGap
        if (!isCompact) contentHeight += 10

        if (dialogue.options.isNotEmpty() && config.showOptions) {
            contentHeight += (if (isCompact) 5 else 10) + dialogue.options.size * (font.lineHeight + lineSpacing)
        }
        
        // Место под подсказку ESC
        if (config.closeOnEsc) {
            contentHeight += font.lineHeight + (if (isCompact) 2 else 4)
        }
        
        // Если задана кастомная высота (минимальная), используем её, если контент меньше
        // Но если контент больше, расширяем
        val boxHeight = if (customHeight > 0) kotlin.math.max(customHeight, contentHeight) else contentHeight
        calculatedHeight = boxHeight

        // Отрисовка фона
        val drawX = x
        val drawY = y
        
        // Используем showBackground от HudElement как главный источник правды
        // Если пользователь отключил фон в редакторе, он должен исчезнуть
        // Также учитываем настройку из конфига
        if (showBackground && config.showBackground) {
            graphics.fill(drawX, drawY, drawX + boxWidth, drawY + boxHeight, Color(0, 0, 0, 180).rgb)
            // Простая обводка
            val outlineColor = Color(255, 255, 255, 100).rgb
            graphics.fill(drawX, drawY, drawX + boxWidth, drawY + 1, outlineColor) // Верх
            graphics.fill(drawX, drawY + boxHeight - 1, drawX + boxWidth, drawY + boxHeight, outlineColor) // Низ
            graphics.fill(drawX, drawY, drawX + 1, drawY + boxHeight, outlineColor) // Лево
            graphics.fill(drawX + boxWidth - 1, drawY, drawX + boxWidth, drawY + boxHeight, outlineColor) // Право
        }

        // Отрисовка имени (по центру)
        val nameWidth = font.width(dialogue.name)
        val nameX = drawX + (boxWidth - nameWidth) / 2
        graphics.drawString(font, dialogue.name, nameX, drawY + padding, 0xFFFFFFFF.toInt())

        // Отрисовка текста
        var currentY = drawY + padding + nameGap
        for (line in wrappedText) {
            graphics.drawString(font, line, drawX + padding, currentY, 0xFFFFFFFF.toInt())
            currentY += font.lineHeight
        }

        // Отрисовка опций
        if (dialogue.options.isNotEmpty() && config.showOptions) {
            currentY += if (isCompact) 5 else 10
            dialogue.options.forEachIndexed { index, option ->
                val optionText = "§6${index + 1}. §b${option.text}"
                graphics.drawString(font, optionText, drawX + padding, currentY, 0xFFFFFFFF.toInt())
                currentY += font.lineHeight + lineSpacing
            }
        }
        
        // Подсказка про ESC
        if (config.closeOnEsc) {
            val escText = "§c(Нажмите ESC для скрытия)"
            val escWidth = font.width(escText)
            // Рисуем снизу по центру
            graphics.drawString(font, escText, drawX + (boxWidth - escWidth) / 2, drawY + boxHeight - font.lineHeight - (if (isCompact) 2 else 4), 0xFFFF5555.toInt())
        }
    }

    fun isActive(): Boolean {
        return currentDialogue != null
    }
    
    fun close() {
        currentDialogue = null
    }

    private fun handleInput() {
        val dialogue = currentDialogue ?: return
        val config = ConfigManager.features.skyblock.npcDialogue
        val mc = Minecraft.getInstance()
        val window = mc.window
        val time = System.currentTimeMillis()
        
        if (config.closeOnEsc && InputConstants.isKeyDown(window, 256)) {
            currentDialogue = null
            return
        }

        if (dialogue.options.isEmpty()) return
        
        if (time - lastInputTime < 200) return
        
        for (i in 0 until dialogue.options.size) {
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
                lastInputTime = time
                return
            }
        }
    }
}
