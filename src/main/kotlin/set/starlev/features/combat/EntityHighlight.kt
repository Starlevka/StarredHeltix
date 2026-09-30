package set.starlev.features.combat

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.entity.monster.spider.Spider
import net.minecraft.world.entity.monster.spider.CaveSpider
import set.starlev.utils.detectors.MobHeadDisplayDetector
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import set.starlev.render.RenderContext
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.DungeonDetector
import set.starlev.utils.detectors.EntityNameCache

/**
 * Система подсветки мобов в мире.
 * Подсвечивает боссов слееров и «звёздных» мобов в данжах.
 * Подсветки по типам (эндермены/криперы/волки/пауки/пещерные пауки/зомби) убраны —
 * вместо них ClickHighlight (клик колёсиком мыши по мобу).
 */
object EntityHighlight {

    private val mc = Minecraft.getInstance()
    private val glowColorsByEntityId = HashMap<Int, Int>()
    private val managedGlowIds = HashSet<Int>()
    private val seenGlowIds = HashSet<Int>()
    private val starredDungeonCache = HashMap<Int, Pair<Long, Boolean>>()
    private var starredDungeonCacheLevelRef: Any? = null

    private var lastStaleSweepMs = 0L

    private fun sweepStaleVanillaGlow(level: net.minecraft.client.multiplayer.ClientLevel) {
        val now = System.currentTimeMillis()
        if (now - lastStaleSweepMs < 2000L) return
        lastStaleSweepMs = now
        try {
            for (entity in level.entitiesForRendering()) {
                if (!entity.hasGlowingTag()) continue
                val id = entity.id
                if (getGlowColor(id) != -1) continue
                if (set.starlev.features.combat.ClickHighlight.getGlowColor(id) != -1) continue
                if (set.starlev.scripting.ModuleGlowRegistry.getGlowColor(id) != -1) continue
                if (set.starlev.features.combat.dungeons.Floor4.getGlowColor(id) != -1) continue
                entity.setGlowingTag(false)
            }
        } catch (_: Exception) {}
    }

    /**
     * Инициализация системы подсветки мобов
     */
    fun init() {
        RenderEvents.register { context ->
            val level = mc.level ?: return@register
            val dungeonConfig = StarredHeltix.feature.dungeons.visuals
            val slayerCfg = StarredHeltix.feature.slayer.slayerBosses
            val slayerBossesEnabled = slayerCfg.enabled
            val starCfg = dungeonConfig.starredMobs
            val dungeonStarGlowEnabled = starCfg.enabled && DungeonDetector.isInDungeon()
            val anyGlowEnabled =
                (slayerBossesEnabled && slayerCfg.glow) ||
                    (dungeonStarGlowEnabled && starCfg.glow)

            if (!slayerBossesEnabled && !dungeonStarGlowEnabled) {
                clearAllGlow(level)
                if (starredDungeonCache.isNotEmpty()) {
                    starredDungeonCache.clear()
                    starredDungeonCacheLevelRef = null
                }
                sweepStaleVanillaGlow(level)
                return@register
            }

            val slayerBossesColor = if (slayerBossesEnabled) ColorUtils.parseColor(slayerCfg.colorV2) else 0
            val slayerBossesAlpha = slayerCfg.transparency
            val slayerBossesOutline = slayerCfg.outline
            val slayerBossesBox = slayerCfg.box
            val slayerBossesGlow = slayerCfg.glow

            val starredMobGlowColor = if (dungeonStarGlowEnabled) ColorUtils.parseColor(starCfg.colorV2) else 0

            if (anyGlowEnabled) {
                seenGlowIds.clear()
            } else if (managedGlowIds.isNotEmpty()) {
                clearAllGlow(level)
            }

            if (!dungeonStarGlowEnabled) {
                if (starredDungeonCache.isNotEmpty()) starredDungeonCache.clear()
                starredDungeonCacheLevelRef = null
            } else if (starredDungeonCacheLevelRef !== level) {
                starredDungeonCache.clear()
                starredDungeonCacheLevelRef = level
            }

            val nowMs = if (dungeonStarGlowEnabled) System.currentTimeMillis() else 0L

            for (entity in level.entitiesForRendering()) {
                if (slayerBossesEnabled) {
                    when (entity) {
                        is Zombie, is Spider, is CaveSpider, is Wolf -> {
                            if (isSlayerBoss(entity)) {
                                if (slayerBossesBox) {
                                    renderEntityBox(context, entity, slayerBossesColor, slayerBossesAlpha, slayerBossesOutline)
                                }
                                if (slayerBossesGlow) {
                                    applyGlow(entity, slayerBossesColor)
                                }
                                continue
                            }
                        }
                    }
                }

                if (dungeonStarGlowEnabled) {
                    val living = entity as? net.minecraft.world.entity.LivingEntity
                    if (living != null && isStarredDungeonMob(living, nowMs)) {
                        if (starCfg.box) {
                            renderEntityBox(context, living, starredMobGlowColor, starCfg.transparency, starCfg.outline)
                        }
                        if (starCfg.glow) {
                            applyGlow(living, starredMobGlowColor)
                        }
                        continue // Не применять другие подсветки к звёздному мобу
                    }
                }
            }

            if (anyGlowEnabled) {
                cleanupGlow(level)
            }
            // Периодическая зачистка застрявшего ванильного флага (миграция) даже когда glow включён
            sweepStaleVanillaGlow(level)
        }
    }

