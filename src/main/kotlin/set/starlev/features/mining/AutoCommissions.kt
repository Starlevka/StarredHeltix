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
    private var isWaitingForClick = false
    private var pigeonSlot = -1
    private var lastTriggerTime = 0L
    private var lastClickTime = 0L
    private val timer = Timer()

    fun init() {
        ChatEventsManager.registerIncoming { message ->
            if (StarredHeltix.feature.mining.commissions.autoCommissions) {
                val cleanMessage = message.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "")
                // "Commission Complete!" or "Поручение выполнено!"
                if (cleanMessage.contains("Commission Complete!") || (cleanMessage.contains("Поручение выполнено!") && cleanMessage.contains("Короля"))) {
                    findPigeonAndTrigger()
                }
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (isWaitingForClick && mc.player != null && mc.screen == null) {
                val currentTime = System.currentTimeMillis()
                // 10 second window to use the pigeon
                if (currentTime - lastTriggerTime > 10000) {
                    isWaitingForClick = false
                    return@register
                }

                if (mc.options.keyUse.isDown) {
                    // 5 second cooldown between activations as requested
                    if (currentTime - lastClickTime < 5000) return@register
                    lastClickTime = currentTime

                    mc.execute {
                        if (pigeonSlot != -1) {
                            val originalSlot = mc.player?.inventory?.selected ?: 0
                            mc.player?.inventory?.selected = pigeonSlot
                            mc.gameMode?.useItem(mc.player!!, InteractionHand.MAIN_HAND)
                            
                            // Optional: switch back to original slot after a short delay
                            // For now, we keep it simple like AutoSlayer
                            
                            isWaitingForClick = false
                            // Clear titles
                            mc.gui.setTitle(Component.empty())
                            mc.gui.setSubtitle(Component.empty())
                        }
                    }
                }
            }
        }
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
