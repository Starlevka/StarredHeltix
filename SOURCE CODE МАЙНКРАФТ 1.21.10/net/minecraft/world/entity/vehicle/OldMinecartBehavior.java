package net.minecraft.world.entity.vehicle;

import com.mojang.datafixers.util.Pair;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class OldMinecartBehavior extends MinecartBehavior {
   private static final double MINECART_RIDABLE_THRESHOLD = 0.01D;
   private static final double MAX_SPEED_IN_WATER = 0.2D;
   private static final double MAX_SPEED_ON_LAND = 0.4D;
   private static final double ABSOLUTE_MAX_SPEED = 0.4D;
   private final InterpolationHandler interpolation;
   private Vec3 targetDeltaMovement;

   public OldMinecartBehavior(AbstractMinecart var1) {
      super(var1);
      this.targetDeltaMovement = Vec3.ZERO;
      this.interpolation = new InterpolationHandler(var1, this::onInterpolation);
   }

   public InterpolationHandler getInterpolation() {
      return this.interpolation;
   }

   public void onInterpolation(InterpolationHandler var1) {
      this.setDeltaMovement(this.targetDeltaMovement);
   }

   public void lerpMotion(Vec3 var1) {
      this.targetDeltaMovement = var1;
      this.setDeltaMovement(this.targetDeltaMovement);
   }

   public void tick() {
      Level var2 = this.level();
      if (var2 instanceof ServerLevel) {
         ServerLevel var1 = (ServerLevel)var2;
         this.minecart.applyGravity();
         BlockPos var11 = this.minecart.getCurrentBlockPosOrRailBelow();
         BlockState var3 = this.level().getBlockState(var11);
         boolean var4 = BaseRailBlock.isRail(var3);
         this.minecart.setOnRails(var4);
         if (var4) {
            this.moveAlongTrack(var1);
            if (var3.is(Blocks.ACTIVATOR_RAIL)) {
               this.minecart.activateMinecart(var11.getX(), var11.getY(), var11.getZ(), (Boolean)var3.getValue(PoweredRailBlock.POWERED));
            }
         } else {
            this.minecart.comeOffTrack(var1);
         }

         this.minecart.applyEffectsFromBlocks();
         this.setXRot(0.0F);
         double var5 = this.minecart.xo - this.getX();
         double var7 = this.minecart.zo - this.getZ();
         if (var5 * var5 + var7 * var7 > 0.001D) {
            this.setYRot((float)(Mth.atan2(var7, var5) * 180.0D / 3.141592653589793D));
            if (this.minecart.isFlipped()) {
               this.setYRot(this.getYRot() + 180.0F);
            }
         }

         double var9 = (double)Mth.wrapDegrees(this.getYRot() - this.minecart.yRotO);
         if (var9 < -170.0D || var9 >= 170.0D) {
            this.setYRot(this.getYRot() + 180.0F);
            this.minecart.setFlipped(!this.minecart.isFlipped());
         }

         this.setXRot(this.getXRot() % 360.0F);
         this.setYRot(this.getYRot() % 360.0F);
         this.pushAndPickupEntities();
      } else {
         if (this.interpolation.hasActiveInterpolation()) {
            this.interpolation.interpolate();
         } else {
            this.minecart.reapplyPosition();
            this.setXRot(this.getXRot() % 360.0F);
            this.setYRot(this.getYRot() % 360.0F);
         }

      }
   }

   public void moveAlongTrack(ServerLevel var1) {
      BlockPos var2 = this.minecart.getCurrentBlockPosOrRailBelow();
      BlockState var3 = this.level().getBlockState(var2);
      this.minecart.resetFallDistance();
      double var4 = this.minecart.getX();
      double var6 = this.minecart.getY();
      double var8 = this.minecart.getZ();
      Vec3 var10 = this.getPos(var4, var6, var8);
      var6 = (double)var2.getY();
      boolean var11 = false;
      boolean var12 = false;
      if (var3.is(Blocks.POWERED_RAIL)) {
         var11 = (Boolean)var3.getValue(PoweredRailBlock.POWERED);
         var12 = !var11;
      }

      double var13 = 0.0078125D;
      if (this.minecart.isInWater()) {
         var13 *= 0.2D;
      }

      Vec3 var15 = this.getDeltaMovement();
      RailShape var16 = (RailShape)var3.getValue(((BaseRailBlock)var3.getBlock()).getShapeProperty());
      switch(var16) {
      case ASCENDING_EAST:
         this.setDeltaMovement(var15.add(-var13, 0.0D, 0.0D));
         ++var6;
         break;
      case ASCENDING_WEST:
         this.setDeltaMovement(var15.add(var13, 0.0D, 0.0D));
         ++var6;
         break;
      case ASCENDING_NORTH:
         this.setDeltaMovement(var15.add(0.0D, 0.0D, var13));
         ++var6;
         break;
      case ASCENDING_SOUTH:
         this.setDeltaMovement(var15.add(0.0D, 0.0D, -var13));
         ++var6;
      }

      var15 = this.getDeltaMovement();
      Pair var17 = AbstractMinecart.exits(var16);
      Vec3i var18 = (Vec3i)var17.getFirst();
      Vec3i var19 = (Vec3i)var17.getSecond();
      double var20 = (double)(var19.getX() - var18.getX());
      double var22 = (double)(var19.getZ() - var18.getZ());
      double var24 = Math.sqrt(var20 * var20 + var22 * var22);
      double var26 = var15.x * var20 + var15.z * var22;
      if (var26 < 0.0D) {
         var20 = -var20;
         var22 = -var22;
      }

      double var28 = Math.min(2.0D, var15.horizontalDistance());
      var15 = new Vec3(var28 * var20 / var24, var15.y, var28 * var22 / var24);
      this.setDeltaMovement(var15);
      Entity var30 = this.minecart.getFirstPassenger();
      Entity var33 = this.minecart.getFirstPassenger();
      Vec3 var31;
      if (var33 instanceof ServerPlayer) {
         ServerPlayer var32 = (ServerPlayer)var33;
         var31 = var32.getLastClientMoveIntent();
      } else {
         var31 = Vec3.ZERO;
      }

      if (var30 instanceof Player && var31.lengthSqr() > 0.0D) {
         Vec3 var59 = var31.normalize();
         double var61 = this.getDeltaMovement().horizontalDistanceSqr();
         if (var59.lengthSqr() > 0.0D && var61 < 0.01D) {
            this.setDeltaMovement(this.getDeltaMovement().add(var31.x * 0.001D, 0.0D, var31.z * 0.001D));
            var12 = false;
         }
      }

      double var60;
      if (var12) {
         var60 = this.getDeltaMovement().horizontalDistance();
         if (var60 < 0.03D) {
            this.setDeltaMovement(Vec3.ZERO);
         } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.5D, 0.0D, 0.5D));
         }
      }

      var60 = (double)var2.getX() + 0.5D + (double)var18.getX() * 0.5D;
      double var34 = (double)var2.getZ() + 0.5D + (double)var18.getZ() * 0.5D;
      double var36 = (double)var2.getX() + 0.5D + (double)var19.getX() * 0.5D;
      double var38 = (double)var2.getZ() + 0.5D + (double)var19.getZ() * 0.5D;
      var20 = var36 - var60;
      var22 = var38 - var34;
      double var40;
      double var42;
      double var44;
      if (var20 == 0.0D) {
         var40 = var8 - (double)var2.getZ();
      } else if (var22 == 0.0D) {
         var40 = var4 - (double)var2.getX();
      } else {
         var42 = var4 - var60;
         var44 = var8 - var34;
         var40 = (var42 * var20 + var44 * var22) * 2.0D;
      }

      var4 = var60 + var20 * var40;
      var8 = var34 + var22 * var40;
      this.setPos(var4, var6, var8);
      var42 = this.minecart.isVehicle() ? 0.75D : 1.0D;
      var44 = this.minecart.getMaxSpeed(var1);
      var15 = this.getDeltaMovement();
      this.minecart.move(MoverType.SELF, new Vec3(Mth.clamp(var42 * var15.x, -var44, var44), 0.0D, Mth.clamp(var42 * var15.z, -var44, var44)));
      if (var18.getY() != 0 && Mth.floor(this.minecart.getX()) - var2.getX() == var18.getX() && Mth.floor(this.minecart.getZ()) - var2.getZ() == var18.getZ()) {
         this.setPos(this.minecart.getX(), this.minecart.getY() + (double)var18.getY(), this.minecart.getZ());
      } else if (var19.getY() != 0 && Mth.floor(this.minecart.getX()) - var2.getX() == var19.getX() && Mth.floor(this.minecart.getZ()) - var2.getZ() == var19.getZ()) {
         this.setPos(this.minecart.getX(), this.minecart.getY() + (double)var19.getY(), this.minecart.getZ());
      }

      this.setDeltaMovement(this.minecart.applyNaturalSlowdown(this.getDeltaMovement()));
      Vec3 var46 = this.getPos(this.minecart.getX(), this.minecart.getY(), this.minecart.getZ());
      Vec3 var49;
      double var50;
      if (var46 != null && var10 != null) {
         double var47 = (var10.y - var46.y) * 0.05D;
         var49 = this.getDeltaMovement();
         var50 = var49.horizontalDistance();
         if (var50 > 0.0D) {
            this.setDeltaMovement(var49.multiply((var50 + var47) / var50, 1.0D, (var50 + var47) / var50));
         }

         this.setPos(this.minecart.getX(), var46.y, this.minecart.getZ());
      }

      int var57 = Mth.floor(this.minecart.getX());
      int var48 = Mth.floor(this.minecart.getZ());
      if (var57 != var2.getX() || var48 != var2.getZ()) {
         var49 = this.getDeltaMovement();
         var50 = var49.horizontalDistance();
         this.setDeltaMovement(var50 * (double)(var57 - var2.getX()), var49.y, var50 * (double)(var48 - var2.getZ()));
      }

      if (var11) {
         var49 = this.getDeltaMovement();
         var50 = var49.horizontalDistance();
         if (var50 > 0.01D) {
            double var52 = 0.06D;
            this.setDeltaMovement(var49.add(var49.x / var50 * 0.06D, 0.0D, var49.z / var50 * 0.06D));
         } else {
            Vec3 var58 = this.getDeltaMovement();
            double var53 = var58.x;
            double var55 = var58.z;
            if (var16 == RailShape.EAST_WEST) {
               if (this.minecart.isRedstoneConductor(var2.west())) {
                  var53 = 0.02D;
               } else if (this.minecart.isRedstoneConductor(var2.east())) {
                  var53 = -0.02D;
               }
            } else {
               if (var16 != RailShape.NORTH_SOUTH) {
                  return;
               }

               if (this.minecart.isRedstoneConductor(var2.north())) {
                  var55 = 0.02D;
               } else if (this.minecart.isRedstoneConductor(var2.south())) {
                  var55 = -0.02D;
               }
            }

            this.setDeltaMovement(var53, var58.y, var55);
         }
      }

   }

   @Nullable
   public Vec3 getPosOffs(double var1, double var3, double var5, double var7) {
      int var9 = Mth.floor(var1);
      int var10 = Mth.floor(var3);
      int var11 = Mth.floor(var5);
      if (this.level().getBlockState(new BlockPos(var9, var10 - 1, var11)).is(BlockTags.RAILS)) {
         --var10;
      }

      BlockState var12 = this.level().getBlockState(new BlockPos(var9, var10, var11));
      if (BaseRailBlock.isRail(var12)) {
         RailShape var13 = (RailShape)var12.getValue(((BaseRailBlock)var12.getBlock()).getShapeProperty());
         var3 = (double)var10;
         if (var13.isSlope()) {
            var3 = (double)(var10 + 1);
         }

         Pair var14 = AbstractMinecart.exits(var13);
         Vec3i var15 = (Vec3i)var14.getFirst();
         Vec3i var16 = (Vec3i)var14.getSecond();
         double var17 = (double)(var16.getX() - var15.getX());
         double var19 = (double)(var16.getZ() - var15.getZ());
         double var21 = Math.sqrt(var17 * var17 + var19 * var19);
         var17 /= var21;
         var19 /= var21;
         var1 += var17 * var7;
         var5 += var19 * var7;
         if (var15.getY() != 0 && Mth.floor(var1) - var9 == var15.getX() && Mth.floor(var5) - var11 == var15.getZ()) {
            var3 += (double)var15.getY();
         } else if (var16.getY() != 0 && Mth.floor(var1) - var9 == var16.getX() && Mth.floor(var5) - var11 == var16.getZ()) {
            var3 += (double)var16.getY();
         }

         return this.getPos(var1, var3, var5);
      } else {
         return null;
      }
   }

   @Nullable
   public Vec3 getPos(double var1, double var3, double var5) {
      int var7 = Mth.floor(var1);
      int var8 = Mth.floor(var3);
      int var9 = Mth.floor(var5);
      if (this.level().getBlockState(new BlockPos(var7, var8 - 1, var9)).is(BlockTags.RAILS)) {
         --var8;
      }

      BlockState var10 = this.level().getBlockState(new BlockPos(var7, var8, var9));
      if (BaseRailBlock.isRail(var10)) {
         RailShape var11 = (RailShape)var10.getValue(((BaseRailBlock)var10.getBlock()).getShapeProperty());
         Pair var12 = AbstractMinecart.exits(var11);
         Vec3i var13 = (Vec3i)var12.getFirst();
         Vec3i var14 = (Vec3i)var12.getSecond();
         double var15 = (double)var7 + 0.5D + (double)var13.getX() * 0.5D;
         double var17 = (double)var8 + 0.0625D + (double)var13.getY() * 0.5D;
         double var19 = (double)var9 + 0.5D + (double)var13.getZ() * 0.5D;
         double var21 = (double)var7 + 0.5D + (double)var14.getX() * 0.5D;
         double var23 = (double)var8 + 0.0625D + (double)var14.getY() * 0.5D;
         double var25 = (double)var9 + 0.5D + (double)var14.getZ() * 0.5D;
         double var27 = var21 - var15;
         double var29 = (var23 - var17) * 2.0D;
         double var31 = var25 - var19;
         double var33;
         if (var27 == 0.0D) {
            var33 = var5 - (double)var9;
         } else if (var31 == 0.0D) {
            var33 = var1 - (double)var7;
         } else {
            double var35 = var1 - var15;
            double var37 = var5 - var19;
            var33 = (var35 * var27 + var37 * var31) * 2.0D;
         }

         var1 = var15 + var27 * var33;
         var3 = var17 + var29 * var33;
         var5 = var19 + var31 * var33;
         if (var29 < 0.0D) {
            ++var3;
         } else if (var29 > 0.0D) {
            var3 += 0.5D;
         }

         return new Vec3(var1, var3, var5);
      } else {
         return null;
      }
   }

   public double stepAlongTrack(BlockPos var1, RailShape var2, double var3) {
      return 0.0D;
   }

   public boolean pushAndPickupEntities() {
      AABB var1 = this.minecart.getBoundingBox().inflate(0.20000000298023224D, 0.0D, 0.20000000298023224D);
      if (this.minecart.isRideable() && this.getDeltaMovement().horizontalDistanceSqr() >= 0.01D) {
         List var5 = this.level().getEntities((Entity)this.minecart, var1, EntitySelector.pushableBy(this.minecart));
         if (!var5.isEmpty()) {
            Iterator var6 = var5.iterator();

            while(true) {
               while(var6.hasNext()) {
                  Entity var4 = (Entity)var6.next();
                  if (!(var4 instanceof Player) && !(var4 instanceof IronGolem) && !(var4 instanceof AbstractMinecart) && !this.minecart.isVehicle() && !var4.isPassenger()) {
                     var4.startRiding(this.minecart);
                  } else {
                     var4.push((Entity)this.minecart);
                  }
               }

               return false;
            }
         }
      } else {
         Iterator var2 = this.level().getEntities(this.minecart, var1).iterator();

         while(var2.hasNext()) {
            Entity var3 = (Entity)var2.next();
            if (!this.minecart.hasPassenger(var3) && var3.isPushable() && var3 instanceof AbstractMinecart) {
               var3.push((Entity)this.minecart);
            }
         }
      }

      return false;
   }

   public Direction getMotionDirection() {
      return this.minecart.isFlipped() ? this.minecart.getDirection().getOpposite().getClockWise() : this.minecart.getDirection().getClockWise();
   }

   public Vec3 getKnownMovement(Vec3 var1) {
      return !Double.isNaN(var1.x) && !Double.isNaN(var1.y) && !Double.isNaN(var1.z) ? new Vec3(Mth.clamp(var1.x, -0.4D, 0.4D), var1.y, Mth.clamp(var1.z, -0.4D, 0.4D)) : Vec3.ZERO;
   }

   public double getMaxSpeed(ServerLevel var1) {
      return this.minecart.isInWater() ? 0.2D : 0.4D;
   }

   public double getSlowdownFactor() {
      return this.minecart.isVehicle() ? 0.997D : 0.96D;
   }
}
