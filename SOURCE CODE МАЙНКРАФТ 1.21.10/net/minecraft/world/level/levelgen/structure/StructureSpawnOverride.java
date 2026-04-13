package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.biome.MobSpawnSettings;

public record StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType boundingBox, WeightedList<MobSpawnSettings.SpawnerData> spawns) {
   public static final Codec<StructureSpawnOverride> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(StructureSpawnOverride.BoundingBoxType.CODEC.fieldOf("bounding_box").forGetter(StructureSpawnOverride::boundingBox), WeightedList.codec(MobSpawnSettings.SpawnerData.CODEC).fieldOf("spawns").forGetter(StructureSpawnOverride::spawns)).apply(var0, StructureSpawnOverride::new);
   });

   public StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType param1, WeightedList<MobSpawnSettings.SpawnerData> param2) {
      super();
      this.boundingBox = var1;
      this.spawns = var2;
   }

   public StructureSpawnOverride.BoundingBoxType boundingBox() {
      return this.boundingBox;
   }

   public WeightedList<MobSpawnSettings.SpawnerData> spawns() {
      return this.spawns;
   }

   public static enum BoundingBoxType implements StringRepresentable {
      PIECE("piece"),
      STRUCTURE("full");

      public static final Codec<StructureSpawnOverride.BoundingBoxType> CODEC = StringRepresentable.fromEnum(StructureSpawnOverride.BoundingBoxType::values);
      private final String id;

      private BoundingBoxType(final String param3) {
         this.id = var3;
      }

      public String getSerializedName() {
         return this.id;
      }

      // $FF: synthetic method
      private static StructureSpawnOverride.BoundingBoxType[] $values() {
         return new StructureSpawnOverride.BoundingBoxType[]{PIECE, STRUCTURE};
      }
   }
}
