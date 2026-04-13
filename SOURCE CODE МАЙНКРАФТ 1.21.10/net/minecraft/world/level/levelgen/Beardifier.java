package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class Beardifier implements DensityFunctions.BeardifierOrMarker {
   public static final int BEARD_KERNEL_RADIUS = 12;
   private static final int BEARD_KERNEL_SIZE = 24;
   private static final float[] BEARD_KERNEL = (float[])Util.make(new float[13824], (var0) -> {
      for(int var1 = 0; var1 < 24; ++var1) {
         for(int var2 = 0; var2 < 24; ++var2) {
            for(int var3 = 0; var3 < 24; ++var3) {
               var0[var1 * 24 * 24 + var2 * 24 + var3] = (float)computeBeardContribution(var2 - 12, var3 - 12, var1 - 12);
            }
         }
      }

   });
   public static final Beardifier EMPTY = new Beardifier(List.of(), List.of(), (BoundingBox)null);
   private final List<Beardifier.Rigid> pieces;
   private final List<JigsawJunction> junctions;
   @Nullable
   private final BoundingBox affectedBox;

   public static Beardifier forStructuresInChunk(StructureManager var0, ChunkPos var1) {
      List var2 = var0.startsForStructure(var1, (var0x) -> {
         return var0x.terrainAdaptation() != TerrainAdjustment.NONE;
      });
      if (var2.isEmpty()) {
         return EMPTY;
      } else {
         int var3 = var1.getMinBlockX();
         int var4 = var1.getMinBlockZ();
         ArrayList var5 = new ArrayList();
         ArrayList var6 = new ArrayList();
         BoundingBox var7 = null;
         Iterator var8 = var2.iterator();

         label61:
         while(var8.hasNext()) {
            StructureStart var9 = (StructureStart)var8.next();
            TerrainAdjustment var10 = var9.getStructure().terrainAdaptation();
            Iterator var11 = var9.getPieces().iterator();

            while(true) {
               while(true) {
                  StructurePiece var12;
                  do {
                     if (!var11.hasNext()) {
                        continue label61;
                     }

                     var12 = (StructurePiece)var11.next();
                  } while(!var12.isCloseToChunk(var1, 12));

                  if (var12 instanceof PoolElementStructurePiece) {
                     PoolElementStructurePiece var13 = (PoolElementStructurePiece)var12;
                     StructureTemplatePool.Projection var14 = var13.getElement().getProjection();
                     if (var14 == StructureTemplatePool.Projection.RIGID) {
                        var5.add(new Beardifier.Rigid(var13.getBoundingBox(), var10, var13.getGroundLevelDelta()));
                        var7 = includeBoundingBox(var7, var12.getBoundingBox());
                     }

                     Iterator var15 = var13.getJunctions().iterator();

                     while(var15.hasNext()) {
                        JigsawJunction var16 = (JigsawJunction)var15.next();
                        int var17 = var16.getSourceX();
                        int var18 = var16.getSourceZ();
                        if (var17 > var3 - 12 && var18 > var4 - 12 && var17 < var3 + 15 + 12 && var18 < var4 + 15 + 12) {
                           var6.add(var16);
                           BoundingBox var19 = new BoundingBox(new BlockPos(var17, var16.getSourceGroundY(), var18));
                           var7 = includeBoundingBox(var7, var19);
                        }
                     }
                  } else {
                     var5.add(new Beardifier.Rigid(var12.getBoundingBox(), var10, 0));
                     var7 = includeBoundingBox(var7, var12.getBoundingBox());
                  }
               }
            }
         }

         if (var7 == null) {
            return EMPTY;
         } else {
            BoundingBox var20 = var7.inflatedBy(24);
            return new Beardifier(List.copyOf(var5), List.copyOf(var6), var20);
         }
      }
   }

   private static BoundingBox includeBoundingBox(@Nullable BoundingBox var0, BoundingBox var1) {
      return var0 == null ? var1 : BoundingBox.encapsulating(var0, var1);
   }

   @VisibleForTesting
   public Beardifier(List<Beardifier.Rigid> var1, List<JigsawJunction> var2, @Nullable BoundingBox var3) {
      super();
      this.pieces = var1;
      this.junctions = var2;
      this.affectedBox = var3;
   }

   public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
      if (this.affectedBox == null) {
         Arrays.fill(var1, 0.0D);
      } else {
         DensityFunctions.BeardifierOrMarker.super.fillArray(var1, var2);
      }

   }

   public double compute(DensityFunction.FunctionContext var1) {
      if (this.affectedBox == null) {
         return 0.0D;
      } else {
         int var2 = var1.blockX();
         int var3 = var1.blockY();
         int var4 = var1.blockZ();
         if (!this.affectedBox.isInside(var2, var3, var4)) {
            return 0.0D;
         } else {
            double var5 = 0.0D;

            Iterator var7;
            int var10;
            int var11;
            double var10001;
            for(var7 = this.pieces.iterator(); var7.hasNext(); var5 += var10001) {
               Beardifier.Rigid var8 = (Beardifier.Rigid)var7.next();
               BoundingBox var9 = var8.box();
               var10 = var8.groundLevelDelta();
               var11 = Math.max(0, Math.max(var9.minX() - var2, var2 - var9.maxX()));
               int var12 = Math.max(0, Math.max(var9.minZ() - var4, var4 - var9.maxZ()));
               int var13 = var9.minY() + var10;
               int var14 = var3 - var13;
               int var10000;
               switch(var8.terrainAdjustment()) {
               case NONE:
                  var10000 = 0;
                  break;
               case BURY:
               case BEARD_THIN:
                  var10000 = var14;
                  break;
               case BEARD_BOX:
                  var10000 = Math.max(0, Math.max(var13 - var3, var3 - var9.maxY()));
                  break;
               case ENCAPSULATE:
                  var10000 = Math.max(0, Math.max(var9.minY() - var3, var3 - var9.maxY()));
                  break;
               default:
                  throw new MatchException((String)null, (Throwable)null);
               }

               int var15 = var10000;
               switch(var8.terrainAdjustment()) {
               case NONE:
                  var10001 = 0.0D;
                  break;
               case BURY:
                  var10001 = getBuryContribution((double)var11, (double)var15 / 2.0D, (double)var12);
                  break;
               case BEARD_THIN:
               case BEARD_BOX:
                  var10001 = getBeardContribution(var11, var15, var12, var14) * 0.8D;
                  break;
               case ENCAPSULATE:
                  var10001 = getBuryContribution((double)var11 / 2.0D, (double)var15 / 2.0D, (double)var12 / 2.0D) * 0.8D;
                  break;
               default:
                  throw new MatchException((String)null, (Throwable)null);
               }
            }

            int var17;
            for(var7 = this.junctions.iterator(); var7.hasNext(); var5 += getBeardContribution(var17, var10, var11, var10) * 0.4D) {
               JigsawJunction var16 = (JigsawJunction)var7.next();
               var17 = var2 - var16.getSourceX();
               var10 = var3 - var16.getSourceGroundY();
               var11 = var4 - var16.getSourceZ();
            }

            return var5;
         }
      }
   }

   public double minValue() {
      return -1.0D / 0.0;
   }

   public double maxValue() {
      return 1.0D / 0.0;
   }

   private static double getBuryContribution(double var0, double var2, double var4) {
      double var6 = Mth.length(var0, var2, var4);
      return Mth.clampedMap(var6, 0.0D, 6.0D, 1.0D, 0.0D);
   }

   private static double getBeardContribution(int var0, int var1, int var2, int var3) {
      int var4 = var0 + 12;
      int var5 = var1 + 12;
      int var6 = var2 + 12;
      if (isInKernelRange(var4) && isInKernelRange(var5) && isInKernelRange(var6)) {
         double var7 = (double)var3 + 0.5D;
         double var9 = Mth.lengthSquared((double)var0, var7, (double)var2);
         double var11 = -var7 * Mth.fastInvSqrt(var9 / 2.0D) / 2.0D;
         return var11 * (double)BEARD_KERNEL[var6 * 24 * 24 + var4 * 24 + var5];
      } else {
         return 0.0D;
      }
   }

   private static boolean isInKernelRange(int var0) {
      return var0 >= 0 && var0 < 24;
   }

   private static double computeBeardContribution(int var0, int var1, int var2) {
      return computeBeardContribution(var0, (double)var1 + 0.5D, var2);
   }

   private static double computeBeardContribution(int var0, double var1, int var3) {
      double var4 = Mth.lengthSquared((double)var0, var1, (double)var3);
      double var6 = Math.pow(2.718281828459045D, -var4 / 16.0D);
      return var6;
   }

   @VisibleForTesting
   public static record Rigid(BoundingBox box, TerrainAdjustment terrainAdjustment, int groundLevelDelta) {
      public Rigid(BoundingBox param1, TerrainAdjustment param2, int param3) {
         super();
         this.box = var1;
         this.terrainAdjustment = var2;
         this.groundLevelDelta = var3;
      }

      public BoundingBox box() {
         return this.box;
      }

      public TerrainAdjustment terrainAdjustment() {
         return this.terrainAdjustment;
      }

      public int groundLevelDelta() {
         return this.groundLevelDelta;
      }
   }
}
