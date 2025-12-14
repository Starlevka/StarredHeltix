package set.starlev.features.chat.mod

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import kotlin.random.Random

object MacroCheck {
    private val mc = Minecraft.getInstance()
    private var isActive = false
    private var correctAnswer = 0

    fun activate() {
        if (isActive) return
        isActive = true
        val a = Random.nextInt(1, 50)
        val b = Random.nextInt(1, 50)
        correctAnswer = a + b
        showTitle("§c&lМакро-чек!", "§e$a + $b = ?")
    }

    fun deactivate() {
        isActive = false
        mc.gui.setTitle(Component.empty())
        mc.gui.setSubtitle(Component.empty())
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
