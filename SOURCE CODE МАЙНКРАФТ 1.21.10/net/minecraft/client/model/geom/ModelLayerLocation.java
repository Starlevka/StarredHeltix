package net.minecraft.client.model.geom;

import net.minecraft.resources.ResourceLocation;

public record ModelLayerLocation(ResourceLocation model, String layer) {
   public ModelLayerLocation(ResourceLocation param1, String param2) {
      super();
      this.model = var1;
      this.layer = var2;
   }

   public String toString() {
      String var10000 = String.valueOf(this.model);
      return var10000 + "#" + this.layer;
   }

   public ResourceLocation model() {
      return this.model;
   }

   public String layer() {
      return this.layer;
   }
}
