package net.minecraft.client.resources.metadata.texture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.packs.metadata.MetadataSectionType;

public record TextureMetadataSection(boolean blur, boolean clamp) {
   public static final boolean DEFAULT_BLUR = false;
   public static final boolean DEFAULT_CLAMP = false;
   public static final Codec<TextureMetadataSection> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(Codec.BOOL.optionalFieldOf("blur", false).forGetter(TextureMetadataSection::blur), Codec.BOOL.optionalFieldOf("clamp", false).forGetter(TextureMetadataSection::clamp)).apply(var0, TextureMetadataSection::new);
   });
   public static final MetadataSectionType<TextureMetadataSection> TYPE;

   public TextureMetadataSection(boolean param1, boolean param2) {
      super();
      this.blur = var1;
      this.clamp = var2;
   }

   public boolean blur() {
      return this.blur;
   }

   public boolean clamp() {
      return this.clamp;
   }

   static {
      TYPE = new MetadataSectionType("texture", CODEC);
   }
}
