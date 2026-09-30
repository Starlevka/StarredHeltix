package set.starlev.scripting

import net.minecraft.client.Minecraft
import java.util.concurrent.ConcurrentHashMap

/**
 * Реестр клиентской подсветки (glow) для JS-модулей.
 * Аналог EntityHighlight.glowColorsByEntityId, но per-module с автоочисткой при выгрузке.
 * Цвет — ARGB Int, хранится по entityId. Mixin EntityMixin читает getGlowColor().
 */
object ModuleGlowRegistry {
    private data class Entry(val module: String, val color: Int)
    private val colors = ConcurrentHashMap<Int, Entry>()
    private val byModule = ConcurrentHashMap<String, MutableSet<Int>>()

    fun setGlow(entityId: Int, module: String, color: Int) {
        val lower = module.lowercase()
        colors[entityId] = Entry(lower, color)
        byModule.computeIfAbsent(lower) { ConcurrentHashMap.newKeySet() }.add(entityId)
        // Без setGlowingTag — миксин форсит glow по карте цветов
    }

    fun clearGlow(entityId: Int) {
        val e = colors.remove(entityId) ?: return
        byModule[e.module]?.remove(entityId)
        // Миграция: чистим застрявший ванильный флаг от старых версий
        try { Minecraft.getInstance().level?.getEntity(entityId)?.let { if (it.hasGlowingTag()) it.setGlowingTag(false) } } catch (_: Exception) {}
    }

    fun clearForModule(moduleName: String) {
        val lower = moduleName.lowercase()
        val ids = byModule.remove(lower) ?: return
        val level = Minecraft.getInstance().level
        for (id in ids.toList()) {
            val cur = colors[id]
            if (cur != null && cur.module == lower) {
                colors.remove(id)
            }
            try { level?.getEntity(id)?.let { if (it.hasGlowingTag()) it.setGlowingTag(false) } } catch (_: Exception) {}
        }
        // Доп. зачистка всех застрявших флагов в мире
        try {
            val lvl = level ?: return
            for (entity in lvl.entitiesForRendering()) {
                if (entity.hasGlowingTag() && getGlowColor(entity.id) == -1 &&
                    set.starlev.features.combat.EntityHighlight.getGlowColor(entity.id) == -1 &&
                    set.starlev.features.combat.ClickHighlight.getGlowColor(entity.id) == -1 &&
                    set.starlev.features.combat.dungeons.Floor4.getGlowColor(entity.id) == -1
                ) entity.setGlowingTag(false)
            }
        } catch (_: Exception) {}
    }

    fun clearAll() {
        val level = Minecraft.getInstance().level
        val ids = colors.keys.toList()
        // Миграция: снять застрявший флаг у всех id из реестра
        for (id in ids) {
            try { level?.getEntity(id)?.let { if (it.hasGlowingTag()) it.setGlowingTag(false) } } catch (_: Exception) {}
        }
        // И у всех энтити в мире на случай рассинхрона id
        try {
            val lvl = level
            if (lvl != null) {
                for (entity in lvl.entitiesForRendering()) {
                    if (entity.hasGlowingTag() && getGlowColor(entity.id) == -1 &&
                        set.starlev.features.combat.EntityHighlight.getGlowColor(entity.id) == -1 &&
                        set.starlev.features.combat.ClickHighlight.getGlowColor(entity.id) == -1 &&
                        set.starlev.features.combat.dungeons.Floor4.getGlowColor(entity.id) == -1
                    ) entity.setGlowingTag(false)
                }
            }
        } catch (_: Exception) {}
        colors.clear()
        byModule.clear()
    }

    fun onWorldChange() = clearAll()

    /** Вызывается из EntityMixin — цвет или -1 */
    @JvmStatic
    fun getGlowColor(entityId: Int): Int = colors[entityId]?.color ?: -1

    fun hasGlow(entityId: Int): Boolean = colors.containsKey(entityId)

    /** Подчистка протухших id (сущность уже деспавнилась) — можно дернуть раз в тик если много. */
    fun cleanupStale(level: net.minecraft.client.multiplayer.ClientLevel) {
        if (colors.isEmpty()) return
        val it = colors.entries.iterator()
        while (it.hasNext()) {
            val (id, entry) = it.next()
            if (level.getEntity(id) == null) {
                it.remove()
                byModule[entry.module]?.remove(id)
            }
        }
    }
}
