package set.starlev.starredheltix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.util.chat.ModerationManager;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatMuteMixin {
    
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        String playerName = client.player.getName().getString();
        
        if (ModerationManager.isPlayerMuted(playerName)) {
            ModerationManager.MuteInfo muteInfo = ModerationManager.getMuteInfo(playerName);
            if (muteInfo != null) {
                long remainingTime = muteInfo.getRemainingTime();
                String timeLeft = formatTime(remainingTime);
                
                client.player.sendMessage(Text.literal("§c§l[МЬЮТ] §cВы не можете писать в чат! Осталось: §e" + timeLeft), false);
                ci.cancel();
            }
        }
    }
    
    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void onSendChatCommand(String command, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        String playerName = client.player.getName().getString();
        
        // Allow certain commands even when muted
        if (command.startsWith("pc ") || command.startsWith("party ") || command.startsWith("p ") || 
            command.startsWith("msg ") || command.startsWith("w ") || command.startsWith("tell ")) {
            
            if (ModerationManager.isPlayerMuted(playerName)) {
                ModerationManager.MuteInfo muteInfo = ModerationManager.getMuteInfo(playerName);
                if (muteInfo != null) {
                    long remainingTime = muteInfo.getRemainingTime();
                    String timeLeft = formatTime(remainingTime);
                    
                    client.player.sendMessage(Text.literal("§c§l[МЬЮТ] §cВы не можете писать в чат! Осталось: §e" + timeLeft), false);
                    ci.cancel();
                }
            }
        }
    }
    
    private String formatTime(long timeMs) {
        long seconds = timeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + " дн. " + (hours % 24) + " ч.";
        } else if (hours > 0) {
            return hours + " ч. " + (minutes % 60) + " мин.";
        } else if (minutes > 0) {
            return minutes + " мин. " + (seconds % 60) + " сек.";
        } else {
            return seconds + " сек.";
        }
    }
}