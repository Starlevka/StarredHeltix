package set.starlev.features.skyblock

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import set.starlev.utils.detectors.MuseumDetector
import set.starlev.utils.ContainerUtils
import set.starlev.utils.detectors.ContainerDetector

/**
 * Функционал для музея: отслеживание предметов и отображение их в HUD.
 */
object Museum : HudElement("MuseumHud") {
    private val mc = Minecraft.getInstance()
    // Используем Component для хранения названия с цветами
    private val foundItems = mutableMapOf<String, Component>()
    private var lastUpdate = 0L
    private var lastScanTime = 0L
    private val unobtainableItems = setOf(
        "Кинжал Ливида",
        "Шлем хранителя",
        "Гигантский тесак",
        "Кнут зомби-командира",
        "Меч Фела",
        "Жнец Фелторна",
        "Броня лорда зомби",
        "Броня лорда скелетов",
        "Броня командира зомби"
    )
    private val legacyFormattingCodeRegex = Regex("§[0-9A-FK-ORa-fk-or]")

    private fun formatForHud(component: Component): Component {
        val name = component.string.replace(legacyFormattingCodeRegex, "").trim()
        if (name.isBlank() || name !in unobtainableItems) return component
        return Component.literal("§c§m$name§r")
    }

    fun init() {
        ContainerDetector.registerOpen { screen ->
            if (!StarredHeltix.feature.skyblock.museum.enabled) return@registerOpen
            if (MuseumDetector.isMuseumMenu(screen)) {
                updateFromContainer(screen)
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            onTick()
        }
    }

    fun onTick() {
        if (!StarredHeltix.feature.skyblock.museum.enabled) return
        
        // Каждые 500мс проверяем зону и контейнер
        val now = System.currentTimeMillis()
        if (now - lastScanTime > 500) {
            lastScanTime = now
            
            // 1. Проверяем скорборд на наличие ключевых слов музея
            if (MuseumDetector.isInMuseumZone()) {
                val screen = mc.screen as? AbstractContainerScreen<*>
                if (screen != null) {
                    updateFromContainer(screen)
                }
            } else {
                // Если мы не в музее - очищаем список (опционально, но логично для HUD)
                if (foundItems.isNotEmpty()) {
                    foundItems.clear()
                    lastUpdate = now
                }
            }
        }
    }

    /**
     * Сканирует контейнер на наличие серого красителя и запоминает названия.
     * Серый краситель в музее Heltix означает, что предмет еще не сдан.
     */
    private fun updateFromContainer(screen: AbstractContainerScreen<*>) {
        if (!StarredHeltix.feature.skyblock.museum.enabled) return
        val items = ContainerUtils.getContainerItems(screen)
        var changed = false
        
        // Временный список для текущего окна, чтобы понимать, что сменилось
        val currentWindowGrayDyes = mutableSetOf<String>()
        
        for (stack in items) {
            if (stack.isEmpty || stack.`is`(Items.AIR)) continue
            
            val isGrayDye = stack.`is`(Items.GRAY_DYE) || stack.`is`(Items.LIGHT_GRAY_DYE)
            val baseName = stack.hoverName
            val nameComponent = if (baseName.string.isBlank()) Component.translatable(stack.item.descriptionId) else baseName
            val cleanName = nameComponent.string
            if (!isGrayDye && (cleanName.isBlank() || cleanName.contains("stained_glass_pane"))) continue

            // Если это серый краситель - добавляем в список "нужных"
            if (isGrayDye) {
                currentWindowGrayDyes.add(cleanName)
                if (!foundItems.containsKey(cleanName)) {
                    foundItems[cleanName] = nameComponent
                    changed = true
                }
            } else {
                // Если это НЕ серый краситель (и не стекло), значит предмет сдан
                if (foundItems.containsKey(cleanName)) {
                    foundItems.remove(cleanName)
                    changed = true
                }
            }
        }

        if (currentWindowGrayDyes.isEmpty()) {
            if (foundItems.isNotEmpty()) {
                foundItems.clear()
                changed = true
            }
        } else {
            val toRemove = foundItems.keys.filter { it !in currentWindowGrayDyes }
            if (toRemove.isNotEmpty()) {
                toRemove.forEach { foundItems.remove(it) }
                changed = true
            }
        }
        
        if (changed) {
            lastUpdate = System.currentTimeMillis()
        }
        
    }

    override fun render() {
        if (!StarredHeltix.feature.skyblock.museum.enabled) return
        if (!isEditing && mc.screen !is AbstractContainerScreen<*>) return
        if (foundItems.isEmpty() && !isEditing) return
        
        val displayList = if (isEditing && foundItems.isEmpty()) {
            listOf(Component.literal("§7Пример предмета 1"), Component.literal("§eЭкспонат: Меч"))
        } else {
            foundItems.values.toList()
        }

        val padding = 4
        val rowHeight = mc.font.lineHeight + 2
        val title = "§6§lМузей: §e${displayList.size}"
        
        var maxWidth = mc.font.width(title)
        for (comp in displayList) {
            val w = mc.font.width(Component.literal("§7- ").append(formatForHud(comp)))
            if (w > maxWidth) maxWidth = w
        }

        val height = rowHeight * (displayList.size + 1)
        
        this.showBackground = StarredHeltix.feature.skyblock.museum.showBackground
        drawBackground(maxWidth, height, padding)
        
        val graphics = cachedGraphics ?: return
        var currentY = y
        
        graphics.drawString(mc.font, title, x, currentY, 0xFFFFFFFF.toInt())
        currentY += rowHeight
        
        for (comp in displayList) {
            graphics.drawString(mc.font, Component.literal("§7- ").append(formatForHud(comp)), x, currentY, 0xFFFFFFFF.toInt())
            currentY += rowHeight
        }
    }

    override fun getWidth(): Int {
        val title = "§6§lМузей: §e${foundItems.size}"
        var maxWidth = mc.font.width(title)
        val displayList = if (isEditing && foundItems.isEmpty()) {
            listOf(Component.literal("§7Пример предмета 1"), Component.literal("§eЭкспонат: Меч"))
        } else foundItems.values
        
        for (comp in displayList) {
            val w = mc.font.width(Component.literal("§7- ").append(formatForHud(comp)))
            if (w > maxWidth) maxWidth = w
        }
        return maxWidth
    }

    override fun getHeight(): Int {
        val displayListSize = if (isEditing && foundItems.isEmpty()) 2 else foundItems.size
        return (mc.font.lineHeight + 2) * (displayListSize + 1)
    }

    override fun getDefaultScale(): Float = 1.0f
    override fun getDefaultX(): Int = 636
    override fun getDefaultY(): Int = 211

    override fun getAccentColor(): Int = 0xFFFFAA00.toInt() // Оранжевый для музея

    /**
     * Очистить список найденных предметов.
     */
    fun clear() {
        foundItems.clear()
        lastUpdate = System.currentTimeMillis()
    }
}
