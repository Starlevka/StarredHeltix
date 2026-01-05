package set.starlev.features.combat.slayer

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

object AutoSlayer {
    private val mc = Minecraft.getInstance()
    private var isWaitingForClick = false
    private var phoneSlot = -1
    private var lastTriggerTime = 0L
    private var lastClickTime = 0L
    private val timer = Timer()
    

    fun init() {
        ChatEventsManager.registerIncoming { message ->
            if (StarredHeltix.feature.slayer.general.autoSlayer) {
                val cleanMessage = message.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "")
                if (cleanMessage.contains("Поговорите с Маддоксом, чтобы получить опыт!")) {
                    findPhoneAndTrigger()
                }
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (isWaitingForClick && mc.player != null && mc.screen == null) {
                val currentTime = System.currentTimeMillis()
                // If 10 seconds passed, cancel waiting
                if (currentTime - lastTriggerTime > 10000) {
                    isWaitingForClick = false
                    return@register
                }

                if (mc.options.keyUse.isDown) {
                    if (currentTime - lastClickTime < 5000) return@register
                    lastClickTime = currentTime

                    mc.execute {
                        if (phoneSlot != -1) {
                            mc.player?.inventory?.selected = phoneSlot
                            mc.gameMode?.useItem(mc.player!!, InteractionHand.MAIN_HAND)
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

    private fun findPhoneAndTrigger() {
        val player = mc.player ?: return
        var foundSlot = -1

        for (i in 0..8) {
            val stack = player.inventory.items[i]
            if (stack.isEmpty) continue
            
            val name = stack.hoverName.string
            if (name.contains("Телефон Маддокса")) {
                foundSlot = i
                break
            }
        }

        if (foundSlot != -1) {
            phoneSlot = foundSlot
            isWaitingForClick = true
            lastTriggerTime = System.currentTimeMillis()

            if (StarredHeltix.feature.slayer.general.showTitle) {
                timer.schedule(500L) {
                    mc.execute {
                        mc.gui.setTimes(10, 60, 10)
                        mc.gui.setTitle(Component.literal("§c§lПозвонить Маддоксу?"))
                        mc.gui.setSubtitle(Component.literal("§4Нажмите ПКМ"))
                        mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.BELL_BLOCK, 1.0f))
                    }
                }
            }
        }
    }
}
