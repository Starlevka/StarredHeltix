package net.minecraft.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public interface ClientAsset {
   ResourceLocation id();

   public static record DownloadedTexture(ResourceLocation texturePath, String url) implements ClientAsset.Texture {
      public DownloadedTexture(ResourceLocation param1, String param2) {
         super();
         this.texturePath = var1;
         this.url = var2;
      }

      public ResourceLocation id() {
         return this.texturePath;
      }

      public ResourceLocation texturePath() {
         return this.texturePath;
      }

      public String url() {
         return this.url;
      }
   }

   public static record ResourceTexture(ResourceLocation id, ResourceLocation texturePath) implements ClientAsset.Texture {
      public static final Codec<ClientAsset.ResourceTexture> CODEC;
      public static final MapCodec<ClientAsset.ResourceTexture> DEFAULT_FIELD_CODEC;
      public static final StreamCodec<ByteBuf, ClientAsset.ResourceTexture> STREAM_CODEC;

      public ResourceTexture(ResourceLocation var1) {
         this(var1, var1.withPath((var0) -> {
            return "textures/" + var0 + ".png";
         }));
      }

      public ResourceTexture(ResourceLocation param1, ResourceLocation param2) {
         super();
         this.id = var1;
         this.texturePath = var2;
      }

      public ResourceLocation id() {
         return this.id;
      }

      public ResourceLocation texturePath() {
         return this.texturePath;
      }

      static {
         CODEC = ResourceLocation.CODEC.xmap(ClientAsset.ResourceTexture::new, ClientAsset.ResourceTexture::id);
         DEFAULT_FIELD_CODEC = CODEC.fieldOf("asset_id");
         STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(ClientAsset.ResourceTexture::new, ClientAsset.ResourceTexture::id);
      }
   }

   public interface Texture extends ClientAsset {
      ResourceLocation texturePath();
   }
}
