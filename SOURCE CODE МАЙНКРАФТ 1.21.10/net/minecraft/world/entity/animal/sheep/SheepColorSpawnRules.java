package net.minecraft.world.entity.animal.sheep;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.biome.Biome;

public class SheepColorSpawnRules {
   private static final SheepColorSpawnRules.SheepColorSpawnConfiguration TEMPERATE_SPAWN_CONFIGURATION;
   private static final SheepColorSpawnRules.SheepColorSpawnConfiguration WARM_SPAWN_CONFIGURATION;
   private static final SheepColorSpawnRules.SheepColorSpawnConfiguration COLD_SPAWN_CONFIGURATION;

   public SheepColorSpawnRules() {
      super();
   }

   private static SheepColorSpawnRules.SheepColorProvider commonColors(DyeColor var0) {
      return weighted(builder().add(single(var0), 499).add(single(DyeColor.PINK), 1).build());
   }

   public static DyeColor getSheepColor(Holder<Biome> var0, RandomSource var1) {
      SheepColorSpawnRules.SheepColorSpawnConfiguration var2 = getSheepColorConfiguration(var0);
      return var2.colors().get(var1);
   }

   private static SheepColorSpawnRules.SheepColorSpawnConfiguration getSheepColorConfiguration(Holder<Biome> var0) {
      if (var0.is(BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS)) {
         return WARM_SPAWN_CONFIGURATION;
      } else {
         return var0.is(BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS) ? COLD_SPAWN_CONFIGURATION : TEMPERATE_SPAWN_CONFIGURATION;
      }
   }

   private static SheepColorSpawnRules.SheepColorProvider weighted(WeightedList<SheepColorSpawnRules.SheepColorProvider> var0) {
      if (var0.isEmpty()) {
         throw new IllegalArgumentException("List must be non-empty");
      } else {
         return (var1) -> {
            return ((SheepColorSpawnRules.SheepColorProvider)var0.getRandomOrThrow(var1)).get(var1);
         };
      }
   }

   private static SheepColorSpawnRules.SheepColorProvider single(DyeColor var0) {
      return (var1) -> {
         return var0;
      };
   }

   private static WeightedList.Builder<SheepColorSpawnRules.SheepColorProvider> builder() {
      return WeightedList.builder();
   }

   static {
      TEMPERATE_SPAWN_CONFIGURATION = new SheepColorSpawnRules.SheepColorSpawnConfiguration(weighted(builder().add(single(DyeColor.BLACK), 5).add(single(DyeColor.GRAY), 5).add(single(DyeColor.LIGHT_GRAY), 5).add(single(DyeColor.BROWN), 3).add(commonColors(DyeColor.WHITE), 82).build()));
      WARM_SPAWN_CONFIGURATION = new SheepColorSpawnRules.SheepColorSpawnConfiguration(weighted(builder().add(single(DyeColor.GRAY), 5).add(single(DyeColor.LIGHT_GRAY), 5).add(single(DyeColor.WHITE), 5).add(single(DyeColor.BLACK), 3).add(commonColors(DyeColor.BROWN), 82).build()));
      COLD_SPAWN_CONFIGURATION = new SheepColorSpawnRules.SheepColorSpawnConfiguration(weighted(builder().add(single(DyeColor.LIGHT_GRAY), 5).add(single(DyeColor.GRAY), 5).add(single(DyeColor.WHITE), 5).add(single(DyeColor.BROWN), 3).add(commonColors(DyeColor.BLACK), 82).build()));
   }

   @FunctionalInterface
   private interface SheepColorProvider {
      DyeColor get(RandomSource var1);
   }

   static record SheepColorSpawnConfiguration(SheepColorSpawnRules.SheepColorProvider colors) {
      SheepColorSpawnConfiguration(SheepColorSpawnRules.SheepColorProvider param1) {
         super();
         this.colors = var1;
      }

      public SheepColorSpawnRules.SheepColorProvider colors() {
         return this.colors;
      }
   }
}
