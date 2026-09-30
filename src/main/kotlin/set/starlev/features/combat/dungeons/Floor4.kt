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
import set.starlev.utils.detectors.EntityNameCache

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
            val level = mc.level
            if (!config.bearSpiritHighlight && !config.chickenSpiritAlert) {
                if (level != null && managedGlowIds.isNotEmpty()) {
                    clearAllGlow(level)
                } else if (managedGlowIds.isNotEmpty()) {
                    managedGlowIds.clear()
                    glowColorsByEntityId.clear()
                    seenGlowIds.clear()
                }
                return@register
            }
            if (!DungeonDetector.isInDungeon()) {
                if (level != null && managedGlowIds.isNotEmpty()) {
                    clearAllGlow(level)
                } else if (managedGlowIds.isNotEmpty()) {
                    managedGlowIds.clear()
                    glowColorsByEntityId.clear()
                    seenGlowIds.clear()
                }
                return@register
            }

            val player = mc.player ?: return@register
            val playerPos = player.position()
            if (level == null) return@register
            seenGlowIds.clear()

            for (entity in level.entitiesForRendering()) {
                if (entity !is LivingEntity) continue

                val displayName = EntityNameCache.getName(entity)
                if (displayName.isBlank()) continue

                val isBear = config.bearSpiritHighlight && displayName.contains("Дух медведя", ignoreCase = true)
                val isChicken = config.chickenSpiritAlert && displayName.contains("Дух курицы", ignoreCase = true)
                if (!isBear && !isChicken) continue

                // Дух медведя — подсветка + glow
                if (isBear) {
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

                // Дух курицы — алерт в радиусе 64 блоков (квадрат дистанции, без sqrt)
                if (isChicken) {
                    val dx = entity.x - playerPos.x
                    val dz = entity.z - playerPos.z
                    if (dx * dx + dz * dz <= DETECTION_RADIUS * DETECTION_RADIUS) {
                        val now = System.currentTimeMillis()
                        if (now - lastChickenAlert >= CHICKEN_ALERT_COOLDOWN) {
                            lastChickenAlert = now
                            triggerChickenAlert(kotlin.math.sqrt(dx * dx + dz * dz))
                        }
                    }
                }
            }

            cleanupGlow(level)
        }
    }

    private fun triggerChickenAlert(distance: Double) {
        val config = StarredHeltix.feature.dungeons.floor4

        if (config.chickenTitleEnabled) {
            mc.gui.hud.setTitle(Component.literal(config.chickenTitleText))
            mc.gui.hud.setSubtitle(Component.literal(config.chickenSubtitleText))
            mc.gui.hud.setTimes(10, 80, 20)
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
        // Без setGlowingTag — используется только EntityMixin
    }

    private fun cleanupGlow(level: net.minecraft.client.multiplayer.ClientLevel) {
        if (managedGlowIds.isEmpty()) return
        val iter = managedGlowIds.iterator()
        while (iter.hasNext()) {
            val id = iter.next()
            if (seenGlowIds.contains(id)) continue
            try { level.getEntity(id)?.let { if (it.hasGlowingTag()) it.setGlowingTag(false) } } catch (_: Exception) {}
            glowColorsByEntityId.remove(id)
            EntityNameCache.invalidate(id)
            iter.remove()
        }
    }

    private fun clearAllGlow(level: net.minecraft.client.multiplayer.ClientLevel) {
        if (managedGlowIds.isNotEmpty()) {
            for (id in managedGlowIds) {
                try { level.getEntity(id)?.let { if (it.hasGlowingTag()) it.setGlowingTag(false) } } catch (_: Exception) {}
                glowColorsByEntityId.remove(id)
                EntityNameCache.invalidate(id)
            }
        }
        // Зачистка застрявшего флага у всех энтити (старые версии ставили hasGlowingTag)
        try {
            for (entity in level.entitiesForRendering()) {
                if (entity.hasGlowingTag() && getGlowColor(entity.id) == -1 &&
                    set.starlev.features.combat.EntityHighlight.getGlowColor(entity.id) == -1 &&
                    set.starlev.features.combat.ClickHighlight.getGlowColor(entity.id) == -1 &&
                    set.starlev.scripting.ModuleGlowRegistry.getGlowColor(entity.id) == -1
                ) {
                    entity.setGlowingTag(false)
                }
            }
        } catch (_: Exception) {}
        managedGlowIds.clear()
        glowColorsByEntityId.clear()
        seenGlowIds.clear()
    }

    fun onWorldChange() {
        managedGlowIds.clear()
        glowColorsByEntityId.clear()
        seenGlowIds.clear()
    }

    @JvmStatic
    fun getGlowColor(entityId: Int): Int = glowColorsByEntityId[entityId] ?: -1
}
