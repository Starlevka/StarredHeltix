package net.minecraft.world.level.block;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface BonemealableBlock {
   boolean isValidBonemealTarget(LevelReader var1, BlockPos var2, BlockState var3);

   boolean isBonemealSuccess(Level var1, RandomSource var2, BlockPos var3, BlockState var4);

   void performBonemeal(ServerLevel var1, RandomSource var2, BlockPos var3, BlockState var4);

   static boolean hasSpreadableNeighbourPos(LevelReader var0, BlockPos var1, BlockState var2) {
      return getSpreadableNeighbourPos(Direction.Plane.HORIZONTAL.stream().toList(), var0, var1, var2).isPresent();
   }

   static Optional<BlockPos> findSpreadableNeighbourPos(Level var0, BlockPos var1, BlockState var2) {
      return getSpreadableNeighbourPos(Direction.Plane.HORIZONTAL.shuffledCopy(var0.random), var0, var1, var2);
   }

   private static Optional<BlockPos> getSpreadableNeighbourPos(List<Direction> var0, LevelReader var1, BlockPos var2, BlockState var3) {
      Iterator var4 = var0.iterator();

      BlockPos var6;
      do {
         if (!var4.hasNext()) {
            return Optional.empty();
         }

         Direction var5 = (Direction)var4.next();
         var6 = var2.relative(var5);
      } while(!var1.isEmptyBlock(var6) || !var3.canSurvive(var1, var6));

      return Optional.of(var6);
   }

   default BlockPos getParticlePos(BlockPos var1) {
      BlockPos var10000;
      switch(this.getType().ordinal()) {
      case 0:
         var10000 = var1.above();
         break;
      case 1:
         var10000 = var1;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   default BonemealableBlock.Type getType() {
      return BonemealableBlock.Type.GROWER;
   }

   public static enum Type {
      NEIGHBOR_SPREADER,
      GROWER;

      private Type() {
      }

      // $FF: synthetic method
      private static BonemealableBlock.Type[] $values() {
         return new BonemealableBlock.Type[]{NEIGHBOR_SPREADER, GROWER};
      }
   }
}
