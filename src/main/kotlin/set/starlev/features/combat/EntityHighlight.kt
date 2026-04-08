package set.starlev.features.combat

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Spider
import net.minecraft.world.entity.monster.CaveSpider
import set.starlev.utils.detectors.MobHeadDisplayDetector
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import set.starlev.render.RenderContext
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.DungeonDetector

/**
 * Система подсветки мобов в мире.
 * Подсвечивает эндерменов, волков, заряженных криперов и летучих мышей в данжах.
 */
object EntityHighlight {

    private val mc = Minecraft.getInstance()
    private val glowColorsByEntityId = HashMap<Int, Int>()
    private val managedGlowIds = HashSet<Int>()
    private val seenGlowIds = HashSet<Int>()
    private val starredDungeonCache = HashMap<Int, Pair<Long, Boolean>>()
    private var starredDungeonCacheLevelRef: Any? = null

    /**
     * Инициализация системы подсветки мобов
     */
    fun init() {
        RenderEvents.register { context ->
            val level = mc.level ?: return@register
            val config = StarredHeltix.feature.combat.highlight
            val dungeonConfig = StarredHeltix.feature.dungeons.visuals
            val endermanEnabled = config.enderman.enabled
            val creeperEnabled = config.creeper.enabled
            val wolfEnabled = config.wolf.enabled
            val caveSpiderEnabled = config.caveSpider.enabled
            val spiderEnabled = config.spider.enabled
            val zombieEnabled = config.zombie.enabled
            val slayerBossesEnabled = config.slayerBosses.enabled
            val dungeonStarGlowEnabled = dungeonConfig.starredMobGlow && DungeonDetector.isInDungeon()
            val anyGlowEnabled =
                (endermanEnabled && config.enderman.glow) ||
                    (creeperEnabled && config.creeper.glow) ||
                    (wolfEnabled && config.wolf.glow) ||
                    (caveSpiderEnabled && config.caveSpider.glow) ||
                    (spiderEnabled && config.spider.glow) ||
                    (zombieEnabled && config.zombie.glow) ||
                    (slayerBossesEnabled && config.slayerBosses.glow) ||
                    dungeonStarGlowEnabled

            if (!endermanEnabled && !creeperEnabled && !wolfEnabled && !caveSpiderEnabled && !spiderEnabled && !zombieEnabled && !slayerBossesEnabled && !dungeonStarGlowEnabled) {
                return@register
            }

            val endermanColor = if (endermanEnabled) ColorUtils.parseColor(config.enderman.colorV2) else 0
            val endermanAlpha = config.enderman.transparency
            val endermanOutline = config.enderman.outline
            val endermanBox = config.enderman.box
            val endermanGlow = config.enderman.glow

            val creeperColor = if (creeperEnabled) ColorUtils.parseColor(config.creeper.colorV2) else 0
            val creeperAlpha = config.creeper.transparency
            val creeperOutline = config.creeper.outline
            val creeperBox = config.creeper.box
            val creeperGlow = config.creeper.glow

            val wolfColor = if (wolfEnabled) ColorUtils.parseColor(config.wolf.colorV2) else 0
            val wolfAlpha = config.wolf.transparency
            val wolfOutline = config.wolf.outline
            val wolfBox = config.wolf.box
            val wolfGlow = config.wolf.glow

            val caveSpiderColor = if (caveSpiderEnabled) ColorUtils.parseColor(config.caveSpider.colorV2) else 0
            val caveSpiderAlpha = config.caveSpider.transparency
            val caveSpiderOutline = config.caveSpider.outline
            val caveSpiderBox = config.caveSpider.box
            val caveSpiderGlow = config.caveSpider.glow

            val spiderColor = if (spiderEnabled) ColorUtils.parseColor(config.spider.colorV2) else 0
            val spiderAlpha = config.spider.transparency
            val spiderOutline = config.spider.outline
            val spiderBox = config.spider.box
            val spiderGlow = config.spider.glow

            val zombieColor = if (zombieEnabled) ColorUtils.parseColor(config.zombie.colorV2) else 0
            val zombieAlpha = config.zombie.transparency
            val zombieOutline = config.zombie.outline
            val zombieBox = config.zombie.box
            val zombieGlow = config.zombie.glow

            val slayerBossesColor = if (slayerBossesEnabled) ColorUtils.parseColor(config.slayerBosses.colorV2) else 0
            val slayerBossesAlpha = config.slayerBosses.transparency
            val slayerBossesOutline = config.slayerBosses.outline
            val slayerBossesBox = config.slayerBosses.box
            val slayerBossesGlow = config.slayerBosses.glow

            val starredMobGlowColor = if (dungeonStarGlowEnabled) ColorUtils.parseColor(dungeonConfig.starredMobGlowColorV2) else 0

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
                        applyGlow(living, starredMobGlowColor)
                        continue // Не применять другие подсветки к звёздному мобу
                    }
                }

