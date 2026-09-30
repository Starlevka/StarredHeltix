package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import set.starlev.features.visual.swing.ChangeResult
import set.starlev.features.visual.swing.SwingCustomizationSnapshot
import set.starlev.features.visual.swing.SwingTransformConfig
import set.starlev.features.visual.swing.SwingTransformSnapshot
import set.starlev.features.visual.swing.snapshot
import set.starlev.features.visual.swing.toConfig

class VisualsConfig {

    @ConfigOption(name = "Редактор HUD", desc = "Открывает настройку HUD. Используйте колёсико мыши для изменения размера!")
    @ConfigEditorButton(buttonText = "Открыть")
    val openHudEditor: Runnable = Runnable { set.starlev.utils.ConfigUtils.openHudEditor() }

    @Expose
    @ConfigOption(name = "Анимации", desc = "Кастомные анимации для игрока и предметов")
    @Accordion
    var animations = AnimationsConfig()

    @Expose
    @ConfigOption(name = "Скрытие второй руки", desc = "Убирает предмет и руку второй руки от первого лица")
    @ConfigEditorBoolean
    var hideOffhand = false

    @Expose
    @ConfigOption(name = "Подсветка второй руки", desc = "Отключает белую подсветку слота второй руки (левой) в инвентаре при наведении")
    @ConfigEditorBoolean
    var hideOffhandSlotHighlight = false

    @Expose
    @ConfigOption(name = "Блеск голов", desc = "Включает зачарованный блеск (glint) для голов/черепов при зачаровании")
    @ConfigEditorBoolean
    var headGlint = true

    @Expose
    @ConfigOption(name = "Оверлей блока", desc = "Настройки выделения блоков")
    @Accordion
    var blockOverlay = BlockOverlayConfig()

    @Expose
    @ConfigOption(name = "Статы (HUD)", desc = "Отдельные HUD для CPS/FPS/Ping/BPS — альтернатива строкам кастомного скорборда. Позиция и масштаб настраиваются в редакторе HUD")
    @Accordion
    var statsHud = StatsHudConfig()

    @Expose
    @ConfigOption(name = "Призрачные рамки", desc = "Настройки отображения призрачных рамок с картинками")
    @Accordion
    var ghostFrames = GhostFrameConfig()

    class GhostFrameConfig {
        @Expose
        @ConfigOption(name = "Картинка 1", desc = "Отображает первую картинку в Хабе")
        @ConfigEditorBoolean
        var image1Enabled = true

        @Expose
        @ConfigOption(name = "Картинка 2", desc = "Отображает вторую картинку на острове Starlev")
        @ConfigEditorBoolean
        var image2Enabled = true
    }

    @Expose
    var inventoryHistory = InventoryHistoryConfig()

