package net.minecraft.client.resources.metadata.animation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.StringRepresentable;

public record VillagerMetadataSection(VillagerMetadataSection.Hat hat) {
   public static final Codec<VillagerMetadataSection> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(VillagerMetadataSection.Hat.CODEC.optionalFieldOf("hat", VillagerMetadataSection.Hat.NONE).forGetter(VillagerMetadataSection::hat)).apply(var0, VillagerMetadataSection::new);
   });
   public static final MetadataSectionType<VillagerMetadataSection> TYPE;

   public VillagerMetadataSection(VillagerMetadataSection.Hat param1) {
      super();
      this.hat = var1;
   }

   public VillagerMetadataSection.Hat hat() {
      return this.hat;
   }

   static {
      TYPE = new MetadataSectionType("villager", CODEC);
   }

   public static enum Hat implements StringRepresentable {
      NONE("none"),
      PARTIAL("partial"),
      FULL("full");

      public static final Codec<VillagerMetadataSection.Hat> CODEC = StringRepresentable.fromEnum(VillagerMetadataSection.Hat::values);
      private final String name;

      private Hat(final String param3) {
         this.name = var3;
      }

      public String getSerializedName() {
         return this.name;
      }

      // $FF: synthetic method
      private static VillagerMetadataSection.Hat[] $values() {
         return new VillagerMetadataSection.Hat[]{NONE, PARTIAL, FULL};
      }
   }
}
