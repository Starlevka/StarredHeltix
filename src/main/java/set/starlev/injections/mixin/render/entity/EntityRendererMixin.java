package set.starlev.injections.mixin.render.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;
import set.starlev.utils.detectors.DungeonDetector;
import set.starlev.utils.detectors.MobHeadDisplayDetector;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Display;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void onShouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        var config = StarredHeltix.getFeature().getOptimization().getVisualOptimizations();
        
        if (config.getHideDeathAnimation() && entity instanceof LivingEntity living && living.isDeadOrDying()) {
            cir.setReturnValue(false);
            return;
        }
        
        if (entity instanceof Display.TextDisplay textDisplay) {
            if (config.getHideAllMobNames()) {
                if (starredheltix$isMobNametag(textDisplay)) {
                    cir.setReturnValue(false);
                    return;
                }
            }

            if (config.getHideNonStarredDungeonNames() && DungeonDetector.INSTANCE.isInDungeon()) {
                if (starredheltix$isMobNametag(textDisplay)) {
                    if (!starredheltix$isStarredText(textDisplay)) {
                        cir.setReturnValue(false);
                    }
                }
            }
        }
    }

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void onShouldShowName(T entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        var config = StarredHeltix.getFeature().getOptimization().getVisualOptimizations();
        
        if (config.getHideAllMobNames()) {
            cir.setReturnValue(false);
            return;
        }

        if (config.getHideNonStarredDungeonNames() && DungeonDetector.INSTANCE.isInDungeon()) {
            if (entity instanceof LivingEntity living) {
                if (!starredheltix$isStarred(living)) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    private boolean starredheltix$isMobNametag(Display.TextDisplay textDisplay) {
        // Проверяем, привязан ли дисплей к кому-то или находится ли он очень близко к мобу
        if (textDisplay.getVehicle() instanceof LivingEntity) return true;
        
        // Поиск ближайшего LivingEntity в радиусе 1.5 блоков (типично для ников)
        var box = textDisplay.getBoundingBox().inflate(1.5);
        var nearby = textDisplay.level().getEntitiesOfClass(LivingEntity.class, box, e -> true);
        return !nearby.isEmpty();
    }

    private boolean starredheltix$isStarredText(Display.TextDisplay textDisplay) {
        return textDisplay.getText().getString().contains("✯");
    }

    private boolean starredheltix$isStarred(LivingEntity entity) {
        var displays = MobHeadDisplayDetector.INSTANCE.getHeadDisplays(entity);
        for (var comp : displays.getTextDisplays()) {
            if (comp.getString().contains("✯")) {
                return true;
            }
        }
        return false;
    }
}
