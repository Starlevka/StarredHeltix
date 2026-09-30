package set.starlev.features.farming.garden

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import set.starlev.utils.detectors.MobHeadDisplayDetector

/**
 * Подсветка вредителей — красный луч маяка 24 блока от энтити, радиус 48, очень лёгкий.
 * Категория Фермерство → Сад.
 */
object PestHighlight {
    private val mc = Minecraft.getInstance()
    private val pestNames = listOf(
        "саранча", "муха", "сверчок", "крыса", "комар",
        "дождевой червь", "клещ", "моль", "слизняк", "жук"
    )
    private var cachedPests: List<Vec3> = emptyList()
    private var lastScan = 0L

    fun init() {
        RenderEvents.register { context ->
            val enabled = try { StarredHeltix.feature.farming.garden.pestHighlight } catch (_: Exception) { return@register }
            if (!enabled) return@register
            if (!GardenApi.inGarden()) return@register
            val level = mc.level ?: return@register
            val player = mc.player ?: return@register
            if (cachedPests.isEmpty() && System.currentTimeMillis() - lastScan > 200) {
                scanPests(player.position())
            } else if (System.currentTimeMillis() - lastScan > 500) {
                scanPests(player.position())
            }
            for (pos in cachedPests) {
                val distSqr = player.position().distanceToSqr(pos)
                if (distSqr > 48 * 48) continue
                val aabb = AABB(pos.x - 0.5, pos.y, pos.z - 0.5, pos.x + 0.5, pos.y + 24, pos.z + 0.5)
                if (!context.isBoxVisible(aabb)) continue
                // Красный луч 24 блока, лёгкий — без текста, только маяк
                context.renderBeaconBeam(pos.x, pos.y, pos.z, 24, 0xFF0000, context.tickDelta, seeThrough = true)
            }
        }
    }

    private fun scanPests(playerPos: Vec3) {
        lastScan = System.currentTimeMillis()
        val level = mc.level ?: run { cachedPests = emptyList(); return }
        val player = mc.player ?: run { cachedPests = emptyList(); return }
        val box = AABB(
            playerPos.x - 48, playerPos.y - 20, playerPos.z - 48,
            playerPos.x + 48, playerPos.y + 20, playerPos.z + 48
        )
        val result = mutableListOf<Vec3>()
        try {
            val entities = level.getEntitiesOfClass(LivingEntity::class.java, box) { it.isAlive }
            for (entity in entities) {
                // Быстрая проверка дистанции до игрока (48)
                if (entity.position().distanceToSqr(playerPos) > 48 * 48) continue
                // Лёгкая проверка: имя энтити или дисплей-хэд содержит ник вредителя
                val displays = try { MobHeadDisplayDetector.getHeadDisplays(entity) } catch (_: Exception) { null }
                val hasPest = if (displays != null && displays.textDisplays.isNotEmpty()) {
                    displays.textDisplays.any { comp ->
                        val s = comp.string.lowercase()
                        pestNames.any { pest -> s.contains(pest) }
                    }
                } else {
                    // Фолбэк: проверяем имя самой энтити
                    val name = entity.name.string.lowercase()
                    pestNames.any { name.contains(it) }
                }
                if (hasPest) {
                    result.add(Vec3(entity.x, entity.y, entity.z))
                    if (result.size >= 12) break // лимит для оптимизации
                }
            }
        } catch (_: Exception) {}
        cachedPests = result
    }
}
