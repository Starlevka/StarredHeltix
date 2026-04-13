package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugValueAccess;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class ChunkCullingDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   public static final Direction[] DIRECTIONS = Direction.values();
   private final Minecraft minecraft;

   public ChunkCullingDebugRenderer(Minecraft var1) {
      super();
      this.minecraft = var1;
   }

   public void render(PoseStack var1, MultiBufferSource var2, double var3, double var5, double var7, DebugValueAccess var9, Frustum var10) {
      LevelRenderer var11 = this.minecraft.levelRenderer;
      boolean var12 = this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_PATHS);
      boolean var13 = this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_VISIBILITY);
      if (var12 || var13) {
         SectionOcclusionGraph var14 = var11.getSectionOcclusionGraph();
         ObjectListIterator var15 = var11.getVisibleSections().iterator();

         label75:
         while(true) {
            SectionRenderDispatcher.RenderSection var16;
            SectionOcclusionGraph.Node var17;
            do {
               if (!var15.hasNext()) {
                  break label75;
               }

               var16 = (SectionRenderDispatcher.RenderSection)var15.next();
               var17 = var14.getNode(var16);
            } while(var17 == null);

            BlockPos var18 = var16.getRenderOrigin();
            var1.pushPose();
            var1.translate((double)var18.getX() - var3, (double)var18.getY() - var5, (double)var18.getZ() - var7);
            Matrix4f var19 = var1.last().pose();
            VertexConsumer var20;
            int var21;
            int var23;
            int var24;
            if (var12) {
               var20 = var2.getBuffer(RenderType.lines());
               var21 = var17.step == 0 ? 0 : Mth.hsvToRgb((float)var17.step / 50.0F, 0.9F, 0.9F);
               int var22 = var21 >> 16 & 255;
               var23 = var21 >> 8 & 255;
               var24 = var21 & 255;

               for(int var25 = 0; var25 < DIRECTIONS.length; ++var25) {
                  if (var17.hasSourceDirection(var25)) {
                     Direction var26 = DIRECTIONS[var25];
                     var20.addVertex(var19, 8.0F, 8.0F, 8.0F).setColor(var22, var23, var24, 255).setNormal((float)var26.getStepX(), (float)var26.getStepY(), (float)var26.getStepZ());
                     var20.addVertex(var19, (float)(8 - 16 * var26.getStepX()), (float)(8 - 16 * var26.getStepY()), (float)(8 - 16 * var26.getStepZ())).setColor(var22, var23, var24, 255).setNormal((float)var26.getStepX(), (float)var26.getStepY(), (float)var26.getStepZ());
                  }
               }
            }

            if (var13 && var16.getSectionMesh().hasRenderableLayers()) {
               var20 = var2.getBuffer(RenderType.lines());
               var21 = 0;
               Direction[] var36 = DIRECTIONS;
               var23 = var36.length;
               var24 = 0;

               while(true) {
                  if (var24 >= var23) {
                     if (var21 > 0) {
                        VertexConsumer var37 = var2.getBuffer(RenderType.debugQuads());
                        float var38 = 0.5F;
                        float var39 = 0.2F;
                        var37.addVertex(var19, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        var37.addVertex(var19, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                     }
                     break;
                  }

                  Direction var40 = var36[var24];
                  Direction[] var41 = DIRECTIONS;
                  int var27 = var41.length;

                  for(int var28 = 0; var28 < var27; ++var28) {
                     Direction var29 = var41[var28];
                     boolean var30 = var16.getSectionMesh().facesCanSeeEachother(var40, var29);
                     if (!var30) {
                        ++var21;
                        var20.addVertex(var19, (float)(8 + 8 * var40.getStepX()), (float)(8 + 8 * var40.getStepY()), (float)(8 + 8 * var40.getStepZ())).setColor(255, 0, 0, 255).setNormal((float)var40.getStepX(), (float)var40.getStepY(), (float)var40.getStepZ());
                        var20.addVertex(var19, (float)(8 + 8 * var29.getStepX()), (float)(8 + 8 * var29.getStepY()), (float)(8 + 8 * var29.getStepZ())).setColor(255, 0, 0, 255).setNormal((float)var29.getStepX(), (float)var29.getStepY(), (float)var29.getStepZ());
                     }
                  }

                  ++var24;
               }
            }

            var1.popPose();
         }
      }

      Frustum var31 = var11.getCapturedFrustum();
      if (var31 != null) {
         var1.pushPose();
         var1.translate((float)(var31.getCamX() - var3), (float)(var31.getCamY() - var5), (float)(var31.getCamZ() - var7));
         Matrix4f var32 = var1.last().pose();
         Vector4f[] var33 = var31.getFrustumPoints();
         VertexConsumer var34 = var2.getBuffer(RenderType.debugQuads());
         this.addFrustumQuad(var34, var32, var33, 0, 1, 2, 3, 0, 1, 1);
         this.addFrustumQuad(var34, var32, var33, 4, 5, 6, 7, 1, 0, 0);
         this.addFrustumQuad(var34, var32, var33, 0, 1, 5, 4, 1, 1, 0);
         this.addFrustumQuad(var34, var32, var33, 2, 3, 7, 6, 0, 0, 1);
         this.addFrustumQuad(var34, var32, var33, 0, 4, 7, 3, 0, 1, 0);
         this.addFrustumQuad(var34, var32, var33, 1, 5, 6, 2, 1, 0, 1);
         VertexConsumer var35 = var2.getBuffer(RenderType.lines());
         this.addFrustumVertex(var35, var32, var33[0]);
         this.addFrustumVertex(var35, var32, var33[1]);
         this.addFrustumVertex(var35, var32, var33[1]);
         this.addFrustumVertex(var35, var32, var33[2]);
         this.addFrustumVertex(var35, var32, var33[2]);
         this.addFrustumVertex(var35, var32, var33[3]);
         this.addFrustumVertex(var35, var32, var33[3]);
         this.addFrustumVertex(var35, var32, var33[0]);
         this.addFrustumVertex(var35, var32, var33[4]);
         this.addFrustumVertex(var35, var32, var33[5]);
         this.addFrustumVertex(var35, var32, var33[5]);
         this.addFrustumVertex(var35, var32, var33[6]);
         this.addFrustumVertex(var35, var32, var33[6]);
         this.addFrustumVertex(var35, var32, var33[7]);
         this.addFrustumVertex(var35, var32, var33[7]);
         this.addFrustumVertex(var35, var32, var33[4]);
         this.addFrustumVertex(var35, var32, var33[0]);
         this.addFrustumVertex(var35, var32, var33[4]);
         this.addFrustumVertex(var35, var32, var33[1]);
         this.addFrustumVertex(var35, var32, var33[5]);
         this.addFrustumVertex(var35, var32, var33[2]);
         this.addFrustumVertex(var35, var32, var33[6]);
         this.addFrustumVertex(var35, var32, var33[3]);
         this.addFrustumVertex(var35, var32, var33[7]);
         var1.popPose();
      }

   }

   private void addFrustumVertex(VertexConsumer var1, Matrix4f var2, Vector4f var3) {
      var1.addVertex(var2, var3.x(), var3.y(), var3.z()).setColor(-16777216).setNormal(0.0F, 0.0F, -1.0F);
   }

   private void addFrustumQuad(VertexConsumer var1, Matrix4f var2, Vector4f[] var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10) {
      float var11 = 0.25F;
      var1.addVertex(var2, var3[var4].x(), var3[var4].y(), var3[var4].z()).setColor((float)var8, (float)var9, (float)var10, 0.25F);
      var1.addVertex(var2, var3[var5].x(), var3[var5].y(), var3[var5].z()).setColor((float)var8, (float)var9, (float)var10, 0.25F);
      var1.addVertex(var2, var3[var6].x(), var3[var6].y(), var3[var6].z()).setColor((float)var8, (float)var9, (float)var10, 0.25F);
      var1.addVertex(var2, var3[var7].x(), var3[var7].y(), var3[var7].z()).setColor((float)var8, (float)var9, (float)var10, 0.25F);
   }
}
