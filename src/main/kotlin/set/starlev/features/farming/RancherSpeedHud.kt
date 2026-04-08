package set.starlev.features.farming

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import set.starlev.StarredHeltix
import set.starlev.events.GuiEvents
import set.starlev.hud.HudElement
import set.starlev.skyblock.ItemRegistry
import set.starlev.utils.detectors.ItemLoreDetector
import set.starlev.utils.detectors.PlayerHeldItemDetector
import java.lang.reflect.Modifier

object RancherSpeedHud : HudElement("RancherSpeedHud") {
    private val mc = Minecraft.getInstance()
    private val presetsGson = GsonBuilder().create()

    private data class StoredCropPreset(val cropId: String, val speed: Int)
    private data class CropMeta(val id: String, val name: String, val iconItem: ItemStack)
    private data class UiCropPreset(val cropId: String, val name: String, val icon: ItemStack, val speed: Int)

    private val defaultSpeedByCropId: Map<String, Int> = mapOf(
        "WHEAT" to 93,
        "CARROT" to 93,
        "POTATO" to 93,
        "NETHER_WART" to 93,
        "PUMPKIN" to 155,
        "MELON" to 155,
        "COCOA" to 155,
        "SUGAR_CANE" to 328,
        "CACTUS" to 400,
        "MUSHROOM" to 233
    )

    private val cropMetas: List<CropMeta> = listOf(
        CropMeta("WHEAT", "Пшеница", ItemStack(Items.WHEAT)),
        CropMeta("CARROT", "Морковь", ItemStack(Items.CARROT)),
        CropMeta("POTATO", "Картошка", ItemStack(Items.POTATO)),
        CropMeta("NETHER_WART", "Нарост", ItemStack(Items.NETHER_WART)),
        CropMeta("COCOA", "Какао", ItemStack(Items.COCOA_BEANS)),
        CropMeta("SUGAR_CANE", "Сахарный тростник", ItemStack(Items.SUGAR_CANE)),
        CropMeta("CACTUS", "Кактус", ItemStack(Items.CACTUS)),
        CropMeta("PUMPKIN", "Тыква", ItemStack(Items.PUMPKIN)),
        CropMeta("MELON", "Арбуз", ItemStack(Items.MELON_SLICE)),
        CropMeta("MUSHROOM", "Грибы", ItemStack(Items.RED_MUSHROOM))
    )

    private var lastBootsSpeed: Int? = null
    private var lastBootsIcon: ItemStack? = null

    private var lastSignScreenIdentity: Int = 0
    private var didAutofillForCurrentSign = false

    private var editingPresetIndex: Int? = null
    private var editBuffer = ""

    fun openPresetsEditor() {
        val parent = mc.screen
        mc.setScreen(RancherSpeedPresetsEditorScreen(parent))
    }

    fun isPresetsEditorScreen(screen: Screen?): Boolean {
        return screen is RancherSpeedPresetsEditorScreen
    }

    fun resetPresetsToDefault() {
        val config = StarredHeltix.feature.farming.rancherSpeed
        config.cropPresetsJson = null
        StarredHeltix.configManager.saveConfig("rancher-speed-crop-presets-reset")
    }

    fun init() {
        GuiEvents.registerOpen { screen ->
            captureBootsGui(screen)
        }
    }

