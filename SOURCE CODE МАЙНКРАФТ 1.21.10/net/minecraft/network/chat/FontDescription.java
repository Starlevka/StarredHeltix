package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ResolvableProfile;

public interface FontDescription {
   Codec<FontDescription> CODEC = ResourceLocation.CODEC.flatComapMap(FontDescription.Resource::new, (var0) -> {
      if (var0 instanceof FontDescription.Resource) {
         FontDescription.Resource var1 = (FontDescription.Resource)var0;
         return DataResult.success(var1.id());
      } else {
         return DataResult.error(() -> {
            return "Unsupported font description type: " + String.valueOf(var0);
         });
      }
   });
   FontDescription.Resource DEFAULT = new FontDescription.Resource(ResourceLocation.withDefaultNamespace("default"));

   public static record Resource(ResourceLocation id) implements FontDescription {
      public Resource(ResourceLocation param1) {
         super();
         this.id = var1;
      }

      public ResourceLocation id() {
         return this.id;
      }
   }

   public static record PlayerSprite(ResolvableProfile profile, boolean hat) implements FontDescription {
      public PlayerSprite(ResolvableProfile param1, boolean param2) {
         super();
         this.profile = var1;
         this.hat = var2;
      }

      public ResolvableProfile profile() {
         return this.profile;
      }

      public boolean hat() {
         return this.hat;
      }
   }

   public static record AtlasSprite(ResourceLocation atlasId, ResourceLocation spriteId) implements FontDescription {
      public AtlasSprite(ResourceLocation param1, ResourceLocation param2) {
         super();
         this.atlasId = var1;
         this.spriteId = var2;
      }

      public ResourceLocation atlasId() {
         return this.atlasId;
      }

      public ResourceLocation spriteId() {
         return this.spriteId;
      }
   }
}
