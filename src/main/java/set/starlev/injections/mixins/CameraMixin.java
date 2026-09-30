package set.starlev.injections.mixins;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.skyblock.SmoothAote;
import set.starlev.scripting.CameraController;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    public abstract Vec3 position();

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onExtractRenderState(net.minecraft.client.renderer.state.level.CameraRenderState cameraState, float partialTick, CallbackInfo ci) {
        // Кастомная камера (JS API `camera`): подмена позиции и/или поворота
        if (CameraController.INSTANCE.getDetached()) {
            this.setPosition(CameraController.INSTANCE.getX(), CameraController.INSTANCE.getY(), CameraController.INSTANCE.getZ());
            if (CameraController.INSTANCE.getRotOverride()) {
                this.setRotation(CameraController.INSTANCE.getYaw(), CameraController.INSTANCE.getPitch());
            }
            return;
        }
        Vec3 offset = SmoothAote.INSTANCE.getOffset();
        if (offset != null) {
            Vec3 current = this.position();
            this.setPosition(current.x + offset.x, current.y + offset.y, current.z + offset.z);
        }
    }
}
