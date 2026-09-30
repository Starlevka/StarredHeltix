package set.starlev.features.combat

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.level.ClipContext
import org.lwjgl.glfw.GLFW
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.EntityNameCache

/**
 * Подсветка мобов по клику — замена подсветкам по типам (эндермены/пауки/волки и т.д.).
 * Хоткей (дефолт — клик колёсиком, переназначается в «Настройки управления» → «Прочее»):
 * мод определяет ник сущности под прицелом (EntityNameCache: customName/TextDisplay)
 * и подсвечивает ВСЕХ мобов с таким же ником — бокс/обводка/свечение.
 * Повторное нажатие по «своему» мобу — снять, по другому — переключить на его тип.
 */
object ClickHighlight {

    /** Дальность entity-pick'а, блоков. */
    private const val PICK_RANGE = 48.0

    private val mc = Minecraft.getInstance()

    /** Хоткей выбора/снятия подсветки. Дефолт — колёсико (button 2): у некоторых
     *  мышиный софт/драйвер не отдаёт клик колеса в игру на уровне ОС
     *  (проверено glfwGetMouseButton = 0), поэтому бинд переназначаемый. */
    @JvmField
    val toggleKey: KeyMapping = KeyMappingHelper.registerKeyMapping(
        KeyMapping(
            "key.starredheltix.clickhighlight",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            set.starlev.events.ModKeybinds.CATEGORY
        )
    )

    /** Нормализованный ник выбранного моба; подсвечиваются все энтити с таким ником. */
    @Volatile
    private var selectedName: String? = null

    /** Режим «только кликнутый моб»: id конкретной сущности вместо ника. */
    @Volatile
    private var selectedId: Int = -1

    private var lastSelectedSeenMs = 0L

    // Реестр свечения — как в EntityHighlight: переприменение каждый кадр
    // переживает перезапись мета-данных с сервера.
    private val glowColors = HashMap<Int, Int>()
    private val managedGlowIds = HashSet<Int>()
    private val seenGlowIds = HashSet<Int>()

    private val colorCodes = Regex("§[0-9a-fk-or]")
    private val junkTokens = Regex("[0-9]+(?:[#/%,+.][0-9]*)*|[#/%,+.]+|❤|✯")
    private val whitespace = Regex("\\s+")

    fun init() {
        RenderEvents.register { context ->
            while (toggleKey.consumeClick()) onMiddleClick()
            val level = mc.level ?: return@register
            val config = StarredHeltix.feature.combat.clickHighlight
            val nowMs = System.currentTimeMillis()
            val hasTarget = if (config.singleTargetOnly) {
                if (selectedId == -1) false else {
                    // Сущность пропала надолго (деспаун/выгрузка) — снимаем выделение
                    val present = try { level.getEntity(selectedId) != null } catch (_: Exception) { false }
                    if (present) lastSelectedSeenMs = nowMs
                    if (nowMs - lastSelectedSeenMs > 5_000) {
                        selectedId = -1
                        false
                    } else present
                }
            } else {
                selectedName != null
            }
            if (!config.enabled || !hasTarget) {
                if (selectedName != null) selectedName = null
                if (selectedId != -1) selectedId = -1
                if (managedGlowIds.isNotEmpty()) clearAllGlow(level)
                return@register
            }
            val color = ColorUtils.parseColor(config.colorV2)
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            val a = ((color shr 24) and 0xFF) / 255f * config.transparency
            val target = selectedName
            seenGlowIds.clear()
            for (entity in level.entitiesForRendering()) {
                val match = if (config.singleTargetOnly) {
                    entity.id == selectedId
                } else {
                    if (target == null) continue
                    val raw = detectName(entity)
                    raw.isNotEmpty() && normalize(raw) == target
                }
                if (!match) continue
                if (config.glow) {
                    applyGlow(entity, color)
                }
                if (config.box) {
                    context.renderBox(entity.boundingBox, r, g, b, a, !config.outline)
                }
            }
            if (config.glow) {
                cleanupGlow(level)
            } else {
                clearAllGlow(level)
            }
        }
    }

    /**
     * Обработка хоткея (дефолт — клик колёсиком).
     * @return true, если нажатие привело к выбору/снятию подсветки
     */
    fun onMiddleClick(): Boolean {
        val player = mc.player ?: return false
        mc.level ?: return false
        if (mc.gui.screen() != null) return false
        if (!StarredHeltix.feature.combat.clickHighlight.enabled) return false
        val entity = raycastEntity(player) ?: return false
        if (StarredHeltix.feature.combat.clickHighlight.singleTargetOnly) {
            return if (entity.id == selectedId) {
                selectedId = -1
                player.sendSystemMessage(Component.literal("§e[StarredHeltix] §7Подсветка убрана"))
                true
            } else {
                selectedId = entity.id
                lastSelectedSeenMs = System.currentTimeMillis()
                val raw = detectName(entity)
                player.sendSystemMessage(Component.literal("§e[StarredHeltix] §7Подсвечен только этот моб: §f${raw.ifBlank { entity.name.string }}"))
                true
            }
        }
        val raw = detectName(entity)
        if (raw.isBlank()) {
            player.sendSystemMessage(Component.literal("§e[StarredHeltix] §cНе удалось определить ник этой сущности"))
            return true
        }
        val norm = normalize(raw).ifBlank { raw }
        return if (norm == selectedName) {
            selectedName = null
            player.sendSystemMessage(Component.literal("§e[StarredHeltix] §7Подсветка убрана: §f$raw"))
            true
        } else {
            selectedName = norm
            player.sendSystemMessage(Component.literal("§e[StarredHeltix] §7Подсвечиваем всех с ником: §f$raw"))
            true
        }
    }

