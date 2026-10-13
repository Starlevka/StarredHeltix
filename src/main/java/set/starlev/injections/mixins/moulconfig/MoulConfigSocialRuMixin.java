package set.starlev.injections.mixins.moulconfig;

import io.github.notenoughupdates.moulconfig.Social;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Кликабельные соцссылки в шапке меню: «Click here to open X» → «Нажми, чтобы открыть X». */
@Mixin(Social.class)
public class MoulConfigSocialRuMixin {

    @ModifyConstant(method = "forLink", constant = @Constant(stringValue = "Click here to open "), require = 0)
    private String ru$clickHereToOpen(String original) {
        return "Нажми, чтобы открыть ";
    }
}