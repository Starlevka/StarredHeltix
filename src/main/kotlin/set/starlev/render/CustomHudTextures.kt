package set.starlev.render

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import set.starlev.StarredHeltix
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Кастомные HUD-текстуры (scoreboard.png, map_background.png) — порт
 * CustomScoreboardBackground.kt (CustomScoreboard-main):
 * приоритет — абсолютный путь из конфига (как customImageFile там), затем
 * config/starredheltix/textures/ в gameDir и в рабочей папке процесса.
 *
 * Загрузка один раз (не каждый кадр): файл читается в RGBA, DynamicTexture +
 * TextureManager.register откладываются на mc.execute (runNextTick в референсе).
 * Разрешение любое — blitStretched растягивает картинку под структуру HUD.
 */
object CustomHudTextures {

    data class Handle(val id: Identifier, val srcW: Int, val srcH: Int)

    private class Entry {
        var handle: Handle? = null
        var mtime = -1L
        var failedMtime = -1L
        var pending = false
        var file: File? = null
        var searchedAt = 0L
    }

    private val entries = HashMap<String, Entry>()

    private fun candidateFiles(name: String, subfolder: String?): List<File> {
        val roots = listOfNotNull(
            Minecraft.getInstance().gameDirectory,
            File("").absoluteFile, // cwd — у лаунчеров с instance-папками может отличаться
        ).distinct()
        val rels = buildList {
            add("config/starredheltix/textures/$name")
            if (subfolder != null) add("config/starredheltix/textures/$subfolder/$name")
        }
        return roots.flatMap { root -> rels.map { rel -> File(root, rel) } }
    }

    /** Только PNG: остальные форматы NativeImage не гарантирует, в конфиге тоже пишем только PNG. */
    private fun findFile(name: String, subfolder: String?, explicitPath: String?): File? {
        if (!explicitPath.isNullOrBlank()) {
            val f = runCatching { File(explicitPath) }.getOrNull()
            // Явный путь принимаем как есть, но загрузится только PNG (остальное отсеется при декодировании)
            if (f != null && f.isFile) return f
        }
        for (f in candidateFiles(name, subfolder)) {
            if (f.isFile) return f
        }
        // Фолбэк только на .png (без jpg/jpeg/bmp): в конфиге заявлен только PNG
        val base = name.substringBeforeLast('.')
        if (!name.endsWith(".png", ignoreCase = true)) {
            for (f in candidateFiles("$base.png", subfolder)) {
                if (f.isFile) return f
            }
        }
        return null
    }

    /**
     * Возвращает загруженную текстуру или null (файла нет / ещё грузится).
     * Вызывается из рендера; сам файл и GPU-регистрация вне горячего пути.
     * @param explicitPath абсолютный путь из конфига (приоритет), или null/пусто.
     */
    @Synchronized
    fun ensure(key: String, name: String, subfolder: String? = null, explicitPath: String? = null): Handle? {
        val e = entries.getOrPut(key) { Entry() }
        val now = System.currentTimeMillis()
        if (now - e.searchedAt > 1000L) {
            e.file = findFile(name, subfolder, explicitPath)
            e.searchedAt = now
        }
        val file = e.file ?: run {
            return null
        }
        val mtime = file.lastModified()
        if (e.handle != null && e.mtime == mtime) return e.handle
        if (e.failedMtime == mtime) return null // этот файл уже не читался — пробуем только после его замены
        if (e.pending) return e.handle // грузим на следующем тике
        e.pending = true

        val bytes = runCatching { file.readBytes() }.getOrElse {
            e.pending = false
            e.failedMtime = mtime
            StarredHeltix.LOGGER.error("[CustomHudTextures] не прочитать ${file.absolutePath}: ${it.message}")
            return null
        }

        // Как CustomScoreboardBackground.load: McClient.runNextTick { register(DynamicTexture(...)) }
        Minecraft.getInstance().execute {
            try {
                val image = NativeImage.read(NativeImage.Format.RGBA, ByteArrayInputStream(bytes))
                val id = Identifier.fromNamespaceAndPath("starredheltix", "hud_textures/$key")
                // register() сам закроет и вытеснит предыдущую AbstractTexture с этим id
                Minecraft.getInstance().textureManager.register(id, DynamicTexture({ "SHHudTexture/$key" }, image))
                e.mtime = mtime
                e.failedMtime = -1L
                e.handle = Handle(id, image.width, image.height)
            } catch (ex: Exception) {
                e.failedMtime = mtime // не повторяем попытку каждый кадр
                StarredHeltix.LOGGER.error("[CustomHudTextures] не загрузить ${file.absolutePath}: ${ex.message}")
            } finally {
                e.pending = false
            }
        }
        return e.handle
    }
}
