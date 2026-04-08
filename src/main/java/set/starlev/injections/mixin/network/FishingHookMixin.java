package set.starlev.injections.mixin.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.fishing.FishingNotifier;

@Mixin(FishingHook.class)
public class FishingHookMixin {
    @Shadow
    @Final
    private static EntityDataAccessor<Boolean> DATA_BITING;

    private boolean starredheltix$wasBiting;

    @Inject(method = "onSyncedDataUpdated", at = @At("TAIL"))
    private void starredheltix$onSyncedDataUpdated(EntityDataAccessor<?> accessor, CallbackInfo ci) {
        if (accessor != DATA_BITING) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        FishingHook hook = client.player.fishing;
        if (hook == null) return;

        if ((Object) this != hook) return;

        boolean isBiting = hook.getEntityData().get(DATA_BITING);
        if (isBiting && !starredheltix$wasBiting) FishingNotifier.INSTANCE.onBite();
        starredheltix$wasBiting = isBiting;
    }
}
