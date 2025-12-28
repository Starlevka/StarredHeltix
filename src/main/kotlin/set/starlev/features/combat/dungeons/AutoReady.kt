package set.starlev.features.combat.dungeons

import net.minecraft.client.Minecraft
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import set.starlev.StarredHeltix

object AutoReadyNotifier {
    private val READY_ZONE = AABB(-72.0, 122.0, -2.0, -70.0, 135.0, 2.0)
    private var wasInZone = false
    private var lastMessageTime = 0L
    private const val MESSAGE_COOLDOWN = 500L

    private val config get() = StarredHeltix.feature.dungeons.autoReady

    fun tick() {
        if (!config.enabled) return

        val player = Minecraft.getInstance().player ?: return
        val playerPos = Vec3(player.x, player.y, player.z)
        val isInZone = READY_ZONE.contains(playerPos)

        if (isInZone && !wasInZone) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastMessageTime > MESSAGE_COOLDOWN) {
                player.connection?.sendCommand("pc ${config.readyMessage}")
                lastMessageTime = currentTime
            }
        }

        wasInZone = isInZone
    }
}
