package net.minecraft.client.renderer.state;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Direction;

public class WorldBorderRenderState {
   public double minX;
   public double maxX;
   public double minZ;
   public double maxZ;
   public int tint;
   public double alpha;

   public WorldBorderRenderState() {
      super();
   }

   public List<WorldBorderRenderState.DistancePerDirection> closestBorder(double var1, double var3) {
      WorldBorderRenderState.DistancePerDirection[] var5 = new WorldBorderRenderState.DistancePerDirection[]{new WorldBorderRenderState.DistancePerDirection(Direction.NORTH, var3 - this.minZ), new WorldBorderRenderState.DistancePerDirection(Direction.SOUTH, this.maxZ - var3), new WorldBorderRenderState.DistancePerDirection(Direction.WEST, var1 - this.minX), new WorldBorderRenderState.DistancePerDirection(Direction.EAST, this.maxX - var1)};
      return Arrays.stream(var5).sorted(Comparator.comparingDouble((var0) -> {
         return var0.distance;
      })).toList();
   }

   public void reset() {
      this.alpha = 0.0D;
   }

   public static record DistancePerDirection(Direction direction, double distance) {
      final double distance;

      public DistancePerDirection(Direction param1, double param2) {
         super();
         this.direction = var1;
         this.distance = var2;
      }

      public Direction direction() {
         return this.direction;
      }

      public double distance() {
         return this.distance;
      }
   }
}
