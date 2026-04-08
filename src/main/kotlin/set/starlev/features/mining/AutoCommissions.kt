package set.starlev.features.mining

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import set.starlev.StarredHeltix
import set.starlev.features.chat.ChatEventsManager
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents
import java.util.*
import kotlin.concurrent.schedule

object AutoCommissions {
    private val mc = Minecraft.getInstance()
    var isWaitingForClick = false
        private set
    private var pigeonSlot = -1
    private var lastTriggerTime = 0L
    private val timer = Timer()

    fun init() {
        ChatEventsManager.registerIncoming { message ->
            if (StarredHeltix.feature.mining.commissions.autoCommissions) {
                val cleanMessage = message.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "")
                if (cleanMessage.contains("Commission Complete!") || (cleanMessage.contains("Поручение выполнено!") && cleanMessage.contains("Короля"))) {
                    findPigeonAndTrigger()
                }
            }
        }
    }

    /**
    * Вызывается из MouseButtonMixin при ПКМ.
    * Переключает слот на голубя и выполняет ПКМ для использования.
    */
    fun onRightClickWhileWaiting() {
        if (!isWaitingForClick || mc.player == null) return
        if (pigeonSlot == -1) return

        // Переключаемся на слот с голубем
        mc.player?.inventory?.selected = pigeonSlot
        isWaitingForClick = false
        mc.gui.setTitle(Component.empty())
        mc.gui.setSubtitle(Component.empty())

        // Выполняем ПКМ для использования голубя
        mc.player?.let { mc.gameMode?.useItem(it, InteractionHand.MAIN_HAND) }
    }

    private fun findPigeonAndTrigger() {
        val player = mc.player ?: return
        var foundSlot = -1

        for (i in 0..8) {
            val stack = player.inventory.items[i]
            if (stack.isEmpty) continue
            
            val name = stack.hoverName.string
            if (name.contains("Royal Pigeon") || name.contains("Королевский голубь")) {
                foundSlot = i
                break
            }
        }

        if (foundSlot != -1) {
            pigeonSlot = foundSlot
            isWaitingForClick = true
            lastTriggerTime = System.currentTimeMillis()

            if (StarredHeltix.feature.mining.commissions.showTitle) {
                timer.schedule(500L) {
                    mc.execute {
                        mc.gui.setTimes(10, 60, 10)
                        mc.gui.setTitle(Component.literal("§b§lСдать поручение?"))
                        mc.gui.setSubtitle(Component.literal("§3Нажмите ПКМ"))
                        mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.PARROT_AMBIENT, 1.0f))
                    }
                }
            }
        }
    }
}
