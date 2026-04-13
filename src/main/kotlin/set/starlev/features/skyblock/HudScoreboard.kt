package set.starlev.features.skyblock

import com.google.gson.GsonBuilder
import net.minecraft.client.Minecraft
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.abs

object HudScoreboard : HudElement("Scoreboard") {
    private val mc = Minecraft.getInstance()
    private var anchorSide: AnchorSide = AnchorSide.RIGHT
    private var anchoredX: Float? = null
    private var anchoredScreenWidth: Int? = null
    private var anchoredContentWidth: Float? = null
    private var anchoredScale: Float? = null
    private var cachedLines: List<ScoreboardLine>? = null

    enum class AnchorSide {
        LEFT,
        RIGHT
    }

    enum class CustomLineAnchor {
        AFTER_XP,
        BOTTOM
    }

    data class ScoreboardLine(
        val key: String,
        val component: Component,
        val centered: Boolean,
        val aliases: List<String> = emptyList()
    )

    data class CustomLineLayout(
        val id: String,
        var anchor: CustomLineAnchor,
        var order: Int
    )

    object CustomLinesLayoutStore {
        private val gson = GsonBuilder().setPrettyPrinting().create()
        private val file: Path = Paths.get(System.getProperty("user.dir"), "config", "starredheltix", "scoreboard-custom-lines.json")
        private val layoutById = mutableMapOf<String, CustomLineLayout>()

        fun getLayout(id: String): CustomLineLayout {
            return layoutById[id] ?: defaultLayout().also { layoutById[id] = it }
        }

        fun getAllLayouts(): List<CustomLineLayout> {
            ensureLoaded()
            val defaults = listOf("slayer", "fps", "ping", "cps", "bps", "gems", "bank", "cookie")
            for (id in defaults) {
                if (!layoutById.containsKey(id)) {
                    layoutById[id] = defaultLayoutFor(id)
                }
            }
            return layoutById.values.sortedWith(compareBy<CustomLineLayout> { it.anchor.name }.thenBy { it.order }.thenBy { it.id })
        }

        fun setAllLayouts(newLayouts: List<CustomLineLayout>) {
            ensureLoaded()
            layoutById.clear()
            for (l in newLayouts) {
                layoutById[l.id] = l
            }
        }

        fun save() {
            ensureLoaded()
            try {
                Files.createDirectories(file.parent)
                Files.writeString(file, gson.toJson(layoutById.values.sortedBy { it.id }))
            } catch (_: Exception) {
            }
        }

        private fun defaultLayout(): CustomLineLayout = defaultLayoutFor("fps")

        private fun defaultLayoutFor(id: String): CustomLineLayout {
            return when (id) {
                "slayer" -> CustomLineLayout("slayer", CustomLineAnchor.AFTER_XP, 0)
                "fps" -> CustomLineLayout("fps", CustomLineAnchor.BOTTOM, 0)
                "ping" -> CustomLineLayout("ping", CustomLineAnchor.BOTTOM, 1)
                "cps" -> CustomLineLayout("cps", CustomLineAnchor.BOTTOM, 2)
                "bps" -> CustomLineLayout("bps", CustomLineAnchor.BOTTOM, 3)
                "gems" -> CustomLineLayout("gems", CustomLineAnchor.BOTTOM, 4)
                "bank" -> CustomLineLayout("bank", CustomLineAnchor.BOTTOM, 5)
                "cookie" -> CustomLineLayout("cookie", CustomLineAnchor.BOTTOM, 6)
                else -> CustomLineLayout(id, CustomLineAnchor.BOTTOM, 99)
            }
        }

        private var loaded = false
        private fun ensureLoaded() {
            if (loaded) return
            loaded = true
            try {
                if (!Files.exists(file)) return
                val json = Files.readString(file)
                val type = object : com.google.gson.reflect.TypeToken<List<CustomLineLayout>>() {}.type
                val list: List<CustomLineLayout> = gson.fromJson(json, type) ?: return
                for (l in list) {
                    layoutById[l.id] = l
                }
            } catch (_: Exception) {
            }
        }
    }

    object ScoreboardLinesOrderStore {
        private val gson = GsonBuilder().setPrettyPrinting().create()
        private val file: Path = Paths.get(System.getProperty("user.dir"), "config", "starredheltix", "scoreboard-lines-order.json")
        private var loaded = false
        private val order = mutableListOf<String>()

        // Запоминаем предыдущие строки для отслеживания изменений
        private var previousLines: List<ScoreboardLine>? = null
        private val contentToPosition = mutableMapOf<String, Int>() // content hash -> position index

