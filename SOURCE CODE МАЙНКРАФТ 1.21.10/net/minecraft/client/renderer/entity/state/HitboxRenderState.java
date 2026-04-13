package net.minecraft.client.renderer.entity.state;

public record HitboxRenderState(double x0, double y0, double z0, double x1, double y1, double z1, float offsetX, float offsetY, float offsetZ, float red, float green, float blue) {
   public HitboxRenderState(double var1, double var3, double var5, double var7, double var9, double var11, float var13, float var14, float var15) {
      this(var1, var3, var5, var7, var9, var11, 0.0F, 0.0F, 0.0F, var13, var14, var15);
   }

   public HitboxRenderState(double param1, double param3, double param5, double param7, double param9, double param11, float param13, float param14, float param15, float param16, float param17, float param18) {
      super();
      this.x0 = var1;
      this.y0 = var3;
      this.z0 = var5;
      this.x1 = var7;
      this.y1 = var9;
      this.z1 = var11;
      this.offsetX = var13;
      this.offsetY = var14;
      this.offsetZ = var15;
      this.red = var16;
      this.green = var17;
      this.blue = var18;
   }

   public double x0() {
      return this.x0;
   }

   public double y0() {
      return this.y0;
   }

   public double z0() {
      return this.z0;
   }

   public double x1() {
      return this.x1;
   }

   public double y1() {
      return this.y1;
   }

   public double z1() {
      return this.z1;
   }

   public float offsetX() {
      return this.offsetX;
   }

   public float offsetY() {
      return this.offsetY;
   }

   public float offsetZ() {
      return this.offsetZ;
   }

   public float red() {
      return this.red;
   }

   public float green() {
      return this.green;
   }

   public float blue() {
      return this.blue;
   }
}
