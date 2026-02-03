package set.starlev.features.skyblock

import com.google.gson.GsonBuilder
import net.minecraft.client.Minecraft
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.features.misc.info.StatsTracker
import set.starlev.hud.HudElement
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.abs

object HudScoreboard : HudElement("Scoreboard") {
    private val mc = Minecraft.getInstance()
    private var anchorSide: AnchorSide = AnchorSide.RIGHT
    private var anchoredX: Float? = null
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
        val includeSlayer = StarredHeltix.feature.slayer.slayerHud.slayerScoreboardHud
        return buildOrderedLines(includeSlayer = includeSlayer)
    }

    fun setEditorOrder(newOrder: List<String>) {
        ScoreboardLinesOrderStore.setOrder(newOrder)
        ScoreboardLinesOrderStore.save()
    }

    fun resetEditorOrder() {
        ScoreboardLinesOrderStore.setOrder(emptyList())
        ScoreboardLinesOrderStore.save()
    }

    private fun calculateSize(includeSlayer: Boolean = true): Pair<Int, Int> {
        val padding = 2
        val lines = buildOrderedLines(includeSlayer = includeSlayer)
        if (lines.isEmpty()) {
            return 120 to 160
        }
        val maxWidth = lines.maxOf { mc.font.width(it.component) }
        val totalHeight = lines.size * 9
        return (maxWidth + padding * 2) to (totalHeight + padding * 2)
    }

    private fun buildCustomLines(includeSlayer: Boolean, anchor: CustomLineAnchor): List<ScoreboardLine> {
        val cfg = StarredHeltix.feature
        val statsCfg = cfg.misc.general.hudStats
        val scoreboardCfg = cfg.skyblock.scoreboard
        val slayerEnabled = includeSlayer && cfg.slayer.slayerHud.slayerScoreboardHud

        val enabledIds = buildSet {
            if (slayerEnabled) add("slayer")
            if (statsCfg.fps.scoreboard) add("fps")
            if (statsCfg.ping.scoreboard) add("ping")
            if (statsCfg.cps.scoreboard) add("cps")
            if (statsCfg.bps.scoreboard) add("bps")
            if (scoreboardCfg.gems.scoreboard) add("gems")
            if (scoreboardCfg.bank.scoreboard) add("bank")
            if (scoreboardCfg.cookie.scoreboard) add("cookie")
        }
        if (enabledIds.isEmpty()) return emptyList()

        val layouts = CustomLinesLayoutStore.getAllLayouts()
            .filter { enabledIds.contains(it.id) && it.anchor == anchor }
            .sortedBy { it.order }

        val out = mutableListOf<ScoreboardLine>()
        for (l in layouts) {
            when (l.id) {
                "slayer" -> {
                    val lines = set.starlev.features.combat.slayer.SlayerScoreboard.getExtraLines()
                    for (i in lines.indices) {
                        val processed = set.starlev.secret.features.SecretFunFeatures.processComponent(lines[i], true)
                        out.add(ScoreboardLine("custom:slayer:$i", processed, centered = false))
                    }
                }
                "fps" -> out.add(
                    ScoreboardLine(
                        "custom:fps",
                        set.starlev.secret.features.SecretFunFeatures.processComponent(Component.literal("§7FPS: §a${mc.fps}"), true),
                        centered = false
                    )
                )
                "ping" -> {
                    val ping = mc.player?.let { mc.connection?.getPlayerInfo(it.uuid)?.latency }
                    out.add(
                        ScoreboardLine(
                            "custom:ping",
                            set.starlev.secret.features.SecretFunFeatures.processComponent(Component.literal("§7Ping: §a${ping ?: 0}ms"), true),
                            centered = false
                        )
                    )
                }
                "cps" -> out.add(
                    ScoreboardLine(
                        "custom:cps",
                        set.starlev.secret.features.SecretFunFeatures.processComponent(Component.literal("§7CPS: §a${StatsTracker.getCps()}"), true),
                        centered = false
                    )
                )
                "bps" -> out.add(
                    ScoreboardLine(
                        "custom:bps",
                        set.starlev.secret.features.SecretFunFeatures.processComponent(Component.literal("§7BPS: §a${StatsTracker.getBps()}"), true),
                        centered = false
                    )
                )
                "gems" -> {
                    val gemsLine = set.starlev.utils.detectors.TabListDetector.getGemsLine()
                    if (gemsLine != null) {
                        out.add(
                            ScoreboardLine(
                                "custom:gems",
                                set.starlev.secret.features.SecretFunFeatures.processComponent(Component.literal(gemsLine), true),
                                centered = false
                            )
                        )
                    }
                }
                "bank" -> {
                    val bankLine = set.starlev.utils.detectors.TabListDetector.getBankLine()
                    if (bankLine != null) {
                        out.add(
                            ScoreboardLine(
                                "custom:bank",
                                set.starlev.secret.features.SecretFunFeatures.processComponent(Component.literal(bankLine), true),
                                centered = false
                            )
                        )
                    }
                }
                "cookie" -> {
                    // Проверяем статус в скорборде и в табе
                    var status = set.starlev.utils.detectors.ScoreboardDetector.getCookieStatus()
                    if (status == "Не активно!") {
                        // Если в скорборде нет, может в табе? (хотя ScoreboardDetector уже мог искать, но проверим логику)
                        // В ScoreboardDetector я реализовал поиск только в scoreboard lines
                        // Так что здесь можно попробовать поискать через TabListDetector если там нет
                        // Но пользователь сказал "находит строчку Магическое печенье и под ним строчку"
                        // Если в TabListDetector тоже искать, то лучше это инкапсулировать
                        // Пока используем то что есть, так как ScoreboardDetector теперь ищет в ScoreboardText
                        // А в скриншоте это в Tab Footer.
                        // Поэтому я должен был обновить ScoreboardDetector чтобы он искал везде, или добавить сюда логику
                        // Но ScoreboardDetector.getScoreboardText() берет только сайдбар.
                        // Так что мне нужен TabListDetector.getCookieStatus() если он там есть, или я его не добавил?
                        // Я добавил getGems и getBank в TabListDetector.
                        // Я добавил getCookieStatus в ScoreboardDetector.
                        // Это ошибка логики. Скриншот показывает Tab List Footer.
                        // Я должен добавить поиск в Tab List Footer тоже.
                        
                        // Ладно, я сейчас добавлю логику сюда, или лучше вынесу в утилиту.
                        // Давайте вызовем TabListDetector.getCookieStatus() если я его добавлю.
                        // Я НЕ добавил getCookieStatus в TabListDetector в предыдущем шаге.
                        // Я добавил его в ScoreboardDetector.
                        // И он ищет только в ScoreboardText.
                        
                        // Исправим: поищем в TabListDetector "Бонус печенья" вручную здесь или добавим метод.
                        // Лучше добавить метод в TabListDetector, но я не хочу тратить еще один tool call на это если могу здесь.
                        // Но здесь код чище если вызывать метод.
                        
                        // Я могу получить все линии таба здесь:
                        val tabLines = set.starlev.utils.detectors.TabListDetector.getAllTabListLines()
                        for (i in tabLines.indices) {
                            val line = tabLines[i]
                            if (line.contains("Бонус печенья") || line.contains("Cookie Buff")) {
                                if (i + 1 < tabLines.size) {
                                    val next = tabLines[i + 1]
                                    if (next.any { it.isDigit() }) {
                                        status = next
                                        break
                                    }
                                }
                            }
                        }
                    }

                    val color = if (status == "Не активно!") "§c" else "§d"
                    out.add(
                        ScoreboardLine(
                            "custom:cookie",
                            set.starlev.secret.features.SecretFunFeatures.processComponent(Component.literal("§7Печенье: $color$status"), true),
                            centered = false
                        )
                    )
                }
            }
        }
        return out
    }

    private fun buildOrderedLines(includeSlayer: Boolean): List<ScoreboardLine> {
        val base = buildBaseLines(includeSlayer = includeSlayer)
        if (base.isEmpty()) return emptyList()
        return applyStoredOrder(base)
    }

    private fun buildBaseLines(includeSlayer: Boolean): List<ScoreboardLine> {
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
                    lines.addAll(buildCustomLines(includeSlayer = includeSlayer, anchor = CustomLineAnchor.AFTER_XP))
                    didInsertAfterXp = true
                }
            }
        }

        if (!didInsertAfterXp) {
            lines.addAll(buildCustomLines(includeSlayer = includeSlayer, anchor = CustomLineAnchor.AFTER_XP))
        }
        lines.addAll(buildCustomLines(includeSlayer = includeSlayer, anchor = CustomLineAnchor.BOTTOM))
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
        for (line in base) {
            if (used.add(line.key)) out.add(line)
        }
        return out
    }

    override fun renderWithGraphics(graphics: net.minecraft.client.gui.GuiGraphics) {
        ensureInitialized()
        cachedGraphics = graphics
        
        // 1. Подготовка данных и расчет ширины
        val includeSlayer = StarredHeltix.feature.slayer.slayerHud.slayerScoreboardHud
        val lines = buildOrderedLines(includeSlayer = includeSlayer)
        cachedLines = lines
        
        if (lines.isNotEmpty()) {
            val padding = 2
            val maxWidth = lines.maxOf { mc.font.width(it.component) }
            val width = maxWidth + padding * 2
            val currentWidth = width * scale
            val screenWidth = mc.window.guiScaledWidth

            // 2. Умное якорение (Smart Anchoring)
            if (isEditing) {
                // В режиме редактирования определяем сторону якорения на основе текущего положения
                val centerX = x + currentWidth / 2
                anchorSide = if (centerX > screenWidth / 2) AnchorSide.RIGHT else AnchorSide.LEFT
                
                // Обновляем точку якоря, чтобы она соответствовала текущему визуальному положению
                anchoredX = if (anchorSide == AnchorSide.RIGHT) {
                    x + currentWidth
                } else {
                    x.toFloat()
                }
            } else {
                // В обычном режиме принудительно используем якорь
                if (anchoredX == null) {
                     val centerX = x + currentWidth / 2
                     anchorSide = if (centerX > screenWidth / 2) AnchorSide.RIGHT else AnchorSide.LEFT
                     anchoredX = if (anchorSide == AnchorSide.RIGHT) x + currentWidth else x.toFloat()
                }
                
                val targetX = if (anchorSide == AnchorSide.RIGHT) {
                    anchoredX!! - currentWidth
                } else {
                    anchoredX!!
                }
                
                // Плавно обновляем x, чтобы избежать дрожания
                if (abs(x - targetX) > 1) {
                    x = targetX.toInt()
                }
            }
        }

        // 3. Стандартный рендер (уже с обновленным x)
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

    override fun getWidth(): Int = calculateSize(true).first

    override fun getHeight(): Int = calculateSize(true).second

    override fun getDefaultX(): Int {
        val baseWidth = calculateSize(false).first
        return mc.window.guiScaledWidth - baseWidth - 3
    }

    override fun getDefaultY(): Int {
        val scoreboard = mc.level?.scoreboard
        val objective = scoreboard?.getDisplayObjective(DisplaySlot.SIDEBAR)
        val scoresCount = if (objective != null) scoreboard.listPlayerScores(objective).size else 15
        val totalHeight = (scoresCount + 1) * 9
        val startY = mc.window.guiScaledHeight / 2 + totalHeight / 3
        return startY - totalHeight - 1
    }

    fun init() {
        // Загрузка сохраненных координат происходит автоматически через HudManager
    }
}
