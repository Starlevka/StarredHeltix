package net.minecraft.core.particles;

public record ParticleLimit(int limit) {
   public static final ParticleLimit SPORE_BLOSSOM = new ParticleLimit(1000);

   public ParticleLimit(int param1) {
      super();
      this.limit = var1;
   }

   public int limit() {
      return this.limit;
   }
}
