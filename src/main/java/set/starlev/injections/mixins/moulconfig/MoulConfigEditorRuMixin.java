package set.starlev.injections.mixins.moulconfig;

import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Русский язык в меню настроек: MoulConfig держит свои строки как обычные
 * Java-литералы (никаких translation-ключей), поэтому лечится только заменой
 * констант в байткоде. Косметика — require = 0, чтобы не падать на старте,
 * если версия библиотеки изменится.
 *
 * В форке Mixin от Fabric у @ModifyConstant нет at(), а у @Constant поле называется
 * stringValue().
 */
@Mixin(MoulConfigEditor.class)
public class MoulConfigEditorRuMixin {

    @ModifyConstant(method = "render()V", constant = @Constant(stringValue = "Categories"), require = 0)
    private String ru$categories(String original) {
        return "Категории";
    }

    @ModifyConstant(
        method = "render()V",
        constant = @Constant(stringValue = "Seems like your search is found in a subcategory."),
        require = 0
    )
    private String ru$searchInSubcategory(String original) {
        return "Похоже, твой поиск нашёл подкатегорию.";
    }

    @ModifyConstant(
        method = "render()V",
        constant = @Constant(stringValue = "Check out the subcategories on the left."),
        require = 0
    )
    private String ru$checkSubcategories(String original) {
        return "Посмотри подкатегории в списке слева.";
    }

    @ModifyConstant(method = "render()V", constant = @Constant(stringValue = "No options found."), require = 0)
    private String ru$noOptionsFound(String original) {
        return "Ничего не найдено.";
    }
}