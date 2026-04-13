package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.FlatLevelGeneratorPresetTags;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets;

public class FlatLevelGeneratorPresetTagsProvider extends KeyTagProvider<FlatLevelGeneratorPreset> {
   public FlatLevelGeneratorPresetTagsProvider(PackOutput var1, CompletableFuture<HolderLookup.Provider> var2) {
      super(var1, Registries.FLAT_LEVEL_GENERATOR_PRESET, var2);
   }

   protected void addTags(HolderLookup.Provider var1) {
      this.tag(FlatLevelGeneratorPresetTags.VISIBLE).add((Object)FlatLevelGeneratorPresets.CLASSIC_FLAT).add((Object)FlatLevelGeneratorPresets.TUNNELERS_DREAM).add((Object)FlatLevelGeneratorPresets.WATER_WORLD).add((Object)FlatLevelGeneratorPresets.OVERWORLD).add((Object)FlatLevelGeneratorPresets.SNOWY_KINGDOM).add((Object)FlatLevelGeneratorPresets.BOTTOMLESS_PIT).add((Object)FlatLevelGeneratorPresets.DESERT).add((Object)FlatLevelGeneratorPresets.REDSTONE_READY).add((Object)FlatLevelGeneratorPresets.THE_VOID);
   }
}
