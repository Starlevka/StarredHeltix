package set.starlev.features.chat

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import java.util.Locale

/**
 * Пользовательские алиасы серверных команд: `/dh` → `/warp dungeon_hub`.
 *
 * Алиасы создаёт игрок; команда переписывается прямо перед отправкой пакета, поэтому
 *  - аргументы сохраняются: `/dh lobby` → `/warp dungeon_hub lobby`;
 *  - алиас не нужно регистрировать в Brigadier — неизвестная клиенту команда просто уходит
 *    на сервер, где мы её уже переименовали (на `/sh ...` и ванильные команды не действует);
 *  - переписывание одноразовое (никакой рекурсии): если результат сам начинается с алиаса,
 *    команда уходит как есть.
 *
 * Настройка: `/sh aliases` (GUI, как `/sh binds`) или `/sh alias create|delete|list`.
 * Хранится в `chat.aliases.aliasesMap`, поэтому переживает перезапуск без миграций.
 */
object CommandAliases {
    private val mc = Minecraft.getInstance()

    private const val MAX_ALIAS_LENGTH = 24
    private const val MAX_COMMAND_LENGTH = 120

    /** alias (без слеша, нижний регистр) → команда (без слеша). */
    private val aliases = LinkedHashMap<String, String>()

    private val enabled: Boolean
        get() = try {
            StarredHeltix.feature.chat.aliases.enabled
        } catch (_: Throwable) {
            false
        }

    fun init() {
        reload()
    }

    /** Перечитывает алиасы из конфига (после сброса конфига или правки руками). */
    fun reload() {
        aliases.clear()
        try {
            StarredHeltix.feature.chat.aliases.aliasesMap.forEach { (alias, command) ->
                val key = normalize(alias) ?: return@forEach
                val value = normalizeCommand(command) ?: return@forEach
                aliases[key] = value
            }
        } catch (t: Throwable) {
            StarredHeltix.LOGGER.warn("[Aliases] Не удалось прочитать конфиг: ${t.message}")
        }
    }

    fun snapshot(): Map<String, String> = LinkedHashMap(aliases)

    /**
     * Переписывает команду, если её первое слово — алиас.
     * @param command команда без ведущего слеша (как приходит в `ClientPacketListener.sendCommand`).
     * @return команда для отправки на сервер.
     */
    fun rewrite(command: String): String {
        if (command.isEmpty() || aliases.isEmpty()) return command
        if (!enabled) return command
        return try {
            val trimmed = command.trim()
            if (trimmed.isEmpty()) return command
            val firstSpace = trimmed.indexOf(' ')
            val head = if (firstSpace < 0) trimmed else trimmed.substring(0, firstSpace)
            val target = aliases[head.lowercase(Locale.ROOT)] ?: return command
            val args = if (firstSpace < 0) "" else trimmed.substring(firstSpace).trim()
            // Рекурсия запрещена: если цель сама начинается с этого алиаса — не трогаем.
            val targetHead = target.substringBefore(' ').lowercase(Locale.ROOT)
            if (targetHead == head.lowercase(Locale.ROOT)) return command
            if (args.isEmpty()) target else "$target $args"
        } catch (t: Throwable) {
            command
        }
    }

    fun create(alias: String, command: String): Boolean {
        val key = normalize(alias)
        if (key == null) {
            sendMsg("§cАлиас: только латиница/цифры, до $MAX_ALIAS_LENGTH символов")
            return false
        }
        val value = normalizeCommand(command)
        if (value == null) {
            sendMsg("§cКоманда: до $MAX_COMMAND_LENGTH символов")
            return false
        }
        if (value.substringBefore(' ').lowercase(Locale.ROOT) == key) {
            sendMsg("§cКоманда не может ссылаться на сам алиас")
            return false
        }
        if (aliases.containsKey(key)) {
            sendMsg("§eАлиас '$key' уже был — перезаписан")
        }
        aliases[key] = value
        save()
        sendMsg("§a/$key §7→ §f/$value")
        return true
    }

    fun delete(alias: String): Boolean {
        val key = alias.trim().trim('/').lowercase(Locale.ROOT)
        if (aliases.remove(key) == null) {
            sendMsg("§cАлиас '$key' не найден")
            return false
        }
        save()
        sendMsg("§aАлиас '$key' удалён")
        return true
    }

    fun list() {
        if (aliases.isEmpty()) {
            sendMsg("§eАлиасов нет — /sh aliases")
            return
        }
        sendMsg("§6=== Алиасы (${aliases.size}) ===")
        aliases.forEach { (alias, command) -> sendMsg("§e/$alias §7→ §f/$command") }
    }

    /** Нормализует и проверяет алиас. */
    private fun normalize(alias: String): String? {
        val key = alias.trim().trim('/').lowercase(Locale.ROOT)
        if (key.isEmpty() || key.length > MAX_ALIAS_LENGTH) return null
        if (!key.all { it.isLetterOrDigit() || it == '_' || it == '-' }) return null
        return key
    }

    /** Убирает слеш и лишние пробелы, режет по длине. */
    private fun normalizeCommand(command: String): String? {
        val value = command.trim().trim('/').replace(Regex("\\s+"), " ")
        if (value.isEmpty() || value.length > MAX_COMMAND_LENGTH) return null
        return value
    }

    private fun save() {
        try {
            val cfg = StarredHeltix.feature.chat.aliases
            cfg.aliasesMap.clear()
            cfg.aliasesMap.putAll(aliases)
            StarredHeltix.configManager.saveConfig("command-aliases")
        } catch (t: Throwable) {
            StarredHeltix.LOGGER.warn("[Aliases] Не удалось сохранить: ${t.message}")
        }
    }

    private fun sendMsg(message: String) {
        mc.player?.sendSystemMessage(Component.literal(message))
    }
}
