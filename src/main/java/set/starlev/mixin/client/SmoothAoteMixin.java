package set.starlev.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.skyblock.SmoothAote;

@Mixin(ClientPacketListener.class)
public class SmoothAoteMixin {
    private Vec3 starredheltix$preMoveCameraPos;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void onHandleMovePlayerHead(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        this.starredheltix$preMoveCameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void onHandleMovePlayer(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().player != null) {
            Vec3 pre = this.starredheltix$preMoveCameraPos;
            Vec3 post = Minecraft.getInstance().player.position();
            if (pre != null) {
                SmoothAote.INSTANCE.onTeleport(pre, post);
            } else {
                SmoothAote.INSTANCE.onTeleport(post);
            }
        }
    }
}
