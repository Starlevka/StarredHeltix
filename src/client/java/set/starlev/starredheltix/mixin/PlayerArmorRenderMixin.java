package set.starlev.starredheltix.mixin;

import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.client.StarredHeltixClient;

@Mixin(ArmorFeatureRenderer.class)
public class PlayerArmorRenderMixin {
    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private void renderArmorPiece(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack, EquipmentSlot slot, int light, BipedEntityModel<?> armorModel, CallbackInfo ci) {
        // Check if armor hiding is enabled in config
        if (StarredHeltixClient.CONFIG.armorHiding.enabled) {
            // Cancel the armor rendering
            ci.cancel();
        }
    }
}