package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class VisualsConfig {

    @Expose
    @ConfigOption(name = "Анимации", desc = "Кастомные анимации для игрока и предметов.")
    @Accordion
    var animations = AnimationsConfig()

    @Expose
    @ConfigOption(name = "Новогодние приколы", desc = "§fНовогодние визуальные эффекты.")
    @Accordion
    var newYear = NewYearVisualsConfig()

    class AnimationsConfig {
        @Expose
        @ConfigOption(name = "Анимация удара", desc = "§c[BETA] §fИзменяет положение и скорость анимации руки.")
        @Accordion
        var swingAnimation = SwingAnimationConfig()

    }

    class SwingAnimationConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включает кастомную анимацию удара.")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Без анимации экипировки", desc = "Отключает анимацию поднятия предмета при его смене.")
        @ConfigEditorBoolean
        var noEquipAnimation = false

        @Expose
        @ConfigOption(name = "Скорость удара", desc = "Изменяет длительность анимации удара (в тиках). Чем меньше значение, тем быстрее анимация. (0 - стандарт)")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 20.0f, minStep = 1.0f)
        var swingSpeed = 0.0

        @Expose
        @ConfigOption(name = "Положение X", desc = "Статичное смещение предмета по горизонтали.")
        @ConfigEditorSlider(minValue = -2.0f, maxValue = 2.0f, minStep = 0.05f)
        var offX = 0.0

        @Expose
        @ConfigOption(name = "Положение Y", desc = "Статичное смещение предмета по вертикали.")
        @ConfigEditorSlider(minValue = -2.0f, maxValue = 2.0f, minStep = 0.05f)
        var offY = 0.0

        @Expose
        @ConfigOption(name = "Положение Z", desc = "Статичное смещение предмета в глубину.")
        @ConfigEditorSlider(minValue = -2.0f, maxValue = 2.0f, minStep = 0.05f)
        var offZ = 0.0

        @Expose
        @ConfigOption(name = "Множитель Swing X", desc = "Множитель анимации удара по X.")
        @ConfigEditorSlider(minValue = -2.0f, maxValue = 2.0f, minStep = 0.05f)
        var swingX = 1.0

        @Expose
        @ConfigOption(name = "Множитель Swing Y", desc = "Множитель анимации удара по Y.")
        @ConfigEditorSlider(minValue = -2.0f, maxValue = 2.0f, minStep = 0.05f)
        var swingY = 1.0

        @Expose
        @ConfigOption(name = "Множитель Swing Z", desc = "Множитель анимации удара по Z.")
        @ConfigEditorSlider(minValue = -2.0f, maxValue = 2.0f, minStep = 0.05f)
        var swingZ = 1.0
    }

    class NewYearVisualsConfig {
        @Expose
        @ConfigOption(name = "§fЗимняя атмосфера", desc = "Включает зимний биом и визуальный дождь (снег) в обычном мире.")
        @ConfigEditorBoolean
        var winterAtmosphere = true

        @ConfigOption(name = "§fСбросить диалог NPC", desc = "Сбрасывает прогресс диалога с Пингвином, позволяя поговорить с ним снова.")
        @io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton(buttonText = "Сбросить")
        val resetNPCDialogue: Runnable = Runnable {
            hasTalkedToPenguin = false
            set.starlev.features.visual.GhostNPCHandler.resetDialogue()
            net.minecraft.client.Minecraft.getInstance().player?.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§e[StarredHeltix] §fДиалог с NPC сброшен!"),
                false
            )
        }

        @Expose
        @ConfigOption(name = "Показывать Пингвина", desc = "Отображает новогоднего NPC-Пингвина.")
        @ConfigEditorBoolean
        var ghostNPC = true

        @Expose
        @ConfigOption(name = "Показывать картинку 1", desc = "Отображает первую секретную картинку.")
        @ConfigEditorBoolean
        var secretFrame = true

        @Expose
        @ConfigOption(name = "Показывать картинку 2", desc = "Отображает вторую секретную картинку.")
        @ConfigEditorBoolean
        var secretFrame2 = true

        @Expose
        var hasTalkedToNPC = false

        @Expose
        var hasTalkedToPenguin = false
    }
}

