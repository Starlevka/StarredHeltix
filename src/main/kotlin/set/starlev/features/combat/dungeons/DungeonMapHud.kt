package set.starlev.features.combat.dungeons

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.state.MapRenderState
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.item.MapItem
import set.starlev.StarredHeltix
import set.starlev.features.chat.ChatEventsManager
import set.starlev.features.combat.dungeons.ScoreCounter
import set.starlev.render.CustomHudTextures
import set.starlev.render.HudElement
import set.starlev.render.blitStretched
import set.starlev.render.drawSmoothRoundedPanel
import set.starlev.utils.detectors.DungeonDetector
import set.starlev.utils.detectors.TabListDetector

/**
 * Рендер карты подземелий 1в1 как в heltix-моде (HeltixMod.java):
 * карта читается из офф-хенда, decorations всегда видимы.
 * Кастомизация: текстура фона вкл/выкл, разметка (decorations) вкл/выкл.
 * Кастомная текстура фона: config/starredheltix/textures/map_background.png
 * (только PNG, любое разрешение — растягивается под структуру). Рисуется и как
 * фон HUD (за картой и склепами), и как рамка самой карты.
 * Позиция/масштаб — через HUD-редактор (HudElement), размер 1в1 = 144x144 с фоном, 128x128 без фона.
 */
object DungeonMapHud : HudElement("DungeonMapHud") {
    private val mc = Minecraft.getInstance()
    private val mapState = MapRenderState()
    private val mapBackground: Identifier = Identifier.fromNamespaceAndPath("minecraft", "textures/map/map_background.png")
    private val mapBackgroundDark: Identifier = Identifier.fromNamespaceAndPath("starredheltix", "textures/map/map_background_dark.png")
    private val mapBackgroundChecker: Identifier = Identifier.fromNamespaceAndPath("minecraft", "textures/map/map_background_checkerboard.png")

    /** Кастомный фон из конфига/папки textures — только PNG, любое разрешение. */
    private fun ensureCustomBackground(): CustomHudTextures.Handle? =
        CustomHudTextures.ensure(
            "map_background", "map_background.png",
            subfolder = "map",
            explicitPath = StarredHeltix.feature.dungeons.visuals.dungeonMap.customImagePath,
        )

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
        val scoreConfig = StarredHeltix.feature.dungeons.visuals.dungeonScore
        if (!mapConfig.enabled && !cryptsConfig.enabled && !scoreConfig.enabled) return
        if (!isEditing && !DungeonDetector.isInDungeon()) return

        val graphics = cachedGraphics ?: return

        // Вне редактора не показываем пустой худ — проверяем наличие контента заранее (карта/склепы/очки)
        if (!isEditing) {
            if (cryptsConfig.enabled) updateCrypts()
            val hasCrypts = cryptsCount != null
            val hasMap = if (mapConfig.enabled) hasMapData() else false
            val hasScore = if (scoreConfig.enabled) {
                val s = try { ScoreCounter.getCurrentScore() } catch (_: Exception) { 0 }
                s > 0
            } else false
            if (!hasCrypts && !hasMap && !hasScore) return
        } else {
            if (cryptsConfig.enabled) updateCrypts()
        }

        val mapSize = 128
        val pad = if (mapConfig.showBackgroundTexture) 8 else 0
        val totalSize = mapSize + pad * 2

