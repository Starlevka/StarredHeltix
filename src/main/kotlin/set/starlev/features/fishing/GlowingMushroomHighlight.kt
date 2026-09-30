package set.starlev.features.fishing

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.LocationDetector
import java.util.concurrent.ConcurrentHashMap

object GlowingMushroomHighlight {
    private val glowingPositions = ConcurrentHashMap<BlockPos, Long>()
    private val colorCache = mutableMapOf<String, Int>()

    fun init() {
        // Удаление при ломании блока
        try {
            net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register { _, _, _, pos, _ ->
                onBlockBreak(pos)
                net.minecraft.world.InteractionResult.PASS
            }
        } catch (_: Exception) {}
        RenderEvents.register { context ->
            val cfg = try { StarredHeltix.feature.fishing.glowingMushroom } catch (_: Exception) { return@register }
            if (!cfg.enabled) return@register
            if (!isInGlowingCave()) return@register
            if (glowingPositions.isEmpty()) return@register

            val colorStr = cfg.colorV2
            val baseColor = ColorUtils.parseColor(colorStr, 0x8000FF00.toInt())
            val r = ((baseColor shr 16) and 0xFF) / 255f
            val g = ((baseColor shr 8) and 0xFF) / 255f
            val b = (baseColor and 0xFF) / 255f
            val aFill = ((baseColor shr 24) and 0xFF) / 255f * 0.5f
            val aLine = ((baseColor shr 24) and 0xFF) / 255f
            val now = System.currentTimeMillis()
            val it = glowingPositions.entries.iterator()
            while (it.hasNext()) {
                val entry = it.next()
                val pos = entry.key
                val lastSeen = entry.value
                // Устаревшие (5 сек как в Skyblocker)
                if (now - lastSeen > 5000) {
                    it.remove()
                    continue
                }
                val level = net.minecraft.client.Minecraft.getInstance().level ?: continue
                val block = try { level.getBlockState(pos).block } catch (_: Exception) { null }
                if (block != Blocks.RED_MUSHROOM && block != Blocks.BROWN_MUSHROOM) {
                    it.remove()
                    continue
                }
                val box = AABB(pos)
                // Лёгкая проверка видимости для оптимизации
                if (!context.isBoxVisible(box)) continue
                context.renderBox(box, r, g, b, aFill, true)
                context.renderBox(box, r, g, b, aLine, false, 3.0f)
            }
        }
    }

    fun onParticle(packet: ClientboundLevelParticlesPacket) {
        val cfg = try { StarredHeltix.feature.fishing.glowingMushroom } catch (_: Exception) { return }
        if (!cfg.enabled) return
        if (!isInGlowingCave()) return
        val level = net.minecraft.client.Minecraft.getInstance().level ?: return
        try {
            val particle = packet.particle
            if (particle.type != ParticleTypes.ENTITY_EFFECT) return
            val pos = BlockPos.containing(packet.x, packet.y, packet.z)
            val block = level.getBlockState(pos).block
            if (block != Blocks.RED_MUSHROOM && block != Blocks.BROWN_MUSHROOM) return
            glowingPositions[pos] = System.currentTimeMillis()
        } catch (_: Exception) {}
    }

    fun onBlockBreak(pos: BlockPos) {
        glowingPositions.remove(pos)
    }

    fun onWorldChange() {
        glowingPositions.clear()
    }

    private fun isInGlowingCave(): Boolean {
        return LocationDetector.isInAnyLocation("Glowing Mushroom Cave", "Пещера светящихся грибов", "Грибная пещера", "Mushroom Cave")
                || try { net.minecraft.client.Minecraft.getInstance().level?.dimension()?.identifier()?.toString()?.contains("mushroom", ignoreCase = true) == true } catch (_: Exception) { false }
    }
}
