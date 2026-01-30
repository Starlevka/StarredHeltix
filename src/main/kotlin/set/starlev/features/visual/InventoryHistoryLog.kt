package set.starlev.features.visual

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.item.ItemStack
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import java.util.concurrent.CopyOnWriteArrayList

object InventoryHistoryLog : HudElement("InventoryHistoryLog") {
    private val mc = Minecraft.getInstance()
    private val history = CopyOnWriteArrayList<LogEntry>()
    private var lastInventory = mutableMapOf<String, Int>()
    private var isInitialized = false

    fun init() {
        // Регистрация не требуется здесь, так как мы регистрируем в StarredHeltix
    }

    data class LogEntry(
        val itemStack: ItemStack,
        val amount: Int,
        val isAdded: Boolean,
        val timestamp: Long,
        val color: Int
    ) {
        val component: Component by lazy {
            val prefix = if (isAdded) "+" else "-"
            val prefixColor = if (isAdded) 0x55FF55 else 0xFF5555 // Lime or Red
            
            val prefixComp = Component.literal("$prefix ")
                .withStyle { it.withColor(prefixColor) }
            
            val countText = if (Math.abs(amount) > 1) " ${Math.abs(amount)}x" else ""
            val nameComp = itemStack.hoverName.copy()
            
            // Если у имени нет цвета, пробуем взять из стиля или дефолтный
            if (nameComp.style.color == null) {
                // Пытаемся сохранить цвет из tooltip если он там есть, 
                // но hoverName обычно уже содержит нужный цвет в 1.21
            }
            
            prefixComp.append(nameComp).append(Component.literal(countText).withStyle { it.withColor(0xAAAAAA) })
        }
    }

    override fun render() {
        val config = StarredHeltix.feature.visuals.inventoryHistory
        if (!config.enabled) return

        val now = System.currentTimeMillis()
        val durationMs = (config.duration * 1000).toLong()
        
        // Очистка старых записей
        history.removeIf { now - it.timestamp > durationMs }

        if (history.isEmpty() && !isEditing) return

        val entriesToShow = if (isEditing && history.isEmpty()) {
            listOf(
                LogEntry(ItemStack(net.minecraft.world.item.Items.DIAMOND), 1, true, now, 0x55FFFF),
                LogEntry(ItemStack(net.minecraft.world.item.Items.COBBLESTONE), 64, false, now, 0xAAAAAA)
            )
        } else {
            history.takeLast(config.maxEntries.toInt())
        }

        val rowHeight = mc.font.lineHeight + 2
        var maxWidth = 0
        entriesToShow.forEach { 
            val width = mc.font.width(it.component)
            if (width > maxWidth) maxWidth = width
        }

        val finalWidth = maxOf(maxWidth + 8, 100)
        val finalHeight = entriesToShow.size * rowHeight + 4

        // Фон отрисовываем только если есть записи
        if (entriesToShow.isNotEmpty()) {
            this.showBackground = config.showBackground
            drawBackground(finalWidth, finalHeight, 0, shadow = false)
            
            var currentY = y + 2
            entriesToShow.forEach { entry ->
                val alpha = if (!isEditing) {
                    val age = now - entry.timestamp
                    val fadeStart = durationMs * 0.8
                    if (age > fadeStart) {
                        1.0f - ((age - fadeStart) / (durationMs - fadeStart)).toFloat()
                    } else 1.0f
                } else 1.0f

                cachedGraphics?.pose()?.pushMatrix()
                // Можно добавить альфу через цвет, но drawString в 1.21.10 умеет в Component стили
                cachedGraphics?.drawString(mc.font, entry.component, x + 4, currentY, 0xFFFFFFFF.toInt(), true)
                cachedGraphics?.pose()?.popMatrix()
                currentY += rowHeight
            }
        }
    }

    fun tick() {
        val player = mc.player ?: return
        val config = StarredHeltix.feature.visuals.inventoryHistory
        if (!config.enabled) return

        val currentInventory = mutableMapOf<String, Int>()
        val itemMap = mutableMapOf<String, ItemStack>()

        // Собираем текущий инвентарь
        // 0-35: основной инвентарь
        // 36-39: броня
        // 40: левая рука
        val maxSlot = if (config.ignoreEquipped) 36 else player.inventory.containerSize
        
        for (i in 0 until maxSlot) {
            val stack = player.inventory.getItem(i)
            if (stack.isEmpty) continue
            
            val key = stack.hoverName.string
            val count = currentInventory.getOrDefault(key, 0)
            currentInventory[key] = count + stack.count
            itemMap[key] = stack
        }

        if (!isInitialized) {
            lastInventory.putAll(currentInventory)
            isInitialized = true
            return
        }

        // Сравниваем по названиям
        val allKeys = (lastInventory.keys + currentInventory.keys).distinct()
        allKeys.forEach { key ->
            val lastCount = lastInventory.getOrDefault(key, 0)
            val currentCount = currentInventory.getOrDefault(key, 0)

            if (lastCount != currentCount) {
                val diff = currentCount - lastCount
                val stack = itemMap[key] ?: ItemStack.EMPTY
                
                if (!stack.isEmpty) {
                    addEntry(stack, diff)
                }
            }
        }

        lastInventory.clear()
        lastInventory.putAll(currentInventory)
    }

    private fun addEntry(stack: ItemStack, amount: Int) {
        val now = System.currentTimeMillis()
        val isAdded = amount > 0
        
        // Пытаемся найти существующую недавнюю запись того же типа для стаканья по названию
        val key = stack.hoverName.string
        val existing = history.findLast { 
            it.isAdded == isAdded && 
            it.itemStack.hoverName.string == key && 
            now - it.timestamp < 1000 
        }

        if (existing != null) {
            history.remove(existing)
            history.add(LogEntry(stack, existing.amount + amount, isAdded, now, 0))
        } else {
            history.add(LogEntry(stack.copy(), amount, isAdded, now, 0))
        }

        // Лимит записей в истории
        if (history.size > 50) {
            history.removeAt(0)
        }
    }

    override fun getWidth(): Int {
        val config = StarredHeltix.feature.visuals.inventoryHistory
        return 100 // Упрощенно, реальный размер в render
    }
    
    override fun getHeight(): Int = 20
    override fun getDefaultScale(): Float = 1.0f
    override fun getDefaultX(): Int = 283
    override fun getDefaultY(): Int = 3
}