        // Фон рисуем только если есть что показать или в редакторе, и уважая showBackground
        val shouldDrawMapBackground = mapConfig.enabled && (isEditing || hasMapData())
        val customStyle = mapConfig.background == set.starlev.config.categories.MapBackgroundStyle.CUSTOM
        val customHandle = if (customStyle) ensureCustomBackground() else null
        if (shouldDrawMapBackground) {
            this.showBackground = mapConfig.showBackground
            // Прозрачность фона единая для всех стилей (HUD-панель / своя картинка / рамка).
            val bgColor = set.starlev.utils.ColorUtils.alphaColor(mapConfig.backgroundAlpha)
            // Кастомная текстура — фон HUD независимо от галки «Фон HUD»:
            // выбор стиля CUSTOM сам по себе означает «рендерить картинку фоном»,
            // растяжка под структуру (totalSize x totalSize) при любом разрешении файла.
            if (customHandle != null) {
                graphics.blitStretched(
                    customHandle.id, x, y, totalSize, totalSize, customHandle.srcW, customHandle.srcH, bgColor
                )
            } else if (showBackground) {
                // Гладкий скруглённый фон вместо резкого fill (как drawBackground, но smooth)
                graphics.drawSmoothRoundedPanel(x - 4, y - 4, totalSize + 8, totalSize + 8, bgColor, 4)
            }
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

        // Склепы и очки — уменьшенный текст (0.65x) и привязаны к карте
        val smallScale = 0.65f
        var extraY = 0
        if (cryptsConfig.enabled) {
            val count = cryptsCount
            if (count != null || isEditing) {
                val n = count ?: 5
                val color = if (n >= 5) "§a" else "§c"
                val text = "§7Склепы: $color$n"
                val textY = y + totalSize + 4 + extraY
                // Фон под уменьшенным текстом — считаем с учётом скейла
                if (cryptsConfig.showBackground) {
                    val w = (mc.font.width(text) * smallScale).toInt() + 8
                    val h = (mc.font.lineHeight * smallScale).toInt() + 4
                    graphics.drawSmoothRoundedPanel(x, textY - 1, w, h + 1, 0x70000000, 3)
                }
                val pose = graphics.pose()
                pose.pushMatrix()
                pose.translate(x + 4f, textY.toFloat())
                pose.scale(smallScale, smallScale)
                pose.translate(-(x + 4f), -textY.toFloat())
                graphics.text(mc.font, text, x + 4, textY, 0xFFFFFFFF.toInt(), true)
                pose.popMatrix()
                extraY += (mc.font.lineHeight * smallScale).toInt() + 4
            }
        }
        val scoreCfg = StarredHeltix.feature.dungeons.visuals.dungeonScore
        if (scoreCfg.enabled) {
            val score = try { ScoreCounter.getCurrentScore() } catch (_: Exception) { 0 }
            if (score > 0 || isEditing) {
                val s = if (isEditing && score == 0) 300 else score
                val sColor = when {
                    s >= 300 -> "§6"
                    s >= 270 -> "§a"
                    else -> "§c"
                }
                val textScore = "§7Очков: $sColor$s"
                val textY2 = y + totalSize + 4 + extraY
                if (scoreCfg.showBackground) {
                    val w2 = (mc.font.width(textScore) * smallScale).toInt() + 8
                    val h2 = (mc.font.lineHeight * smallScale).toInt() + 4
                    graphics.drawSmoothRoundedPanel(x, textY2 - 1, w2, h2 + 1, 0x70000000, 3)
                }
                val pose2 = graphics.pose()
                pose2.pushMatrix()
                pose2.translate(x + 4f, textY2.toFloat())
                pose2.scale(smallScale, smallScale)
                pose2.translate(-(x + 4f), -textY2.toFloat())
                graphics.text(mc.font, textScore, x + 4, textY2, 0xFFFFFFFF.toInt(), true)
                pose2.popMatrix()
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
                val bgColor = set.starlev.utils.ColorUtils.alphaColor(mapConfig.backgroundAlpha)
                val handle = if (style == set.starlev.config.categories.MapBackgroundStyle.CUSTOM) ensureCustomBackground() else null
                if (handle != null) {
                    // Рамка карты: файл любого разрешения растягивается в квадрат 144x144
                    graphics.blitStretched(
                        handle.id, 0, 0, 144, 144, handle.srcW, handle.srcH, bgColor
                    )
                } else {
                    val bg = when (style) {
                        set.starlev.config.categories.MapBackgroundStyle.DARK -> mapBackgroundDark
                        set.starlev.config.categories.MapBackgroundStyle.CHECKER -> mapBackgroundChecker
                        else -> mapBackground
                    }
                    graphics.blit(
                        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                        bg, 0, 0, 0.0f, 0.0f,
                        144, 144, 144, 144, 144, 144, bgColor
                    )
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