                when (entity) {
                    is EnderMan -> {
                        if (endermanEnabled) {
                            if (endermanBox) {
                                renderEntityBox(context, entity, endermanColor, endermanAlpha, endermanOutline)
                            }
                            if (endermanGlow) {
                                applyGlow(entity, endermanColor)
                            }
                        }
                    }
                    is Creeper -> {
                        if (creeperEnabled && entity.isPowered) {
                            if (creeperBox) {
                                renderEntityBox(context, entity, creeperColor, creeperAlpha, creeperOutline)
                            }
                            if (creeperGlow) {
                                applyGlow(entity, creeperColor)
                            }
                        }
                    }
                    is Wolf -> {
                        if (wolfEnabled) {
                            if (wolfBox) {
                                renderEntityBox(context, entity, wolfColor, wolfAlpha, wolfOutline)
                            }
                            if (wolfGlow) {
                                applyGlow(entity, wolfColor)
                            }
                        }
                    }
                    is CaveSpider -> {
                        if (caveSpiderEnabled) {
                            if (caveSpiderBox) {
                                renderEntityBox(context, entity, caveSpiderColor, caveSpiderAlpha, caveSpiderOutline)
                            }
                            if (caveSpiderGlow) {
                                applyGlow(entity, caveSpiderColor)
                            }
                        }
                    }
                    is Spider -> {
                        if (spiderEnabled) {
                            if (spiderBox) {
                                renderEntityBox(context, entity, spiderColor, spiderAlpha, spiderOutline)
                            }
                            if (spiderGlow) {
                                applyGlow(entity, spiderColor)
                            }
                        }
                    }
                    is Zombie -> {
                        if (zombieEnabled) {
                            if (zombieBox) {
                                renderEntityBox(context, entity, zombieColor, zombieAlpha, zombieOutline)
                            }
                            if (zombieGlow) {
                                applyGlow(entity, zombieColor)
                            }
                        }
                    }
                }
            }

            if (anyGlowEnabled) {
                cleanupGlow(level)
            }
        }
    }

    private fun isSlayerBoss(entity: Entity): Boolean {
        val living = entity as? net.minecraft.world.entity.LivingEntity ?: return false
        val custom = living.customName?.string?.let { cleanText(it) } ?: ""
        if (custom.isNotEmpty() && matchesSlayerBossName(custom)) return true

        val displays = MobHeadDisplayDetector.getHeadDisplays(living)
        for (comp in displays.textDisplays) {
            val text = cleanText(comp.string)
            if (text.isNotEmpty() && matchesSlayerBossName(text)) return true
        }
        return false
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
        if (cached != null && nowMs - cached.first <= 50L) {
            return cached.second
        }

        val displays = MobHeadDisplayDetector.getHeadDisplays(entity)
        var starred = false
        for (comp in displays.textDisplays) {
            if (comp.string.contains("✯")) {
                starred = true
                break
            }
        }
        starredDungeonCache[id] = nowMs to starred
        return starred
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

    private fun clearAllGlow(level: net.minecraft.client.multiplayer.ClientLevel) {
        if (managedGlowIds.isEmpty()) return
        for (id in managedGlowIds) {
            val entity = level.getEntity(id)
            if (entity != null) {
                entity.setGlowingTag(false)
            }
        }
        managedGlowIds.clear()
        glowColorsByEntityId.clear()
        seenGlowIds.clear()
    }

    @JvmStatic
    fun getGlowColor(entityId: Int): Int {
        return glowColorsByEntityId[entityId] ?: -1
    }

}
