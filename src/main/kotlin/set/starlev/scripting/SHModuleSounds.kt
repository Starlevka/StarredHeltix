package set.starlev.scripting

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.Sound
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.WeighedSoundEvents
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.IoSupplier
import net.minecraft.server.packs.resources.Resource
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.util.valueproviders.SampledFloat
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.UniqueTag
import set.starlev.injections.accessors.AbstractSoundInstanceAccessor
import set.starlev.injections.accessors.SoundManagerAccessor
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Кастомные ogg-звуки для JS-модулей (sound.playFile).
 *
 * Механизм (как в ctjs Sound.bootstrap): SoundEvent + Sound(Type.FILE) регистрируются
 * напрямую в приватных реестрах SoundManager через миксин-аксессоры, а байты файла
 * отдаются через Resource с IoSupplier. Категория управляет громкостью слайдера,
 * зацикливание — флагом loop инстанса.
 *
 * Песочница: файлы читаются только из папки модуля (config/starredheltix/modules/<модуль>/).
 * Реестр чистится при выгрузке модуля и выгрузке движка.
 */
object SHModuleSounds {

    private const val NAMESPACE = "starredheltix"
    private const val PATH_PREFIX = "modsnd/"

    /** Лимит активных хэндлов на модуль: защита от утечки при playFile в цикле. */
    private const val MAX_HANDLES_PER_MODULE = 100

    private data class Playing(
        val module: String,
        val handle: String,
        val instance: SHSoundInstance
    )

    private data class Registered(
        val module: String,
        val soundId: Identifier,      // идентификатор звука (registry)
        val fileId: Identifier        // sounds/<...>.ogg (soundCache)
    )

    /** key: "<module>:<handle>" lower. */
    private val playing = ConcurrentHashMap<String, Playing>()

    /** key: "<module>\u0000<fileName>" lower. */
    private val registered = ConcurrentHashMap<String, Registered>()

    private val handleCounter = java.util.concurrent.atomic.AtomicInteger(0)

    // ==================== JS API ====================

    /**
     * Проиграть ogg-файл из папки модуля. Возвращает хэндл (string) или null при ошибке.
     * Опции: volume, pitch, loop, loopDelay (в тиках), x, y, z (позиция в мире), category.
     */
    fun play(moduleName: String, fileName: String, options: Scriptable?): String? {
        val file = resolveModuleFile(moduleName, fileName)
        if (file == null || !file.exists()) {
            ScriptConsole.error(moduleName, "sound.playFile('$fileName'): файл не найден в папке модуля")
            return null
        }
        if (!file.name.lowercase().endsWith(".ogg")) {
            ScriptConsole.error(moduleName, "sound.playFile('$fileName'): поддерживаются только .ogg файлы")
            return null
        }

        val volume = optNumber(options, "volume")?.toFloat() ?: 1.0f
        val pitch = optNumber(options, "pitch")?.toFloat() ?: 1.0f
        val loop = opt(options, "loop")?.let { it as? Boolean } ?: false
        val loopDelay = optNumber(options, "loopDelay")?.toInt() ?: 0
        val category = opt(options, "category")?.toString() ?: "master"
        val x = optNumber(options, "x")?.toDouble()
        val y = optNumber(options, "y")?.toDouble()
        val z = optNumber(options, "z")?.toDouble()

        val mc = Minecraft.getInstance()
        val player = mc.player
        val px = x ?: player?.x ?: 0.0
        val py = y ?: player?.y ?: 64.0
        val pz = z ?: player?.z ?: 0.0

        val handle = "s${handleCounter.incrementAndGet()}"
        val key = "${moduleName.lowercase()}:$handle"

        // Регистрация в SoundManager и запуск — на рендер-потоке
        mc.execute {
            try {
                val soundId = ensureRegistered(moduleName, fileName, file) ?: return@execute
                val source = SoundSource.entries.firstOrNull {
                    it.name.equals(category, ignoreCase = true)
                } ?: SoundSource.MASTER
                val soundEvent = SoundEvent.createVariableRangeEvent(soundId)
                val instance = SHSoundInstance(
                    soundEvent, source, px, py, pz,
                    volume.coerceIn(0f, 4f), pitch.coerceIn(0.5f, 2f),
                    loop, loopDelay
                )
                mc.soundManager.play(instance)
                playing[key] = Playing(moduleName, handle, instance)
                evictIfOverflow(moduleName)
            } catch (e: Exception) {
                ScriptConsole.error(moduleName, "sound.playFile('$fileName'): ${e.message}", e)
            }
        }
        return handle
    }

