package set.starlev.features.chat.mod

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import kotlin.random.Random

object MacroCheck {
    private val mc = Minecraft.getInstance()
    private var isActive = false
    private var correctAnswer = 0
    private var lastTitleTime = 0L
    private var checkerName: String? = null

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

    /** Сброс при смене мира/сервера: иначе исходящий чат остаётся заблоканным на другом сервере. */
    fun onWorldChange() {
        isActive = false
        checkerName = null
        correctAnswer = 0
        lastTitleTime = 0L
    }

    fun activate(checker: String? = null) {
        if (isActive) return
        isActive = true
        checkerName = checker
        currentA = Random.nextInt(1, 99)
        currentB = Random.nextInt(1, 99)
        correctAnswer = currentA + currentB
        
        mc.player?.sendSystemMessage(Component.literal("§c§l[MACRO] §fАдминистратор начал проверку на макросы!\n§fВведите в чат результат выражения: §b$currentA + $currentB"))
        
        showTitle("§c§lМакро-чек!", "§eРешите пример: §b$currentA + $currentB §e= ?")
        lastTitleTime = System.currentTimeMillis()
    }

    fun deactivate() {
        isActive = false
        mc.gui.hud.setTitle(Component.empty())
        mc.gui.hud.setSubtitle(Component.empty())
        mc.player?.sendSystemMessage(Component.literal("§a§l[MACRO] §fПроверка на макросы успешно пройдена."))
        
        // Отправляем сообщение проверяющему
        checkerName?.let { checker ->
            val message = "Я прошел макро-чек! Ответ: $correctAnswer"
            mc.player?.connection?.sendCommand("unignore $checker")
            mc.player?.connection?.sendCommand("msg $checker $message")
            // На всякий случай дублируем в пати чат, если мы в пати
            mc.player?.connection?.sendCommand("pc $message")
        }
        checkerName = null
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
        mc.gui.hud.setTitle(Component.literal(title))
        mc.gui.hud.setSubtitle(Component.literal(subtitle))
    }
}
