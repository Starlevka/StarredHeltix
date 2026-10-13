package set.starlev.injections.mixins.moulconfig;

import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDraggableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Перетаскиваемые списки: «Add» → «Добавить», «<NONE>» → «<НЕТ>». */
@Mixin(GuiOptionEditorDraggableList.class)
public class MoulConfigDraggableListRuMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = " Add "), require = 0)
    private String ru$add(String original) {
        return " Добавить ";
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "<NONE>"), require = 0)
    private String ru$none(String original) {
        return "<НЕТ>";
    }
}