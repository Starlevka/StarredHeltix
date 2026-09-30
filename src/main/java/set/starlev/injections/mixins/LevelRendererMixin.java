package set.starlev.injections.mixins;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import set.starlev.render.RenderContext;
import set.starlev.render.RenderEvents;
import set.starlev.render.RenderEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.visual.BlockOverlayFeature;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {


    @Unique
    private static void starredheltix$onCollectSubmits(LevelRenderContext context) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        var poseStack = context.poseStack();

        RenderContext renderContext = new RenderContext(
            poseStack,
            mc.gameRenderer.mainCamera(),
            mc.getDeltaTracker().getGameTimeDeltaTicks(),
            context.submitNodeCollector(),
            mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState
        );

        RenderEvents.fireWorldRender(renderContext);
        set.starlev.events.StarredEvents.INSTANCE.getWorldRender().post(renderContext);
        RenderEngine.renderWorld(renderContext);
    }

    static {
        LevelRenderEvents.COLLECT_SUBMITS.register(LevelRendererMixin::starredheltix$onCollectSubmits);
    }



    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onSubmitBlockOutline(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        HitResult hitResult = mc.hitResult;

        if (hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockState blockState = mc.level.getBlockState(blockPos);
            Entity entity = mc.getCameraEntity();

            if (entity != null && BlockOverlayFeature.INSTANCE.onRenderBlockOverlay(poseStack, entity, 0, 0, 0, blockPos, blockState, submitNodeCollector, levelRenderState.cameraRenderState)) {
                ci.cancel();
            }
        }
    }

}