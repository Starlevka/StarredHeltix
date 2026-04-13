package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.UniquelyIdentifyable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ConduitBlockEntity extends BlockEntity {
   private static final int BLOCK_REFRESH_RATE = 2;
   private static final int EFFECT_DURATION = 13;
   private static final float ROTATION_SPEED = -0.0375F;
   private static final int MIN_ACTIVE_SIZE = 16;
   private static final int MIN_KILL_SIZE = 42;
   private static final int KILL_RANGE = 8;
   private static final Block[] VALID_BLOCKS;
   public int tickCount;
   private float activeRotation;
   private boolean isActive;
   private boolean isHunting;
   private final List<BlockPos> effectBlocks = Lists.newArrayList();
   @Nullable
   private EntityReference<LivingEntity> destroyTarget;
   private long nextAmbientSoundActivation;

   public ConduitBlockEntity(BlockPos var1, BlockState var2) {
      super(BlockEntityType.CONDUIT, var1, var2);
   }

   protected void loadAdditional(ValueInput var1) {
      super.loadAdditional(var1);
      this.destroyTarget = EntityReference.read(var1, "Target");
   }

   protected void saveAdditional(ValueOutput var1) {
      super.saveAdditional(var1);
      EntityReference.store(this.destroyTarget, var1, "Target");
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag(HolderLookup.Provider var1) {
      return this.saveCustomOnly(var1);
   }

   public static void clientTick(Level var0, BlockPos var1, BlockState var2, ConduitBlockEntity var3) {
      ++var3.tickCount;
      long var4 = var0.getGameTime();
      List var6 = var3.effectBlocks;
      if (var4 % 40L == 0L) {
         var3.isActive = updateShape(var0, var1, var6);
         updateHunting(var3, var6);
      }

      LivingEntity var7 = EntityReference.getLivingEntity(var3.destroyTarget, var0);
      animationTick(var0, var1, var6, var7, var3.tickCount);
      if (var3.isActive()) {
         ++var3.activeRotation;
      }

   }

   public static void serverTick(Level var0, BlockPos var1, BlockState var2, ConduitBlockEntity var3) {
      ++var3.tickCount;
      long var4 = var0.getGameTime();
      List var6 = var3.effectBlocks;
      if (var4 % 40L == 0L) {
         boolean var7 = updateShape(var0, var1, var6);
         if (var7 != var3.isActive) {
            SoundEvent var8 = var7 ? SoundEvents.CONDUIT_ACTIVATE : SoundEvents.CONDUIT_DEACTIVATE;
            var0.playSound((Entity)null, (BlockPos)var1, var8, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         var3.isActive = var7;
         updateHunting(var3, var6);
         if (var7) {
            applyEffects(var0, var1, var6);
            updateAndAttackTarget((ServerLevel)var0, var1, var2, var3, var6.size() >= 42);
         }
      }

      if (var3.isActive()) {
         if (var4 % 80L == 0L) {
            var0.playSound((Entity)null, (BlockPos)var1, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         if (var4 > var3.nextAmbientSoundActivation) {
            var3.nextAmbientSoundActivation = var4 + 60L + (long)var0.getRandom().nextInt(40);
            var0.playSound((Entity)null, (BlockPos)var1, SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.BLOCKS, 1.0F, 1.0F);
         }
      }

   }

   private static void updateHunting(ConduitBlockEntity var0, List<BlockPos> var1) {
      var0.setHunting(var1.size() >= 42);
   }

   private static boolean updateShape(Level var0, BlockPos var1, List<BlockPos> var2) {
      var2.clear();

      int var3;
      int var4;
      int var5;
      for(var3 = -1; var3 <= 1; ++var3) {
         for(var4 = -1; var4 <= 1; ++var4) {
            for(var5 = -1; var5 <= 1; ++var5) {
               BlockPos var6 = var1.offset(var3, var4, var5);
               if (!var0.isWaterAt(var6)) {
                  return false;
               }
            }
         }
      }

      for(var3 = -2; var3 <= 2; ++var3) {
         for(var4 = -2; var4 <= 2; ++var4) {
            for(var5 = -2; var5 <= 2; ++var5) {
               int var15 = Math.abs(var3);
               int var7 = Math.abs(var4);
               int var8 = Math.abs(var5);
               if ((var15 > 1 || var7 > 1 || var8 > 1) && (var3 == 0 && (var7 == 2 || var8 == 2) || var4 == 0 && (var15 == 2 || var8 == 2) || var5 == 0 && (var15 == 2 || var7 == 2))) {
                  BlockPos var9 = var1.offset(var3, var4, var5);
                  BlockState var10 = var0.getBlockState(var9);
                  Block[] var11 = VALID_BLOCKS;
                  int var12 = var11.length;

                  for(int var13 = 0; var13 < var12; ++var13) {
                     Block var14 = var11[var13];
                     if (var10.is(var14)) {
                        var2.add(var9);
                     }
                  }
               }
            }
         }
      }

      return var2.size() >= 16;
   }

   private static void applyEffects(Level var0, BlockPos var1, List<BlockPos> var2) {
      int var3 = var2.size();
      int var4 = var3 / 7 * 16;
      int var5 = var1.getX();
      int var6 = var1.getY();
      int var7 = var1.getZ();
      AABB var8 = (new AABB((double)var5, (double)var6, (double)var7, (double)(var5 + 1), (double)(var6 + 1), (double)(var7 + 1))).inflate((double)var4).expandTowards(0.0D, (double)var0.getHeight(), 0.0D);
      List var9 = var0.getEntitiesOfClass(Player.class, var8);
      if (!var9.isEmpty()) {
         Iterator var10 = var9.iterator();

         while(var10.hasNext()) {
            Player var11 = (Player)var10.next();
            if (var1.closerThan(var11.blockPosition(), (double)var4) && var11.isInWaterOrRain()) {
               var11.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 260, 0, true, true));
            }
         }

      }
   }

   private static void updateAndAttackTarget(ServerLevel var0, BlockPos var1, BlockState var2, ConduitBlockEntity var3, boolean var4) {
      EntityReference var5 = updateDestroyTarget(var3.destroyTarget, var0, var1, var4);
      LivingEntity var6 = EntityReference.getLivingEntity(var5, var0);
      if (var6 != null) {
         var0.playSound((Entity)null, var6.getX(), var6.getY(), var6.getZ(), SoundEvents.CONDUIT_ATTACK_TARGET, SoundSource.BLOCKS, 1.0F, 1.0F);
         var6.hurtServer(var0, var0.damageSources().magic(), 4.0F);
      }

      if (!Objects.equals(var5, var3.destroyTarget)) {
         var3.destroyTarget = var5;
         var0.sendBlockUpdated(var1, var2, var2, 2);
      }

   }

   @Nullable
   private static EntityReference<LivingEntity> updateDestroyTarget(@Nullable EntityReference<LivingEntity> var0, ServerLevel var1, BlockPos var2, boolean var3) {
      if (!var3) {
         return null;
      } else if (var0 == null) {
         return selectNewTarget(var1, var2);
      } else {
         LivingEntity var4 = EntityReference.getLivingEntity(var0, var1);
         return var4 != null && var4.isAlive() && var2.closerThan(var4.blockPosition(), 8.0D) ? var0 : null;
      }
   }

   @Nullable
   private static EntityReference<LivingEntity> selectNewTarget(ServerLevel var0, BlockPos var1) {
      List var2 = var0.getEntitiesOfClass(LivingEntity.class, getDestroyRangeAABB(var1), (var0x) -> {
         return var0x instanceof Enemy && var0x.isInWaterOrRain();
      });
      return var2.isEmpty() ? null : EntityReference.of((UniquelyIdentifyable)((LivingEntity)Util.getRandom(var2, var0.random)));
   }

   private static AABB getDestroyRangeAABB(BlockPos var0) {
      return (new AABB(var0)).inflate(8.0D);
   }

   private static void animationTick(Level var0, BlockPos var1, List<BlockPos> var2, @Nullable Entity var3, int var4) {
      RandomSource var5 = var0.random;
      double var6 = (double)(Mth.sin((float)(var4 + 35) * 0.1F) / 2.0F + 0.5F);
      var6 = (var6 * var6 + var6) * 0.30000001192092896D;
      Vec3 var8 = new Vec3((double)var1.getX() + 0.5D, (double)var1.getY() + 1.5D + var6, (double)var1.getZ() + 0.5D);
      Iterator var9 = var2.iterator();

      float var12;
      while(var9.hasNext()) {
         BlockPos var10 = (BlockPos)var9.next();
         if (var5.nextInt(50) == 0) {
            BlockPos var11 = var10.subtract(var1);
            var12 = -0.5F + var5.nextFloat() + (float)var11.getX();
            float var13 = -2.0F + var5.nextFloat() + (float)var11.getY();
            float var14 = -0.5F + var5.nextFloat() + (float)var11.getZ();
            var0.addParticle(ParticleTypes.NAUTILUS, var8.x, var8.y, var8.z, (double)var12, (double)var13, (double)var14);
         }
      }

      if (var3 != null) {
         Vec3 var18 = new Vec3(var3.getX(), var3.getEyeY(), var3.getZ());
         float var15 = (-0.5F + var5.nextFloat()) * (3.0F + var3.getBbWidth());
         float var16 = -1.0F + var5.nextFloat() * var3.getBbHeight();
         var12 = (-0.5F + var5.nextFloat()) * (3.0F + var3.getBbWidth());
         Vec3 var17 = new Vec3((double)var15, (double)var16, (double)var12);
         var0.addParticle(ParticleTypes.NAUTILUS, var18.x, var18.y, var18.z, var17.x, var17.y, var17.z);
      }

   }

   public boolean isActive() {
      return this.isActive;
   }

   public boolean isHunting() {
      return this.isHunting;
   }

   private void setHunting(boolean var1) {
      this.isHunting = var1;
   }

   public float getActiveRotation(float var1) {
      return (this.activeRotation + var1) * -0.0375F;
   }

   // $FF: synthetic method
   public Packet getUpdatePacket() {
      return this.getUpdatePacket();
   }

   static {
      VALID_BLOCKS = new Block[]{Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN, Blocks.DARK_PRISMARINE};
   }
}
