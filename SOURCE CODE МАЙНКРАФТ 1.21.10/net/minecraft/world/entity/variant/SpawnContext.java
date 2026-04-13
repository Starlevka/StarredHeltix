package net.minecraft.world.entity.variant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;

public record SpawnContext(BlockPos pos, ServerLevelAccessor level, Holder<Biome> biome) {
   public SpawnContext(BlockPos param1, ServerLevelAccessor param2, Holder<Biome> param3) {
      super();
      this.pos = var1;
      this.level = var2;
      this.biome = var3;
   }

   public static SpawnContext create(ServerLevelAccessor var0, BlockPos var1) {
      Holder var2 = var0.getBiome(var1);
      return new SpawnContext(var1, var0, var2);
   }

   public BlockPos pos() {
      return this.pos;
   }

   public ServerLevelAccessor level() {
      return this.level;
   }

   public Holder<Biome> biome() {
      return this.biome;
   }
}