        fun getOrder(): List<String> {
            ensureLoaded()
            return order.toList()
        }

        fun setOrder(newOrder: List<String>) {
            ensureLoaded()
            order.clear()
            order.addAll(newOrder)
        }

        fun save() {
            ensureLoaded()
            try {
                Files.createDirectories(file.parent)
                Files.writeString(file, gson.toJson(order))
            } catch (_: Exception) {
            }
        }

        /**
        * Отслеживает изменения строк и сохраняет правильный порядок.
        * Если строка изменила содержимое, она остаётся на своём месте.
        * Если строка новая, она вставляется рядом с похожей или в конец.
        */
        fun trackAndReorder(lines: List<ScoreboardLine>): List<ScoreboardLine> {
            val prev = previousLines
            if (prev == null) {
                previousLines = lines.toList()
                updateContentMap(lines)
                return lines
            }

            // Строим карту: позиция -> строка из предыдущего кадра
            val prevByPosition = prev.mapIndexed { idx, line -> idx to line }.toMap()
            val usedKeys = mutableSetOf<String>()
            val result = mutableListOf<ScoreboardLine>()

            // Проходим по предыдущим позициям и ищем соответствующие строки
            for ((prevIdx, prevLine) in prevByPosition) {
                // 1. Ищем точное совпадение по ключу
                val exactMatch = lines.find { it.key == prevLine.key && it.key !in usedKeys }
                if (exactMatch != null) {
                    result.add(exactMatch)
                    usedKeys.add(exactMatch.key)
                    continue
                }

                // 2. Ищем совпадение по алиасам
                val aliasMatch = lines.find { line ->
                    line.key !in usedKeys && prevLine.aliases.any { alias -> alias == line.key }
                }
                if (aliasMatch != null) {
                    result.add(aliasMatch)
                    usedKeys.add(aliasMatch.key)
                    continue
                }

                // 3. Ищем по содержимому (текст без форматирования) - строка изменилась, но это та же позиция
                val prevText = prevLine.component.string.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
                val contentMatch = lines.find { line ->
                    if (line.key in usedKeys) return@find false
                    val lineText = line.component.string.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
                    // Сравниваем начало строки (до чисел) - если совпадает, это та же строка с обновлённым значением
                    val prevPrefix = prevText.takeWhile { !it.isDigit() }.trim()
                    val linePrefix = lineText.takeWhile { !it.isDigit() }.trim()
                    prevPrefix.isNotEmpty() && prevPrefix == linePrefix
                }
                if (contentMatch != null) {
                    result.add(contentMatch)
                    usedKeys.add(contentMatch.key)
                    continue
                }

                // 4. Строка исчезла - пропускаем позицию
            }

            // Добавляем новые строки, которых не было раньше
            for (line in lines) {
                if (line.key !in usedKeys) {
                    result.add(line)
                    usedKeys.add(line.key)
                }
            }

            previousLines = result.toList()
            updateContentMap(result)
            return result
        }

        private fun updateContentMap(lines: List<ScoreboardLine>) {
            contentToPosition.clear()
            lines.forEachIndexed { idx, line ->
                val text = line.component.string.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
                contentToPosition[text] = idx
            }
        }

        fun reset() {
            previousLines = null
            contentToPosition.clear()
        }

        private fun ensureLoaded() {
            if (loaded) return
            loaded = true
            try {
                if (!Files.exists(file)) return
                val json = Files.readString(file)
                val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                val list: List<String> = gson.fromJson(json, type) ?: return
                order.clear()
                order.addAll(list)
            } catch (_: Exception) {
            }
        }
    }

    fun getAdjustedX(): Int {
        ensureInitialized()
        return x
    }

    fun getAdjustedY(): Int {
        ensureInitialized()
        return y
    }

    fun getEditorLines(): List<ScoreboardLine> {
        ensureInitialized()
        val base = buildBaseLines()
        if (base.isEmpty()) return emptyList()
        return applyStoredOrder(base)
    }

    fun setEditorOrder(newOrder: List<String>) {
        ScoreboardLinesOrderStore.setOrder(newOrder)
        ScoreboardLinesOrderStore.save()
    }

    fun resetEditorOrder() {
        ScoreboardLinesOrderStore.setOrder(emptyList())
        ScoreboardLinesOrderStore.save()
    }

