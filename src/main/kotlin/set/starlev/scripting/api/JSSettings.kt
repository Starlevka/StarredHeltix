package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.player.PlayerModelPart

/**
 * JS API: Объект `settings` — чтение и изменение клиентских настроек игры
 * (графика, звук, чат, скин). Аналог Settings API из ctjs.
 *
 * Видеонастройки для оптимизаторских модулей (renderDistance, clouds, particles,
 * entityShadows) продублированы в `utils.*` — используйте их из оптимизаторов.
 */
class JSSettings {
    private val mc: Minecraft get() = Minecraft.getInstance()
    private val options get() = mc.options

    // ==================== Общие ====================

    /** Поле зрения FOV (30-110). */
    fun getFov(): Int = options.fov().get()

    fun setFov(fov: Int) {
        mc.execute { options.fov().set(fov.coerceIn(30, 110)) }
    }

    /** Яркость (0.0 — мрак, 1.0 — ярко). */
    fun getBrightness(): Double = options.gamma().get()

    fun setBrightness(brightness: Double) {
        mc.execute { options.gamma().set(brightness.coerceIn(0.0, 1.0)) }
    }

    /** Масштаб GUI (0 — авто, 1-4+). */
    fun getGuiScale(): Int = options.guiScale().get()

    fun setGuiScale(scale: Int) {
        mc.execute { options.guiScale().set(scale.coerceIn(0, 4)); mc.resizeGui() }
    }

    // ==================== Видео ====================

    /** Максимальный FPS (10-260). */
    fun getMaxFrameRate(): Int = options.framerateLimit().get()

    fun setMaxFrameRate(fps: Int) {
        mc.execute { options.framerateLimit().set(fps.coerceIn(10, 260)) }
    }

    /** Вертикальная синхронизация. */
    fun isVsync(): Boolean = options.enableVsync().get()

    fun setVsync(enabled: Boolean) {
        mc.execute { options.enableVsync().set(enabled) }
    }

    /** Полноэкранный режим. */
    fun isFullscreen(): Boolean = mc.window.isFullscreen()

    fun setFullscreen(enabled: Boolean) {
        mc.execute {
            if (mc.window.isFullscreen() != enabled) mc.window.toggleFullScreen()
            options.fullscreen().set(enabled)
        }
    }

    /** Уровень мипмапов (0-4). */
    fun getMipmapLevels(): Int = options.mipmapLevels().get()

    fun setMipmapLevels(levels: Int) {
        mc.execute { options.mipmapLevels().set(levels.coerceIn(0, 4)) }
    }

    /** Плавное освещение. */
    fun isSmoothLighting(): Boolean = options.ambientOcclusion().get()

    fun setSmoothLighting(enabled: Boolean) {
        mc.execute { options.ambientOcclusion().set(enabled) }
    }

    /** Покачивание камеры (bobbing). */
    fun isBobbing(): Boolean = options.bobView().get()

    fun setBobbing(enabled: Boolean) {
        mc.execute { options.bobView().set(enabled) }
    }

    /** Пресет графики: "fast" | "fancy" | "fabulous" | "custom". */
    fun getGraphicsMode(): String = options.graphicsPreset().get().name.lowercase()

    fun setGraphicsMode(mode: String) {
        val preset = when (mode.lowercase()) {
            "fast" -> net.minecraft.client.GraphicsPreset.FAST
            "fancy" -> net.minecraft.client.GraphicsPreset.FANCY
            "fabulous" -> net.minecraft.client.GraphicsPreset.FABULOUS
            else -> return
        }
        mc.execute { options.graphicsPreset().set(preset) }
    }

    /** Дальность прорисовки в чанках (2-32). */
    fun getRenderDistance(): Int = options.renderDistance().get()

    fun setRenderDistance(distance: Int) {
        mc.execute { options.renderDistance().set(distance.coerceIn(2, 32)) }
    }

    /** Облака: "off" | "fast" | "fancy". */
    fun getClouds(): String = options.cloudStatus().get().name.lowercase()

    fun setClouds(mode: String) {
        val status = when (mode.lowercase()) {
            "off", "false" -> net.minecraft.client.CloudStatus.OFF
            "fast" -> net.minecraft.client.CloudStatus.FAST
            "fancy", "true" -> net.minecraft.client.CloudStatus.FANCY
            else -> return
        }
        mc.execute { options.cloudStatus().set(status) }
    }

    /** Частицы: "all" | "decreased" | "minimal". */
    fun getParticles(): String = options.particles().get().name.lowercase()

    fun setParticles(mode: String) {
        val status = when (mode.lowercase()) {
            "all" -> net.minecraft.server.level.ParticleStatus.ALL
            "decreased" -> net.minecraft.server.level.ParticleStatus.DECREASED
            "minimal" -> net.minecraft.server.level.ParticleStatus.MINIMAL
            else -> return
        }
        mc.execute { options.particles().set(status) }
    }

    /** Тени сущностей. */
    fun isEntityShadows(): Boolean = options.entityShadows().get()

    fun setEntityShadows(enabled: Boolean) {
        mc.execute { options.entityShadows().set(enabled) }
    }

    // ==================== Звук (0.0-1.0) ====================

    private fun volume(source: SoundSource): Double = options.getSoundSourceOptionInstance(source).get()

    private fun setVolume(source: SoundSource, level: Double) {
        mc.execute { options.getSoundSourceOptionInstance(source).set(level.coerceIn(0.0, 1.0)) }
    }

    fun getMasterVolume(): Double = volume(SoundSource.MASTER)
    fun setMasterVolume(level: Double) = setVolume(SoundSource.MASTER, level)