    override fun render() {
        val config = StarredHeltix.feature.farming.rancherSpeed
        if (!config.enabled) return

        val screen = mc.screen ?: return
        val signScreen = screen is AbstractSignEditScreen

        val player = mc.player
        val held = player?.mainHandItem ?: ItemStack.EMPTY
        val holdingBoots = ItemRegistry.isItem(held, ItemRegistry.SkyblockItem.RANCHERS_BOOTS)
        if (!isEditing && !(signScreen && holdingBoots)) return

        if (!isEditing && signScreen) {
            val identity = System.identityHashCode(screen)
            if (identity != lastSignScreenIdentity) {
                lastSignScreenIdentity = identity
                didAutofillForCurrentSign = false
                editingPresetIndex = null
                editBuffer = ""
            }

            if (!didAutofillForCurrentSign && config.autoFill) {
                val speed = lastBootsSpeed
                if (speed == null) {
                    didAutofillForCurrentSign = true
                } else if (!isSignEmpty(screen)) {
                    didAutofillForCurrentSign = true
                } else {
                    val ok = setSignLine(screen, 0, speed.toString())
                    if (ok) didAutofillForCurrentSign = true
                }
            }
        }

        val presets = getCropPresets()

        val padding = 4
        val rowH = mc.font.lineHeight + 3
        val contentW = 158
        val contentH = 18 + rowH * (presets.size.coerceAtLeast(1) + 2)

        showBackground = config.showBackground
        drawBackground(contentW, contentH, padding)

        val g = cachedGraphics ?: return

        val title = Component.literal("§6§lСкорость Ранчеров")
        g.drawString(mc.font, title, x, y, 0xFFFFFFFF.toInt())

        val signSpeed = if (signScreen) getSignLineSpeed(screen, 0) else null
        val shownSpeed = (signSpeed ?: lastBootsSpeed)?.toString() ?: if (isEditing) "100" else "?"
        g.drawString(mc.font, Component.literal("§b✦ §f$shownSpeed"), x, y + 14, 0xFFFFFFFF.toInt())

        var listY = y + 12 + rowH
        g.drawString(mc.font, Component.literal("§7ЛКМ: применить  §7ПКМ: редактор"), x, listY, 0xFFAAAAAA.toInt())
        listY += rowH

        if (presets.isEmpty()) {
            g.drawString(mc.font, Component.literal("§8Пресеты пустые"), x, listY, 0xFF777777.toInt())
            return
        }

        for ((i, preset) in presets.withIndex()) {
            val boxX1 = x
            val boxY1 = listY + i * rowH
            val boxX2 = x + contentW - 6
            val boxY2 = boxY1 + rowH - 1

            val isEditing = editingPresetIndex == i
            val icon = preset.icon.copy().also { it.count = 1 }
            g.renderItem(icon, boxX1 + 2, boxY1 + 1)

            val nameX = boxX1 + 20
            val valueX = boxX2 - 6
            g.drawString(mc.font, Component.literal("§f${preset.name}"), nameX, boxY1 + 2, 0xFFFFFFFF.toInt())

            val valueText = if (isEditing) {
                val shown = if (editBuffer.isEmpty()) preset.speed.toString() else editBuffer
                "§e$shown§b_"
            } else {
                "§b${preset.speed}"
            }
            val valueWidth = mc.font.width(valueText.replace(Regex("§."), ""))
            g.drawString(mc.font, Component.literal(valueText), (valueX - valueWidth).coerceAtLeast(nameX + 8), boxY1 + 2, 0xFFFFFFFF.toInt())
        }
    }

    private fun captureBootsGui(screen: AbstractContainerScreen<*>) {
        val config = StarredHeltix.feature.farming.rancherSpeed
        if (!config.enabled) return

        val held = PlayerHeldItemDetector.getMainHandItem()
        if (!ItemRegistry.isItem(held, ItemRegistry.SkyblockItem.RANCHERS_BOOTS)) return

        val title = screen.title.string
        val normalized = title.replace(Regex("§."), "").lowercase()
        if (!normalized.contains("ranch") && !normalized.contains("boot") && !normalized.contains("ранчер")) return

        var bestSpeed: Int? = null
        var bestIcon: ItemStack? = null

        for (slot in screen.menu.slots) {
            val stack = slot.item
            if (stack.isEmpty) continue

            val lines = ItemLoreDetector.getFullTooltip(stack)
            val speed = parseSpeedFromLines(lines)
            if (speed != null) {
                bestSpeed = speed
                bestIcon = stack
                break
            }
        }

        if (bestSpeed != null) {
            lastBootsSpeed = bestSpeed
            lastBootsIcon = bestIcon
        }
    }