    private fun calculateSize(): Pair<Int, Int> {
        val padding = 2
        val lines = buildOrderedLines()
        if (lines.isEmpty()) {
            return 120 to 160
        }
        val maxWidth = lines.maxOf { mc.font.width(it.component) }
        val totalHeight = lines.size * 9
        return (maxWidth + padding * 2) to (totalHeight + padding * 2)
    }

    private fun buildCustomLines(): List<ScoreboardLine> {
        val scoreboardCfg = StarredHeltix.feature.skyblock.scoreboard

        // Получаем порядок элементов из конфига (ConfigEditorDraggableList)
        val orderedElements = scoreboardCfg.scoreboardEntries.toList()

        val out = mutableListOf<ScoreboardLine>()
        for (configElement in orderedElements) {
            if (configElement == null) continue
            val element = configElement.element
            if (!element.showWhen()) continue
            if (!element.showIsland()) continue

            val lines = element.getDisplay()
            if (lines.isEmpty()) continue

            // Фильтрация последовательных пустых строк
            if (lines.first().component.string.isEmpty() && out.lastOrNull()?.component?.string?.isEmpty() == true) continue

            out.addAll(lines)
        }
        return out
    }

    private fun buildOrderedLines(): List<ScoreboardLine> {
        val base = buildBaseLines()
        if (base.isEmpty()) return emptyList()
        val ordered = applyStoredOrder(base)
        return ordered
    }

    private fun buildBaseLines(): List<ScoreboardLine> {
        val scoreboard = mc.level?.scoreboard
        val objective = scoreboard?.getDisplayObjective(DisplaySlot.SIDEBAR)
        if (objective == null) {
            if (!isEditing) return emptyList()
            return listOf(
                ScoreboardLine("title", Component.literal("§6§lScoreboard"), centered = true),
                ScoreboardLine("placeholder:1", Component.literal("§7(Placeholder)"), centered = false),
                ScoreboardLine("placeholder:2", Component.literal("§eМасштаб: ${(scale * 100).toInt()}%"), centered = false)
            )
        }

        val lines = mutableListOf<ScoreboardLine>()
        val processedTitle = set.starlev.secret.features.SecretFunFeatures.processComponent(objective.displayName, true)
        lines.add(ScoreboardLine("title", processedTitle, centered = true))
        val scores = scoreboard.listPlayerScores(objective).sortedByDescending { it.value }
        val valueOrdinal = HashMap<Int, Int>()
        var didInsertAfterXp = false
        for (score in scores) {
            val owner = score.owner
            val team = scoreboard.getPlayersTeam(owner)
            var lineComponent = score.display ?: if (team != null) {
                PlayerTeam.formatNameForTeam(team, Component.literal(owner))
            } else {
                Component.literal(owner)
            }
            lineComponent = set.starlev.secret.features.SecretFunFeatures.processComponent(lineComponent, true)

            val legacyKey = "score:$owner"
            val stableKey = if (team != null) {
                "scoreTeam:${team.name}"
            } else {
                val current = valueOrdinal[score.value] ?: 0
                valueOrdinal[score.value] = current + 1
                "scoreValue:${score.value}:$current"
            }
            lines.add(
                ScoreboardLine(
                    stableKey,
                    lineComponent,
                    centered = false,
                    aliases = listOf(legacyKey)
                )
            )

            if (!didInsertAfterXp) {
                val text = lineComponent.string
                if (text.contains("/") && (text.contains("опыта") || text.contains("XP") || text.contains("опыта Боя"))) {
                    lines.addAll(buildCustomLines())
                    didInsertAfterXp = true
                }
            }
        }

        if (!didInsertAfterXp) {
            lines.addAll(buildCustomLines())
        }
        return lines
    }

    private fun applyStoredOrder(base: List<ScoreboardLine>): List<ScoreboardLine> {
        val stored = ScoreboardLinesOrderStore.getOrder()
        if (stored.isEmpty()) return base

        val anyKeyToLine = HashMap<String, ScoreboardLine>(base.size * 2)
        for (line in base) {
            anyKeyToLine[line.key] = line
            for (alias in line.aliases) {
                anyKeyToLine.putIfAbsent(alias, line)
            }
        }
        val used = HashSet<String>(base.size)
        val out = ArrayList<ScoreboardLine>(base.size)
        for (k in stored) {
            val line = anyKeyToLine[k] ?: continue
            if (used.add(line.key)) out.add(line)
        }
        // Новые строки вставляем рядом с похожими, а не в конец
        val newLines = base.filter { it.key !in used }
        for (newLine in newLines) {
            val insertIdx = findBestInsertPosition(out, newLine)
            out.add(insertIdx, newLine)
            used.add(newLine.key)
        }
        return out
    }

