package set.starlev.injections.mixins;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.events.GuiEvents;
import set.starlev.features.visual.RecipeBookHide;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {

    @Inject(method = "initButton", at = @At("HEAD"), cancellable = true)
    private void starredheltix$hideRecipeBookButton(CallbackInfo ci) {
        if (RecipeBookHide.INSTANCE.isHidden()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onRenderTail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object) this).getClass().getName().startsWith("set.starlev.")) {
            GuiEvents.fireForeground(graphics, mouseX, mouseY, (AbstractContainerScreen<?>) (Object) this);
        }
    }
}
