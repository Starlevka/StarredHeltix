package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ItemPickupParticleGroup extends ParticleGroup<ItemPickupParticle> {
   public ItemPickupParticleGroup(ParticleEngine var1) {
      super(var1);
   }

   public ParticleGroupRenderState extractRenderState(Frustum var1, Camera var2, float var3) {
      return new ItemPickupParticleGroup.State(this.particles.stream().map((var2x) -> {
         return ItemPickupParticleGroup.ParticleInstance.fromParticle(var2x, var2, var3);
      }).toList());
   }

   static record State(List<ItemPickupParticleGroup.ParticleInstance> instances) implements ParticleGroupRenderState {
      State(List<ItemPickupParticleGroup.ParticleInstance> param1) {
         super();
         this.instances = var1;
      }

      public void submit(SubmitNodeCollector var1, CameraRenderState var2) {
         PoseStack var3 = new PoseStack();
         EntityRenderDispatcher var4 = Minecraft.getInstance().getEntityRenderDispatcher();
         Iterator var5 = this.instances.iterator();

         while(var5.hasNext()) {
            ItemPickupParticleGroup.ParticleInstance var6 = (ItemPickupParticleGroup.ParticleInstance)var5.next();
            var4.submit(var6.itemRenderState, var2, var6.xOffset, var6.yOffset, var6.zOffset, var3, var1);
         }

      }

      public List<ItemPickupParticleGroup.ParticleInstance> instances() {
         return this.instances;
      }
   }

   private static record ParticleInstance(EntityRenderState itemRenderState, double xOffset, double yOffset, double zOffset) {
      final EntityRenderState itemRenderState;
      final double xOffset;
      final double yOffset;
      final double zOffset;

      private ParticleInstance(EntityRenderState param1, double param2, double param4, double param6) {
         super();
         this.itemRenderState = var1;
         this.xOffset = var2;
         this.yOffset = var4;
         this.zOffset = var6;
      }

      public static ItemPickupParticleGroup.ParticleInstance fromParticle(ItemPickupParticle var0, Camera var1, float var2) {
         float var3 = ((float)var0.life + var2) / 3.0F;
         var3 *= var3;
         double var4 = Mth.lerp((double)var2, var0.targetXOld, var0.targetX);
         double var6 = Mth.lerp((double)var2, var0.targetYOld, var0.targetY);
         double var8 = Mth.lerp((double)var2, var0.targetZOld, var0.targetZ);
         double var10 = Mth.lerp((double)var3, var0.itemRenderState.x, var4);
         double var12 = Mth.lerp((double)var3, var0.itemRenderState.y, var6);
         double var14 = Mth.lerp((double)var3, var0.itemRenderState.z, var8);
         Vec3 var16 = var1.getPosition();
         return new ItemPickupParticleGroup.ParticleInstance(var0.itemRenderState, var10 - var16.x(), var12 - var16.y(), var14 - var16.z());
      }

      public EntityRenderState itemRenderState() {
         return this.itemRenderState;
      }

      public double xOffset() {
         return this.xOffset;
      }

      public double yOffset() {
         return this.yOffset;
      }

      public double zOffset() {
         return this.zOffset;
      }
   }
}