    fun getMusicVolume(): Double = volume(SoundSource.MUSIC)
    fun setMusicVolume(level: Double) = setVolume(SoundSource.MUSIC, level)

    fun getRecordsVolume(): Double = volume(SoundSource.RECORDS)
    fun setRecordsVolume(level: Double) = setVolume(SoundSource.RECORDS, level)

    fun getWeatherVolume(): Double = volume(SoundSource.WEATHER)
    fun setWeatherVolume(level: Double) = setVolume(SoundSource.WEATHER, level)

    fun getBlocksVolume(): Double = volume(SoundSource.BLOCKS)
    fun setBlocksVolume(level: Double) = setVolume(SoundSource.BLOCKS, level)

    /** Алиас к громкости блоков (общие «звуковые эффекты»). */
    fun getSoundEffectsVolume(): Double = getBlocksVolume()
    fun setSoundEffectsVolume(level: Double) = setBlocksVolume(level)

    fun getHostileVolume(): Double = volume(SoundSource.HOSTILE)
    fun setHostileVolume(level: Double) = setVolume(SoundSource.HOSTILE, level)

    fun getNeutralVolume(): Double = volume(SoundSource.NEUTRAL)
    fun setNeutralVolume(level: Double) = setVolume(SoundSource.NEUTRAL, level)

    fun getPlayersVolume(): Double = volume(SoundSource.PLAYERS)
    fun setPlayersVolume(level: Double) = setVolume(SoundSource.PLAYERS, level)

    fun getAmbientVolume(): Double = volume(SoundSource.AMBIENT)
    fun setAmbientVolume(level: Double) = setVolume(SoundSource.AMBIENT, level)

    // ==================== Чат ====================

    /** Видимость чата: "full" | "system" | "hidden". */
    fun getChatVisibility(): String = options.chatVisibility().get().name.lowercase()

    fun setChatVisibility(mode: String) {
        val visibility = when (mode.lowercase()) {
            "full" -> net.minecraft.world.entity.player.ChatVisiblity.FULL
            "system" -> net.minecraft.world.entity.player.ChatVisiblity.SYSTEM
            "hidden" -> net.minecraft.world.entity.player.ChatVisiblity.HIDDEN
            else -> return
        }
        mc.execute { options.chatVisibility().set(visibility) }
    }

    /** Цветовые коды в чате. */
    fun isChatColors(): Boolean = options.chatColors().get()

    fun setChatColors(enabled: Boolean) {
        mc.execute { options.chatColors().set(enabled) }
    }

    /** Веб-ссылки в чате кликабельны. */
    fun isChatLinks(): Boolean = options.chatLinks().get()

    fun setChatLinks(enabled: Boolean) {
        mc.execute { options.chatLinks().set(enabled) }
    }

    /** Непрозрачность фона чата (0.0-1.0). */
    fun getChatOpacity(): Double = options.chatOpacity().get()

    fun setChatOpacity(opacity: Double) {
        mc.execute { options.chatOpacity().set(opacity.coerceIn(0.0, 1.0)) }
    }

    /** Масштаб чата (0.0-1.0). */
    fun getChatScale(): Double = options.chatScale().get()

    fun setChatScale(scale: Double) {
        mc.execute { options.chatScale().set(scale.coerceIn(0.0, 1.0)) }
    }

    /** Ширина чата (0.0-1.0). */
    fun getChatWidth(): Double = options.chatWidth().get()

    fun setChatWidth(width: Double) {
        mc.execute { options.chatWidth().set(width.coerceIn(0.0, 1.0)) }
    }

    /** Высота открытого чата (0.0-1.0). */
    fun getChatFocusedHeight(): Double = options.chatHeightFocused().get()

    fun setChatFocusedHeight(height: Double) {
        mc.execute { options.chatHeightFocused().set(height.coerceIn(0.0, 1.0)) }
    }

    /** Высота свёрнутого чата (0.0-1.0). */
    fun getChatUnfocusedHeight(): Double = options.chatHeightUnfocused().get()

    fun setChatUnfocusedHeight(height: Double) {
        mc.execute { options.chatHeightUnfocused().set(height.coerceIn(0.0, 1.0)) }
    }

    /** Сокращённая отладочная информация (F3). */
    fun isReducedDebugInfo(): Boolean = options.reducedDebugInfo().get()

    fun setReducedDebugInfo(enabled: Boolean) {
        mc.execute { options.reducedDebugInfo().set(enabled) }
    }

    // ==================== Части скина ====================

    /** Видимость части скина: "cape", "jacket", "leftSleeve", "rightSleeve", "leftPants", "rightPants", "hat". */
    fun isSkinPartEnabled(part: String): Boolean = options.isModelPartEnabled(parseModelPart(part) ?: return false)

    fun setSkinPartEnabled(part: String, enabled: Boolean) {
        val modelPart = parseModelPart(part) ?: return
        mc.execute { options.setModelPart(modelPart, enabled) }
    }

    private fun parseModelPart(part: String): PlayerModelPart? = when (part.lowercase()) {
        "cape" -> PlayerModelPart.CAPE
        "jacket" -> PlayerModelPart.JACKET
        "leftsleeve", "left_sleeve" -> PlayerModelPart.LEFT_SLEEVE
        "rightsleeve", "right_sleeve" -> PlayerModelPart.RIGHT_SLEEVE
        "leftpants", "left_pants", "leftpantsleg" -> PlayerModelPart.LEFT_PANTS_LEG
        "rightpants", "right_pants", "rightpantsleg" -> PlayerModelPart.RIGHT_PANTS_LEG
        "hat" -> PlayerModelPart.HAT
        else -> null
    }
}