    /**
        * Ищет лучшую позицию для вставки новой строки.
        * Сравнивает начало текста (до чисел) с существующими строками.
        */
    private fun findBestInsertPosition(existing: List<ScoreboardLine>, newLine: ScoreboardLine): Int {
        val newText = newLine.component.string.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
        val newPrefix = newText.takeWhile { !it.isDigit() }.trim()
        if (newPrefix.isEmpty()) return existing.size

        for (i in existing.indices) {
            val existText = existing[i].component.string.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
            val existPrefix = existText.takeWhile { !it.isDigit() }.trim()
            if (existPrefix.isNotEmpty() && existPrefix == newPrefix) {
                return i + 1 // Вставляем сразу после похожей строки
            }
        }
        return existing.size
    }

    override fun renderWithGraphics(graphics: net.minecraft.client.gui.GuiGraphics) {
        ensureInitialized()
        cachedGraphics = graphics
        
        // 1. Подготовка данных и расчет ширины
        val lines = buildOrderedLines()
        cachedLines = lines
        
        if (lines.isNotEmpty()) {
            val padding = 2
            val maxWidth = lines.maxOf { mc.font.width(it.component) }
            val width = maxWidth + padding * 2
            val currentWidth = width * scale
            val screenWidth = mc.window.guiScaledWidth
            val height = lines.size * 9 + padding * 2
            val currentHeight = height * scale
            val screenHeight = mc.window.guiScaledHeight

            // Если экран не инициализирован, ничего не меняем и не рендерим
            if (screenWidth <= 0 || screenHeight <= 0) return

            // Если мы в режиме редактирования, позволяем выходить за края (для удобства)
            // Но если нет - удерживаем в пределах экрана, НЕ меняя оригинальные x/y навсегда
            if (!isEditing) {
                val maxX = (screenWidth - currentWidth).coerceAtLeast(0f).toInt()
                val maxY = (screenHeight - currentHeight).coerceAtLeast(0f).toInt()
                
                val oldX = x
                val oldY = y
                
                // Временно меняем x/y для super.renderWithGraphics
                x = x.coerceIn(0, maxX)
                y = y.coerceIn(0, maxY)
                
                super.renderWithGraphics(graphics)
                
                // Возвращаем как было, чтобы не портить сохраненную позицию
                x = oldX
                y = oldY
                return
            }
        }

        super.renderWithGraphics(graphics)
    }

    override fun render() {
        val config = StarredHeltix.feature.skyblock.scoreboard
        if (!config.enabled) return

        val graphics = cachedGraphics ?: return
        // Используем кэшированные линии, чтобы не пересчитывать
        val lines = cachedLines ?: return
        if (lines.isEmpty()) return

        val padding = 2
        val maxWidth = lines.maxOf { mc.font.width(it.component) }
        val width = maxWidth + padding * 2
        val height = lines.size * 9 + padding * 2

        this.showBackground = config.showBackground
        
        drawBackground(width, height, 0, shadow = false, shadowBottom = false)

        var currentY = y + padding
        for (line in lines) {
            val drawX = if (line.centered) {
                x + (width - mc.font.width(line.component)) / 2
            } else {
                x + padding
            }
            graphics.drawString(mc.font, line.component, drawX, currentY, 0xFFFFFFFF.toInt())
            currentY += 9
        }
    }

    override fun getWidth(): Int = calculateSize().first

    override fun getHeight(): Int = calculateSize().second

    override fun getDefaultX(): Int {
        val screenWidth = mc.window.guiScaledWidth
        if (screenWidth <= 0) return 500 // Разумное дефолтное значение если окно еще не готово
        val baseSize = calculateSize()
        return screenWidth - baseSize.first - 3
    }

    override fun getDefaultY(): Int {
        val screenHeight = mc.window.guiScaledHeight
        if (screenHeight <= 0) return 100
        val scoreboard = mc.level?.scoreboard
        val objective = scoreboard?.getDisplayObjective(DisplaySlot.SIDEBAR)
        val scoresCount = if (objective != null) scoreboard.listPlayerScores(objective).size else 15
        val totalHeight = (scoresCount + 1) * 9
        val startY = screenHeight / 2 + totalHeight / 3
        return startY - totalHeight - 1
    }

    fun init() {
        // Загрузка сохраненных координат происходит автоматически через HudManager
    }
}
