package net.minecraft.client.renderer.block.model;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.core.Direction;

public record SimpleUnbakedGeometry(List<BlockElement> elements) implements UnbakedGeometry {
   public SimpleUnbakedGeometry(List<BlockElement> param1) {
      super();
      this.elements = var1;
   }

   public QuadCollection bake(TextureSlots var1, ModelBaker var2, ModelState var3, ModelDebugName var4) {
      return bake(this.elements, var1, var2.sprites(), var3, var4);
   }

   public static QuadCollection bake(List<BlockElement> var0, TextureSlots var1, SpriteGetter var2, ModelState var3, ModelDebugName var4) {
      QuadCollection.Builder var5 = new QuadCollection.Builder();
      Iterator var6 = var0.iterator();

      while(var6.hasNext()) {
         BlockElement var7 = (BlockElement)var6.next();
         var7.faces().forEach((var6x, var7x) -> {
            TextureAtlasSprite var8 = var2.resolveSlot(var1, var7x.texture(), var4);
            if (var7x.cullForDirection() == null) {
               var5.addUnculledFace(bakeFace(var7, var7x, var8, var6x, var3));
            } else {
               var5.addCulledFace(Direction.rotate(var3.transformation().getMatrix(), var7x.cullForDirection()), bakeFace(var7, var7x, var8, var6x, var3));
            }

         });
      }

      return var5.build();
   }

   private static BakedQuad bakeFace(BlockElement var0, BlockElementFace var1, TextureAtlasSprite var2, Direction var3, ModelState var4) {
      return FaceBakery.bakeQuad(var0.from(), var0.to(), var1, var2, var3, var4, var0.rotation(), var0.shade(), var0.lightEmission());
   }

   public List<BlockElement> elements() {
      return this.elements;
   }
}
