package set.starlev.starredheltix.mixin;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.Clipboard;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen {
    @Shadow
    protected TextFieldWidget chatField;

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // Check if Shift is pressed and right mouse button is clicked
        if (button == 1 && hasShiftDown()) { // 1 is right mouse button
            // Copy the content of the chat field to clipboard
            if (chatField != null) {
                String text = chatField.getText();
                if (!text.isEmpty()) {
                    try (Clipboard clipboard = new Clipboard()) {
                        clipboard.setClipboard(client, text);
                    }
                    
                    // Cancel the original mouse click to prevent context menu
                    cir.setReturnValue(true);
                }
            }
        }
    }
}