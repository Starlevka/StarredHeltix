package set.starlev.utils.detectors

import net.minecraft.world.entity.LivingEntity

/**
 * Общий кэш имён сущностей для оптимизации world-сканов.
 * Объединяет customName + TextDisplay над головой, троттлит повторные чтения
 * MobHeadDisplayDetector.getHeadDisplays (дорогой AABB-запрос) до 250мс на сущность.
 * Используется Floor4, MiniBossHighlight, EntityHighlight.
 */
object EntityNameCache {
    private data class Entry(val name: String, val timeMs: Long)
    private val cache = HashMap<Int, Entry>()
    private const val TTL_MS = 250L
    private const val MAX_SIZE = 512

    private val colorRegex = Regex("(?i)§[0-9a-fk-orlnmxz]")

    fun getName(entity: LivingEntity): String {
        val id = entity.id
        val now = System.currentTimeMillis()
        val cached = cache[id]
        if (cached != null && now - cached.timeMs <= TTL_MS) return cached.name
        val name = resolveName(entity)
        if (cache.size >= MAX_SIZE) {
            val iter = cache.entries.iterator()
            var removed = 0
            while (iter.hasNext() && removed < 128) {
                val e = iter.next()
                if (now - e.value.timeMs > TTL_MS) {
                    iter.remove()
                    removed++
                }
            }
            if (cache.size >= MAX_SIZE) cache.clear()
        }
        cache[id] = Entry(name, now)
        return name
    }

    fun invalidate(id: Int) {
        cache.remove(id)
    }

    fun clear() {
        cache.clear()
    }

    private fun resolveName(entity: LivingEntity): String {
        entity.customName?.let {
            val s = colorRegex.replace(it.string, "").trim()
            if (s.isNotBlank()) return s
        }
        return try {
            val headDisplays = MobHeadDisplayDetector.getHeadDisplays(entity)
            for (comp in headDisplays.textDisplays) {
                val text = colorRegex.replace(comp.string, "").trim()
                if (text.isNotBlank()) return text
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }
}
