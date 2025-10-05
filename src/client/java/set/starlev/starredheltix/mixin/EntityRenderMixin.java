package set.starlev.starredheltix.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.client.StarredHeltixClient;
import set.starlev.starredheltix.util.entity.CharacterHighlighter;

@Mixin(EntityRenderer.class)
public class EntityRenderMixin {
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void onEntityRender(EntityRenderState entityRenderState, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        // Check if the feature is enabled
        if (!StarredHeltixClient.CONFIG.general.enabled) return;
        
        // We can't directly access the entity from EntityRenderState in this mixin
        // The character highlighting is already handled by WorldRenderEvents in CharacterHighlighter
        // So we don't need to do anything here
    }
}