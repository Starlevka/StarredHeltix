package set.starlev.injections.mixin.render.entity;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.features.combat.EntityHighlight;

@Mixin(Entity.class)
public class EntityGlowColorMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void starredheltix$forceClientGlow(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        Entity entity = (Entity) (Object) this;
        int id = entity.getId();
        if (EntityHighlight.getGlowColor(id) != -1) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && mc.player != entity && !mc.player.hasLineOfSight(entity)) {
                return;
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void starredheltix$overrideGlowColor(CallbackInfoReturnable<Integer> cir) {
        int id = ((Entity) (Object) this).getId();
        int color = EntityHighlight.getGlowColor(id);
        if (color != -1) {
            cir.setReturnValue(color);
        }
    }
}
