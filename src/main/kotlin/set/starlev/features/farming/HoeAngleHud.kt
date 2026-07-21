package set.starlev.features.farming

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import set.starlev.StarredHeltix
import set.starlev.features.Category
import set.starlev.hud.HudElement

/**
 * HUD отображения текущего Yaw и Pitch для точного фарма.
 * Активируется когда в руках любой инструмент для сбора урожая (мотыга, нож, резчик, топор).
 */
object HoeAngleHud : HudElement(
    id = "HoeAngleHud",
    name = "Hoe Angle HUD",
    category = Category.FARMING,
    description = "Угол мотыги"
) {
    private var lastYaw: Float = 0f
    private var lastPitch: Float = 0f

    override fun render() {
        val player = mc.player ?: return
        val config = StarredHeltix.feature.farming.hoeAngleHud
        if (!config.enabled) return

        // Любой инструмент для сбора урожая: мотыга, нож, резчик, топор
        val heldItem = player.mainHandItem
        val itemName = heldItem.hoverName.string.lowercase()
        val itemType = heldItem.item.toString().lowercase()
        
        val isFarmingTool = itemName.contains("мотыга") || itemName.contains("hoe") || 
                            itemName.contains("нож") || itemName.contains("knife") ||
                            itemName.contains("резчик") || itemName.contains("dicer") ||
                            itemName.contains("топор") || itemName.contains("axe") ||
                            itemType.contains("hoe")

        if (!isFarmingTool && !isEditing) return

        val graphics = cachedGraphics ?: return
        val font = mc.font

        // Отключаем фон принудительно
        showBackground = false

        // Получаем yaw и pitch
        lastYaw = player.yRot
        lastPitch = player.xRot

        // Нормализуем Yaw к диапазону -180..180
        var displayYaw = lastYaw % 360f
        if (displayYaw > 180f) displayYaw -= 360f
        if (displayYaw < -180f) displayYaw += 360f

        val yawText = "Yaw/Угол: ${String.format("%.2f", displayYaw)}°"
        val pitchText = "Pitch/Наклон: ${String.format("%.2f", lastPitch)}°"

        graphics.drawString(font, yawText, x, y, 0xFFFFFFFF.toInt(), true)
        graphics.drawString(font, pitchText, x, y + font.lineHeight + 2, 0xFFFFFFFF.toInt(), true)
    }

    override fun getWidth(): Int = 85
    override fun getHeight(): Int = mc.font.lineHeight * 2 + 4

    override fun getDefaultX(): Int = 5
    override fun getDefaultY(): Int = 70

    override fun init() {
    }
}
