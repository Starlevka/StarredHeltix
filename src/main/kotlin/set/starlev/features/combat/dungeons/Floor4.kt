package set.starlev.features.combat.dungeons

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.DungeonDetector
import set.starlev.utils.detectors.MobHeadDisplayDetector

/**
    * Функции для Ф4 (Floor 4) подземелий:
    * - Подсветка + glow моба "Дух медведя"
    * - Title-уведомление + звук при обнаружении "Дух курицы" в радиусе 64 блоков
    */
object Floor4 {
    private val mc = Minecraft.getInstance()
    private var lastChickenAlert = 0L
    private const val CHICKEN_ALERT_COOLDOWN = 5000L
    private const val DETECTION_RADIUS = 64.0

    // Glow management для Духа медведя
    private val managedGlowIds = mutableSetOf<Int>()
    private val seenGlowIds = mutableSetOf<Int>()
    private val glowColorsByEntityId = mutableMapOf<Int, Int>()

    fun init() {
        // Миграция: принудительно заменяем старые цвета на розовый
        val config = StarredHeltix.feature.dungeons.floor4
        val oldColors = setOf("0:255:255:0:0", "0:255:255:255:0:0", "0:255:255:0:0:0")
        if (config.bearSpiritColorV2 in oldColors || config.bearSpiritColorV2.startsWith("0:255:255:0")) {
            config.bearSpiritColorV2 = "0:255:255:105:180"
        }

        RenderEvents.register { context ->
            if (!config.bearSpiritHighlight && !config.chickenSpiritAlert) return@register
            if (!DungeonDetector.isInDungeon()) return@register

            val player = mc.player ?: return@register
            val playerPos = player.position()
            val level = mc.level ?: return@register
            seenGlowIds.clear()

            for (entity in level.entitiesForRendering()) {
                if (entity !is LivingEntity) continue

                val displayName = getEntityDisplayName(entity)
                if (displayName.isBlank()) continue

                // Дух медведя — подсветка + glow
                if (config.bearSpiritHighlight && displayName.contains("Дух медведя", ignoreCase = true)) {
                    val color = ColorUtils.parseColor(config.bearSpiritColorV2)
                    val r = ((color shr 16) and 0xFF) / 255f
                    val g = ((color shr 8) and 0xFF) / 255f
                    val b = (color and 0xFF) / 255f
                    val a = ((color shr 24) and 0xFF) / 255f

                    val box = entity.boundingBox
                    context.renderBox(box, r, g, b, a, fill = false)

                    if (config.bearSpiritText) {
                        val centerX = box.minX + (box.maxX - box.minX) / 2
                        val centerZ = box.minZ + (box.maxZ - box.minZ) / 2
                        val textY = box.maxY + 1.5
                        val scale = 1.6f
                        val pinkColor = 0xFFFF69B4.toInt()
                        context.renderText(
                            "§lДух медведя",
                            centerX,
                            textY,
                            centerZ,
                            scale = scale,
                            color = pinkColor,
                            seeThrough = true
                        )
                    }

                    applyGlow(entity, color)
                }

                // Дух курицы — алерт в радиусе 64 блоков
                if (config.chickenSpiritAlert && displayName.contains("Дух курицы", ignoreCase = true)) {
                    val dist = playerPos.distanceTo(entity.position())
                    if (dist <= DETECTION_RADIUS) {
                        val now = System.currentTimeMillis()
                        if (now - lastChickenAlert >= CHICKEN_ALERT_COOLDOWN) {
                            lastChickenAlert = now
                            triggerChickenAlert(dist)
                        }
                    }
                }
            }

            cleanupGlow(level)
        }
    }

    private fun getEntityDisplayName(entity: LivingEntity): String {
        // 1. Проверяем кастомное имя
        entity.customName?.let { return cleanText(it.string) }

        // 2. Проверяем TextDisplay над головой (через MobHeadDisplayDetector)
        val headDisplays = MobHeadDisplayDetector.getHeadDisplays(entity)
        for (comp in headDisplays.textDisplays) {
            val text = cleanText(comp.string)
            if (text.isNotBlank()) return text
        }

        return ""
    }

    private fun triggerChickenAlert(distance: Double) {
        val config = StarredHeltix.feature.dungeons.floor4

        if (config.chickenTitleEnabled) {
            mc.gui.setTitle(Component.literal(config.chickenTitleText))
            mc.gui.setSubtitle(Component.literal(config.chickenSubtitleText))
            mc.gui.setTimes(10, 80, 20)
        }

        if (config.chickenSoundEnabled) {
            mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f))
            mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 0.8f))
            mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.5f))
        }
    }

    private fun applyGlow(entity: Entity, color: Int) {
        val id = entity.id
        managedGlowIds.add(id)
        seenGlowIds.add(id)
        glowColorsByEntityId[id] = color
        if (!entity.isCurrentlyGlowing) {
            entity.setGlowingTag(true)
        }
    }

    private fun cleanupGlow(level: net.minecraft.client.multiplayer.ClientLevel) {
        if (managedGlowIds.isEmpty()) return
        val iter = managedGlowIds.iterator()
        while (iter.hasNext()) {
            val id = iter.next()
            if (seenGlowIds.contains(id)) continue
            val entity = level.getEntity(id)
            if (entity != null) {
                entity.setGlowingTag(false)
            }
            glowColorsByEntityId.remove(id)
            iter.remove()
        }
    }
    private fun cleanText(text: String): String {
        return text.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
    }
}
