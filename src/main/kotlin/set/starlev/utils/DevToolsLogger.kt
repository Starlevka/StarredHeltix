package set.starlev.utils

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.phys.AABB
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import set.starlev.config.categories.AboutModConfig
import set.starlev.features.Feature
import set.starlev.features.Category
import set.starlev.utils.detectors.ContainerDetector
import set.starlev.utils.detectors.MobHeadDisplayDetector
import set.starlev.StarredHeltix
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item

object DevToolsLogger : Feature(
    name = "Dev Tools Logger",
    category = Category.SECRET,
    description = "Дев-инструменты"
) {
    private var lastLoggedContainer: String? = null
    private var lastLoggedMobs: Set<String> = emptySet()
    private var lastLoggedHeldItem: String? = null
    private var lastLoggedHoveredItem: String? = null

    private val hoveredSlotField: java.lang.reflect.Field? by lazy {
        try {
            val field = AbstractContainerScreen::class.java.getDeclaredField("hoveredSlot")
            if (field.trySetAccessible()) field else null
        } catch (e: Exception) {
            StarredHeltix.LOGGER.warn("[DevToolsLogger] Не удалось получить доступ к hoveredSlot: ${e.message}")
            null
        }
    }

    override fun init() {
        // Инициализация будет происходить через события
    }

    /**
     * Возвращает true, если хотя бы один из четырёх дев-логов включён.
     * Используется в hot-path тиков, чтобы не вызывать по отдельности
     * logContainer/logNearbyMobs/logHeldItem/logHoveredItem, когда все флаги выключены.
     */
    fun isAnyEnabled(): Boolean {
        val cfg = StarredHeltix.feature.about.devTools
        return cfg.logContainer || cfg.logMobs || cfg.logHeldItem || cfg.logHoveredItem
    }

    fun logContainer() {
        if (!StarredHeltix.feature.about.devTools.logContainer) return

        val containerInfo = ContainerDetector.getCurrentContainerInfo()
        val currentContainer = containerInfo?.decodedTitle ?: "Нет открытого контейнера"
        
        if (currentContainer != lastLoggedContainer) {
            lastLoggedContainer = currentContainer
            StarredHeltix.LOGGER.info("[DEV-TOOLS] Контейнер: $currentContainer")
            mc.player?.displayClientMessage(
                Component.literal("§7[DEV] §fКонтейнер: §b$currentContainer"), 
                false
            )
        }
    }

    fun logNearbyMobs() {
        if (!StarredHeltix.feature.about.devTools.logMobs) return

        val player = mc.player ?: return
        val level = mc.level ?: return

        val boundingBox = AABB(
            player.x - 32.0,
            player.y - 32.0,
            player.z - 32.0,
            player.x + 32.0,
            player.y + 32.0,
            player.z + 32.0
        )

        val nearbyEntities = level.getEntities(null, boundingBox)
        val livingEntities = nearbyEntities.filterIsInstance<LivingEntity>()
        
        val mobInfo = livingEntities.map { entity ->
            val entityType = EntityType.getKey(entity.type)?.toString() ?: "unknown"
            val name = entity.name.string
            val distance = entity.distanceToSqr(player)
            "$name ($entityType) [${String.format("%.1f", kotlin.math.sqrt(distance))}м]"
        }.toSet()

        if (mobInfo != lastLoggedMobs) {
            lastLoggedMobs = mobInfo
            if (mobInfo.isNotEmpty()) {
                StarredHeltix.LOGGER.info("[DEV-TOOLS] Мобы рядом: ${mobInfo.joinToString(", ")}")
                mc.player?.displayClientMessage(
                    Component.literal("§7[DEV] §fМобы рядом: §e${mobInfo.size} шт"), 
                    false
                )
                mobInfo.take(3).forEach { mob ->
                    mc.player?.displayClientMessage(
                        Component.literal("§7  - §f$mob"), 
                        false
                    )
                }
            }
        }
    }

    fun logHeldItem() {
        if (!StarredHeltix.feature.about.devTools.logHeldItem) return

        val player = mc.player ?: return
        val heldItem = player.mainHandItem
        
        if (heldItem.isEmpty) {
            val current = "Пустая рука"
            if (current != lastLoggedHeldItem) {
                lastLoggedHeldItem = current
                StarredHeltix.LOGGER.info("[DEV-TOOLS] Предмет в руках: $current")
                mc.player?.displayClientMessage(
                    Component.literal("§7[DEV] §fВ руках: §7$current"), 
                    false
                )
            }
        } else {
            val itemName = heldItem.displayName.string
            val itemType = heldItem.item.toString().split(".").lastOrNull() ?: "unknown"
            val context = Item.TooltipContext.EMPTY
            val lore = heldItem.getTooltipLines(context, mc.player, TooltipFlag.NORMAL)
                .joinToString(" | ") { it.string }
            
            val current = "$itemName ($itemType)"
            if (current != lastLoggedHeldItem) {
                lastLoggedHeldItem = current
                StarredHeltix.LOGGER.info("[DEV-TOOLS] Предмет в руках: $current | Лор: $lore")
                mc.player?.displayClientMessage(
                    Component.literal("§7[DEV] §fВ руках: §b$itemName §7($itemType)"), 
                    false
                )
                if (lore.isNotEmpty()) {
                    mc.player?.displayClientMessage(
                        Component.literal("§7  Лор: $lore"), 
                        false
                    )
                }
            }
        }
    }

    fun logHoveredItem() {
        if (!StarredHeltix.feature.about.devTools.logHoveredItem) return

        val screen = mc.screen
        if (screen !is AbstractContainerScreen<*>) return

        val hoveredSlot = try {
            hoveredSlotField?.get(screen) as? net.minecraft.world.inventory.Slot
        } catch (e: Exception) {
            null
        }
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            val current = "Нет предмета под курсором"
            if (current != lastLoggedHoveredItem) {
                lastLoggedHoveredItem = current
                StarredHeltix.LOGGER.info("[DEV-TOOLS] Предмет под курсором: $current")
            }
            return
        }

        val hoveredItem = hoveredSlot.item
        val player = mc.player ?: return
        val itemName = hoveredItem.displayName.string
        val itemType = hoveredItem.item.toString().split(".").lastOrNull() ?: "unknown"
        val context = Item.TooltipContext.EMPTY
        val lore = hoveredItem.getTooltipLines(context, mc.player, TooltipFlag.NORMAL)
            .joinToString(" | ") { it.string }
        
        val current = "$itemName ($itemType)"
        if (current != lastLoggedHoveredItem) {
            lastLoggedHoveredItem = current
            StarredHeltix.LOGGER.info("[DEV-TOOLS] Предмет под курсором: $current | Лор: $lore")
            mc.player?.displayClientMessage(
                Component.literal("§7[DEV] §fПод курсором: §b$itemName §7($itemType)"), 
                false
            )
            if (lore.isNotEmpty()) {
                mc.player?.displayClientMessage(
                    Component.literal("§7  Лор: $lore"), 
                    false
                )
            }
        }
    }

    fun reset() {
        lastLoggedContainer = null
        lastLoggedMobs = emptySet()
        lastLoggedHeldItem = null
        lastLoggedHoveredItem = null
    }
}
