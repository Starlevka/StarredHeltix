package set.starlev.features.combat.dungeons

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.state.MapRenderState
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.item.MapItem
import set.starlev.StarredHeltix
import set.starlev.features.chat.ChatEventsManager
import set.starlev.render.HudElement
import set.starlev.utils.detectors.DungeonDetector
import set.starlev.utils.detectors.TabListDetector

/**
 * Рендер карты подземелий 1в1 как в heltix-моде (HeltixMod.java):
 * карта читается из офф-хенда, decorations всегда видимы.
 * Кастомизация: текстура фона вкл/выкл, разметка (decorations) вкл/выкл.
 * Позиция/масштаб — через HUD-редактор (HudElement), размер 1в1 = 144x144 с фоном, 128x128 без фона.
 */
object DungeonMapHud : HudElement("DungeonMapHud") {
    private val mc = Minecraft.getInstance()
    private val mapState = MapRenderState()
    private val mapBackground: Identifier = Identifier.fromNamespaceAndPath("minecraft", "textures/map/map_background.png")
    private val mapBackgroundDark: Identifier = Identifier.fromNamespaceAndPath("starredheltix", "textures/map/map_background_dark.png")
    private val mapBackgroundChecker: Identifier = Identifier.fromNamespaceAndPath("minecraft", "textures/map/map_background_checkerboard.png")
    private val mapBackgroundCustom: Identifier = Identifier.fromNamespaceAndPath("starredheltix", "dungeon_map/background_custom")

    // Кастомный фон: config/starredheltix/textures/map/map_background.png — DynamicTexture,
    // перезагружается по lastModified. srcW/H — реальные размеры файла (для blit-растяжки).
    private var customSrcW = 0
    private var customSrcH = 0
    private var customLoadedMtime = -1L
    private var customRegistered = false

    /** @return true, если файл есть и текстурa загружена; иначе false (фолбэк на ванильную). */
    private fun ensureCustomBackground(): Boolean {
        val file = java.io.File(mc.gameDirectory, "config/starredheltix/textures/map/map_background.png")
        if (!file.isFile) return false
        val mtime = file.lastModified()
        if (customRegistered && mtime == customLoadedMtime) return true
        return try {
            val bytes = file.readBytes()
            val image = com.mojang.blaze3d.platform.NativeImage.read(bytes)
            customSrcW = image.width
            customSrcH = image.height
            val tm = mc.textureManager
            if (customRegistered) {
                try {
                    tm.getTexture(mapBackgroundCustom).close()
                    tm.release(mapBackgroundCustom)
                } catch (_: Exception) {}
            }
            tm.register(mapBackgroundCustom, net.minecraft.client.renderer.texture.DynamicTexture({ "SHDungeonMapBg" }, image))
            customRegistered = true
            customLoadedMtime = mtime
            true
        } catch (e: Exception) {
            set.starlev.StarredHeltix.LOGGER.error("[DungeonMap] Не удалось загрузить кастомный фон: ${e.message}")
            false
        }
    }

    private var cryptsCount: Int? = null
    private var lastCryptsUpdate = 0L

    // Поддержка: Склепы: N, Склепов: N, Crypts: N, Crypt: N, с возможными цветами и суффиксами типа "5/5"
    private val cryptPattern = Regex("(?i)(?:склеп[а-яё]*|crypts?)\\s*:?\\s*(\\d+)")
    private val strippedPattern = Regex("(?i)§[0-9a-fk-orlnmx]")

    fun init() {
        ChatEventsManager.registerIncoming { _ ->
            lastCryptsUpdate = 0L
        }
    }

