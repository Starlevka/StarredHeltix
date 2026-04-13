package net.minecraft.client.renderer.entity.state;

import javax.annotation.Nullable;

public record ServerHitboxesRenderState(boolean missing, double serverEntityX, double serverEntityY, double serverEntityZ, double deltaMovementX, double deltaMovementY, double deltaMovementZ, float eyeHeight, @Nullable HitboxesRenderState hitboxes) {
   public ServerHitboxesRenderState(boolean var1) {
      this(var1, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0F, (HitboxesRenderState)null);
   }

   public ServerHitboxesRenderState(boolean param1, double param2, double param4, double param6, double param8, double param10, double param12, float param14, @Nullable HitboxesRenderState param15) {
      super();
      this.missing = var1;
      this.serverEntityX = var2;
      this.serverEntityY = var4;
      this.serverEntityZ = var6;
      this.deltaMovementX = var8;
      this.deltaMovementY = var10;
      this.deltaMovementZ = var12;
      this.eyeHeight = var14;
      this.hitboxes = var15;
   }

   public boolean missing() {
      return this.missing;
   }

   public double serverEntityX() {
      return this.serverEntityX;
   }

   public double serverEntityY() {
      return this.serverEntityY;
   }

   public double serverEntityZ() {
      return this.serverEntityZ;
   }

   public double deltaMovementX() {
      return this.deltaMovementX;
   }

   public double deltaMovementY() {
      return this.deltaMovementY;
   }

   public double deltaMovementZ() {
      return this.deltaMovementZ;
   }

   public float eyeHeight() {
      return this.eyeHeight;
   }

   @Nullable
   public HitboxesRenderState hitboxes() {
      return this.hitboxes;
   }
}