    /** Остановить звук по хэндлу (возвращает true, если хэндл был активен). */
    fun stop(moduleName: String, handle: String): Boolean {
        val key = "${moduleName.lowercase()}:${handle.lowercase()}"
        val p = playing.remove(key) ?: return false
        val mc = Minecraft.getInstance()
        mc.execute { runCatching { mc.soundManager.stop(p.instance) } }
        return true
    }

    /** Остановить все звуки модуля (без снятия регистрации файлов). */
    fun stopForModule(moduleName: String) {
        val prefix = "${moduleName.lowercase()}:"
        val mc = Minecraft.getInstance()
        playing.keys.filter { it.startsWith(prefix) }.forEach { key ->
            playing.remove(key)?.let { p ->
                mc.execute { runCatching { mc.soundManager.stop(p.instance) } }
            }
        }
    }

    /** Полная очистка модуля: остановка + снятие регистрации звуков из SoundManager. */
    fun removeForModule(moduleName: String) {
        stopForModule(moduleName)
        val prefix = "${moduleName.lowercase()}\u0000"
        val mc = Minecraft.getInstance()
        registered.keys.filter { it.startsWith(prefix) }.toList().forEach { key ->
            registered.remove(key)?.let { reg ->
                mc.execute {
                    runCatching {
                        (mc.soundManager as SoundManagerAccessor).shGetRegistry().remove(reg.soundId)
                        (mc.soundManager as SoundManagerAccessor).shGetSoundCache().remove(reg.fileId)
                    }
                }
            }
        }
    }

    /** Выгрузка движка: остановить и разрегистрировать всё. */
    fun clearAll() {
        val mc = Minecraft.getInstance()
        playing.values.forEach { p ->
            mc.execute { runCatching { mc.soundManager.stop(p.instance) } }
        }
        playing.clear()
        registered.clear()
    }

    // ==================== Внутреннее ====================

    /**
     * Гарантирует регистрацию звука в SoundManager (идемпотентно по имени файла).
     * Вызывается ТОЛЬКО на рендер-потоке. Возвращает Identifier звука.
     */
    private fun ensureRegistered(moduleName: String, fileName: String, file: File): Identifier? {
        val fileKey = "${moduleName.lowercase()}\u0000${fileName.lowercase()}"
        registered[fileKey]?.let { return it.soundId }

        val mc = Minecraft.getInstance()
        val accessor = mc.soundManager as SoundManagerAccessor

        val unique = sanitize("$moduleName/$fileName")
        if (unique.isEmpty()) {
            ScriptConsole.error(moduleName, "sound.playFile('$fileName'): не удалось построить идентификатор")
            return null
        }
        val soundId = Identifier.fromNamespaceAndPath(NAMESPACE, PATH_PREFIX + unique)
        val fileId = soundId.withPrefix("sounds/").withSuffix(".ogg")

        try {
            // Байты файла: новый поток на каждое воспроизведение (IoSupplier)
            accessor.shGetSoundCache()[fileId] =
                Resource(SHSoundPack, IoSupplier<InputStream> { FileInputStream(file) })
            // WeighedSoundEvents с одним Sound типа FILE
            val events = WeighedSoundEvents(soundId, null)
            events.addSound(
                Sound(
                    soundId,
                    SampledFloat { 1f },
                    SampledFloat { 1f },
                    1,
                    Sound.Type.FILE,
                    false,
                    false,
                    16
                )
            )
            accessor.shGetRegistry()[soundId] = events
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "sound.playFile('$fileName'): регистрация не удалась: ${e.message}", e)
            return null
        }

