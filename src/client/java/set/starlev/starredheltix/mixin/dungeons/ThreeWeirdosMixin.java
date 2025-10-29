package set.starlev.starredheltix.mixin.dungeons;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// import set.starlev.starredheltix.util.solver.dungeons.ThreeWeirdosSolver;

@Mixin(ClientPlayNetworkHandler.class)
public class ThreeWeirdosMixin {
    /**
     * Detect Three Weirdos puzzle by monitoring game messages
     */
    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        // This method is called when a game message is received
        Text message = packet.content();
        if (message != null && message.getString() != null) {
            String text = message.getString();
            // Look for Three Weirdos related keywords in game messages
            if (text.contains("[Персонаж]") || text.contains("[NPC]") || text.contains("Character")) {
                // This is likely a Three Weirdos puzzle
                // ThreeWeirdosSolver.setInThreeWeirdosPuzzle();
            }
            
            // Also check for pattern that indicates leaving the puzzle
            if (text.matches(".*(?:won|проиграли|game over|победил|выиграл|вы проиграли|вы победили|поздравляем|congratulations|defeat|victory|chest|сундук|награда|reward|получите награду|reward chest).*$")) {
                // ThreeWeirdosSolver.setNotInThreeWeirdosPuzzle();
            }
        }
    }
}