package set.starlev.features.misc

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.hud.HudElement

object MouseLock : HudElement("MouseLock") {
    private var isLocked = false
    private val mc = Minecraft.getInstance()
    private val text = "Движение мыши заблокировано"

    fun init() {
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            off()
        }
    }

    fun toggle(): Boolean {
        isLocked = !isLocked
        if (isLocked) {
            mc.player?.displayClientMessage(Component.literal("§a✓ Движение мыши заблокировано"), true)
        } else {
            mc.player?.displayClientMessage(Component.literal("§c✗ Движение мыши разблокировано"), true)
        }
        return isLocked
    }

    fun on(): Boolean {
        if (!isLocked) {
            isLocked = true
            mc.player?.displayClientMessage(Component.literal("§a✓ Движение мыши заблокировано"), true)
        }
        return true
    }

    fun off(): Boolean {
        if (isLocked) {
            isLocked = false
            mc.player?.displayClientMessage(Component.literal("§c✗ Движение мыши разблокировано"), true)
        }
        return true
    }

    fun active(): Boolean {
        if (!isLocked) return false
        return mc.screen == null
    }

    fun raw(): Boolean = isLocked

    // HudElement implementation
    override fun render() {
        if (!raw() && !isEditing) return
        this.showBackground = set.starlev.StarredHeltix.feature.misc.mouselock.showBackground
        drawBackground(getWidth(), getHeight())
        
        // Используем forceEffects для применения эффектов текста точечно
        set.starlev.secret.features.SecretFunFeatures.withForceEffects {
            cachedGraphics?.drawString(mc.font, text, x, y, 0xFFFFAA00.toInt())
        }
    }

    override fun getWidth(): Int = mc.font.width(text)
    override fun getHeight(): Int = mc.font.lineHeight

    override fun getDefaultScale(): Float = 1.2f

    override fun getDefaultX(): Int = 420

    override fun getDefaultY(): Int = 285
}
