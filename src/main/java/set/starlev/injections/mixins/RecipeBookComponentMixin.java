package set.starlev.injections.mixins;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.features.visual.RecipeBookHide;

@Mixin(RecipeBookComponent.class)
public class RecipeBookComponentMixin {

    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void starredheltix$forceNotVisible(CallbackInfoReturnable<Boolean> cir) {
        if (RecipeBookHide.INSTANCE.isHidden()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void starredheltix$hideRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (RecipeBookHide.INSTANCE.isHidden()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractGhostRecipe", at = @At("HEAD"), cancellable = true)
    private void starredheltix$hideGhost(GuiGraphicsExtractor graphics, boolean isBiggerResultSlot, CallbackInfo ci) {
        if (RecipeBookHide.INSTANCE.isHidden()) {
            ci.cancel();
        }
    }
}
