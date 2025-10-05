package set.starlev.starredheltix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.util.user.ModUserManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class ChatPlayerNameIconMixin {
    // Pattern to match player names in chat messages
    @Unique
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("<([^>]+)>");
    
    // Flag to prevent infinite recursion
    @Unique
    private static boolean isProcessing = false;

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        // Prevent infinite recursion
        if (isProcessing) {
            return;
        }
        
        String messageString = message.getString();
        Matcher matcher = PLAYER_NAME_PATTERN.matcher(messageString);
        
        // Check if this is a standard chat message with a player name
        if (matcher.find()) {
            String playerName = matcher.group(1);
            
            // Find the player in the player list
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getNetworkHandler() != null) {
                // Check other players in the player list
                for (PlayerListEntry player : client.getNetworkHandler().getPlayerList()) {
                    if (player.getProfile() != null && playerName.equals(player.getProfile().getName())) {
                        // In a client-only mod, we show stars for all players
                        // Check if this player should have the mod icon
                        if (ModUserManager.getInstance().isModUser(player.getProfile().getId())) {
                            // Add the star icon for mod users
                            String newMessageString = messageString.replaceFirst("<" + playerName + ">",
                                "<" + playerName + " ⭐>");
                            Text newMessage = Text.literal(newMessageString);
                            
                            // Set processing flag to prevent recursion
                            isProcessing = true;
                            
                            // Cancel the original message and send our modified version
                            ci.cancel();
                            
                            // Use the ChatHud instance to add our new message
                            ChatHud chatHud = (ChatHud) (Object) this;
                            chatHud.addMessage(newMessage, signature, indicator);
                            
                            // Reset processing flag
                            isProcessing = false;
                        }
                        return;
                    }
                }
                
                // Check if this is the current player
                if (client.player != null && playerName.equals(client.player.getName().getString())) {
                    // In a client-only mod, the current player should always have the star
                    // Check if current player should have the mod icon
                    if (ModUserManager.getInstance().isModUser(client.player.getUuid())) {
                        // This is the current player, add the icon
                        String newMessageString = messageString.replaceFirst("<" + playerName + ">",
                                "<" + playerName + " ⭐>");
                        Text newMessage = Text.literal(newMessageString);
                        
                        // Set processing flag to prevent recursion
                        isProcessing = true;
                        
                        // Cancel the original message and send our modified version
                        ci.cancel();
                        
                        // Use the ChatHud instance to add our new message
                        ChatHud chatHud = (ChatHud) (Object) this;
                        chatHud.addMessage(newMessage, signature, indicator);
                        
                        // Reset processing flag
                        isProcessing = false;
                    }
                    return;
                }
            }
        }
    }
}