package set.starlev.features.inventory

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * Кнопка-таб в инвентаре (как в Skyblocker).
 * Индекс 0-6 = верхний ряд, 7-13 = нижний ряд.
 */
data class InventoryButton(
   var index: Int = 0,
   var icon: String? = "",
   var command: String? = "",
   var tooltip: String? = "",
) {
    companion object {
        const val TAB_WIDTH = 26
        const val TAB_HEIGHT = 32
        const val MAX_TABS = 14 // 7 верх + 7 низ
    }

    fun isValid(): Boolean = !icon.isNullOrBlank() && !command.isNullOrBlank()

    fun isTopTab(): Boolean = index < 7

    /**
     * Вычисляет позицию таба относительно GUI инвентаря.
     */
    fun getX(guiLeft: Int, guiWidth: Int): Int {
        return guiLeft + (index % 7) * 25 + guiWidth / 2 - 176 / 2
    }

    fun getY(guiTop: Int, guiHeight: Int): Int {
        return if (isTopTab()) guiTop - 28 else guiTop + guiHeight - 4
    }

    fun contains(mouseX: Double, mouseY: Double, guiLeft: Int, guiWidth: Int, guiTop: Int, guiHeight: Int): Boolean {
        val sx = getX(guiLeft, guiWidth)
        val sy = getY(guiTop, guiHeight)
        return mouseX >= sx && mouseX < sx + TAB_WIDTH && mouseY >= sy && mouseY < sy + TAB_HEIGHT
    }

    fun render(graphics: GuiGraphics, guiLeft: Int, guiWidth: Int, guiTop: Int, guiHeight: Int, hovered: Boolean) {
        val sx = getX(guiLeft, guiWidth)
        val sy = getY(guiTop, guiHeight)

        // Фон таба
        val bgColor = if (hovered) 0xC0000000.toInt() else 0x80000000.toInt()
        graphics.fill(sx, sy, sx + TAB_WIDTH, sy + TAB_HEIGHT, bgColor)

        // Рамка
        val borderColor = if (hovered) 0xFFDDDDDD.toInt() else 0xFFAAAAAA.toInt()
        graphics.fill(sx, sy, sx + TAB_WIDTH, sy + 1, borderColor)
        graphics.fill(sx, sy + TAB_HEIGHT - 1, sx + TAB_WIDTH, sy + TAB_HEIGHT, 0xFF555555.toInt())
        graphics.fill(sx, sy, sx + 1, sy + TAB_HEIGHT, borderColor)
        graphics.fill(sx + TAB_WIDTH - 1, sy, sx + TAB_WIDTH, sy + TAB_HEIGHT, 0xFF555555.toInt())

        // Иконка по центру таба
        val item = getItem()
        val iconX = sx + (TAB_WIDTH - 16) / 2
        val iconY = sy + if (isTopTab()) 8 else 6
        graphics.renderItem(item, iconX, iconY)
    }

    fun getItem(): ItemStack {
        val iconName = icon ?: return Items.AIR.defaultInstance
        return try {
            val location = if (iconName.contains(":")) {
                net.minecraft.resources.ResourceLocation.tryParse(iconName)
            } else {
                net.minecraft.resources.ResourceLocation.tryParse("minecraft:$iconName")
            } ?: return Items.AIR.defaultInstance
            val item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(location)
                .orElse(null)
            item?.defaultInstance ?: Items.AIR.defaultInstance
        } catch (_: Exception) {
            Items.AIR.defaultInstance
        }
    }
}