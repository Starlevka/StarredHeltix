package set.starlev.features.misc

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW
import set.starlev.StarredHeltix

object CustomBindManager {
    private val mc = Minecraft.getInstance()
    private val binds = mutableMapOf<String, Pair<String, Int>>()
    private val keyStates = mutableMapOf<Int, Boolean>()

    fun init() {
        loadFromConfig()
    }

    fun tick() {
        if (!StarredHeltix.feature.misc.general.customBinds) return
        if (mc.player == null) return
        if (mc.screen != null) return

        binds.forEach { (_, pair) ->
            val keyCode = pair.second
            if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return@forEach

            val current = InputConstants.isKeyDown(mc.window, keyCode)
            val previous = keyStates[keyCode] ?: false

            if (current && !previous) execute(pair.first)
            keyStates[keyCode] = current
        }
    }

    fun create(name: String, command: String): Boolean {
        if (binds.containsKey(name)) {
            sendMsg("§cБинд '$name' уже существует!")
            return false
        }
        if (name.length > 20) {
            sendMsg("§cНазвание не может быть длиннее 20 символов!")
            return false
        }
        if (command.length > 100) {
            sendMsg("§cКоманда не может быть длиннее 100 символов!")
            return false
        }
        binds[name] = command to GLFW.GLFW_KEY_UNKNOWN
        save()
        sendMsg("§aБинд '$name' создан! Команда: '$command'")
        sendMsg("§eИспользуйте /sh binds setkey \"$name\" <клавиша>")
        return true
    }

    fun delete(name: String): Boolean {
        if (binds.remove(name) == null) {
            sendMsg("§cБинд '$name' не найден!")
            return false
        }
        save()
        sendMsg("§aБинд '$name' удалён!")
        return true
    }

