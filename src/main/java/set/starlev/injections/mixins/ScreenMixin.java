package set.starlev.injections.mixins;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.render.RenderEngine;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onRenderHead(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object) this instanceof AbstractContainerScreen)) {
            // Не рисуем HUD под редактором — редактор сам отрисует его нижним слоем, чтобы кнопки были поверх
            if ((Object) this instanceof set.starlev.render.HudEditorScreen) return;
            if ((Object) this instanceof set.starlev.render.ScoreboardEditorScreen) return;
            RenderEngine.renderHud(graphics, net.minecraft.client.Minecraft.getInstance().getDeltaTracker());
        }
    }
}