        registered[fileKey] = Registered(moduleName, soundId, fileId)
        return soundId
    }

    /** Вытеснение старых одноразовых хэндлов при переполнении (зацикленные не трогаем). */
    private fun evictIfOverflow(moduleName: String) {
        val prefix = "${moduleName.lowercase()}:"
        val mine = playing.keys.filter { it.startsWith(prefix) }
        if (mine.size <= MAX_HANDLES_PER_MODULE) return
        val excess = mine.size - MAX_HANDLES_PER_MODULE
        val mc = Minecraft.getInstance()
        var removed = 0
        for (key in mine.sorted()) {
            if (removed >= excess) break
            val p = playing[key] ?: continue
            if (!p.instance.isLooping()) {
                playing.remove(key)
                mc.execute { runCatching { mc.soundManager.stop(p.instance) } }
                removed++
            }
        }
    }

    /** Файл внутри папки модуля (песочница, без выхода через ..). */
    private fun resolveModuleFile(moduleName: String, fileName: String): File? {
        return try {
            val moduleDir = ModuleLoader.modulesDir.absoluteFile
            val f = File(moduleDir, moduleName).resolve(fileName).canonicalFile
            if (!f.path.startsWith(File(moduleDir, moduleName).canonicalFile.path)) {
                ScriptConsole.error(moduleName, "sound.playFile('$fileName'): доступ за пределами папки модуля запрещён")
                null
            } else f
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "sound.playFile('$fileName'): ${e.message}")
            null
        }
    }

    /** Только валидные для Identifier.path символы; остальное — 'u' + hex-код символа. */
    private fun sanitize(s: String): String {
        val sb = StringBuilder()
        for (c in s.lowercase()) {
            if (c in 'a'..'z' || c in '0'..'9' || c == '_' || c == '.' || c == '-' || c == '/') {
                sb.append(c)
            } else {
                sb.append('u').append(Integer.toHexString(c.code))
            }
        }
        return sb.toString()
    }

    private fun opt(options: Scriptable?, key: String): Any? {
        if (options == null) return null
        val v = options.get(key, options)
        if (v === UniqueTag.NOT_FOUND || v === org.mozilla.javascript.Undefined.instance) return null
        return v
    }

    private fun optNumber(options: Scriptable?, key: String): Number? = opt(options, key) as? Number

    // ==================== Инстанс звука ====================

    /**
     * Кастомный тикающий инстанс. Поля (volume/pitch/x/y/z/looping/delay) ставятся
     * через миксин-аксессор. Тик останавливает звук при выходе из мира.
     */
    internal class SHSoundInstance(
        soundEvent: SoundEvent,
        source: SoundSource,
        x: Double, y: Double, z: Double,
        volume: Float, pitch: Float,
        looping: Boolean, delay: Int
    ) : AbstractTickableSoundInstance(soundEvent, source, RandomSource.create()) {

        init {
            @Suppress("CAST_NEVER_SUCCEEDS")
            val acc = this as AbstractSoundInstanceAccessor
            acc.shSetVolume(volume)
            acc.shSetPitch(pitch)
            acc.shSetX(x)
            acc.shSetY(y)
            acc.shSetZ(z)
            acc.shSetLooping(looping)
            acc.shSetDelay(delay.coerceIn(0, 200))
            acc.shSetAttenuation(SoundInstance.Attenuation.LINEAR)
            acc.shSetRelative(false)
        }

        override fun tick() {
            if (Minecraft.getInstance().level == null) stop()
        }
    }

    // ==================== Пустой PackResources ====================

    /**
     * Формальный source для Resource. Байты отдаются напрямую через IoSupplier,
     * поэтому pack'у ничего не нужно уметь (как CTResourcePack в ctjs).
     */
    private object SHSoundPack : PackResources {
        override fun packId(): String = "starredheltix-module-sounds"

        override fun close() {
            throw UnsupportedOperationException()
        }

        override fun getRootResource(vararg segments: String): IoSupplier<InputStream>? {
            throw UnsupportedOperationException()
        }

        override fun getResource(type: PackType, id: Identifier): IoSupplier<InputStream>? {
            throw UnsupportedOperationException()
        }

        override fun listResources(
            type: PackType,
            namespace: String,
            prefix: String,
            consumer: PackResources.ResourceOutput
        ) {
            throw UnsupportedOperationException()
        }

        override fun getNamespaces(type: PackType): MutableSet<String> {
            throw UnsupportedOperationException()
        }

        override fun <T : Any> getMetadataSection(metaReader: net.minecraft.server.packs.metadata.MetadataSectionType<T>): T? {
            throw UnsupportedOperationException()
        }

        override fun location(): PackLocationInfo {
            throw NotImplementedError()
        }
    }
}
