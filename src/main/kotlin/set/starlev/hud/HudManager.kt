package set.starlev.hud

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.minecraft.client.gui.GuiGraphics
import org.slf4j.LoggerFactory
import set.starlev.StarredHeltix
import set.starlev.render.RenderEngine
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Менеджер HUD элементов
 */
object HudManager {
    private val logger = LoggerFactory.getLogger("StarredHeltix/HUD")
    private val elements = mutableMapOf<String, HudElement>()
    private val renderers = mutableMapOf<String, (GuiGraphics) -> Unit>()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val hudLayoutFile: Path = Paths.get(System.getProperty("user.dir"), "config", "starredheltix", "hud-layouts.json")

    var isEditMode = false

    /**
     * Инициализировать HUD менеджер
     */
    fun init() {
        // Регистрируем HUD элементы
        registerElement(set.starlev.features.fishing.FishingNotifier)
        registerElement(set.starlev.features.fishing.LegendaryFishingNotifier)
        registerElement(set.starlev.features.foraging.TreeCapCooldown)
        registerElement(set.starlev.features.mining.PickaxeCooldownHud)
        registerElement(set.starlev.features.mining.SpeedBoostCooldownHud)
        registerElement(set.starlev.features.mining.CommissionsHud)
        registerElement(set.starlev.features.combat.slayer.SlayerHud)
        registerElement(set.starlev.features.skyblock.SkillXpHud)
        registerElement(set.starlev.features.skyblock.Museum)
        registerElement(set.starlev.features.skyblock.PetOverlay)
        registerElement(set.starlev.features.combat.dungeons.BloodRoomTimer)
        registerElement(set.starlev.features.combat.dungeons.ScoreCounter)
        registerElement(set.starlev.features.misc.MouseLock)
        registerElement(set.starlev.hud.HudScoreboard)
        registerElement(set.starlev.features.visual.InventoryHistoryLog)
        
        // Загружаем сохранённые позиции элементов
        loadAllLayouts()
        
        // Создать и зарегистрировать HUD renderer
        val hudRenderer: RenderEngine.HudRenderer = object : RenderEngine.HudRenderer {
            override fun render(graphics: GuiGraphics, delta: Float) {
                renderAll(graphics)
            }
        }
        RenderEngine.registerHud(hudRenderer)
    }

    fun registerElement(element: HudElement) {
        elements[element.id] = element
    }



    /**
     * Получить элемент по ID
     */
    fun getElement(id: String): HudElement? = elements[id]

    /**
     * Отрисовать все зарегистрированные элементы
     */
    fun renderAll(guiGraphics: GuiGraphics) {
        val mc = net.minecraft.client.Minecraft.getInstance()
        val inWorld = mc.player != null && mc.level != null
        val screen = mc.screen
        val inInventory = screen is net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>
        
        for ((id, element) in elements) {
            element.isEditing = isEditMode
            
            // Логика видимости
            if (!isEditMode) {
                // 1. Скрывать всё вне игры (главное меню и т.д.)
                if (!inWorld) continue
                
                // 2. В инвентаре отображать ТОЛЬКО Музей
                val isMuseum = id == "MuseumHud"
                if (inInventory) {
                    if (!isMuseum) continue
                } else {
                    // Вне инвентаря скрывать Музей (согласно его собственной логике, но продублируем здесь для надёжности)
                    if (isMuseum) continue
                }
            }
            
            try {
                element.renderWithGraphics(guiGraphics)
            } catch (e: Exception) {
                logger.error("Ошибка при отрисовке HUD элемента: $id", e)
            }

            if (isEditMode && element.getScaledWidth() > 0 && element.getScaledHeight() > 0) {
                drawHudBorder(guiGraphics, element)
            }
        }
    }

    /**
     * Отрисовать рамку элемента в режиме редактирования
     */
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

    /**
     * Переместить элемент
     */
    fun moveElement(id: String, dx: Int, dy: Int) {
        val element = elements[id] ?: return
        element.x = (element.x + dx).coerceAtLeast(0)
        element.y = (element.y + dy).coerceAtLeast(0)
    }



    /**
     * Сохранить позиции всех элементов в отдельный JSON файл
     */
    fun saveAllLayouts() {
        try {
            val layoutData = elements.mapValues { (_, element) ->
                HudLayoutData(element.x, element.y, element.scale, element.showBackground)
            }
            
            // Создаём директорию если её нет
            Files.createDirectories(hudLayoutFile.parent)
            
            // Сохраняем в JSON файл
            val jsonString = gson.toJson(layoutData)
            Files.write(hudLayoutFile, jsonString.toByteArray())
            logger.info("HUD позиции сохранены в: $hudLayoutFile")
        } catch (e: Exception) {
            logger.error("Ошибка при сохранении HUD позиций в файл $hudLayoutFile", e)
        }
    }

    /**
     * Загрузить позиции элементов из JSON файла
     */
    private fun loadAllLayouts() {
        try {
            if (!Files.exists(hudLayoutFile)) {
                logger.info("Файл HUD layouts не найден, используются значения по умолчанию")
                return
            }
            
            val jsonString = Files.readString(hudLayoutFile)
            val layoutDataType = object : com.google.gson.reflect.TypeToken<Map<String, HudLayoutData>>() {}.type
            val layoutData: Map<String, HudLayoutData> = gson.fromJson(jsonString, layoutDataType)
            
            for ((id, layout) in layoutData) {
                val element = elements[id] ?: continue
                element.x = layout.x
                element.y = layout.y
                element.scale = layout.scale
                element.showBackground = layout.showBackground
                element.markAsInitialized()
            }
            logger.info("HUD позиции загружены из: $hudLayoutFile")
        } catch (e: Exception) {
            logger.error("Ошибка при загрузке HUD позиций", e)
        }
    }

    /**
     * Загрузить позицию элемента (вызывается при регистрации)
     */
    private fun loadLayout(id: String) {
        // Загрузка происходит в loadAllLayouts(), вызываемой в init()
    }

    /**
     * Получить все зарегистрированные элементы
     */
    fun getAllElements(): Map<String, HudElement> = elements.toMap()

    /**
     * Сбросить позиции и масштаб всех элементов на значения по умолчанию
     */
    fun resetAllPositions() {
        elements.forEach { (_, element) ->
            element.x = element.getDefaultX()
            element.y = element.getDefaultY()
            element.scale = element.getDefaultScale()
        }
        saveAllLayouts()
    }

    /**
     * Data class для хранения данных о расположении
     */
    data class HudLayoutData(
        val x: Int,
        val y: Int,
        val scale: Float,
        val showBackground: Boolean = true
    )
}