    class InventoryHistoryConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает отображение лога предметов на экране")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Длительность (сек)", desc = "Время отображения записи в логе")
        @ConfigEditorSlider(minValue = 1f, maxValue = 15f, minStep = 0.5f)
        var duration = 5f

        @Expose
        @ConfigOption(name = "Макс. записей", desc = "Максимальное количество одновременно отображаемых записей")
        @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
        var maxEntries = 8f

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображает фон в логе предметов")
        @ConfigEditorBoolean
        var showBackground = false

        @Expose
        @ConfigOption(name = "Игнорировать экипировку", desc = "Не отображать изменения брони и предметов в руках. §cЛучше оставить ВКЛ")
        @ConfigEditorBoolean
        var ignoreEquipped = true
    }

    class AnimationsConfig {
        @Expose
        @ConfigOption(name = "Анимация удара", desc = "Изменяет положение и скорость анимации руки")
        @Accordion
        var swingAnimation = SwingAnimationConfig()
    }

    class SwingAnimationConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает кастомную анимацию удара и положение предмета в руке")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Без анимации экипировки", desc = "Отключает анимацию поднятия предмета при его смене")
        @ConfigEditorBoolean
        var noEquipAnimation = false

        @Expose
        @ConfigOption(name = "Игнорировать эффекты добычи", desc = "Скорость удара не зависит от Haste и усталости mining-эффектов")
        @ConfigEditorBoolean
        var ignoresMiningEffects = false

        @ConfigOption(name = "Редактор анимации", desc = "Открывает отдельное окно: положение, повороты, масштаб, скорость и стиль удара. Работает как в Skysoft")
        @ConfigEditorButton(buttonText = "Открыть")
        val openEditor = Runnable {
            set.starlev.features.visual.swing.SwingEditorScreen.open()
        }

        // ---- Данные редактора (в UI конфига не отображаются) ----

        @Expose
        val global = SwingTransformConfig()

        @Expose
        var itemTransforms: MutableMap<String, SwingTransformConfig> = linkedMapOf()

        @Expose
        var editorX = AUTO_EDITOR_POSITION

        @Expose
        var editorY = AUTO_EDITOR_POSITION

        // ---- Legacy-поля старой системы (для миграции, в UI не отображаются) ----

        @Expose
        var offX = 0.0

        @Expose
        var offY = 0.0

        @Expose
        var offZ = 0.0

        @Expose
        var swingX = 1.0

        @Expose
        var swingY = 1.0

        @Expose
        var swingZ = 1.0

        @Expose
        var swingSpeed = 0.0

        @Expose
        var migratedToEditor = false

        // ---- API редактора ----

        fun transformFor(itemId: String?): SwingTransformConfig =
            itemId?.let { itemTransforms[normalizedItemId(it)] } ?: global

        fun customize(itemId: String): SwingTransformConfig =
            itemTransforms.getOrPut(normalizedItemId(itemId)) { global.copyValues() }

        fun removeCustomization(itemId: String): ChangeResult =
            ChangeResult.from(itemTransforms.remove(normalizedItemId(itemId)) != null)

        fun hasCustomization(itemId: String?): Boolean =
            itemId != null && itemTransforms.containsKey(normalizedItemId(itemId))

        fun hasGlobalCustomization(): Boolean = !global.isDefault()

        fun resetGlobalCustomization(): ChangeResult {
            val result = ChangeResult.from(hasGlobalCustomization())
            global.reset()
            return result
        }

        fun hasItemCustomization(itemId: String?): Boolean = hasCustomization(itemId)

        fun removeItemCustomization(itemId: String): ChangeResult =
            removeCustomization(itemId)

        fun snapshotCustomization(itemId: String?): SwingCustomizationSnapshot {
            if (itemId == null) {
                return SwingCustomizationSnapshot(global.snapshot())
            }
            val normalizedId = normalizedItemId(itemId)
            return SwingCustomizationSnapshot(itemTransforms[normalizedId]?.snapshot())
        }

        fun restoreCustomization(itemId: String?, snapshot: SwingCustomizationSnapshot) {
            if (itemId == null) {
                val transform = requireNotNull(snapshot.transform) { "Global swing history is missing its transform" }
                global.restore(transform)
                return
            }
            val normalizedId = normalizedItemId(itemId)
            snapshot.transform?.let { itemTransforms[normalizedId] = it.toConfig() } ?: itemTransforms.remove(normalizedId)
        }

        fun repairLoadedValues() {
            global.repairLoadedValues()
            val repairedTransforms = linkedMapOf<String, SwingTransformConfig>()
            itemTransforms.forEach { (itemId, transform) ->
                val normalizedId = normalizedItemId(itemId)
                if (normalizedId.isNotEmpty()) {
                    transform.repairLoadedValues()
                    repairedTransforms[normalizedId] = transform
                }
            }
            itemTransforms = repairedTransforms
            if (editorX < AUTO_EDITOR_POSITION) editorX = AUTO_EDITOR_POSITION
            if (editorY < AUTO_EDITOR_POSITION) editorY = AUTO_EDITOR_POSITION
        }

        fun applyLegacyMigration() {
            if (migratedToEditor) return
            global.x = offX.toFloat()
            global.y = offY.toFloat()
            global.z = offZ.toFloat()
            if (swingSpeed > 0.0) {
                global.swingSpeed = (6.0 / swingSpeed).coerceIn(0.25, 3.0).toFloat()
            }
            migratedToEditor = true
        }

        private fun SwingTransformConfig.restore(snapshot: SwingTransformSnapshot) {
            x = snapshot.x
            y = snapshot.y
            z = snapshot.z
            scale = snapshot.scale
            swingSpeed = snapshot.swingSpeed
            swingStyle = snapshot.swingStyle
            rotationX = snapshot.rotationX
            rotationY = snapshot.rotationY
            rotationZ = snapshot.rotationZ
        }

        companion object {
            const val AUTO_EDITOR_POSITION = -1

            fun normalizedItemId(itemId: String): String = itemId.trim().uppercase(java.util.Locale.US)
        }
    }

    class BlockOverlayConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает подсветку выбранного блока и настройку цветов в формате A:R:G:B")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Цвет заливки", desc = "Цвет заливки выбранного блока в формате A:R:G:B")
        @ConfigEditorColour
        var fillColor = "0:80:0:255:0"

        @Expose
        @ConfigOption(name = "Цвет обводки", desc = "Цвет обводки выбранного блока в формате A:R:G:B (0 уровень — без обводки)")
        @ConfigEditorColour
        var outlineColor = "0:180:255:255:255"
    }

    class StatsHudConfig {
        @Expose
        @ConfigOption(name = "КПС", desc = "Отдельный HUD кликов в секунду")
        @Accordion
        var cps = StatHudConfig()

        @Expose
        @ConfigOption(name = "ФПС", desc = "Отдельный HUD кадров в секунду")
        @Accordion
        var fps = StatHudConfig()

        @Expose
        @ConfigOption(name = "Пинг", desc = "Отдельный HUD пинга до сервера")
        @Accordion
        var ping = StatHudConfig()

        @Expose
        @ConfigOption(name = "БПС", desc = "Отдельный HUD блоков в секунду")
        @Accordion
        var bps = StatHudConfig()
    }

    class StatHudConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывать отдельный HUD (позиция и масштаб — в редакторе HUD)")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Цвет значения", desc = "Цвет числового значения")
        @ConfigEditorColour
        var valueColorV2 = "0:255:85:255:85"

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображать фон позади текста")
        @ConfigEditorBoolean
        var showBackground = false
    }
}
