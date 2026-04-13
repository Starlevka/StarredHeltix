package net.minecraft.client.resources.model;

import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.resources.ResourceLocation;

public interface UnbakedModel {
   String PARTICLE_TEXTURE_REFERENCE = "particle";

   @Nullable
   default Boolean ambientOcclusion() {
      return null;
   }

   @Nullable
   default UnbakedModel.GuiLight guiLight() {
      return null;
   }

   @Nullable
   default ItemTransforms transforms() {
      return null;
   }

   default TextureSlots.Data textureSlots() {
      return TextureSlots.Data.EMPTY;
   }

   @Nullable
   default UnbakedGeometry geometry() {
      return null;
   }

   @Nullable
   default ResourceLocation parent() {
      return null;
   }

   public static enum GuiLight {
      FRONT("front"),
      SIDE("side");

      private final String name;

      private GuiLight(final String param3) {
         this.name = var3;
      }

      public static UnbakedModel.GuiLight getByName(String var0) {
         UnbakedModel.GuiLight[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            UnbakedModel.GuiLight var4 = var1[var3];
            if (var4.name.equals(var0)) {
               return var4;
            }
         }

         throw new IllegalArgumentException("Invalid gui light: " + var0);
      }

      public boolean lightLikeBlock() {
         return this == SIDE;
      }

      // $FF: synthetic method
      private static UnbakedModel.GuiLight[] $values() {
         return new UnbakedModel.GuiLight[]{FRONT, SIDE};
      }
   }
}
