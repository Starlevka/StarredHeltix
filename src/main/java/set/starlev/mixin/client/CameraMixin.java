package set.starlev.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.skyblock.SmoothAote;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    public abstract Vec3 getPosition();

    @Inject(method = "setup", at = @At("RETURN"))
    private void onSetup(BlockGetter level, Entity entity, boolean detached, boolean thirdPerson, float partialTick, CallbackInfo ci) {
        Vec3 offset = SmoothAote.INSTANCE.getOffset();
        if (offset != null) {
            Vec3 current = this.getPosition();
            this.setPosition(current.x + offset.x, current.y + offset.y, current.z + offset.z);
        }
    }
}