    /**
     * Кастомный entity-pick. Ванильный mc.hitResult отсекает сущности дальше
     * entityInteractionRange (~3 блока), поэтому выделять моба с дистанции было бы
     * невозможно. Луч считаем от глаз, ограничиваем блоками (сквозь стены не выделяем).
     */
    private fun raycastEntity(player: LocalPlayer): Entity? {
        val level = mc.level ?: return null
        val from = player.getEyePosition(0f)
        val dir = player.getViewVector(0f)
        val to = from.add(dir.x * PICK_RANGE, dir.y * PICK_RANGE, dir.z * PICK_RANGE)
        val blockHit = level.clip(ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player))
        val end = blockHit.location
        val searchBox = player.boundingBox
            .expandTowards(dir.x * PICK_RANGE, dir.y * PICK_RANGE, dir.z * PICK_RANGE)
            .inflate(1.0, 1.0, 1.0)
        return ProjectileUtil.getEntityHitResult(
            player, from, end, searchBox, EntitySelector.CAN_BE_PICKED, from.distanceToSqr(end)
        )?.entity
    }

    /** Ник сущности: customName/TextDisplay над головой (EntityNameCache), fallback — ванильное имя. */
    private fun detectName(entity: Entity): String {
        val living = entity as? LivingEntity
        val detected = living?.let { EntityNameCache.getName(it) }?.takeIf { it.isNotBlank() }
        return detected ?: entity.name.string
    }

    /** «Revenant Horror II §c340/500❤ #123» -> «Revenant Horror II»: чтобы один и тот же
     *  тип моба с разным HP/индексом считался совпадением. */
    private fun normalize(raw: String): String {
        val s = colorCodes.replace(raw, "")
        val noJunk = junkTokens.replace(s, " ")
        return whitespace.replace(noJunk, " ").trim()
    }

    private fun applyGlow(entity: Entity, color: Int) {
        val id = entity.id
        managedGlowIds.add(id)
        seenGlowIds.add(id)
        glowColors[id] = color
        // Только карта цветов — EntityMixin форсит isCurrentlyGlowing, без hasGlowingTag
    }

    private fun cleanupGlow(level: ClientLevel) {
        if (managedGlowIds.isEmpty()) return
        val iter = managedGlowIds.iterator()
        while (iter.hasNext()) {
            val id = iter.next()
            if (seenGlowIds.contains(id)) continue
            releaseGlow(level, id)
            glowColors.remove(id)
            iter.remove()
        }
    }

    private fun clearAllGlow(level: ClientLevel) {
        for (id in managedGlowIds) {
            releaseGlow(level, id)
        }
        // Доп. зачистка застрявшего ванильного флага у всех энтити (миграция со старых версий)
        try {
            for (entity in level.entitiesForRendering()) {
                if (entity.hasGlowingTag() && getGlowColor(entity.id) == -1 &&
                    EntityHighlight.getGlowColor(entity.id) == -1 &&
                    set.starlev.scripting.ModuleGlowRegistry.getGlowColor(entity.id) == -1 &&
                    set.starlev.features.combat.dungeons.Floor4.getGlowColor(entity.id) == -1
                ) {
                    entity.setGlowingTag(false)
                }
            }
        } catch (_: Exception) {}
        managedGlowIds.clear()
        glowColors.clear()
        seenGlowIds.clear()
    }

    /** Снимаем застрявший ванильный флаг (для миграции). Миксин уже не требует hasGlowingTag. */
    private fun releaseGlow(level: ClientLevel, id: Int) {
        try { level.getEntity(id)?.let { if (it.hasGlowingTag()) it.setGlowingTag(false) } } catch (_: Exception) {}
    }

    fun onWorldChange() {
        selectedName = null
        selectedId = -1
        lastSelectedSeenMs = 0L
        managedGlowIds.clear()
        glowColors.clear()
        seenGlowIds.clear()
    }

    /** Вызывается из EntityMixin — цвет подсветки выделенных кликом сущностей. */
    @JvmStatic
    fun getGlowColor(entityId: Int): Int = glowColors[entityId] ?: -1

}
