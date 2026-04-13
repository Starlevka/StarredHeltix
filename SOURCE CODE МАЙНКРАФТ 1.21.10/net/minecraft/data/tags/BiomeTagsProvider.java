package net.minecraft.data.tags;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

public class BiomeTagsProvider extends KeyTagProvider<Biome> {
   public BiomeTagsProvider(PackOutput var1, CompletableFuture<HolderLookup.Provider> var2) {
      super(var1, Registries.BIOME, var2);
   }

   protected void addTags(HolderLookup.Provider var1) {
      this.tag(BiomeTags.IS_DEEP_OCEAN).add((Object)Biomes.DEEP_FROZEN_OCEAN).add((Object)Biomes.DEEP_COLD_OCEAN).add((Object)Biomes.DEEP_OCEAN).add((Object)Biomes.DEEP_LUKEWARM_OCEAN);
      this.tag(BiomeTags.IS_OCEAN).addTag(BiomeTags.IS_DEEP_OCEAN).add((Object)Biomes.FROZEN_OCEAN).add((Object)Biomes.OCEAN).add((Object)Biomes.COLD_OCEAN).add((Object)Biomes.LUKEWARM_OCEAN).add((Object)Biomes.WARM_OCEAN);
      this.tag(BiomeTags.IS_BEACH).add((Object)Biomes.BEACH).add((Object)Biomes.SNOWY_BEACH);
      this.tag(BiomeTags.IS_RIVER).add((Object)Biomes.RIVER).add((Object)Biomes.FROZEN_RIVER);
      this.tag(BiomeTags.IS_MOUNTAIN).add((Object)Biomes.MEADOW).add((Object)Biomes.FROZEN_PEAKS).add((Object)Biomes.JAGGED_PEAKS).add((Object)Biomes.STONY_PEAKS).add((Object)Biomes.SNOWY_SLOPES).add((Object)Biomes.CHERRY_GROVE);
      this.tag(BiomeTags.IS_BADLANDS).add((Object)Biomes.BADLANDS).add((Object)Biomes.ERODED_BADLANDS).add((Object)Biomes.WOODED_BADLANDS);
      this.tag(BiomeTags.IS_HILL).add((Object)Biomes.WINDSWEPT_HILLS).add((Object)Biomes.WINDSWEPT_FOREST).add((Object)Biomes.WINDSWEPT_GRAVELLY_HILLS);
      this.tag(BiomeTags.IS_TAIGA).add((Object)Biomes.TAIGA).add((Object)Biomes.SNOWY_TAIGA).add((Object)Biomes.OLD_GROWTH_PINE_TAIGA).add((Object)Biomes.OLD_GROWTH_SPRUCE_TAIGA);
      this.tag(BiomeTags.IS_JUNGLE).add((Object)Biomes.BAMBOO_JUNGLE).add((Object)Biomes.JUNGLE).add((Object)Biomes.SPARSE_JUNGLE);
      this.tag(BiomeTags.IS_FOREST).add((Object)Biomes.FOREST).add((Object)Biomes.FLOWER_FOREST).add((Object)Biomes.BIRCH_FOREST).add((Object)Biomes.OLD_GROWTH_BIRCH_FOREST).add((Object)Biomes.DARK_FOREST).add((Object)Biomes.PALE_GARDEN).add((Object)Biomes.GROVE);
      this.tag(BiomeTags.IS_SAVANNA).add((Object)Biomes.SAVANNA).add((Object)Biomes.SAVANNA_PLATEAU).add((Object)Biomes.WINDSWEPT_SAVANNA);
      this.tag(BiomeTags.IS_NETHER).addAll(MultiNoiseBiomeSourceParameterList.Preset.NETHER.usedBiomes());
      List var2 = MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD.usedBiomes().toList();
      this.tag(BiomeTags.IS_OVERWORLD).addAll((Collection)var2);
      this.tag(BiomeTags.IS_END).add((Object)Biomes.THE_END).add((Object)Biomes.END_HIGHLANDS).add((Object)Biomes.END_MIDLANDS).add((Object)Biomes.SMALL_END_ISLANDS).add((Object)Biomes.END_BARRENS);
      this.tag(BiomeTags.HAS_BURIED_TREASURE).addTag(BiomeTags.IS_BEACH);
      this.tag(BiomeTags.HAS_DESERT_PYRAMID).add((Object)Biomes.DESERT);
      this.tag(BiomeTags.HAS_IGLOO).add((Object)Biomes.SNOWY_TAIGA).add((Object)Biomes.SNOWY_PLAINS).add((Object)Biomes.SNOWY_SLOPES);
      this.tag(BiomeTags.HAS_JUNGLE_TEMPLE).add((Object)Biomes.BAMBOO_JUNGLE).add((Object)Biomes.JUNGLE);
      this.tag(BiomeTags.HAS_MINESHAFT).addTag(BiomeTags.IS_OCEAN).addTag(BiomeTags.IS_RIVER).addTag(BiomeTags.IS_BEACH).addTag(BiomeTags.IS_MOUNTAIN).addTag(BiomeTags.IS_HILL).addTag(BiomeTags.IS_TAIGA).addTag(BiomeTags.IS_JUNGLE).addTag(BiomeTags.IS_FOREST).add((Object)Biomes.STONY_SHORE).add((Object)Biomes.MUSHROOM_FIELDS).add((Object)Biomes.ICE_SPIKES).add((Object)Biomes.WINDSWEPT_SAVANNA).add((Object)Biomes.DESERT).add((Object)Biomes.SAVANNA).add((Object)Biomes.SNOWY_PLAINS).add((Object)Biomes.PLAINS).add((Object)Biomes.SUNFLOWER_PLAINS).add((Object)Biomes.SWAMP).add((Object)Biomes.MANGROVE_SWAMP).add((Object)Biomes.SAVANNA_PLATEAU).add((Object)Biomes.DRIPSTONE_CAVES).add((Object)Biomes.LUSH_CAVES);
      this.tag(BiomeTags.HAS_MINESHAFT_MESA).addTag(BiomeTags.IS_BADLANDS);
      this.tag(BiomeTags.MINESHAFT_BLOCKING).add((Object)Biomes.DEEP_DARK);
      this.tag(BiomeTags.HAS_OCEAN_MONUMENT).addTag(BiomeTags.IS_DEEP_OCEAN);
      this.tag(BiomeTags.REQUIRED_OCEAN_MONUMENT_SURROUNDING).addTag(BiomeTags.IS_OCEAN).addTag(BiomeTags.IS_RIVER);
      this.tag(BiomeTags.HAS_OCEAN_RUIN_COLD).add((Object)Biomes.FROZEN_OCEAN).add((Object)Biomes.COLD_OCEAN).add((Object)Biomes.OCEAN).add((Object)Biomes.DEEP_FROZEN_OCEAN).add((Object)Biomes.DEEP_COLD_OCEAN).add((Object)Biomes.DEEP_OCEAN);
      this.tag(BiomeTags.HAS_OCEAN_RUIN_WARM).add((Object)Biomes.LUKEWARM_OCEAN).add((Object)Biomes.WARM_OCEAN).add((Object)Biomes.DEEP_LUKEWARM_OCEAN);
      this.tag(BiomeTags.HAS_PILLAGER_OUTPOST).add((Object)Biomes.DESERT).add((Object)Biomes.PLAINS).add((Object)Biomes.SAVANNA).add((Object)Biomes.SNOWY_PLAINS).add((Object)Biomes.TAIGA).addTag(BiomeTags.IS_MOUNTAIN).add((Object)Biomes.GROVE);
      this.tag(BiomeTags.HAS_RUINED_PORTAL_DESERT).add((Object)Biomes.DESERT);
      this.tag(BiomeTags.HAS_RUINED_PORTAL_JUNGLE).addTag(BiomeTags.IS_JUNGLE);
      this.tag(BiomeTags.HAS_RUINED_PORTAL_OCEAN).addTag(BiomeTags.IS_OCEAN);
      this.tag(BiomeTags.HAS_RUINED_PORTAL_SWAMP).add((Object)Biomes.SWAMP).add((Object)Biomes.MANGROVE_SWAMP);
      this.tag(BiomeTags.HAS_RUINED_PORTAL_MOUNTAIN).addTag(BiomeTags.IS_BADLANDS).addTag(BiomeTags.IS_HILL).add((Object)Biomes.SAVANNA_PLATEAU).add((Object)Biomes.WINDSWEPT_SAVANNA).add((Object)Biomes.STONY_SHORE).addTag(BiomeTags.IS_MOUNTAIN);
      this.tag(BiomeTags.HAS_RUINED_PORTAL_STANDARD).addTag(BiomeTags.IS_BEACH).addTag(BiomeTags.IS_RIVER).addTag(BiomeTags.IS_TAIGA).addTag(BiomeTags.IS_FOREST).add((Object)Biomes.MUSHROOM_FIELDS).add((Object)Biomes.ICE_SPIKES).add((Object)Biomes.DRIPSTONE_CAVES).add((Object)Biomes.LUSH_CAVES).add((Object)Biomes.SAVANNA).add((Object)Biomes.SNOWY_PLAINS).add((Object)Biomes.PLAINS).add((Object)Biomes.SUNFLOWER_PLAINS);
      this.tag(BiomeTags.HAS_SHIPWRECK_BEACHED).addTag(BiomeTags.IS_BEACH);
      this.tag(BiomeTags.HAS_SHIPWRECK).addTag(BiomeTags.IS_OCEAN);
      this.tag(BiomeTags.HAS_SWAMP_HUT).add((Object)Biomes.SWAMP);
      this.tag(BiomeTags.HAS_VILLAGE_DESERT).add((Object)Biomes.DESERT);
      this.tag(BiomeTags.HAS_VILLAGE_PLAINS).add((Object)Biomes.PLAINS).add((Object)Biomes.MEADOW);
      this.tag(BiomeTags.HAS_VILLAGE_SAVANNA).add((Object)Biomes.SAVANNA);
      this.tag(BiomeTags.HAS_VILLAGE_SNOWY).add((Object)Biomes.SNOWY_PLAINS);
      this.tag(BiomeTags.HAS_VILLAGE_TAIGA).add((Object)Biomes.TAIGA);
      this.tag(BiomeTags.HAS_TRAIL_RUINS).add((Object)Biomes.TAIGA).add((Object)Biomes.SNOWY_TAIGA).add((Object)Biomes.OLD_GROWTH_PINE_TAIGA).add((Object)Biomes.OLD_GROWTH_SPRUCE_TAIGA).add((Object)Biomes.OLD_GROWTH_BIRCH_FOREST).add((Object)Biomes.JUNGLE);
      this.tag(BiomeTags.HAS_WOODLAND_MANSION).add((Object)Biomes.DARK_FOREST).add((Object)Biomes.PALE_GARDEN);
      this.tag(BiomeTags.STRONGHOLD_BIASED_TO).add((Object)Biomes.PLAINS).add((Object)Biomes.SUNFLOWER_PLAINS).add((Object)Biomes.SNOWY_PLAINS).add((Object)Biomes.ICE_SPIKES).add((Object)Biomes.DESERT).add((Object)Biomes.FOREST).add((Object)Biomes.FLOWER_FOREST).add((Object)Biomes.BIRCH_FOREST).add((Object)Biomes.DARK_FOREST).add((Object)Biomes.PALE_GARDEN).add((Object)Biomes.OLD_GROWTH_BIRCH_FOREST).add((Object)Biomes.OLD_GROWTH_PINE_TAIGA).add((Object)Biomes.OLD_GROWTH_SPRUCE_TAIGA).add((Object)Biomes.TAIGA).add((Object)Biomes.SNOWY_TAIGA).add((Object)Biomes.SAVANNA).add((Object)Biomes.SAVANNA_PLATEAU).add((Object)Biomes.WINDSWEPT_HILLS).add((Object)Biomes.WINDSWEPT_GRAVELLY_HILLS).add((Object)Biomes.WINDSWEPT_FOREST).add((Object)Biomes.WINDSWEPT_SAVANNA).add((Object)Biomes.JUNGLE).add((Object)Biomes.SPARSE_JUNGLE).add((Object)Biomes.BAMBOO_JUNGLE).add((Object)Biomes.BADLANDS).add((Object)Biomes.ERODED_BADLANDS).add((Object)Biomes.WOODED_BADLANDS).add((Object)Biomes.MEADOW).add((Object)Biomes.CHERRY_GROVE).add((Object)Biomes.GROVE).add((Object)Biomes.SNOWY_SLOPES).add((Object)Biomes.FROZEN_PEAKS).add((Object)Biomes.JAGGED_PEAKS).add((Object)Biomes.STONY_PEAKS).add((Object)Biomes.MUSHROOM_FIELDS).add((Object)Biomes.DRIPSTONE_CAVES).add((Object)Biomes.LUSH_CAVES);
      this.tag(BiomeTags.HAS_STRONGHOLD).addTag(BiomeTags.IS_OVERWORLD);
      this.tag(BiomeTags.HAS_TRIAL_CHAMBERS).addAll(var2.stream().filter((var0) -> {
         return var0 != Biomes.DEEP_DARK;
      }));
      this.tag(BiomeTags.HAS_NETHER_FORTRESS).addTag(BiomeTags.IS_NETHER);
      this.tag(BiomeTags.HAS_NETHER_FOSSIL).add((Object)Biomes.SOUL_SAND_VALLEY);
      this.tag(BiomeTags.HAS_BASTION_REMNANT).add((Object)Biomes.CRIMSON_FOREST).add((Object)Biomes.NETHER_WASTES).add((Object)Biomes.SOUL_SAND_VALLEY).add((Object)Biomes.WARPED_FOREST);
      this.tag(BiomeTags.HAS_ANCIENT_CITY).add((Object)Biomes.DEEP_DARK);
      this.tag(BiomeTags.HAS_RUINED_PORTAL_NETHER).addTag(BiomeTags.IS_NETHER);
      this.tag(BiomeTags.HAS_END_CITY).add((Object)Biomes.END_HIGHLANDS).add((Object)Biomes.END_MIDLANDS);
      this.tag(BiomeTags.PRODUCES_CORALS_FROM_BONEMEAL).add((Object)Biomes.WARM_OCEAN);
      this.tag(BiomeTags.PLAYS_UNDERWATER_MUSIC).addTag(BiomeTags.IS_OCEAN).addTag(BiomeTags.IS_RIVER);
      this.tag(BiomeTags.HAS_CLOSER_WATER_FOG).add((Object)Biomes.SWAMP).add((Object)Biomes.MANGROVE_SWAMP);
      this.tag(BiomeTags.WATER_ON_MAP_OUTLINES).addTag(BiomeTags.IS_OCEAN).addTag(BiomeTags.IS_RIVER).add((Object)Biomes.SWAMP).add((Object)Biomes.MANGROVE_SWAMP);
      this.tag(BiomeTags.WITHOUT_ZOMBIE_SIEGES).add((Object)Biomes.MUSHROOM_FIELDS);
      this.tag(BiomeTags.WITHOUT_PATROL_SPAWNS).add((Object)Biomes.MUSHROOM_FIELDS);
      this.tag(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS).add((Object)Biomes.THE_VOID);
      this.tag(BiomeTags.SPAWNS_COLD_VARIANT_FROGS).add((Object)Biomes.SNOWY_PLAINS).add((Object)Biomes.ICE_SPIKES).add((Object)Biomes.FROZEN_PEAKS).add((Object)Biomes.JAGGED_PEAKS).add((Object)Biomes.SNOWY_SLOPES).add((Object)Biomes.FROZEN_OCEAN).add((Object)Biomes.DEEP_FROZEN_OCEAN).add((Object)Biomes.GROVE).add((Object)Biomes.DEEP_DARK).add((Object)Biomes.FROZEN_RIVER).add((Object)Biomes.SNOWY_TAIGA).add((Object)Biomes.SNOWY_BEACH).addTag(BiomeTags.IS_END);
      this.tag(BiomeTags.SPAWNS_WARM_VARIANT_FROGS).add((Object)Biomes.DESERT).add((Object)Biomes.WARM_OCEAN).addTag(BiomeTags.IS_JUNGLE).addTag(BiomeTags.IS_SAVANNA).addTag(BiomeTags.IS_NETHER).addTag(BiomeTags.IS_BADLANDS).add((Object)Biomes.MANGROVE_SWAMP);
      this.tag(BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS).add((Object)Biomes.SNOWY_PLAINS).add((Object)Biomes.ICE_SPIKES).add((Object)Biomes.FROZEN_PEAKS).add((Object)Biomes.JAGGED_PEAKS).add((Object)Biomes.SNOWY_SLOPES).add((Object)Biomes.FROZEN_OCEAN).add((Object)Biomes.DEEP_FROZEN_OCEAN).add((Object)Biomes.GROVE).add((Object)Biomes.DEEP_DARK).add((Object)Biomes.FROZEN_RIVER).add((Object)Biomes.SNOWY_TAIGA).add((Object)Biomes.SNOWY_BEACH).addTag(BiomeTags.IS_END).add((Object)Biomes.COLD_OCEAN).add((Object)Biomes.DEEP_COLD_OCEAN).add((Object)Biomes.OLD_GROWTH_PINE_TAIGA).add((Object)Biomes.OLD_GROWTH_SPRUCE_TAIGA).add((Object)Biomes.TAIGA).add((Object)Biomes.WINDSWEPT_FOREST).add((Object)Biomes.WINDSWEPT_GRAVELLY_HILLS).add((Object)Biomes.WINDSWEPT_HILLS).add((Object)Biomes.STONY_PEAKS);
      this.tag(BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS).add((Object)Biomes.DESERT).add((Object)Biomes.WARM_OCEAN).addTag(BiomeTags.IS_JUNGLE).addTag(BiomeTags.IS_SAVANNA).addTag(BiomeTags.IS_NETHER).addTag(BiomeTags.IS_BADLANDS).add((Object)Biomes.MANGROVE_SWAMP).add((Object)Biomes.DEEP_LUKEWARM_OCEAN).add((Object)Biomes.LUKEWARM_OCEAN);
      this.tag(BiomeTags.SPAWNS_GOLD_RABBITS).add((Object)Biomes.DESERT);
      this.tag(BiomeTags.SPAWNS_WHITE_RABBITS).add((Object)Biomes.SNOWY_PLAINS).add((Object)Biomes.ICE_SPIKES).add((Object)Biomes.FROZEN_OCEAN).add((Object)Biomes.SNOWY_TAIGA).add((Object)Biomes.FROZEN_RIVER).add((Object)Biomes.SNOWY_BEACH).add((Object)Biomes.FROZEN_PEAKS).add((Object)Biomes.JAGGED_PEAKS).add((Object)Biomes.SNOWY_SLOPES).add((Object)Biomes.GROVE);
      this.tag(BiomeTags.REDUCED_WATER_AMBIENT_SPAWNS).addTag(BiomeTags.IS_RIVER);
      this.tag(BiomeTags.ALLOWS_TROPICAL_FISH_SPAWNS_AT_ANY_HEIGHT).add((Object)Biomes.LUSH_CAVES);
      this.tag(BiomeTags.POLAR_BEARS_SPAWN_ON_ALTERNATE_BLOCKS).add((Object)Biomes.FROZEN_OCEAN).add((Object)Biomes.DEEP_FROZEN_OCEAN);
      this.tag(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS).addTag(BiomeTags.IS_RIVER);
      this.tag(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS).add((Object)Biomes.SWAMP).add((Object)Biomes.MANGROVE_SWAMP);
      this.tag(BiomeTags.SPAWNS_SNOW_FOXES).add((Object)Biomes.SNOWY_PLAINS).add((Object)Biomes.ICE_SPIKES).add((Object)Biomes.FROZEN_OCEAN).add((Object)Biomes.SNOWY_TAIGA).add((Object)Biomes.FROZEN_RIVER).add((Object)Biomes.SNOWY_BEACH).add((Object)Biomes.FROZEN_PEAKS).add((Object)Biomes.JAGGED_PEAKS).add((Object)Biomes.SNOWY_SLOPES).add((Object)Biomes.GROVE);
      this.tag(BiomeTags.INCREASED_FIRE_BURNOUT).add((Object)Biomes.BAMBOO_JUNGLE).add((Object)Biomes.MUSHROOM_FIELDS).add((Object)Biomes.MANGROVE_SWAMP).add((Object)Biomes.SNOWY_SLOPES).add((Object)Biomes.FROZEN_PEAKS).add((Object)Biomes.JAGGED_PEAKS).add((Object)Biomes.SWAMP).add((Object)Biomes.JUNGLE);
      this.tag(BiomeTags.SNOW_GOLEM_MELTS).add((Object)Biomes.BADLANDS).add((Object)Biomes.BASALT_DELTAS).add((Object)Biomes.CRIMSON_FOREST).add((Object)Biomes.DESERT).add((Object)Biomes.ERODED_BADLANDS).add((Object)Biomes.NETHER_WASTES).add((Object)Biomes.SAVANNA).add((Object)Biomes.SAVANNA_PLATEAU).add((Object)Biomes.SOUL_SAND_VALLEY).add((Object)Biomes.WARPED_FOREST).add((Object)Biomes.WINDSWEPT_SAVANNA).add((Object)Biomes.WOODED_BADLANDS);
   }
}