    override fun render() {
        val mapConfig = StarredHeltix.feature.dungeons.visuals.dungeonMap
        val cryptsConfig = StarredHeltix.feature.dungeons.visuals.crypts
        if (!mapConfig.enabled && !cryptsConfig.enabled) return
        if (!isEditing && StarredHeltix.feature.dungeons.visuals.onlyInDungeons && !DungeonDetector.isInDungeon()) return

        val graphics = cachedGraphics ?: return

        // Вне редактора не показываем пустой худ (только фон) — проверяем наличие контента заранее
        if (!isEditing) {
            if (cryptsConfig.enabled) updateCrypts()
            val hasCrypts = cryptsCount != null
            val hasMap = if (mapConfig.enabled) hasMapData() else false
            if (!hasCrypts && !hasMap) return
        } else {
            if (cryptsConfig.enabled) updateCrypts()
        }

        val mapSize = 128
        val pad = if (mapConfig.showBackgroundTexture) 8 else 0
        val totalSize = mapSize + pad * 2

        // Фон рисуем только если есть что показать или в редакторе, и уважая showBackground
        val shouldDrawMapBackground = mapConfig.enabled && (isEditing || hasMapData())
        if (shouldDrawMapBackground) {
            this.showBackground = mapConfig.showBackground
            if (showBackground) drawBackground(totalSize, totalSize)
        } else if (cryptsConfig.enabled && cryptsCount != null) {
            // Если карты нет, но есть склепы — фон карты не рисуем, только фон склепов (отдельно ниже)
            this.showBackground = false
        }

        if (mapConfig.enabled) {
            if (isEditing) {
                graphics.fill(x + pad, y + pad, x + pad + mapSize, y + pad + mapSize, 0xFF0A2A12.toInt())
                graphics.fill(x + pad, y + pad, x + pad + mapSize, y + pad + 1, 0xFF55FF55.toInt())
                graphics.fill(x + pad, y + pad + mapSize - 1, x + pad + mapSize, y + pad + mapSize, 0xFF55FF55.toInt())
                graphics.fill(x + pad, y + pad, x + pad + 1, y + pad + mapSize, 0xFF55FF55.toInt())
                graphics.fill(x + pad + mapSize - 1, y + pad, x + pad + mapSize, y + pad + mapSize, 0xFF55FF55.toInt())
            } else {
                renderMap(graphics, mapSize, pad)
            }
        }

        if (cryptsConfig.enabled) {
            val count = cryptsCount
            if (count != null || isEditing) {
                val n = count ?: 5
                val color = if (n >= 5) "§a" else "§c"
                val text = "§7Склепы: $color$n"
                val textY = y + totalSize + 4
                if (cryptsConfig.showBackground) {
                    graphics.fill(x, textY - 2, x + mc.font.width(text) + 8, textY + mc.font.lineHeight + 2, 0x70000000.toInt())
                }
                val g2 = cachedGraphics ?: return
                g2.text(mc.font, text, x + 4, textY, 0xFFFFFFFF.toInt(), true)
            }
        }
    }

    private fun hasMapData(): Boolean {
        return try {
            val player = mc.player ?: return false
            val level = mc.level ?: return false
            val stack = player.offhandItem
            val mapId = stack.get(DataComponents.MAP_ID) ?: return false
            MapItem.getSavedData(mapId, level) != null
        } catch (_: Exception) { false }
    }

    private fun renderMap(graphics: net.minecraft.client.gui.GuiGraphicsExtractor, mapSize: Int, pad: Int) {
        val player = mc.player ?: return
        val level = mc.level ?: return
        val mapConfig = StarredHeltix.feature.dungeons.visuals.dungeonMap
        try {
            val stack = player.offhandItem
            val mapId = stack.get(DataComponents.MAP_ID) ?: return
            val state = MapItem.getSavedData(mapId, level) ?: return
            mc.mapRenderer.extractRenderState(mapId, state, mapState)
            if (!mapConfig.showDecorations) {
                mapState.decorations.clear()
            } else {
                for (decoration in mapState.decorations) {
                    decoration.renderOnFrame = true
                }
            }
            val pose = graphics.pose()
            pose.pushMatrix()
            pose.translate(x.toFloat(), y.toFloat())
            if (mapConfig.showBackgroundTexture) {
                val style = mapConfig.background
                val useCustom = style == set.starlev.config.categories.MapBackgroundStyle.CUSTOM && ensureCustomBackground()
                when {
                    useCustom -> {
                        // Растягиваем файл произвольного размера в квадрат 144x144
                        graphics.blit(
                            net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                            mapBackgroundCustom, 0, 0, 0.0f, 0.0f,
                            144, 144, customSrcW, customSrcH, customSrcW, customSrcH
                        )
                    }
                    else -> {
                        val bg = when (style) {
                            set.starlev.config.categories.MapBackgroundStyle.DARK -> mapBackgroundDark
                            set.starlev.config.categories.MapBackgroundStyle.CHECKER -> mapBackgroundChecker
                            else -> mapBackground
                        }
                        graphics.blit(
                            net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                            bg, 0, 0, 0.0f, 0.0f,
                            144, 144, 144, 144
                        )
                    }
                }
                pose.translate(pad.toFloat(), pad.toFloat())
            }
            graphics.map(mapState)
            pose.popMatrix()
        } catch (_: Exception) {
        }
    }

