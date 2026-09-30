package set.starlev.features.combat.dungeons

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import set.starlev.StarredHeltix
import set.starlev.events.GuiEvents
import set.starlev.render.drawSmoothRoundedPanel
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.ContainerDetector
import set.starlev.utils.detectors.ItemLoreDetector

/**
 * Leap-оверлей меню «Spirit Leap» в данжах (порт идеи NoFrills LeapOverlay /
 * Odin LeapMenu / Skyblocker LeapOverlay): мелкие головы тиммейтов в контейнере
 * дублируются крупными кнопками 2x2 по центру экрана — имя, иконка-хед и слот
 * хоткея. Клик по кнопке или хоткей (бинды «Leap 1..4», дефолт 1-4) кликает
 * настоящий слот головы (PICKUP) — сервер телепортирует к игроку.
 *
 * Распознавание меню: заголовок контейнера содержит одну из подстрок из конфига
 * (RU/EN варианты). Кандидаты: PLAYER_HEAD со «телепортной» лорой; если лоры нет —
 * любые PLAYER_HEAD с непустым именем (фолбэк на случай своего формата у Heltix).
 */
object LeapOverlay {
    private val mc = Minecraft.getInstance()

    private val leapKeys: List<KeyMapping> = listOf(
        GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4
    ).mapIndexed { i, key ->
        KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.starredheltix.leap${i + 1}",
                InputConstants.Type.KEYSYM,
                key,
                set.starlev.events.ModKeybinds.CATEGORY
            )
        )
    }

    private data class LeapButton(val slotIndex: Int, val head: ItemStack, val playerName: String)

    private var buttons: List<LeapButton> = emptyList()

    @Volatile
    private var lastLeapMs = 0L

    fun init() {
        GuiEvents.registerForeground { graphics, mouseX, mouseY, screen -> onRender(graphics, mouseX, mouseY, screen) }
        GuiEvents.registerClick { mouseX, mouseY, button, screen -> onClick(button, mouseX, mouseY, screen) }
        GuiEvents.registerKey { event, screen -> onKey(event, screen) }
    }

    fun isLeapScreen(screen: AbstractContainerScreen<*>): Boolean {
        val cfg = StarredHeltix.feature.dungeons.leap
        if (!cfg.enabled) return false
        // Гейт по данжам не нужен: меню липа существует только в подземельях,
        // заголовка достаточно (DungeonDetector здесь давал бы ложные срабатывания).
        val title = try {
            ContainerDetector.getCurrentContainerInfo()?.normalizedTitle
                ?: screen.title.string.lowercase().trim()
        } catch (_: Exception) {
            return false
        }
        if (title.isBlank()) return false
        return cfg.menuTitles.split(',').map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .any { title.contains(it) }
    }

    // ——— раскладка 1в1 как Odin LeapMenu ———
    // Экран делится на 4 квадранта (крест по центру), в каждом — бокс 200×75
    // с отступом 24px от центра. Ховер/клик — по всему квадранту, а не по боксу.

    private const val BOX_W = 200
    private const val BOX_H = 75
    private const val CENTER_GAP = 24
    private const val BOX_RADIUS = 9
    private const val GROW = 5f
    private const val HOVER_MS = 200L

    /** Анимация наведения на квадрант (0..GROW), как Fade в Odin. */
    private val growAnims = FloatArray(4)
    private var lastFrameMs = 0L

    private fun quadrantOf(mx: Int, my: Int, halfW: Int, halfH: Int): Int =
        (if (my >= halfH) 2 else 0) + (if (mx >= halfW) 1 else 0)

    private fun collectTargets(menu: AbstractContainerMenu): List<LeapButton> {
        val strict = ArrayList<LeapButton>()
        val loose = ArrayList<LeapButton>()
        for (slot in menu.slots) {
            val stack = slot.item
            if (stack.isEmpty || !stack.`is`(Items.PLAYER_HEAD)) continue
            val name = stack.hoverName.string.trim()
            if (name.isEmpty()) continue
            val entry = LeapButton(slot.index, stack, name)
            val teleportLore = try {
                ItemLoreDetector.getLore(stack).any {
                    val l = it.lowercase()
                    l.contains("teleport") || l.contains("телепорт")
                }
            } catch (_: Exception) {
                false
            }
            if (teleportLore) strict.add(entry) else loose.add(entry)
        }
        val result = strict.ifEmpty { loose }
        return result.sortedBy { it.slotIndex }.take(4)
    }

    // ——— обработка ———

    private fun onRender(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, screen: AbstractContainerScreen<*>) {
        if (!isLeapScreen(screen)) {
            if (buttons.isNotEmpty()) buttons = emptyList()
            growAnims.fill(0f)
            lastFrameMs = 0L
            return
        }
        val cfg = StarredHeltix.feature.dungeons.leap
        buttons = collectTargets(screen.menu)
        if (buttons.isEmpty()) {
            growAnims.fill(0f)
            return
        }

        val w = graphics.guiWidth()
        val h = graphics.guiHeight()
        val halfW = w / 2
        val halfH = h / 2
        // Затемнение фона (оригинальный инвентарь уже скрыт миксином)
        graphics.fill(0, 0, w, h, 0xB0000000.toInt())

        val now = System.currentTimeMillis()
        val dt = if (lastFrameMs == 0L) 16L else (now - lastFrameMs).coerceIn(0L, 100L)
        lastFrameMs = now
        val step = dt * GROW / HOVER_MS

        val boxColor = ColorUtils.parseColor(cfg.accentColorV2, 0xFFFF55FF.toInt())
        val scale = if (cfg.scale <= 0f) 1f else cfg.scale
        val face = (BOX_H * 0.76f).toInt()

        for (i in buttons.indices) {
            val button = buttons[i]
            val col = i % 2
            val row = i / 2
            val hovered = (if (col == 0) mouseX < halfW else mouseX >= halfW) &&
                (if (row == 0) mouseY < halfH else mouseY >= halfH)
            // Grow-анимация наведения (Odin: Fade 200мс)
            val target = if (hovered) GROW else 0f
            growAnims[i] = (growAnims[i] + (target - growAnims[i]).coerceIn(-step, step)).coerceIn(0f, GROW)
            val grow = growAnims[i]

            val nearX = if (col == 0) halfW - CENTER_GAP else halfW + CENTER_GAP
            val nearY = if (row == 0) halfH - CENTER_GAP else halfH + CENTER_GAP
            val localX = if (col == 0) -BOX_W else 0
            val localY = if (row == 0) -BOX_H else 0

            val pose = graphics.pose()
            pose.pushMatrix()
            pose.translate(nearX.toFloat(), nearY.toFloat())
            pose.scale(scale * (BOX_W + grow * 2f) / BOX_W, scale * (BOX_H + grow * 2f) / BOX_H)
            try {
                // Бокс со скруглением 9 (Odin: roundedRect ..., 9f)
                graphics.drawSmoothRoundedPanel(localX, localY, BOX_W, BOX_H, boxColor, BOX_RADIUS)

                // Лицо скина 57×57 (Odin: PlayerFaceExtractor), фолбэк — предмет-голова
                val faceX = localX + 9
                val faceY = localY + 9
                val profile = try {
                    button.head.get(net.minecraft.core.component.DataComponents.PROFILE)
                } catch (_: Exception) {
                    null
                }
                if (profile != null) {
                    try {
                        net.minecraft.client.gui.components.PlayerFaceExtractor.extractRenderState(
                            graphics, profile, faceX, faceY, face
                        )
                    } catch (_: Exception) {
                        graphics.item(button.head, faceX + (face - 16) / 2, faceY + (face - 16) / 2)
                    }
                } else {
                    graphics.item(button.head, faceX + (face - 16) / 2, faceY + (face - 16) / 2)
                }

                // Ник + вторая строка (класс у Odin; у нас — хоткей)
                val textX = localX + 15 + face
                graphics.text(mc.font, "§f" + button.playerName, textX, localY + (BOX_H / 2.5f).toInt(), 0xFFFFFFFF.toInt(), true)
                val sub = if (leapKeys.size > i) {
                    "§7[§e${leapKeys[i].translatedKeyMessage.string}§7]"
                } else {
                    "§7 leap ${i + 1}"
                }
                graphics.text(mc.font, sub, textX, localY + (BOX_H / 1.7f).toInt(), 0xFFFFFFFF.toInt(), true)
            } finally {
                pose.popMatrix()
            }
        }
        // Сбрасываем анимацию пустых квадрантов
        for (i in buttons.size until 4) growAnims[i] = 0f
    }

    private fun onClick(clickButton: Int, mouseX: Double, mouseY: Double, screen: AbstractContainerScreen<*>): Boolean {
        if (clickButton != 0) return false
        if (!isLeapScreen(screen)) return false
        if (buttons.isEmpty()) return false
        // Клик по квадранту (Odin: весь четверть-экрана, а не только бокс)
        val w = screen.width
        val h = screen.height
        if (w <= 0 || h <= 0) return false
        val quadrant = quadrantOf(mouseX.toInt(), mouseY.toInt(), w / 2, h / 2)
        if (quadrant !in buttons.indices) return true // пустой квадрант — глотаем, в контейнер не пропускаем
        return performLeap(screen.menu, quadrant)
    }

    private fun onKey(event: net.minecraft.client.input.KeyEvent, screen: AbstractContainerScreen<*>): Boolean {
        if (!isLeapScreen(screen)) return false
        val now = System.currentTimeMillis()
        if (now - lastLeapMs < 500L) return true // заглотить и repeat, и спам
        val index = leapKeys.indexOfFirst { it.matches(event) }
        if (index == -1) return false
        if (index >= buttons.size) return true // клавиша есть, а тиммейта — нет: просто не свапаем хотбар
        return performLeap(screen.menu, index)
    }

    private fun performLeap(menu: AbstractContainerMenu, index: Int): Boolean {
        val player = mc.player ?: return false
        val button = buttons.getOrNull(index) ?: return false
        lastLeapMs = System.currentTimeMillis()
        mc.gameMode?.handleContainerInput(menu.containerId, button.slotIndex, 0, ContainerInput.PICKUP, player)
        menu.setCarried(ItemStack.EMPTY)
        val msg = StarredHeltix.feature.dungeons.leap.leapMessage
        if (msg.isNotBlank()) {
            try {
                player.connection.sendCommand("pc ${msg.replace("%name%", button.playerName)}")
            } catch (_: Exception) {
            }
        }
        buttons = emptyList()
        return true
    }

    /** Сброс состояния (мир/выход). */
    fun onWorldChange() {
        buttons = emptyList()
        lastLeapMs = 0L
        growAnims.fill(0f)
        lastFrameMs = 0L
    }
}
