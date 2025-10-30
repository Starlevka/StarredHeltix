package set.starlev.starredheltix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Shadow private List<ChatHudLine.Visible> visibleMessages;
    @Shadow private List<ChatHudLine> messages;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Check if Left Shift is pressed
        if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
            ChatHud chatHud = (ChatHud)(Object)this;
            
            // Calculate message index from mouse position
            int lineHeight = 9; // Default chat line height
            int chatHeight = client.getWindow().getScaledHeight() - 40;
            int messageIndex = (int) ((chatHeight - mouseY) / lineHeight);
            
            if (messageIndex >= 0 && messageIndex < messages.size()) {
                ChatHudLine line = messages.get(messageIndex);
                String messageText = line.content().getString();

                // Copy to clipboard
                client.keyboard.setClipboard(messageText);

                // Send feedback message in action bar
                assert client.player != null;
                client.player.sendMessage(Text.literal("§aСообщение скопировано в буфер обмена"), true);

                cir.setReturnValue(true);
            }
        }
    }
}