    private fun updateCrypts() {
        val now = System.currentTimeMillis()
        if (now - lastCryptsUpdate < 500L) return
        lastCryptsUpdate = now
        try {
            // 1) TabList — пробуем все источники (clean, formatted, header/footer)
            val tabLines = mutableListOf<String>()
            try { tabLines.addAll(TabListDetector.getAllTabListLines()) } catch (_: Exception) {}
            try { tabLines.addAll(TabListDetector.getAllTabListLinesFormatted().map { TabListDetector.cleanLine(it) }) } catch (_: Exception) {}
            try { tabLines.addAll(TabListDetector.getHeaderAndFooterLines()) } catch (_: Exception) {}
            // Также пробуем напрямую сырые компоненты таб-листа (с форматированием) — чистим сами
            try {
                val rawFormatted = TabListDetector.getAllTabListLinesFormatted()
                for (raw in rawFormatted) {
                    val cleaned = raw.replace(strippedPattern, "").trim()
                    if (cleaned.isNotEmpty() && cleaned !in tabLines) tabLines.add(cleaned)
                }
            } catch (_: Exception) {}

            for (line in tabLines) {
                val stripped = line.replace(strippedPattern, "").trim()
                val m = cryptPattern.find(stripped) ?: cryptPattern.find(line) ?: continue
                val num = m.groupValues[1].toIntOrNull()
                if (num != null) {
                    cryptsCount = num
                    return
                }
            }

            // 2) Фолбэк: Scoreboard (на Heltix иногда данные дублируются)
            try {
                val sbLines = set.starlev.utils.detectors.ScoreboardDetector.getScoreboardText()
                for (line in sbLines) {
                    val stripped = line.replace(strippedPattern, "").trim()
                    val m = cryptPattern.find(stripped) ?: continue
                    val num = m.groupValues[1].toIntOrNull()
                    if (num != null) {
                        cryptsCount = num
                        return
                    }
                }
            } catch (_: Exception) {}

            // 3) Последний шанс: ищем в любом месте строк таб-листа подстроку "Склеп" + число рядом
            for (line in tabLines) {
                val lower = line.lowercase()
                if (lower.contains("склеп") || lower.contains("crypt")) {
                    val digits = Regex("(\\d+)").findAll(line).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
                    if (digits.isNotEmpty()) {
                        // Берём первое число после слова "склеп"
                        val idxWord = lower.indexOf("склеп").takeIf { it >= 0 } ?: lower.indexOf("crypt")
                        val afterWord = if (idxWord >= 0) line.substring(idxWord) else line
                        val m = Regex("(\\d+)").find(afterWord)
                        m?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                            cryptsCount = it
                            return
                        }
                        cryptsCount = digits.first()
                        return
                    }
                }
            }

            cryptsCount = null
        } catch (_: Exception) {
            cryptsCount = null
        }
    }

    fun getCryptsCount(): Int? = cryptsCount

    override fun getWidth(): Int = 144
    override fun getHeight(): Int = 160
    override fun getDefaultX(): Int = 10
    override fun getDefaultY(): Int = 10
    override fun getDefaultScale(): Float = 1.0f
}
