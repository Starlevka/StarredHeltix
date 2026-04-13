package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fc;

public class BlockFeatureRenderer {
   private final PoseStack poseStack = new PoseStack();

   public BlockFeatureRenderer() {
      super();
   }

   public void render(SubmitNodeCollection var1, MultiBufferSource.BufferSource var2, BlockRenderDispatcher var3, OutlineBufferSource var4) {
      Iterator var5 = var1.getMovingBlockSubmits().iterator();

      while(var5.hasNext()) {
         SubmitNodeStorage.MovingBlockSubmit var6 = (SubmitNodeStorage.MovingBlockSubmit)var5.next();
         MovingBlockRenderState var7 = var6.movingBlockRenderState();
         BlockState var8 = var7.blockState;
         List var9 = var3.getBlockModel(var8).collectParts(RandomSource.create(var8.getSeed(var7.randomSeedPos)));
         PoseStack var10 = new PoseStack();
         var10.mulPose((Matrix4fc)var6.pose());
         var3.getModelRenderer().tesselateBlock(var7, var9, var8, var7.blockPos, var10, var2.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(var8)), false, OverlayTexture.NO_OVERLAY);
      }

      for(var5 = var1.getBlockSubmits().iterator(); var5.hasNext(); this.poseStack.popPose()) {
         SubmitNodeStorage.BlockSubmit var11 = (SubmitNodeStorage.BlockSubmit)var5.next();
         this.poseStack.pushPose();
         this.poseStack.last().set(var11.pose());
         var3.renderSingleBlock(var11.state(), this.poseStack, var2, var11.lightCoords(), var11.overlayCoords());
         if (var11.outlineColor() != 0) {
            var4.setColor(var11.outlineColor());
            var3.renderSingleBlock(var11.state(), this.poseStack, var4, var11.lightCoords(), var11.overlayCoords());
         }
      }

      var5 = var1.getBlockModelSubmits().iterator();

      while(var5.hasNext()) {
         SubmitNodeStorage.BlockModelSubmit var12 = (SubmitNodeStorage.BlockModelSubmit)var5.next();
         ModelBlockRenderer.renderModel(var12.pose(), var2.getBuffer(var12.renderType()), var12.model(), var12.r(), var12.g(), var12.b(), var12.lightCoords(), var12.overlayCoords());
         if (var12.outlineColor() != 0) {
            var4.setColor(var12.outlineColor());
            ModelBlockRenderer.renderModel(var12.pose(), var4.getBuffer(var12.renderType()), var12.model(), var12.r(), var12.g(), var12.b(), var12.lightCoords(), var12.overlayCoords());
         }
      }

   }
}
