package set.starlev.features.misc

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
    private var lastItemStacks = mutableMapOf<String, ItemStack>()
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
        val config = StarredHeltix.feature.misc.general.inventoryHistory
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
        val config = StarredHeltix.feature.misc.general.inventoryHistory
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
            lastItemStacks.putAll(itemMap)
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
                // Пытаемся взять текущий стак, если его нет (предмет удален) - берем из сохраненных
                val stack = itemMap[key] ?: lastItemStacks[key] ?: ItemStack.EMPTY
                
                if (!stack.isEmpty) {
                    addEntry(stack, diff)
                }
            }
        }

        lastInventory.clear()
        lastInventory.putAll(currentInventory)
        
        // Обновляем кэш стаков: берем старые и заменяем новыми, если они есть
        // Если предмета больше нет в инвентаре, оставляем старый образец на случай, если он вернется или для логирования
        // Но лучше хранить только актуальные + те, что только что исчезли?
        // Проще: храним все, что видели, но очищаем иногда? Нет, просто обновляем.
        itemMap.forEach { (k, v) -> lastItemStacks[k] = v }
        // Удаляем те, которых совсем нет? Нет, иначе не сможем залогировать удаление в следующем тике если логика изменится.
        // Но сейчас мы уже залогировали.
        // Очистим lastItemStacks и заполним itemMap, чтобы не хранить мусор.
        // Но если предмет удален, нам нужен его стак ТОЛЬКО в этот тик.
        // В следующем тике lastCount=0, currentCount=0 -> diff=0. Стак не нужен.
        lastItemStacks.clear()
        lastItemStacks.putAll(itemMap)
    }

    private fun addEntry(stack: ItemStack, amount: Int) {
        val now = System.currentTimeMillis()
        val isAdded = amount > 0
        
        // Пытаемся найти существующую недавнюю запись того же типа для стаканья по названию
        val key = net.minecraft.ChatFormatting.stripFormatting(stack.hoverName.string) ?: stack.hoverName.string
        val existing = history.findLast { 
            val itKey = net.minecraft.ChatFormatting.stripFormatting(it.itemStack.hoverName.string) ?: it.itemStack.hoverName.string
            it.isAdded == isAdded && 
            itKey == key && 
            now - it.timestamp < 3000 // Увеличили тайм-аут до 3 секунд
        }

        if (existing != null) {
            // Обновляем существующую запись, сохраняя её позицию, но обновляя время
            val index = history.indexOf(existing)
            if (index != -1) {
                val newAmount = existing.amount + amount
                // Если сумма стала 0, удаляем запись? Обычно логируем изменения, так что 0 странно.
                // Но если +1 и -1, то это разные записи (isAdded разный).
                // Тут мы мержим только + с + и - с -.
                
                // Создаем новую запись
                val mergedEntry = LogEntry(stack, newAmount, isAdded, now, 0)
                
                // Заменяем старую запись на новую (обновляем кол-во и время)
                history[index] = mergedEntry
                
                // Вариант 2: Переместить в конец (как было раньше)
                // history.removeAt(index)
                // history.add(mergedEntry)
                // Оставим вариант с обновлением на месте, чтобы не прыгало, или лучше прыгать?
                // Пользователь жаловался на дубли. Обновление на месте выглядит чище.
                // НО: Если прошло 2 секунды, и мы обновляем старую запись, она "моргнет" или просто изменит цифру.
                // Если переместить вниз, она появится как новая.
                // Давайте переместим вниз, чтобы было видно "активность".
                history.removeAt(index)
                history.add(mergedEntry)
            } else {
                // Если вдруг не нашли по индексу (конкуренция?), добавляем новую
                history.add(LogEntry(stack.copy(), amount, isAdded, now, 0))
            }
        } else {
            history.add(LogEntry(stack.copy(), amount, isAdded, now, 0))
        }

        // Лимит записей в истории
        if (history.size > 50) {
            history.removeAt(0)
        }
    }

    override fun getWidth(): Int {
        val config = StarredHeltix.feature.misc.general.inventoryHistory
        return 100 // Упрощенно, реальный размер в render
    }
    
    override fun getHeight(): Int = 20
    override fun getDefaultScale(): Float = 1.0f
    override fun getDefaultX(): Int = 283
    override fun getDefaultY(): Int = 3
}
