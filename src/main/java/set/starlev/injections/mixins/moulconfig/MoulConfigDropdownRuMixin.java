package set.starlev.injections.mixins.moulconfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Выпадающие списки MoulConfig: «- Select -» и «<NONE>».
 *
 * Строки лежат в анонимных внутренних классах компонента списка, поэтому целимся
 * сразу в оба ($1 — оверле выпадающего меню, $2 — основной компонент) и во все
 * методы («*»), чтобы не зависеть от того, куда компилятор положил литерал.
 */
@Mixin(targets = {
    "io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDropdown$1",
    "io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDropdown$2"
})
public class MoulConfigDropdownRuMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = " - Select - "), require = 0)
    private String ru$select(String original) {
        return " - Выбрать - ";
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "<NONE>"), require = 0)
    private String ru$none(String original) {
        return "<НЕТ>";
    }
}