    fun setKey(name: String, keyName: String): Boolean {
        val bind = binds[name] ?: run {
            sendMsg("§cБинд '$name' не найден!")
            return false
        }
        if (keyName.equals("ESCAPE", true) || keyName.equals("ESC", true)) {
            binds[name] = bind.first to GLFW.GLFW_KEY_UNKNOWN
            save()
            sendMsg("§aКлавиша для '$name' сброшена")
            return true
        }
        val keyCode = parseKey(keyName)
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            sendMsg("§cНеизвестная клавиша: $keyName")
            return false
        }
        binds[name] = bind.first to keyCode
        save()
        sendMsg("§aКлавиша '$keyName' назначена для '$name'")
        return true
    }

    fun list() {
        if (binds.isEmpty()) {
            sendMsg("§eНет биндов!")
            return
        }
        sendMsg("§6=== Бинды ===")
        binds.forEach { (name, pair) ->
            val keyName = getKeyName(pair.second)
            sendMsg("§e$name§7: §f${pair.first} §7[$keyName]")
        }
    }

    private fun execute(action: String) {
        if (action.isEmpty()) return
        if (action.startsWith("/")) {
            val cmd = action.substring(1)
            mc.player?.connection?.sendCommand(cmd)
        } else {
            mc.player?.connection?.sendChat(action)
        }
    }

    private fun loadFromConfig() {
        binds.clear()
        val config = StarredHeltix.feature.misc
        config.customBindsMap.forEach { (name, command) ->
            val keyCode = config.customBindsKeys[name] ?: GLFW.GLFW_KEY_UNKNOWN
            binds[name] = command to keyCode
        }
    }

    private fun save() {
        val config = StarredHeltix.feature.misc
        config.customBindsMap.clear()
        config.customBindsKeys.clear()
        binds.forEach { (name, pair) ->
            config.customBindsMap[name] = pair.first
            if (pair.second != GLFW.GLFW_KEY_UNKNOWN) {
                config.customBindsKeys[name] = pair.second
            }
        }
        StarredHeltix.configManager.saveConfig("custom-binds")
    }

    private fun parseKey(name: String): Int = when (name.uppercase()) {
        "F1" -> GLFW.GLFW_KEY_F1; "F2" -> GLFW.GLFW_KEY_F2; "F3" -> GLFW.GLFW_KEY_F3; "F4" -> GLFW.GLFW_KEY_F4
        "F5" -> GLFW.GLFW_KEY_F5; "F6" -> GLFW.GLFW_KEY_F6; "F7" -> GLFW.GLFW_KEY_F7; "F8" -> GLFW.GLFW_KEY_F8
        "F9" -> GLFW.GLFW_KEY_F9; "F10" -> GLFW.GLFW_KEY_F10; "F11" -> GLFW.GLFW_KEY_F11; "F12" -> GLFW.GLFW_KEY_F12
        "A" -> GLFW.GLFW_KEY_A; "B" -> GLFW.GLFW_KEY_B; "C" -> GLFW.GLFW_KEY_C; "D" -> GLFW.GLFW_KEY_D
        "E" -> GLFW.GLFW_KEY_E; "F" -> GLFW.GLFW_KEY_F; "G" -> GLFW.GLFW_KEY_G; "H" -> GLFW.GLFW_KEY_H
        "I" -> GLFW.GLFW_KEY_I; "J" -> GLFW.GLFW_KEY_J; "K" -> GLFW.GLFW_KEY_K; "L" -> GLFW.GLFW_KEY_L
        "M" -> GLFW.GLFW_KEY_M; "N" -> GLFW.GLFW_KEY_N; "O" -> GLFW.GLFW_KEY_O; "P" -> GLFW.GLFW_KEY_P
        "Q" -> GLFW.GLFW_KEY_Q; "R" -> GLFW.GLFW_KEY_R; "S" -> GLFW.GLFW_KEY_S; "T" -> GLFW.GLFW_KEY_T
        "U" -> GLFW.GLFW_KEY_U; "V" -> GLFW.GLFW_KEY_V; "W" -> GLFW.GLFW_KEY_W; "X" -> GLFW.GLFW_KEY_X
        "Y" -> GLFW.GLFW_KEY_Y; "Z" -> GLFW.GLFW_KEY_Z
        "0" -> GLFW.GLFW_KEY_0; "1" -> GLFW.GLFW_KEY_1; "2" -> GLFW.GLFW_KEY_2; "3" -> GLFW.GLFW_KEY_3
        "4" -> GLFW.GLFW_KEY_4; "5" -> GLFW.GLFW_KEY_5; "6" -> GLFW.GLFW_KEY_6; "7" -> GLFW.GLFW_KEY_7
        "8" -> GLFW.GLFW_KEY_8; "9" -> GLFW.GLFW_KEY_9
        "SPACE" -> GLFW.GLFW_KEY_SPACE; "ENTER" -> GLFW.GLFW_KEY_ENTER; "TAB" -> GLFW.GLFW_KEY_TAB
        "LEFT_SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT; "RIGHT_SHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT
        "LEFT_CONTROL" -> GLFW.GLFW_KEY_LEFT_CONTROL; "RIGHT_CONTROL" -> GLFW.GLFW_KEY_RIGHT_CONTROL
        "LEFT_ALT" -> GLFW.GLFW_KEY_LEFT_ALT; "RIGHT_ALT" -> GLFW.GLFW_KEY_RIGHT_ALT
        else -> GLFW.GLFW_KEY_UNKNOWN
    }

    private fun getKeyName(keyCode: Int): String = when (keyCode) {
        GLFW.GLFW_KEY_F1 -> "F1"; GLFW.GLFW_KEY_F2 -> "F2"; GLFW.GLFW_KEY_F3 -> "F3"; GLFW.GLFW_KEY_F4 -> "F4"
        GLFW.GLFW_KEY_F5 -> "F5"; GLFW.GLFW_KEY_F6 -> "F6"; GLFW.GLFW_KEY_F7 -> "F7"; GLFW.GLFW_KEY_F8 -> "F8"
        GLFW.GLFW_KEY_F9 -> "F9"; GLFW.GLFW_KEY_F10 -> "F10"; GLFW.GLFW_KEY_F11 -> "F11"; GLFW.GLFW_KEY_F12 -> "F12"
        GLFW.GLFW_KEY_A -> "A"; GLFW.GLFW_KEY_B -> "B"; GLFW.GLFW_KEY_C -> "C"; GLFW.GLFW_KEY_D -> "D"
        GLFW.GLFW_KEY_E -> "E"; GLFW.GLFW_KEY_F -> "F"; GLFW.GLFW_KEY_G -> "G"; GLFW.GLFW_KEY_H -> "H"
        GLFW.GLFW_KEY_I -> "I"; GLFW.GLFW_KEY_J -> "J"; GLFW.GLFW_KEY_K -> "K"; GLFW.GLFW_KEY_L -> "L"
        GLFW.GLFW_KEY_M -> "M"; GLFW.GLFW_KEY_N -> "N"; GLFW.GLFW_KEY_O -> "O"; GLFW.GLFW_KEY_P -> "P"
        GLFW.GLFW_KEY_Q -> "Q"; GLFW.GLFW_KEY_R -> "R"; GLFW.GLFW_KEY_S -> "S"; GLFW.GLFW_KEY_T -> "T"
        GLFW.GLFW_KEY_U -> "U"; GLFW.GLFW_KEY_V -> "V"; GLFW.GLFW_KEY_W -> "W"; GLFW.GLFW_KEY_X -> "X"
        GLFW.GLFW_KEY_Y -> "Y"; GLFW.GLFW_KEY_Z -> "Z"
        GLFW.GLFW_KEY_0 -> "0"; GLFW.GLFW_KEY_1 -> "1"; GLFW.GLFW_KEY_2 -> "2"; GLFW.GLFW_KEY_3 -> "3"
        GLFW.GLFW_KEY_4 -> "4"; GLFW.GLFW_KEY_5 -> "5"; GLFW.GLFW_KEY_6 -> "6"; GLFW.GLFW_KEY_7 -> "7"
        GLFW.GLFW_KEY_8 -> "8"; GLFW.GLFW_KEY_9 -> "9"
        GLFW.GLFW_KEY_SPACE -> "SPACE"; GLFW.GLFW_KEY_ENTER -> "ENTER"; GLFW.GLFW_KEY_TAB -> "TAB"
        GLFW.GLFW_KEY_UNKNOWN -> "Не назначена"
        else -> "Неизвестная"
    }

    private fun sendMsg(msg: String) {
        mc.player?.displayClientMessage(Component.literal(msg), false)
    }
}
