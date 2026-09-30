package set.starlev.features.combat

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import set.starlev.StarredHeltix
import set.starlev.render.HudElement
import set.starlev.render.RenderEvents
import set.starlev.render.drawString
import set.starlev.utils.ColorUtils

/**
 * Радиус костеранга (порт BonemerangOverlay из Firmament):
 * пока в руке предмет с «Костеранг» в названии — считает мобов в створе броска
 * (луч из глаз по взгляду на дальность из конфига) и показывает:
 * - HUD «Костеранг: N» (дотянется или нет — по счётчику);
 * - обводку достижимых мобов + опционально другим цветом тех, кто рядом, но мимо прицела.
 */
object BonemerangRange : HudElement("BonemerangHud") {
    private val mc = Minecraft.getInstance()
    private val stripRegex = Regex("(?i)§[0-9a-fk-orlnmxz]")

    @Volatile
    private var targets: List<LivingEntity> = emptyList()

    @Volatile
    private var offAim: List<LivingEntity> = emptyList()

    fun init() {
        RenderEvents.register { context ->
            val cfg = StarredHeltix.feature.combat.bonemerang
            if (!cfg.enabled || (!cfg.highlightTargets && !cfg.showOffAim)) {
                if (targets.isNotEmpty()) targets = emptyList()
                if (offAim.isNotEmpty()) offAim = emptyList()
                return@register
            }
            val player = mc.player ?: return@register
            val level = mc.level ?: return@register
            if (!isHolding(player.mainHandItem) && !isHolding(player.offhandItem)) {
                if (targets.isNotEmpty()) targets = emptyList()
                if (offAim.isNotEmpty()) offAim = emptyList()
                return@register
            }
            val range = cfg.range.toDouble().coerceIn(1.0, 49.0)
            val from = player.getEyePosition()
            val dir = player.getLookAngle()
            val to = from.add(dir.scale(range))
            val rayBox = AABB(from, to).inflate(1.0)

            val hit = ArrayList<LivingEntity>()
            val near = ArrayList<LivingEntity>()
            for (entity in level.entitiesForRendering()) {
                val living = entity as? LivingEntity ?: continue
                if (living == player || living is Player) continue
                if (living is ArmorStand || living.isInvisible) continue
                if (living.isDeadOrDying) continue
                if (!living.boundingBox.intersects(rayBox)) {
                    // Кандидат «рядом, но мимо»: в сфере дальности от глаз
                    if (cfg.showOffAim && living.boundingBox.center.distanceTo(from) <= range) {
                        near.add(living)
                    }
                    continue
                }
                val clipped = try {
                    living.boundingBox.inflate(0.35).clip(from, to).orElse(null)
                } catch (_: Exception) {
                    null
                }
                if (clipped != null) {
                    hit.add(living)
                } else if (cfg.showOffAim && living.boundingBox.center.distanceTo(from) <= range) {
                    near.add(living)
                }
            }
            targets = hit
            offAim = if (cfg.showOffAim) near else emptyList()

            if (cfg.highlightTargets && hit.isNotEmpty()) {
                val color = ColorUtils.parseColor(cfg.targetColorV2, 0xFF00FF00.toInt())
                val r = ((color shr 16) and 0xFF) / 255f
                val g = ((color shr 8) and 0xFF) / 255f
                val b = (color and 0xFF) / 255f
                val a = ((color shr 24) and 0xFF) / 255f
                for (mob in hit) {
                    context.renderBox(mob.boundingBox, r, g, b, a, fill = false)
                }
            }
            if (cfg.showOffAim && near.isNotEmpty()) {
                val color = ColorUtils.parseColor(cfg.offAimColorV2, 0xFFFFFF00.toInt())
                val r = ((color shr 16) and 0xFF) / 255f
                val g = ((color shr 8) and 0xFF) / 255f
                val b = (color and 0xFF) / 255f
                val a = ((color shr 24) and 0xFF) / 255f
                for (mob in near) {
                    context.renderBox(mob.boundingBox, r, g, b, a, fill = false)
                }
            }
        }
    }

    /** В руке костеранг: ищем «костеранг»/«bonemerang» в названии предмета. */
    private fun isHolding(stack: net.minecraft.world.item.ItemStack): Boolean {
        if (stack.isEmpty) return false
        return try {
            val name = stripRegex.replace(stack.hoverName.string, "").lowercase()
            name.contains("костеранг") || name.contains("bonemerang")
        } catch (_: Exception) {
            false
        }
    }

    /** Сколько мобов сейчас в створе (для HUD и сторонних вызовов). */
    fun targetCount(): Int = targets.size

    fun isActive(): Boolean {
        val player = mc.player ?: return false
        return isHolding(player.mainHandItem) || isHolding(player.offhandItem)
    }

    override fun render() {
        val cfg = StarredHeltix.feature.combat.bonemerang
        if (!cfg.enabled || !cfg.showHud) return
        if (!isEditing && !isActive()) return
        val graphics = cachedGraphics ?: return
        val n = if (isEditing) 3 else targets.size
        val text = if (n > 0) Component.literal("§7Костеранг: §a$n") else Component.literal("§7Костеранг: §c$n")
        this.showBackground = cfg.showBackground
        drawBackground(getWidth(), getHeight())
        graphics.drawString(mc.font, text, x, y, 0xFFFFFFFF.toInt(), true)
    }

    override fun getWidth(): Int {
        val n = if (isEditing) 3 else targets.size
        return mc.font.width("Костеранг: $n")
    }

    override fun getHeight(): Int = mc.font.lineHeight

    override fun getDefaultX(): Int = 2
    override fun getDefaultY(): Int = 100
}
