package net.minecraft.world.entity.variant;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;

public record BiomeCheck(HolderSet<Biome> requiredBiomes) implements SpawnCondition {
   public static final MapCodec<BiomeCheck> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(BiomeCheck::requiredBiomes)).apply(var0, BiomeCheck::new);
   });

   public BiomeCheck(HolderSet<Biome> param1) {
      super();
      this.requiredBiomes = var1;
   }

   public boolean test(SpawnContext var1) {
      return this.requiredBiomes.contains(var1.biome());
   }

   public MapCodec<BiomeCheck> codec() {
      return MAP_CODEC;
   }

   public HolderSet<Biome> requiredBiomes() {
      return this.requiredBiomes;
   }

   // $FF: synthetic method
   public boolean test(final Object param1) {
      return this.test((SpawnContext)var1);
   }
}
