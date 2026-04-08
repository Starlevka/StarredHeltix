package set.starlev.hud

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.minecraft.client.gui.GuiGraphics
import set.starlev.StarredHeltix
import set.starlev.features.skyblock.HudScoreboard
import set.starlev.render.RenderEngine
import set.starlev.skyblock.ItemRegistry
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object HudManager {
    private val elements = mutableMapOf<String, HudElement>()
    private val renderers = mutableMapOf<String, (GuiGraphics) -> Unit>()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val hudLayoutFile: Path = Paths.get(System.getProperty("user.dir"), "config", "starredheltix", "hud-layouts.json")
    private var loadedLayouts: Map<String, HudLayoutData> = emptyMap()

    var isEditMode = false

    fun init() {
        registerElement(set.starlev.features.fishing.FishingNotifier)
        registerElement(set.starlev.features.fishing.LegendaryFishingNotifier)
        registerElement(set.starlev.features.foraging.TreeCapCooldown)
        registerElement(set.starlev.features.mining.PickaxeCooldownHud)
        registerElement(set.starlev.features.mining.SpeedBoostCooldownHud)
        registerElement(set.starlev.features.mining.CommissionsHud)
        registerElement(set.starlev.features.combat.slayer.SlayerHud)
        registerElement(set.starlev.features.skyblock.SkillXpHud)
        registerElement(set.starlev.features.skyblock.EnchantmentProgressHud)
        registerElement(set.starlev.features.skyblock.Museum)
        registerElement(set.starlev.features.skyblock.PetOverlay)
        registerElement(set.starlev.features.combat.dungeons.BloodRoomTimer)
        registerElement(set.starlev.features.combat.dungeons.ScoreCounter)
        registerElement(set.starlev.features.overlays.NpcDialogueOverlay)
        registerElement(set.starlev.features.misc.MouseLock)
        registerElement(set.starlev.features.skyblock.HudScoreboard)
        registerElement(set.starlev.features.misc.InventoryHistoryLog)
        registerElement(set.starlev.features.farming.RancherSpeedHud)

        registerElement(set.starlev.features.misc.info.FpsHud)
        registerElement(set.starlev.features.misc.info.PingHud)
        registerElement(set.starlev.features.misc.info.CpsHud)
        registerElement(set.starlev.features.misc.info.BpsHud)

        loadAllLayouts()
        HudScoreboard.CustomLinesLayoutStore.getAllLayouts()
        HudScoreboard.ScoreboardLinesOrderStore.getOrder()

        val hudRenderer: RenderEngine.HudRenderer = object : RenderEngine.HudRenderer {
            override fun render(graphics: GuiGraphics, delta: Float) {
                renderAll(graphics)
            }
        }
        RenderEngine.registerHud(hudRenderer)
    }

    fun registerElement(element: HudElement) {
        elements[element.id] = element
        getLayoutForId(element.id)?.let { layout ->
            applyLayout(element, layout)
        }
    }

    fun getElement(id: String): HudElement? = elements[id]

    fun renderAll(guiGraphics: GuiGraphics) {
        val mc = net.minecraft.client.Minecraft.getInstance()
        val inWorld = mc.player != null && mc.level != null
        val screen = mc.screen
        val inInventory = screen is net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>
        val inScoreboardEditor = screen is ScoreboardEditorScreen
        val rancherEditorOpen = set.starlev.features.farming.RancherSpeedHud.isPresetsEditorScreen(screen)
        val inRancherSignEdit = screen is net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen &&
            StarredHeltix.feature.farming.rancherSpeed.enabled

        // Кастомные GUI мода — скрываем весь HUD
        val inCustomGui = screen is set.starlev.features.inventory.InventoryButtonsGui ||
            screen is set.starlev.render.FilterGui ||
            screen is set.starlev.render.BindsGui ||
            screen is set.starlev.render.WaypointsGui

        // Tab list открыт — скрываем scoreboard
        val tabListOpen = mc.options.keyPlayerList.isDown

        for ((id, element) in elements) {
            element.isEditing = isEditMode

            if (!isEditMode) {
                if (!inWorld) continue
                if (rancherEditorOpen) continue
                if (inRancherSignEdit && id != "RancherSpeedHud") continue
                if (inScoreboardEditor && id == HudScoreboard.id) continue

                // Скрываем HUD когда открыт кастомный GUI мода
                if (inCustomGui) continue

                // Скрываем scoreboard при нажатом Tab
                if (tabListOpen && id == "Scoreboard") continue

                val isMuseum = id == "MuseumHud"
                if (inInventory) {
                    if (!isMuseum) continue
                } else {
                    if (isMuseum) continue
                }
            }

            try {
                element.renderWithGraphics(guiGraphics)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (isEditMode && element.getScaledWidth() > 0 && element.getScaledHeight() > 0) {
                drawHudBorder(guiGraphics, element)
            }
        }
    }

    private fun drawHudBorder(guiGraphics: GuiGraphics, element: HudElement) {
        val x = element.x
        val y = element.y
        val width = element.getScaledWidth()
        val height = element.getScaledHeight()
        val color = 0xFF00FF00.toInt()

        guiGraphics.fill(x, y, x + width, y + 1, color)
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color)
        guiGraphics.fill(x, y, x + 1, y + height, color)
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color)
    }

    fun moveElement(id: String, dx: Int, dy: Int) {
        val element = elements[id] ?: return
        element.x = (element.x + dx).coerceAtLeast(0)
        element.y = (element.y + dy).coerceAtLeast(0)
    }

    fun saveAllLayouts() {
        try {
            val layoutData = elements.mapValues { (_, element) ->
                HudLayoutData(element.x, element.y, element.scale, element.showBackground, element.customWidth, element.customHeight)
            }
            Files.createDirectories(hudLayoutFile.parent)
            val jsonString = gson.toJson(layoutData)
            Files.write(hudLayoutFile, jsonString.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        HudScoreboard.CustomLinesLayoutStore.save()
        HudScoreboard.ScoreboardLinesOrderStore.save()
    }

    private fun loadAllLayouts() {
        try {
            if (!Files.exists(hudLayoutFile)) return
            val jsonString = Files.readString(hudLayoutFile)
            val layoutDataType = object : com.google.gson.reflect.TypeToken<Map<String, HudLayoutData>>() {}.type
            val layoutData: Map<String, HudLayoutData> = gson.fromJson(jsonString, layoutDataType)
            loadedLayouts = layoutData

            for ((id, layout) in layoutData) {
                val element = elements[id] ?: continue
                applyLayout(element, layout)
            }

            elements["Scoreboard"]?.let { element ->
                if (!layoutData.containsKey("Scoreboard")) {
                    getLayoutForId("Scoreboard")?.let { applyLayout(element, it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyLayout(element: HudElement, layout: HudLayoutData) {
        element.x = layout.x
        element.y = layout.y
        element.scale = layout.scale
        element.showBackground = layout.showBackground
        element.customWidth = layout.width
        element.customHeight = layout.height
        element.markAsInitialized()
    }

    private fun getLayoutForId(id: String): HudLayoutData? {
        loadedLayouts[id]?.let { return it }
        return when (id) {
            "Scoreboard" -> loadedLayouts["HudScoreboard"] ?: loadedLayouts["ScoreboardHud"]
            else -> null
        }
    }

    private fun loadLayout(id: String) {
    }

    fun getAllElements(): Map<String, HudElement> = elements.toMap()

    fun resetAllPositions() {
        elements.forEach { (_, element) ->
            element.x = element.getDefaultX()
            element.y = element.getDefaultY()
            element.scale = element.getDefaultScale()
            element.customWidth = 0
            element.customHeight = 0
        }
        saveAllLayouts()
    }

    data class HudLayoutData(
        val x: Int,
        val y: Int,
        val scale: Float,
        val showBackground: Boolean = true,
        val width: Int = 0,
        val height: Int = 0
    )
}