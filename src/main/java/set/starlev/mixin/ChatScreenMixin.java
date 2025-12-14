package set.starlev.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.features.chat.ChatCopyFeature;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        try {
            ChatCopyFeature.INSTANCE.handleRender(graphics, mouseX, mouseY);
        } catch (Exception e) {
            System.err.println("Error in ChatCopyFeature.handleRender: " + e);
            e.printStackTrace();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("ChatScreen.mouseClicked called with button=" + event.button() + " at (" + event.x() + ", " + event.y() + ") doubleClick=" + doubleClick);
        try {
            if (ChatCopyFeature.INSTANCE.handleMouseClick(event.x(), event.y(), event.button())) {
                System.out.println("ChatCopyFeature handled the click");
                cir.setReturnValue(true);
            }
        } catch (Exception e) {
            System.err.println("Error in ChatCopyFeature.handleMouseClick: " + e);
            e.printStackTrace();
        }
    }
}
