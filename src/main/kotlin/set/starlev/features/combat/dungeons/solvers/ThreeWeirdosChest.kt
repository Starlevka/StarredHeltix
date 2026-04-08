package set.starlev.features.combat.dungeons.solvers

import net.minecraft.client.Minecraft
import net.minecraft.world.phys.AABB
import org.slf4j.LoggerFactory
import set.starlev.StarredHeltix
import set.starlev.render.RenderContext
import set.starlev.render.RenderEvents
import set.starlev.utils.detectors.DungeonDetector

/**
 * Подсветка сундука Трёх незнакомцев.
 * Использует позиции, найденные ThreeWeirdos, для отрисовки подсветки.
 */
object ThreeWeirdosChest {
    private val LOGGER = LoggerFactory.getLogger(ThreeWeirdosChest::class.java)
    private val mc = Minecraft.getInstance()

    fun init() {
        RenderEvents.register { context ->
            if (!StarredHeltix.feature.dungeons.solvers.threeWeirdos) {
                return@register
            }
            if (!DungeonDetector.isInDungeon()) {
                return@register
            }

            // Не рендерим, если головоломка уже решена
            if (ThreeWeirdos.puzzleSolved) {
                return@register
            }
            
            val chestPos = ThreeWeirdos.foundChestPos ?: return@register
            val correctName = ThreeWeirdos.getCorrectStranger() ?: "Незнакомец"

            // Подсвечиваем сундук (красный)
            try {
                val chestBox = AABB(chestPos)
                // Заполнение красным
                context.renderBox(chestBox, 1f, 0f, 0f, 0.3f, fill = true)
                // Обводка красная
                context.renderBox(chestBox, 1f, 0f, 0f, 1.0f, fill = false)
                // Толстая обводка через блоки
                context.renderBoxThroughBlocks(chestBox.inflate(0.01), 1f, 0f, 0f, 1.0f, fill = false, thickness = 3f)
            } catch (e: Exception) {
                LOGGER.error("[ThreeWeirdosChest] Ошибка при отрисовке подсветки", e)
            }
        }
    }

    fun reset() {
        ThreeWeirdos.fullReset()
    }
}