    private fun isSlayerBoss(entity: Entity): Boolean {
        val living = entity as? net.minecraft.world.entity.LivingEntity ?: return false
        // Оптимизация: имя из общего кэша (троттлинг 250мс), без повторного AABB-запроса
        val name = EntityNameCache.getName(living)
        if (name.isEmpty()) return false
        return matchesSlayerBossName(name)
    }

    private fun matchesSlayerBossName(cleanLower: String): Boolean {
        val s = cleanLower.lowercase()
        return s.contains("revenant horror") ||
            s.contains("мститель") ||
            s.contains("tarantula broodfather") ||
            s.contains("тарантул") ||
            s.contains("sven packmaster") ||
            s.contains("свен")
    }

    private fun cleanText(text: String): String {
        return text.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
    }

    private fun renderEntityBox(context: RenderContext, entity: Entity, color: Int, globalAlpha: Float, outline: Boolean = false) {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val baseAlpha = ((color shr 24) and 0xFF) / 255f
        
        // Применяем глобальную прозрачность
        val a = baseAlpha * globalAlpha
        
        context.renderBox(entity.boundingBox, r, g, b, a, !outline)
    }

    private fun isStarredDungeonMob(entity: net.minecraft.world.entity.LivingEntity, nowMs: Long): Boolean {
        val id = entity.id
        val cached = starredDungeonCache[id]
        if (cached != null && nowMs - cached.first <= 250L) {
            return cached.second
        }

        // Оптимизация: имя уже закэшировано в EntityNameCache — ищем ✯ в нём
        val name = EntityNameCache.getName(entity)
        var starred = name.contains("✯")
        if (!starred) {
            val displays = MobHeadDisplayDetector.getHeadDisplays(entity)
            for (comp in displays.textDisplays) {
                if (comp.string.contains("✯")) {
                    starred = true
                    break
                }
            }
        }
        if (starredDungeonCache.size > 512) starredDungeonCache.clear()
        starredDungeonCache[id] = nowMs to starred
        return starred
    }

    private fun applyGlow(entity: Entity, color: Int) {
        val id = entity.id
        managedGlowIds.add(id)
        seenGlowIds.add(id)
        glowColorsByEntityId[id] = color
        // Не трогаем entity.setGlowingTag — подсветка идёт только через EntityMixin
        // (иначе флаг остаётся после выключения и даёт ванильный seeThrough glow сквозь стены)
    }

    private fun cleanupGlow(level: net.minecraft.client.multiplayer.ClientLevel) {
        if (managedGlowIds.isEmpty()) return
        val iter = managedGlowIds.iterator()
        while (iter.hasNext()) {
            val id = iter.next()
            if (seenGlowIds.contains(id)) continue
            // Снимаем застрявший ванильный флаг от старых версий (миксин уже не требует hasGlowingTag)
            try { level.getEntity(id)?.let { if (it.hasGlowingTag()) it.setGlowingTag(false) } } catch (_: Exception) {}
            glowColorsByEntityId.remove(id)
            iter.remove()
        }
    }

    private fun clearAllGlow(level: net.minecraft.client.multiplayer.ClientLevel) {
        for (id in managedGlowIds) {
            try { level.getEntity(id)?.let { if (it.hasGlowingTag()) it.setGlowingTag(false) } } catch (_: Exception) {}
        }
        managedGlowIds.clear()
        glowColorsByEntityId.clear()
        seenGlowIds.clear()
        // Сразу подчищаем весь мир от застрявших флагов
        sweepStaleVanillaGlow(level)
    }

    fun onWorldChange() {
        managedGlowIds.clear()
        glowColorsByEntityId.clear()
        seenGlowIds.clear()
        starredDungeonCache.clear()
        starredDungeonCacheLevelRef = null
    }

    @JvmStatic
    fun getGlowColor(entityId: Int): Int {
        return glowColorsByEntityId[entityId] ?: -1
    }

}
