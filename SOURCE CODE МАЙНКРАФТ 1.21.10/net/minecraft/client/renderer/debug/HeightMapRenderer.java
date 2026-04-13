package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3f;

public class HeightMapRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int CHUNK_DIST = 2;
   private static final float BOX_HEIGHT = 0.09375F;

   public HeightMapRenderer(Minecraft var1) {
      super();
      this.minecraft = var1;
   }

   public void render(PoseStack var1, MultiBufferSource var2, double var3, double var5, double var7, DebugValueAccess var9, Frustum var10) {
      ClientLevel var11 = this.minecraft.level;
      VertexConsumer var12 = var2.getBuffer(RenderType.debugFilledBox());
      BlockPos var13 = BlockPos.containing(var3, 0.0D, var7);

      for(int var14 = -2; var14 <= 2; ++var14) {
         for(int var15 = -2; var15 <= 2; ++var15) {
            ChunkAccess var16 = var11.getChunk(var13.offset(var14 * 16, 0, var15 * 16));
            Iterator var17 = var16.getHeightmaps().iterator();

            while(var17.hasNext()) {
               Entry var18 = (Entry)var17.next();
               Heightmap.Types var19 = (Heightmap.Types)var18.getKey();
               ChunkPos var20 = var16.getPos();
               Vector3f var21 = this.getColor(var19);

               for(int var22 = 0; var22 < 16; ++var22) {
                  for(int var23 = 0; var23 < 16; ++var23) {
                     int var24 = SectionPos.sectionToBlockCoord(var20.x, var22);
                     int var25 = SectionPos.sectionToBlockCoord(var20.z, var23);
                     float var26 = (float)((double)((float)var11.getHeight(var19, var24, var25) + (float)var19.ordinal() * 0.09375F) - var5);
                     ShapeRenderer.addChainedFilledBoxVertices(var1, var12, (double)((float)var24 + 0.25F) - var3, (double)var26, (double)((float)var25 + 0.25F) - var7, (double)((float)var24 + 0.75F) - var3, (double)(var26 + 0.09375F), (double)((float)var25 + 0.75F) - var7, var21.x(), var21.y(), var21.z(), 1.0F);
                  }
               }
            }
         }
      }

   }

   private Vector3f getColor(Heightmap.Types var1) {
      Vector3f var10000;
      switch(var1) {
      case WORLD_SURFACE_WG:
         var10000 = new Vector3f(1.0F, 1.0F, 0.0F);
         break;
      case OCEAN_FLOOR_WG:
         var10000 = new Vector3f(1.0F, 0.0F, 1.0F);
         break;
      case WORLD_SURFACE:
         var10000 = new Vector3f(0.0F, 0.7F, 0.0F);
         break;
      case OCEAN_FLOOR:
         var10000 = new Vector3f(0.0F, 0.0F, 0.5F);
         break;
      case MOTION_BLOCKING:
         var10000 = new Vector3f(0.0F, 0.3F, 0.3F);
         break;
      case MOTION_BLOCKING_NO_LEAVES:
         var10000 = new Vector3f(0.0F, 0.5F, 0.5F);
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }
}
