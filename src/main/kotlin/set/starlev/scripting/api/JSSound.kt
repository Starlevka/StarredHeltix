package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource
import org.mozilla.javascript.Scriptable
import set.starlev.scripting.ScriptConsole

/**
 * JS API: Объект `sound` — проигрывание звуков.
 *
 * ```js
 * sound.play("minecraft:entity.experience_orb.pickup");
 * sound.play("minecraft:block.note_block.pling", 0.5, 2.0);
 * sound.playAt("minecraft:entity.player.levelup", 100, 64, 200, 3.0, 1.0, "neutral");
 * sound.getCategories(); // список категорий
 * ```
 */
class JSSound(private val moduleName: String) {
    private val client: Minecraft get() = Minecraft.getInstance()

    /** Проиграть звук у игрока. */
    fun play(name: String, volume: Float = 1.0f, pitch: Float = 1.0f, category: String = "players"): Boolean {
        val player = client.player ?: return false
        return playAt(name, player.x, player.y, player.z, volume, pitch, category)
    }

    /**
     * Проиграть звук в мировой точке.
     * @param category MASTER/MUSIC/RECORDS/WEATHER/BLOCKS/HOSTILE/NEUTRAL/PLAYERS/AMBIENT/VOICE (регистр не важен)
     */
    fun playAt(name: String, x: Double, y: Double, z: Double, volume: Float = 1.0f, pitch: Float = 1.0f, category: String = "master"): Boolean {
        return try {
            val level = client.level ?: return false
            val loc = Identifier.tryParse(if (name.contains(':')) name else "minecraft:$name") ?: return false
            val soundEvent = BuiltInRegistries.SOUND_EVENT.getOptional(loc).orElse(null) ?: return false
            val source = parseSource(category)
            client.execute {
                level.playSound(null, x, y, z, soundEvent, source, volume, pitch)
            }
            true
        } catch (e: Exception) {
            set.starlev.scripting.ScriptConsole.error(moduleName, "sound.playAt('$name'): ${e.message}")
            false
        }
    }

    /** Категории звука (для UI-подсказок). */
    fun getCategories(): List<String> = SoundSource.entries.map { it.name.lowercase() }

    // ==================== Кастомные ogg-файлы модуля ====================

    /**
     * Проиграть .ogg файл из папки модуля (config/starredheltix/modules/<модуль>/).
     * Возвращает хэндл (string) для sound.stopFile(handle), или null при ошибке.
     *
     * Опции (JS-объект): volume (0..4), pitch (0.5..2), loop, loopDelay (тики),
     * x/y/z (мировая позиция, по умолчанию — позиция игрока), category (см. getCategories).
     *
     * ```js
     * var h = sound.playFile("notify.ogg", { volume: 0.6, loop: false });
     * sound.playFile("ambient.ogg", { loop: true, category: "music" });
     * ```
     */
    fun playFile(fileName: String, options: Scriptable? = null): String? =
        set.starlev.scripting.SHModuleSounds.play(moduleName, fileName, options)

    /** Остановить звук по хэндлу из playFile. Возвращает true, если хэндл был активен. */
    fun stopFile(handle: String): Boolean =
        set.starlev.scripting.SHModuleSounds.stop(moduleName, handle)

    /** Остановить все запущенные через playFile звуки модуля. */
    fun stopAll(): Unit =
        set.starlev.scripting.SHModuleSounds.stopForModule(moduleName)

    private fun parseSource(name: String): SoundSource {
        return SoundSource.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SoundSource.MASTER
    }
}
