package set.starlev.features.combat.dungeons.solvers

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.utils.detectors.DungeonDetector
import java.util.regex.Pattern

object ThreeWeirdos {
    private val STRANGER_PATTERN = Pattern.compile("\\[Персонаж] (.+?):")
    private val statements = mutableMapOf<String, String>()
    
    fun init() {
        ClientReceiveMessageEvents.GAME.register(this::onChat)
    }
    
    private fun onChat(message: Component, overlay: Boolean) {
        if (!StarredHeltix.feature.dungeons.solvers.threeWeirdos) {
            return
        }
        
        val text = message.string
        
        // Check if this is a stranger message
        val matcher = STRANGER_PATTERN.matcher(text)
        if (matcher.find()) {
            val name = matcher.group(1)
            val statement = text.substring(matcher.end()).trim()
            
            // Store the statement
            statements[name] = statement
            
            // Check if we have exactly 3 statements to solve
            if (statements.size == 3) {
                // Wait a bit to ensure all messages are processed, then solve
                Thread {
                    try {
                        Thread.sleep(500) // Wait 0.5 seconds
                        Minecraft.getInstance().execute(this::solve)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }.start()
            }
        }
    }
    
    private fun solve() {
        val client = Minecraft.getInstance()
        if (client.player == null) return
        
        // Проверка на нахождение в подземельях
        if (!DungeonDetector.isInDungeon()) {
            statements.clear()
            return
        }

        var correct: String? = null
        
        for ((name, statement) in statements) {
            // Проверяем все верные случаи
            if (statement.contains("В моем сундуке находится награда, и я говорю правду!") ||
                statement.startsWith("Они оба говорят правду. Также") ||
                statement.startsWith("По крайней мере один из них лжёт, и награды нет в сундуке") ||
                statement == "Награды нет ни в одном из сундуков." ||
                statement == "Награда не в моём сундуке!") {
                correct = name
                break
            }
        }
        
        if (correct != null) {
            client.player!!.displayClientMessage(Component.literal("§a§l[Три незнакомца] §aНаграда в сундуке: §e$correct"), false)
        }
        
        // Clear statements for next puzzle
        statements.clear()
    }
}
