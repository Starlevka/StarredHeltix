package set.starlev.features.combat.solvers.dungeons

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import java.util.regex.Pattern

object ThreeWeirdosSolverObj {
    private val STRANGER_PATTERN = Pattern.compile("\\[Персонаж] (.+?):")
    private val strangerStatements = mutableMapOf<String, String>()
    
    fun register() {
        ClientReceiveMessageEvents.GAME.register(this::onChatMessage)
    }
    
    private fun onChatMessage(message: Component, overlay: Boolean) {
        if (!StarredHeltix.feature.dungeons.threeWeirdos.enabled) {
            return
        }
        
        val messageText = message.string
        
        // Check if this is a stranger message
        val matcher = STRANGER_PATTERN.matcher(messageText)
        if (matcher.find()) {
            val strangerName = matcher.group(1)
            val statement = messageText.substring(matcher.end()).trim()
            
            // Store the statement
            strangerStatements[strangerName] = statement
            
            // Check if we have exactly 3 statements to solve
            if (strangerStatements.size == 3) {
                // Wait a bit to ensure all messages are processed, then solve
                Thread {
                    try {
                        Thread.sleep(500) // Wait 0.5 seconds
                        Minecraft.getInstance().execute(this::solvePuzzle)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }.start()
            }
        }
    }
    
    private fun solvePuzzle() {
        val client = Minecraft.getInstance()
        if (client.player == null) return
        
        var correctStranger: String? = null
        
        for ((name, statement) in strangerStatements) {
            // Проверяем все верные случаи
            if (statement.contains("В моем сундуке находится награда, и я говорю правду!") ||
                statement.startsWith("Они оба говорят правду. Также") ||
                statement.startsWith("По крайней мере один из них лжёт, и награды нет в сундуке") ||
                statement == "Награды нет ни в одном из сундуков." ||
                statement == "Награда не в моём сундуке!") {
                correctStranger = name
                break
            }
        }
        
        if (correctStranger != null) {
            client.player!!.displayClientMessage(Component.literal("§a§l[Три незнакомца] §aНаграда в сундуке: §e$correctStranger"), false)
        }
        
        // Clear statements for next puzzle
        strangerStatements.clear()
    }
}
