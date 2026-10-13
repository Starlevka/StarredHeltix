package set.starlev.injections.mixins.moulconfig;

import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorKeybind;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Клавиши в меню настроек: подсказка «Reset to Default» → «Сбросить». */
@Mixin(GuiOptionEditorKeybind.class)
public class MoulConfigKeybindRuMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Reset to Default"), require = 0)
    private String ru$resetToDefault(String original) {
        return "Сбросить";
    }
}