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
        if (!raw()) return
        cachedGraphics?.drawString(mc.font, text, x, y, 0xFFFFAA00.toInt())
    }

    override fun getWidth(): Int = mc.font.width(text)
    override fun getHeight(): Int = mc.font.lineHeight

    override fun getDefaultX(): Int {
        val screenWidth = mc.window.guiScaledWidth
        return screenWidth / 2 - 60
    }

    override fun getDefaultY(): Int {
        val screenHeight = mc.window.guiScaledHeight
        return screenHeight / 2 + 15
    }
}
