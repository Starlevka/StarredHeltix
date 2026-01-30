package set.starlev.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;
import set.starlev.secret.features.SecretFunFeatures;
import set.starlev.secret.config.SecretMenuManager;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Display;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    private final java.util.Map<T, Component> starred_cache = new java.util.WeakHashMap<>();

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void onShouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (entity == null) return;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.world.phys.Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        // ArmorStand culling
        if (entity instanceof ArmorStand) {
            if (StarredHeltix.getFeature().getOptimization().getEntityOptimization().getCullArmorStands()) {
                double distanceSq = entity.distanceToSqr(cameraPos);
                double maxDistance = StarredHeltix.getFeature().getOptimization().getEntityOptimization().getArmorStandDistance();
                if (distanceSq > maxDistance * maxDistance) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }

        // Display Entity culling
        if (entity instanceof Display) {
            if (StarredHeltix.getFeature().getOptimization().getEntityOptimization().getCullDisplayEntities()) {
                double distanceSq = entity.distanceToSqr(cameraPos);
                double maxDistance = StarredHeltix.getFeature().getOptimization().getEntityOptimization().getDisplayEntityDistance();
                if (distanceSq > maxDistance * maxDistance) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void onGetNameTag(T entity, CallbackInfoReturnable<Component> cir) {
        if (!SecretMenuManager.INSTANCE.isConfigInitialized() || !SecretMenuManager.INSTANCE.getSecretConfig().getFunCategory().getStarlevNameEffect()) {
            return;
        }

        Component original = cir.getReturnValue();
        if (original == null) return;

        Component cached = starred_cache.get(entity);
        if (cached != null && cached.getString().equals(original.getString())) {
            cir.setReturnValue(cached);
            return;
        }

        Component modified = SecretFunFeatures.processComponent(original);
        if (modified != original) {
            starred_cache.put(entity, modified);
            cir.setReturnValue(modified);
        }
    }

    @org.spongepowered.asm.mixin.injection.ModifyArg(
        method = "submitNameTag", 
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V"), 
        index = 3
    )
    private Component onModifyNameTagArg(Component component) {
        if (!SecretMenuManager.INSTANCE.isConfigInitialized() || !SecretMenuManager.INSTANCE.getSecretConfig().getFunCategory().getStarlevNameEffect()) {
            return component;
        }
        if (component == null) return null;
        return SecretFunFeatures.processComponent(component);
    }
}
