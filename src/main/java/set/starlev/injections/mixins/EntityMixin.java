package set.starlev.injections.mixins;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.features.combat.EntityHighlight;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void starredheltix$forceClientGlow(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        Entity entity = (Entity) (Object) this;
        int id = entity.getId();
        // Модульный glow — без проверки lineOfSight (сквозь стены, как CTJS)
        if (set.starlev.scripting.ModuleGlowRegistry.getGlowColor(id) != -1) {
            cir.setReturnValue(true);
            return;
        }
        // Floor4 bear spirit — тоже сквозь стены (раньше был ванильный flag)
        if (set.starlev.features.combat.dungeons.Floor4.getGlowColor(id) != -1) {
            cir.setReturnValue(true);
            return;
        }
        if (EntityHighlight.getGlowColor(id) != -1) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && mc.player != entity && !mc.player.hasLineOfSight(entity)) {
                return;
            }
            cir.setReturnValue(true);
            return;
        }
        // Подсветка по клику — форс так же переживает перезапись флага метаданными с сервера,
        // но только при прямой видимости: сквозь стены glow НЕ светит.
        if (set.starlev.features.combat.ClickHighlight.getGlowColor(id) != -1) {
            net.minecraft.client.Minecraft mc2 = net.minecraft.client.Minecraft.getInstance();
            if (mc2.player != null && mc2.player != entity && !mc2.player.hasLineOfSight(entity)) {
                return;
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void starredheltix$overrideGlowColor(CallbackInfoReturnable<Integer> cir) {
        int id = ((Entity) (Object) this).getId();
        // Подсветка по клику (СКМ) — явный выбор игрока,highest приоритет
        int clickColor = set.starlev.features.combat.ClickHighlight.getGlowColor(id);
        if (clickColor != -1) {
            cir.setReturnValue(clickColor);
            return;
        }
        int modColor = set.starlev.scripting.ModuleGlowRegistry.getGlowColor(id);
        if (modColor != -1) {
            cir.setReturnValue(modColor);
            return;
        }
        int floorColor = set.starlev.features.combat.dungeons.Floor4.getGlowColor(id);
        if (floorColor != -1) {
            cir.setReturnValue(floorColor);
            return;
        }
        int color = EntityHighlight.getGlowColor(id);
        if (color != -1) {
            cir.setReturnValue(color);
        }
    }
}
