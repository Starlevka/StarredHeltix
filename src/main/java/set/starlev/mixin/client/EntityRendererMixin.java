package set.starlev.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.secret.features.SecretFunFeatures;

@Mixin(LivingEntityRenderer.class)
public abstract class EntityRendererMixin {
    private static java.lang.reflect.Field starlev$idField;

    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void onSetupRotations(LivingEntityRenderState state, PoseStack poseStack, float f, float g, CallbackInfo ci) {
        if (SecretFunFeatures.INSTANCE.isFlipEnabled()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;

            boolean isLocalPlayer = false;

            // В новых версиях Minecraft (1.21.2+) EntityRenderState не всегда имеет id напрямую в маппингах.
            // Мы пытаемся получить его максимально надежно через рефлексию.
            try {
                if (starlev$idField == null) {
                    // Ищем поле, которое может содержать ID или признак локального игрока
                    for (java.lang.reflect.Field field : state.getClass().getFields()) {
                        if ((field.getName().equals("id") || field.getName().equals("isLocalPlayer")) && 
                            (field.getType() == int.class || field.getType() == boolean.class)) {
                            field.setAccessible(true);
                            starlev$idField = field;
                            break;
                        }
                    }
                    
                    // Если не нашли по имени, ищем по значению (как было раньше)
                    if (starlev$idField == null) {
                        for (java.lang.reflect.Field field : state.getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            if (field.getType() == int.class && field.getInt(state) == mc.player.getId()) {
                                starlev$idField = field;
                                isLocalPlayer = true;
                                break;
                            }
                            if (field.getType() == boolean.class && field.getBoolean(state) && state instanceof AvatarRenderState) {
                                // Вероятно это isLocalPlayer
                                starlev$idField = field;
                                isLocalPlayer = true;
                                break;
                            }
                        }
                    }
                }
                
                if (starlev$idField != null) {
                    if (starlev$idField.getType() == int.class) {
                        if (starlev$idField.getInt(state) == mc.player.getId()) {
                            isLocalPlayer = true;
                        }
                    } else if (starlev$idField.getType() == boolean.class) {
                        if (starlev$idField.getBoolean(state)) {
                            isLocalPlayer = true;
                        }
                    }
                }
            } catch (Exception ignored) {}
            
            if (isLocalPlayer) {
                poseStack.translate(0.0D, state.boundingBoxHeight + 0.1F, 0.0D);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            }
        }
    }
}