    private fun parseSpeedFromLines(lines: List<String>): Int? {
        for (line in lines) {
            val clean = line.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "")
            if (!clean.contains("speed", ignoreCase = true) && !clean.contains("скорост", ignoreCase = true)) continue
            Regex("(\\d{1,4})").find(clean)?.let { m ->
                return m.groupValues[1].toIntOrNull()
            }
        }
        for (line in lines) {
            val clean = line.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "")
            Regex("^[^0-9]*(\\d{1,4})[^0-9]*$").find(clean)?.let { m ->
                val v = m.groupValues[1].toIntOrNull()
                if (v != null && v in 1..500) return v
            }
        }
        return null
    }

    @JvmStatic
    fun onMouseClicked(event: net.minecraft.client.input.MouseButtonEvent): Boolean {
        val config = StarredHeltix.feature.farming.rancherSpeed
        if (!config.enabled) return false

        val screen = mc.screen ?: return false
        if (screen !is AbstractSignEditScreen) return false

        val player = mc.player ?: return false
        val held = player.mainHandItem
        if (!ItemRegistry.isItem(held, ItemRegistry.SkyblockItem.RANCHERS_BOOTS)) return false

        val presets = getCropPresets()
        if (presets.isEmpty()) return false

        val rowH = mc.font.lineHeight + 3
        val contentW = 158
        var listY = y + 12 + rowH * 2
        
        val mx = (x + (event.x().toFloat() - x) / scale).toInt()
        val my = (y + (event.y().toFloat() - y) / scale).toInt()

        for (i in presets.indices) {
            val boxX1 = x
            val boxY1 = listY + i * rowH
            val boxX2 = x + contentW - 6
            val boxY2 = boxY1 + rowH - 1
            val inside = mx in boxX1..boxX2 && my in boxY1..boxY2
            if (!inside) continue

            when (event.button()) {
                0 -> {
                    editingPresetIndex = null
                    editBuffer = ""
                    lastBootsSpeed = presets[i].speed
                    setSignLine(screen, 0, presets[i].speed.toString())
                    return true
                }
                1 -> {
                    openPresetsEditor()
                    return true
                }
            }
        }

        return false
    }

    @JvmStatic
    fun onKeyPressed(event: net.minecraft.client.input.KeyEvent): Boolean {
        val config = StarredHeltix.feature.farming.rancherSpeed
        if (!config.enabled) return false

        val screen = mc.screen ?: return false
        if (screen !is AbstractSignEditScreen) return false

        val idx = editingPresetIndex ?: return false
        val presets = getCropPresets()
        if (idx !in presets.indices) {
            editingPresetIndex = null
            editBuffer = ""
            return false
        }

        val key = event.input()
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            editingPresetIndex = null
            editBuffer = ""
            return true
        }

        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            if (editBuffer.isNotEmpty()) {
                editBuffer = editBuffer.dropLast(1)
            }
            return true
        }

        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            val v = editBuffer.toIntOrNull()
            if (v != null && v in 1..500) {
                setCropPresetSpeed(presets[idx].cropId, v)
            }
            editingPresetIndex = null
            editBuffer = ""
            return true
        }

        return false
    }

    @JvmStatic
    fun onCharTyped(event: net.minecraft.client.input.CharacterEvent): Boolean {
        val config = StarredHeltix.feature.farming.rancherSpeed
        if (!config.enabled) return false

        val screen = mc.screen ?: return false
        if (screen !is AbstractSignEditScreen) return false

        if (editingPresetIndex == null) return false

        val ch = event.codepoint().toChar()
        if (!ch.isDigit()) return true

        if (editBuffer.length >= 4) return true
        editBuffer += ch
        return true
    }

    private fun parsePresets(raw: String): List<Int> {
        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..500 }
            .distinct()
            .take(12)
    }

    private class RancherSpeedPresetsEditorScreen(
        private val parent: Screen?
    ) : Screen(Component.literal("Rancher Boots: Presets")) {
        private val mc = Minecraft.getInstance()
        private var entries: MutableList<UiCropPreset> = mutableListOf()
        private var selectedIndex: Int? = null
        private var buffer: String = ""

        override fun init() {
            super.init()
            reload()

            val doneBtn = Button.builder(Component.literal("§aСохранить и закрыть")) {
                commitSelected()
                onClose()
            }
                .pos(this.width - 170, 10)
                .size(160, 20)
                .build()
            this.addRenderableWidget(doneBtn)

            val resetBtn = Button.builder(Component.literal("§cСбросить")) {
                RancherSpeedHud.resetPresetsToDefault()
                reload()
            }
                .pos(this.width - 170, 35)
                .size(160, 20)
                .build()
            this.addRenderableWidget(resetBtn)
        }

        private fun commitSelected(): Boolean {
            val idx = selectedIndex ?: return false
            val v = buffer.toIntOrNull() ?: return false
            if (v !in 1..500) return false
            RancherSpeedHud.setCropPresetSpeed(entries[idx].cropId, v)
            entries[idx] = entries[idx].copy(speed = v)
            return true
        }

        private fun reload() {
            entries = RancherSpeedHud.getCropPresets().toMutableList()
            selectedIndex = null
            buffer = ""
        }

        override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            guiGraphics.fill(0, 0, width, height, 0xC0101010.toInt())
            guiGraphics.drawCenteredString(
                this.font,
                Component.literal("§7ЛКМ: выбрать  §7Цифры: ввод  §7Enter: сохранить  §7Esc: выйти"),
                this.width / 2,
                10,
                0xFFFFFF
            )

            val boxW = 220
            val startX = (this.width - boxW) / 2
            var y = 40
            val rowH = this.font.lineHeight + 6

            for ((i, e) in entries.withIndex()) {
                val x1 = startX
                val x2 = startX + boxW
                val y1 = y
                val y2 = y + rowH

                val selected = selectedIndex == i
                val bg: Int = if (selected) 0x80333333.toInt() else 0x60222222
                guiGraphics.fill(x1, y1, x2, y2, bg)

                guiGraphics.renderItem(e.icon.copy().also { it.count = 1 }, x1 + 4, y1 + 2)
                val shownSpeed = if (selected) {
                    val v = if (buffer.isBlank()) e.speed.toString() else buffer
                    "§e$v§b_"
                } else {
                    "§b${e.speed}"
                }
                guiGraphics.drawString(
                    this.font,
                    Component.literal("§f${e.name} - $shownSpeed"),
                    x1 + 24,
                    y1 + 3,
                    0xFFFFFFFF.toInt()
                )

                y += rowH + 2
            }

            super.render(guiGraphics, mouseX, mouseY, partialTick)
        }

        override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
            if (event.button() != 0) return super.mouseClicked(event, isDoubleClick)
            val boxW = 220
            val startX = (this.width - boxW) / 2
            var y = 40
            val rowH = this.font.lineHeight + 6
            val mx = event.x().toInt()
            val my = event.y().toInt()

            for (i in entries.indices) {
                val x1 = startX
                val x2 = startX + boxW
                val y1 = y
                val y2 = y + rowH
                if (mx in x1..x2 && my in y1..y2) {
                    if (selectedIndex != null && selectedIndex != i) {
                        commitSelected()
                    }
                    selectedIndex = i
                    buffer = entries[i].speed.toString()
                    return true
                }
                y += rowH + 2
            }
            return super.mouseClicked(event, isDoubleClick)
        }

        override fun charTyped(event: CharacterEvent): Boolean {
            val idx = selectedIndex ?: return super.charTyped(event)
            val ch = event.codepoint().toChar()
            if (!ch.isDigit()) return true
            if (buffer.length >= 4) return true
            buffer += ch
            return true
        }

        override fun keyPressed(event: KeyEvent): Boolean {
            val key = event.input()
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                onClose()
                return true
            }

            val idx = selectedIndex ?: return super.keyPressed(event)

            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                if (buffer.isNotEmpty()) buffer = buffer.dropLast(1)
                return true
            }

            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                commitSelected()
                return true
            }

            return super.keyPressed(event)
        }

        override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
            guiGraphics.fill(0, 0, width, height, 0x80000000.toInt())
        }

        override fun onClose() {
            commitSelected()
            mc.setScreen(parent)
        }

        override fun isPauseScreen(): Boolean = false
    }

    private fun getCropPresets(): List<UiCropPreset> {
        val config = StarredHeltix.feature.farming.rancherSpeed
        val stored = loadStoredCropPresets(config.cropPresetsJson)
        val storedById = stored.associateBy { it.cropId }.toMutableMap()
        var changed = stored.isEmpty()

        for ((i, meta) in cropMetas.withIndex()) {
            if (!storedById.containsKey(meta.id)) {
                val speed = defaultSpeedByCropId[meta.id] ?: 100
                storedById[meta.id] = StoredCropPreset(meta.id, speed.coerceIn(1, 500))
                changed = true
            }
        }

        val unknown = stored.filter { s -> cropMetas.none { it.id == s.cropId } }
            .distinctBy { it.cropId }

        val normalizedStored = cropMetas.map { meta ->
            storedById[meta.id] ?: StoredCropPreset(meta.id, (defaultSpeedByCropId[meta.id] ?: 100).coerceIn(1, 500))
        } + unknown

        if (changed) {
            config.cropPresetsJson = presetsGson.toJson(normalizedStored)
            StarredHeltix.configManager.saveConfig("rancher-speed-crop-presets-init")
        }

        val speedById = normalizedStored.associate { it.cropId to it.speed.coerceIn(1, 500) }
        val ui = cropMetas.map { meta ->
            UiCropPreset(
                cropId = meta.id,
                name = meta.name,
                icon = meta.iconItem,
                speed = (speedById[meta.id] ?: (defaultSpeedByCropId[meta.id] ?: 100)).coerceIn(1, 500)
            )
        }

        val unknownUi = unknown.map { s ->
            UiCropPreset(
                cropId = s.cropId,
                name = s.cropId,
                icon = ItemStack(Items.PAPER),
                speed = s.speed.coerceIn(1, 500)
            )
        }

        return (ui + unknownUi).take(12)
    }

    private fun loadStoredCropPresets(raw: String?): List<StoredCropPreset> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<StoredCropPreset>>() {}.type
            val list: List<StoredCropPreset> = presetsGson.fromJson(raw, type) ?: emptyList()
            list.mapNotNull { p ->
                val id = p.cropId.trim()
                val speed = p.speed
                if (id.isBlank()) return@mapNotNull null
                if (speed !in 1..500) return@mapNotNull null
                StoredCropPreset(id, speed)
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun setCropPresetSpeed(cropId: String, speed: Int) {
        val config = StarredHeltix.feature.farming.rancherSpeed
        val current = loadStoredCropPresets(config.cropPresetsJson).toMutableList()
        val idx = current.indexOfFirst { it.cropId.equals(cropId, ignoreCase = true) }
        if (idx >= 0) {
            current[idx] = StoredCropPreset(current[idx].cropId, speed.coerceIn(1, 500))
        } else {
            current.add(StoredCropPreset(cropId, speed.coerceIn(1, 500)))
        }
        config.cropPresetsJson = presetsGson.toJson(current)
        StarredHeltix.configManager.saveConfig("rancher-speed-crop-presets-edit")
    }

    private fun isSignEmpty(screen: Screen): Boolean {
        val lines = getSignLines(screen) ?: return true
        return lines.all { it.isBlank() }
    }

    private fun getSignLineSpeed(screen: Screen, index: Int): Int? {
        val lines = getSignLines(screen) ?: return null
        val raw = lines.getOrNull(index)?.trim().orEmpty()
        val v = raw.toIntOrNull() ?: return null
        return v.takeIf { it in 1..500 }
    }

    private fun setSignLine(screen: Screen, index: Int, text: String): Boolean {
        return setSignLines(screen, mapOf(index to text))
    }

    @Suppress("UNCHECKED_CAST")
    private fun getSignLines(screen: Screen): Array<String>? {
        val cls = screen.javaClass
        var c: Class<*>? = cls
        while (c != null && c != Any::class.java) {
            for (f in c.declaredFields) {
                if (Modifier.isStatic(f.modifiers)) continue
                if (!f.type.isArray) continue
                if (f.type.componentType != String::class.java) continue
                try {
                    f.isAccessible = true
                    val arr = f.get(screen) as? Array<String> ?: continue
                    if (arr.size == 4) return arr
                } catch (_: Throwable) {
                }
            }
            c = c.superclass
        }
        return null
    }

    private fun setSignLines(screen: Screen, changes: Map<Int, String>): Boolean {
        if (trySetStringArray(screen, changes)) return true
        if (trySetComponentArray(screen, changes)) return true
        if (tryInvokeSetMessage(screen, changes)) return true
        return false
    }

    @Suppress("UNCHECKED_CAST")
    private fun trySetStringArray(screen: Screen, changes: Map<Int, String>): Boolean {
        val cls = screen.javaClass
        var c: Class<*>? = cls
        while (c != null && c != Any::class.java) {
            for (f in c.declaredFields) {
                if (Modifier.isStatic(f.modifiers)) continue
                if (!f.type.isArray) continue
                if (f.type.componentType != String::class.java) continue
                try {
                    f.isAccessible = true
                    val arr = f.get(screen) as? Array<String> ?: continue
                    if (arr.size != 4) continue
                    for ((idx, v) in changes) {
                        if (idx in 0..3) arr[idx] = v
                    }
                    return true
                } catch (_: Throwable) {
                }
            }
            c = c.superclass
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    private fun trySetComponentArray(screen: Screen, changes: Map<Int, String>): Boolean {
        val cls = screen.javaClass
        var c: Class<*>? = cls
        while (c != null && c != Any::class.java) {
            for (f in c.declaredFields) {
                if (Modifier.isStatic(f.modifiers)) continue
                if (!f.type.isArray) continue
                if (f.type.componentType != Component::class.java) continue
                try {
                    f.isAccessible = true
                    val arr = f.get(screen) as? Array<Component> ?: continue
                    if (arr.size != 4) continue
                    for ((idx, v) in changes) {
                        if (idx in 0..3) arr[idx] = Component.literal(v)
                    }
                    return true
                } catch (_: Throwable) {
                }
            }
            c = c.superclass
        }
        return false
    }

    private fun tryInvokeSetMessage(screen: Screen, changes: Map<Int, String>): Boolean {
        val cls = screen.javaClass
        val methods = cls.methods + cls.declaredMethods
        for ((idx, v) in changes) {
            if (idx !in 0..3) continue
            for (m in methods) {
                if (Modifier.isStatic(m.modifiers)) continue
                if (m.returnType != Void.TYPE) continue
                if (m.parameterCount != 2) continue
                val p0 = m.parameterTypes[0]
                val p1 = m.parameterTypes[1]
                if (p0 != Int::class.javaPrimitiveType && p0 != Int::class.javaObjectType) continue
                try {
                    if (p1 == String::class.java) {
                        m.isAccessible = true
                        m.invoke(screen, idx, v)
                        return true
                    }
                    if (p1 == Component::class.java) {
                        m.isAccessible = true
                        m.invoke(screen, idx, Component.literal(v))
                        return true
                    }
                } catch (_: Throwable) {
                }
            }
        }
        return false
    }

    override fun getWidth(): Int = 158
    override fun getHeight(): Int = 90
    override fun getDefaultX(): Int = 12
    override fun getDefaultY(): Int = 110
}
