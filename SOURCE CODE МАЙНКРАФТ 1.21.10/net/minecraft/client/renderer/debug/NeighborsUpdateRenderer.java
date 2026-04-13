package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;

public class NeighborsUpdateRenderer implements DebugRenderer.SimpleDebugRenderer {
   public NeighborsUpdateRenderer() {
      super();
   }

   public void render(PoseStack var1, MultiBufferSource var2, double var3, double var5, double var7, DebugValueAccess var9, Frustum var10) {
      int var11 = DebugSubscriptions.NEIGHBOR_UPDATES.expireAfterTicks();
      double var12 = 1.0D / (double)(var11 * 2);
      HashMap var14 = new HashMap();
      var9.forEachEvent(DebugSubscriptions.NEIGHBOR_UPDATES, (var1x, var2x, var3x) -> {
         long var4 = (long)(var3x - var2x);
         NeighborsUpdateRenderer.LastUpdate var6 = (NeighborsUpdateRenderer.LastUpdate)var14.getOrDefault(var1x, NeighborsUpdateRenderer.LastUpdate.NONE);
         var14.put(var1x, var6.tryCount((int)var4));
      });
      VertexConsumer var15 = var2.getBuffer(RenderType.lines());
      Iterator var16 = var14.entrySet().iterator();

      Entry var17;
      BlockPos var18;
      NeighborsUpdateRenderer.LastUpdate var19;
      while(var16.hasNext()) {
         var17 = (Entry)var16.next();
         var18 = (BlockPos)var17.getKey();
         var19 = (NeighborsUpdateRenderer.LastUpdate)var17.getValue();
         AABB var20 = (new AABB(BlockPos.ZERO)).inflate(0.002D).deflate(var12 * (double)var19.age).move((double)var18.getX(), (double)var18.getY(), (double)var18.getZ()).move(-var3, -var5, -var7);
         ShapeRenderer.renderLineBox(var1.last(), var15, var20.minX, var20.minY, var20.minZ, var20.maxX, var20.maxY, var20.maxZ, 1.0F, 1.0F, 1.0F, 1.0F);
      }

      var16 = var14.entrySet().iterator();

      while(var16.hasNext()) {
         var17 = (Entry)var16.next();
         var18 = (BlockPos)var17.getKey();
         var19 = (NeighborsUpdateRenderer.LastUpdate)var17.getValue();
         DebugRenderer.renderFloatingText(var1, var2, String.valueOf(var19.count), var18.getX(), var18.getY(), var18.getZ(), -1);
      }

   }

   private static record LastUpdate(int count, int age) {
      final int count;
      final int age;
      static final NeighborsUpdateRenderer.LastUpdate NONE = new NeighborsUpdateRenderer.LastUpdate(0, 2147483647);

      private LastUpdate(int param1, int param2) {
         super();
         this.count = var1;
         this.age = var2;
      }

      public NeighborsUpdateRenderer.LastUpdate tryCount(int var1) {
         if (var1 == this.age) {
            return new NeighborsUpdateRenderer.LastUpdate(this.count + 1, var1);
         } else {
            return var1 < this.age ? new NeighborsUpdateRenderer.LastUpdate(1, var1) : this;
         }
      }

      public int count() {
         return this.count;
      }

      public int age() {
         return this.age;
      }
   }
}
