package set.starlev.features.inventory

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.events.GuiEvents
import set.starlev.injections.accessors.ContainerScreenAccessor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object InventoryButtonsManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file: Path = Paths.get(System.getProperty("user.dir"), "config", "starredheltix", "inventory-buttons.json")
    private var buttons = mutableListOf<InventoryButton>()
    private var loaded = false
    private var hoveredButton: InventoryButton? = null

    fun init() {
        ensureLoaded()
        GuiEvents.registerClick { mouseX, mouseY, button, screen ->
            if (button != 0) return@registerClick false
            val config = StarredHeltix.feature.misc.general.inventoryButtons
            if (!config.enabled) return@registerClick false
            if (config.onlyInventory && screen !is InventoryScreen) return@registerClick false

            val accessor = screen as ContainerScreenAccessor
            val guiLeft = accessor.leftPos
            val guiTop = accessor.topPos
            val guiWidth = accessor.imageWidth
            val guiHeight = accessor.imageHeight

            handleClick(mouseX, mouseY, guiLeft, guiWidth, guiTop, guiHeight)
        }
        GuiEvents.registerForeground { graphics, mouseX, mouseY, screen ->
            val config = StarredHeltix.feature.misc.general.inventoryButtons
            if (!config.enabled) return@registerForeground
            if (config.onlyInventory && screen !is InventoryScreen) return@registerForeground

            val accessor = screen as ContainerScreenAccessor
            val guiLeft = accessor.leftPos
            val guiTop = accessor.topPos
            val guiWidth = accessor.imageWidth
            val guiHeight = accessor.imageHeight

            renderButtons(graphics, mouseX, mouseY, guiLeft, guiWidth, guiTop, guiHeight)
        }
    }

    fun getButtons(): List<InventoryButton> {
        ensureLoaded()
        return buttons.toList()
    }

    fun setButtons(newButtons: List<InventoryButton>) {
        buttons = newButtons.toMutableList()
        save()
    }

    fun save() {
        try {
            Files.createDirectories(file.parent)
            Files.writeString(file, gson.toJson(buttons))
        } catch (_: Exception) {
        }
    }

    private fun handleClick(mouseX: Double, mouseY: Double, guiLeft: Int, guiWidth: Int, guiTop: Int, guiHeight: Int): Boolean {
        for (button in buttons) {
            if (!button.isValid()) continue
            if (button.contains(mouseX, mouseY, guiLeft, guiWidth, guiTop, guiHeight)) {
                val mc = Minecraft.getInstance()
                val cmd = button.command ?: continue
                if (cmd.startsWith("/")) {
                    // Команда — выполняется как /command
                    mc.player?.connection?.sendCommand(cmd.removePrefix("/"))
                } else {
                    // Сообщение — отправляется в чат
                    mc.player?.connection?.sendChat(cmd)
                }
                return true
            }
        }
        return false
    }

    private fun renderButtons(graphics: GuiGraphics, mouseX: Int, mouseY: Int, guiLeft: Int, guiWidth: Int, guiTop: Int, guiHeight: Int) {
        hoveredButton = null
        for (button in buttons) {
            if (!button.isValid()) continue
            val hovered = button.contains(mouseX.toDouble(), mouseY.toDouble(), guiLeft, guiWidth, guiTop, guiHeight)
            if (hovered) hoveredButton = button
            button.render(graphics, guiLeft, guiWidth, guiTop, guiHeight, hovered)
        }

        // Тултип при наведении
        hoveredButton?.let { btn ->
            val cmd = btn.command ?: return@let
            val tooltipLines = mutableListOf<Component>()

            // Кастомная подсказка если есть
            if (!btn.tooltip.isNullOrBlank()) {
                tooltipLines.add(Component.literal("§e${btn.tooltip}"))
                tooltipLines.add(Component.literal(""))
            }

            // Действие
            val actionText = if (cmd.startsWith("/")) {
                "§7Выполнить: §f$cmd"
            } else {
                "§7Написать в чат: §f$cmd"
            }
            tooltipLines.add(Component.literal(actionText))

            val font = Minecraft.getInstance().font
            graphics.setComponentTooltipForNextFrame(font, tooltipLines, mouseX, mouseY)
        }
    }

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        try {
            if (!Files.exists(file)) {
                buttons = mutableListOf(
                    InventoryButton(0, "oak_door", "/hub"),
                    InventoryButton(1, "iron_door", "/is"),
                )
                save()
                return
            }
            val json = Files.readString(file)
            val type = object : TypeToken<List<InventoryButton>>() {}.type
            val list: List<InventoryButton> = gson.fromJson(json, type) ?: return
            buttons = list.toMutableList()
            // Миграция со старого формата (x, y, anchorRight, anchorBottom)
            migrateOldFormat()
        } catch (_: Exception) {
            buttons = mutableListOf()
        }
    }

    /**
     * Миграция со старого формата кнопок (если index == 0 но есть старые поля).
     */
    private fun migrateOldFormat() {
        // Просто переназначаем индексы если они все 0
        if (buttons.size > 1 && buttons.all { it.index == 0 }) {
            buttons.forEachIndexed { i, btn -> btn.index = i }
            save()
        }
    }
}