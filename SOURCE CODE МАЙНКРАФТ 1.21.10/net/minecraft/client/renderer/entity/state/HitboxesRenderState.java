package net.minecraft.client.renderer.entity.state;

import com.google.common.collect.ImmutableList;

public record HitboxesRenderState(double viewX, double viewY, double viewZ, ImmutableList<HitboxRenderState> hitboxes) {
   public HitboxesRenderState(double param1, double param3, double param5, ImmutableList<HitboxRenderState> param7) {
      super();
      this.viewX = var1;
      this.viewY = var3;
      this.viewZ = var5;
      this.hitboxes = var7;
   }

   public double viewX() {
      return this.viewX;
   }

   public double viewY() {
      return this.viewY;
   }

   public double viewZ() {
      return this.viewZ;
   }

   public ImmutableList<HitboxRenderState> hitboxes() {
      return this.hitboxes;
   }
}
