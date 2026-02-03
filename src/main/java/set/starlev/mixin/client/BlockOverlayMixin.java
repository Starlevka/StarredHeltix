package set.starlev.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Coerce;
import set.starlev.features.visual.BlockOverlayFeature;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

@Mixin(LevelRenderer.class)
public class BlockOverlayMixin {

    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void onRenderHitOutline(PoseStack poseStack, VertexConsumer vertexConsumer, double x, double y, double z, @Coerce Object shape, int color, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        HitResult hitResult = mc.hitResult;
        
        if (hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockState blockState = mc.level.getBlockState(blockPos);
            Entity entity = mc.getCameraEntity();
            
            if (entity != null && BlockOverlayFeature.INSTANCE.onRenderBlockOverlay(poseStack, entity, x, y, z, blockPos, blockState)) {
                ci.cancel();
            }
        }
    }
}
