

package moe.nea.firmament.gui.config

import io.github.notenoughupdates.moulconfig.gui.component.TextFieldComponent
import io.github.notenoughupdates.moulconfig.observer.GetSetter
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.network.chat.Component
import moe.nea.firmament.util.data.ManagedConfig

class StringHandler(val config: ManagedConfig) : ManagedConfig.OptionHandler<String> {
    override fun toJson(element: String): JsonElement? {
        return JsonPrimitive(element)
    }

    override fun fromJson(element: JsonElement): String {
        return element.jsonPrimitive.content
    }

    override fun emitGuiElements(opt: ManagedOption<String>, guiAppender: GuiAppender) {
        guiAppender.appendLabeledRow(
            opt.labelText,
            TextFieldComponent(
                object : GetSetter<String> by opt {
                    override fun set(newValue: String) {
                        opt.set(newValue)
                        config.markDirty()
                    }
                },
                130,
                suggestion = Component.translatableWithFallback(opt.rawLabelText + ".hint", "").string
            ),
        )
    }
}
