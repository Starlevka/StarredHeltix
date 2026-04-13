package net.minecraft.world.entity.monster;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Ravager extends Raider {
   private static final Predicate<Entity> ROAR_TARGET_WITH_GRIEFING = (var0) -> {
      return !(var0 instanceof Ravager) && var0.isAlive();
   };
   private static final Predicate<Entity> ROAR_TARGET_WITHOUT_GRIEFING = (var0) -> {
      return ROAR_TARGET_WITH_GRIEFING.test(var0) && !var0.getType().equals(EntityType.ARMOR_STAND);
   };
   private static final Predicate<LivingEntity> ROAR_TARGET_ON_CLIENT = (var0) -> {
      return !(var0 instanceof Ravager) && var0.isAlive() && var0.isLocalInstanceAuthoritative();
   };
   private static final double BASE_MOVEMENT_SPEED = 0.3D;
   private static final double ATTACK_MOVEMENT_SPEED = 0.35D;
   private static final int STUNNED_COLOR = 8356754;
   private static final float STUNNED_COLOR_BLUE = 0.57254905F;
   private static final float STUNNED_COLOR_GREEN = 0.5137255F;
   private static final float STUNNED_COLOR_RED = 0.49803922F;
   public static final int ATTACK_DURATION = 10;
   public static final int STUN_DURATION = 40;
   private static final int DEFAULT_ATTACK_TICK = 0;
   private static final int DEFAULT_STUN_TICK = 0;
   private static final int DEFAULT_ROAR_TICK = 0;
   private int attackTick = 0;
   private int stunnedTick = 0;
   private int roarTick = 0;

   public Ravager(EntityType<? extends Ravager> var1, Level var2) {
      super(var1, var2);
      this.xpReward = 20;
      this.setPathfindingMalus(PathType.LEAVES, 0.0F);
   }

   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.4D));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
      this.targetSelector.addGoal(2, (new HurtByTargetGoal(this, new Class[]{Raider.class})).setAlertOthers());
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, Player.class, true));
      this.targetSelector.addGoal(4, new NearestAttackableTargetGoal(this, AbstractVillager.class, true, (var0, var1) -> {
         return !var0.isBaby();
      }));
      this.targetSelector.addGoal(4, new NearestAttackableTargetGoal(this, IronGolem.class, true));
   }

   protected void updateControlFlags() {
      boolean var1 = !(this.getControllingPassenger() instanceof Mob) || this.getControllingPassenger().getType().is(EntityTypeTags.RAIDERS);
      boolean var2 = !(this.getVehicle() instanceof AbstractBoat);
      this.goalSelector.setControlFlag(Goal.Flag.MOVE, var1);
      this.goalSelector.setControlFlag(Goal.Flag.JUMP, var1 && var2);
      this.goalSelector.setControlFlag(Goal.Flag.LOOK, var1);
      this.goalSelector.setControlFlag(Goal.Flag.TARGET, var1);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100.0D).add(Attributes.MOVEMENT_SPEED, 0.3D).add(Attributes.KNOCKBACK_RESISTANCE, 0.75D).add(Attributes.ATTACK_DAMAGE, 12.0D).add(Attributes.ATTACK_KNOCKBACK, 1.5D).add(Attributes.FOLLOW_RANGE, 32.0D).add(Attributes.STEP_HEIGHT, 1.0D);
   }

   protected void addAdditionalSaveData(ValueOutput var1) {
      super.addAdditionalSaveData(var1);
      var1.putInt("AttackTick", this.attackTick);
      var1.putInt("StunTick", this.stunnedTick);
      var1.putInt("RoarTick", this.roarTick);
   }

   protected void readAdditionalSaveData(ValueInput var1) {
      super.readAdditionalSaveData(var1);
      this.attackTick = var1.getIntOr("AttackTick", 0);
      this.stunnedTick = var1.getIntOr("StunTick", 0);
      this.roarTick = var1.getIntOr("RoarTick", 0);
   }

   public SoundEvent getCelebrateSound() {
      return SoundEvents.RAVAGER_CELEBRATE;
   }

   public int getMaxHeadYRot() {
      return 45;
   }

   public void aiStep() {
      super.aiStep();
      if (this.isAlive()) {
         if (this.isImmobile()) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
         } else {
            double var1 = this.getTarget() != null ? 0.35D : 0.3D;
            double var3 = this.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Mth.lerp(0.1D, var3, var1));
         }

         Level var2 = this.level();
         if (var2 instanceof ServerLevel) {
            ServerLevel var8 = (ServerLevel)var2;
            if (this.horizontalCollision && var8.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
               boolean var9 = false;
               AABB var10 = this.getBoundingBox().inflate(0.2D);
               Iterator var4 = BlockPos.betweenClosed(Mth.floor(var10.minX), Mth.floor(var10.minY), Mth.floor(var10.minZ), Mth.floor(var10.maxX), Mth.floor(var10.maxY), Mth.floor(var10.maxZ)).iterator();

               label61:
               while(true) {
                  BlockPos var5;
                  Block var7;
                  do {
                     if (!var4.hasNext()) {
                        if (!var9 && this.onGround()) {
                           this.jumpFromGround();
                        }
                        break label61;
                     }

                     var5 = (BlockPos)var4.next();
                     BlockState var6 = var8.getBlockState(var5);
                     var7 = var6.getBlock();
                  } while(!(var7 instanceof LeavesBlock));

                  var9 = var8.destroyBlock(var5, true, this) || var9;
               }
            }
         }

         if (this.roarTick > 0) {
            --this.roarTick;
            if (this.roarTick == 10) {
               this.roar();
            }
         }

         if (this.attackTick > 0) {
            --this.attackTick;
         }

         if (this.stunnedTick > 0) {
            --this.stunnedTick;
            this.stunEffect();
            if (this.stunnedTick == 0) {
               this.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.0F);
               this.roarTick = 20;
            }
         }

      }
   }

   private void stunEffect() {
      if (this.random.nextInt(6) == 0) {
         double var1 = this.getX() - (double)this.getBbWidth() * Math.sin((double)(this.yBodyRot * 0.017453292F)) + (this.random.nextDouble() * 0.6D - 0.3D);
         double var3 = this.getY() + (double)this.getBbHeight() - 0.3D;
         double var5 = this.getZ() + (double)this.getBbWidth() * Math.cos((double)(this.yBodyRot * 0.017453292F)) + (this.random.nextDouble() * 0.6D - 0.3D);
         this.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.49803922F, 0.5137255F, 0.57254905F), var1, var3, var5, 0.0D, 0.0D, 0.0D);
      }

   }

   protected boolean isImmobile() {
      return super.isImmobile() || this.attackTick > 0 || this.stunnedTick > 0 || this.roarTick > 0;
   }

   public boolean hasLineOfSight(Entity var1) {
      return this.stunnedTick <= 0 && this.roarTick <= 0 ? super.hasLineOfSight(var1) : false;
   }

   protected void blockedByItem(LivingEntity var1) {
      if (this.roarTick == 0) {
         if (this.random.nextDouble() < 0.5D) {
            this.stunnedTick = 40;
            this.playSound(SoundEvents.RAVAGER_STUNNED, 1.0F, 1.0F);
            this.level().broadcastEntityEvent(this, (byte)39);
            var1.push(this);
         } else {
            this.strongKnockback(var1);
         }

         var1.hurtMarked = true;
      }

   }

   private void roar() {
      if (this.isAlive()) {
         Level var2 = this.level();
         if (var2 instanceof ServerLevel) {
            ServerLevel var1 = (ServerLevel)var2;
            Predicate var6 = var1.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) ? ROAR_TARGET_WITH_GRIEFING : ROAR_TARGET_WITHOUT_GRIEFING;
            List var3 = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0D), var6);
            Iterator var4 = var3.iterator();

            while(var4.hasNext()) {
               LivingEntity var5 = (LivingEntity)var4.next();
               if (!(var5 instanceof AbstractIllager)) {
                  var5.hurtServer(var1, this.damageSources().mobAttack(this), 6.0F);
               }

               if (!(var5 instanceof Player)) {
                  this.strongKnockback(var5);
               }
            }

            this.gameEvent(GameEvent.ENTITY_ACTION);
            var1.broadcastEntityEvent(this, (byte)69);
         }
      }

   }

   private void applyRoarKnockbackClient() {
      List var1 = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0D), ROAR_TARGET_ON_CLIENT);
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         LivingEntity var3 = (LivingEntity)var2.next();
         this.strongKnockback(var3);
      }

   }

   private void strongKnockback(Entity var1) {
      double var2 = var1.getX() - this.getX();
      double var4 = var1.getZ() - this.getZ();
      double var6 = Math.max(var2 * var2 + var4 * var4, 0.001D);
      var1.push(var2 / var6 * 4.0D, 0.2D, var4 / var6 * 4.0D);
   }

   public void handleEntityEvent(byte var1) {
      if (var1 == 4) {
         this.attackTick = 10;
         this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 1.0F);
      } else if (var1 == 39) {
         this.stunnedTick = 40;
      } else if (var1 == 69) {
         this.addRoarParticleEffects();
         this.applyRoarKnockbackClient();
      }

      super.handleEntityEvent(var1);
   }

   private void addRoarParticleEffects() {
      Vec3 var1 = this.getBoundingBox().getCenter();

      for(int var2 = 0; var2 < 40; ++var2) {
         double var3 = this.random.nextGaussian() * 0.2D;
         double var5 = this.random.nextGaussian() * 0.2D;
         double var7 = this.random.nextGaussian() * 0.2D;
         this.level().addParticle(ParticleTypes.POOF, var1.x, var1.y, var1.z, var3, var5, var7);
      }

   }

   public int getAttackTick() {
      return this.attackTick;
   }

   public int getStunnedTick() {
      return this.stunnedTick;
   }

   public int getRoarTick() {
      return this.roarTick;
   }

   public boolean doHurtTarget(ServerLevel var1, Entity var2) {
      this.attackTick = 10;
      var1.broadcastEntityEvent(this, (byte)4);
      this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 1.0F);
      return super.doHurtTarget(var1, var2);
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return SoundEvents.RAVAGER_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.RAVAGER_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.RAVAGER_DEATH;
   }

   protected void playStepSound(BlockPos var1, BlockState var2) {
      this.playSound(SoundEvents.RAVAGER_STEP, 0.15F, 1.0F);
   }

   public boolean checkSpawnObstruction(LevelReader var1) {
      return !var1.containsAnyLiquid(this.getBoundingBox());
   }

   public void applyRaidBuffs(ServerLevel var1, int var2, boolean var3) {
   }

   public boolean canBeLeader() {
      return false;
   }

   protected AABB getAttackBoundingBox() {
      AABB var1 = super.getAttackBoundingBox();
      return var1.deflate(0.05D, 0.0D, 0.05D);
   }
}
