package net.minecraft.world.entity.projectile;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public abstract class AbstractThrownPotion extends ThrowableItemProjectile {
   public static final double SPLASH_RANGE = 4.0D;
   protected static final double SPLASH_RANGE_SQ = 16.0D;
   public static final Predicate<LivingEntity> WATER_SENSITIVE_OR_ON_FIRE = (var0) -> {
      return var0.isSensitiveToWater() || var0.isOnFire();
   };

   public AbstractThrownPotion(EntityType<? extends AbstractThrownPotion> var1, Level var2) {
      super(var1, var2);
   }

   public AbstractThrownPotion(EntityType<? extends AbstractThrownPotion> var1, Level var2, LivingEntity var3, ItemStack var4) {
      super(var1, var3, var2, var4);
   }

   public AbstractThrownPotion(EntityType<? extends AbstractThrownPotion> var1, Level var2, double var3, double var5, double var7, ItemStack var9) {
      super(var1, var3, var5, var7, var2, var9);
   }

   protected double getDefaultGravity() {
      return 0.05D;
   }

   protected void onHitBlock(BlockHitResult var1) {
      super.onHitBlock(var1);
      if (!this.level().isClientSide()) {
         ItemStack var2 = this.getItem();
         Direction var3 = var1.getDirection();
         BlockPos var4 = var1.getBlockPos();
         BlockPos var5 = var4.relative(var3);
         PotionContents var6 = (PotionContents)var2.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         if (var6.is(Potions.WATER)) {
            this.dowseFire(var5);
            this.dowseFire(var5.relative(var3.getOpposite()));
            Iterator var7 = Direction.Plane.HORIZONTAL.iterator();

            while(var7.hasNext()) {
               Direction var8 = (Direction)var7.next();
               this.dowseFire(var5.relative(var8));
            }
         }

      }
   }

   protected void onHit(HitResult var1) {
      super.onHit(var1);
      Level var3 = this.level();
      if (var3 instanceof ServerLevel) {
         ServerLevel var2 = (ServerLevel)var3;
         ItemStack var6 = this.getItem();
         PotionContents var4 = (PotionContents)var6.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         if (var4.is(Potions.WATER)) {
            this.onHitAsWater(var2);
         } else if (var4.hasEffects()) {
            this.onHitAsPotion(var2, var6, var1);
         }

         int var5 = var4.potion().isPresent() && ((Potion)((Holder)var4.potion().get()).value()).hasInstantEffects() ? 2007 : 2002;
         var2.levelEvent(var5, this.blockPosition(), var4.getColor());
         this.discard();
      }
   }

   private void onHitAsWater(ServerLevel var1) {
      AABB var2 = this.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);
      List var3 = this.level().getEntitiesOfClass(LivingEntity.class, var2, WATER_SENSITIVE_OR_ON_FIRE);
      Iterator var4 = var3.iterator();

      while(var4.hasNext()) {
         LivingEntity var5 = (LivingEntity)var4.next();
         double var6 = this.distanceToSqr(var5);
         if (var6 < 16.0D) {
            if (var5.isSensitiveToWater()) {
               var5.hurtServer(var1, this.damageSources().indirectMagic(this, this.getOwner()), 1.0F);
            }

            if (var5.isOnFire() && var5.isAlive()) {
               var5.extinguishFire();
            }
         }
      }

      List var8 = this.level().getEntitiesOfClass(Axolotl.class, var2);
      Iterator var9 = var8.iterator();

      while(var9.hasNext()) {
         Axolotl var10 = (Axolotl)var9.next();
         var10.rehydrate();
      }

   }

   protected abstract void onHitAsPotion(ServerLevel var1, ItemStack var2, HitResult var3);

   private void dowseFire(BlockPos var1) {
      BlockState var2 = this.level().getBlockState(var1);
      if (var2.is(BlockTags.FIRE)) {
         this.level().destroyBlock(var1, false, this);
      } else if (AbstractCandleBlock.isLit(var2)) {
         AbstractCandleBlock.extinguish((Player)null, var2, this.level(), var1);
      } else if (CampfireBlock.isLitCampfire(var2)) {
         this.level().levelEvent((Entity)null, 1009, var1, 0);
         CampfireBlock.dowse(this.getOwner(), this.level(), var1, var2);
         this.level().setBlockAndUpdate(var1, (BlockState)var2.setValue(CampfireBlock.LIT, false));
      }

   }

   public DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(LivingEntity var1, DamageSource var2) {
      double var3 = var1.position().x - this.position().x;
      double var5 = var1.position().z - this.position().z;
      return DoubleDoubleImmutablePair.of(var3, var5);
   }
}
