package set.starlev.features.chat.mod

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import kotlin.random.Random

object MacroCheck {
    private val mc = Minecraft.getInstance()
    private var isActive = false
    private var correctAnswer = 0
    private var lastTitleTime = 0L

    fun init() {
        set.starlev.render.RenderEvents.register { _ ->
            if (isActive) {
                val now = System.currentTimeMillis()
                if (now - lastTitleTime > 1000) {
                    showTitle("§c§lМакро-чек!", "§eРешите пример: §b${getExpression()} §e= ?")
                    lastTitleTime = now
                }
            }
        }
    }

    private var currentA = 0
    private var currentB = 0

    private fun getExpression(): String = "$currentA + $currentB"

    fun activate() {
        if (isActive) return
        isActive = true
        currentA = Random.nextInt(1, 99)
        currentB = Random.nextInt(1, 99)
        correctAnswer = currentA + currentB
        
        mc.player?.displayClientMessage(
            Component.literal("§c§l[MACRO] §fАдминистратор начал проверку на макросы!\n§fВведите в чат результат выражения: §b$currentA + $currentB"),
            false
        )
        
        showTitle("§c§lМакро-чек!", "§eРешите пример: §b$currentA + $currentB §e= ?")
        lastTitleTime = System.currentTimeMillis()
    }

    fun deactivate() {
        isActive = false
        mc.gui.setTitle(Component.empty())
        mc.gui.setSubtitle(Component.empty())
        mc.player?.displayClientMessage(
            Component.literal("§a§l[MACRO] §fПроверка на макросы успешно пройдена."),
            false
        )
    }

    fun checkAnswer(message: String): Boolean {
        if (!isActive) return false
        if (message.toIntOrNull() == correctAnswer) {
            deactivate()
            return true
        }
        return false
    }

    fun isBlocked() = isActive

    private fun showTitle(title: String, subtitle: String) {
        mc.gui.setTitle(Component.literal(title))
        mc.gui.setSubtitle(Component.literal(subtitle))
    }
}
