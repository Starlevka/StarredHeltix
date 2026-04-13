package net.minecraft.world.level.block.entity.trialspawner;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.slf4j.Logger;

public final class TrialSpawner {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int DETECT_PLAYER_SPAWN_BUFFER = 40;
   private static final int DEFAULT_TARGET_COOLDOWN_LENGTH = 36000;
   private static final int DEFAULT_PLAYER_SCAN_RANGE = 14;
   private static final int MAX_MOB_TRACKING_DISTANCE = 47;
   private static final int MAX_MOB_TRACKING_DISTANCE_SQR = Mth.square(47);
   private static final float SPAWNING_AMBIENT_SOUND_CHANCE = 0.02F;
   private final TrialSpawnerStateData data = new TrialSpawnerStateData();
   private TrialSpawner.FullConfig config;
   private final TrialSpawner.StateAccessor stateAccessor;
   private PlayerDetector playerDetector;
   private final PlayerDetector.EntitySelector entitySelector;
   private boolean overridePeacefulAndMobSpawnRule;
   private boolean isOminous;

   public TrialSpawner(TrialSpawner.FullConfig var1, TrialSpawner.StateAccessor var2, PlayerDetector var3, PlayerDetector.EntitySelector var4) {
      super();
      this.config = var1;
      this.stateAccessor = var2;
      this.playerDetector = var3;
      this.entitySelector = var4;
   }

   public TrialSpawnerConfig activeConfig() {
      return this.isOminous ? (TrialSpawnerConfig)this.config.ominous().value() : (TrialSpawnerConfig)this.config.normal.value();
   }

   public TrialSpawnerConfig normalConfig() {
      return (TrialSpawnerConfig)this.config.normal.value();
   }

   public TrialSpawnerConfig ominousConfig() {
      return (TrialSpawnerConfig)this.config.ominous.value();
   }

   public void load(ValueInput var1) {
      Optional var10000 = var1.read(TrialSpawnerStateData.Packed.MAP_CODEC);
      TrialSpawnerStateData var10001 = this.data;
      Objects.requireNonNull(var10001);
      var10000.ifPresent(var10001::apply);
      this.config = (TrialSpawner.FullConfig)var1.read(TrialSpawner.FullConfig.MAP_CODEC).orElse(TrialSpawner.FullConfig.DEFAULT);
   }

   public void store(ValueOutput var1) {
      var1.store(TrialSpawnerStateData.Packed.MAP_CODEC, this.data.pack());
      var1.store(TrialSpawner.FullConfig.MAP_CODEC, this.config);
   }

   public void applyOminous(ServerLevel var1, BlockPos var2) {
      var1.setBlock(var2, (BlockState)var1.getBlockState(var2).setValue(TrialSpawnerBlock.OMINOUS, true), 3);
      var1.levelEvent(3020, var2, 1);
      this.isOminous = true;
      this.data.resetAfterBecomingOminous(this, var1);
   }

   public void removeOminous(ServerLevel var1, BlockPos var2) {
      var1.setBlock(var2, (BlockState)var1.getBlockState(var2).setValue(TrialSpawnerBlock.OMINOUS, false), 3);
      this.isOminous = false;
   }

   public boolean isOminous() {
      return this.isOminous;
   }

   public int getTargetCooldownLength() {
      return this.config.targetCooldownLength;
   }

   public int getRequiredPlayerRange() {
      return this.config.requiredPlayerRange;
   }

   public TrialSpawnerState getState() {
      return this.stateAccessor.getState();
   }

   public TrialSpawnerStateData getStateData() {
      return this.data;
   }

   public void setState(Level var1, TrialSpawnerState var2) {
      this.stateAccessor.setState(var1, var2);
   }

   public void markUpdated() {
      this.stateAccessor.markUpdated();
   }

   public PlayerDetector getPlayerDetector() {
      return this.playerDetector;
   }

   public PlayerDetector.EntitySelector getEntitySelector() {
      return this.entitySelector;
   }

   public boolean canSpawnInLevel(ServerLevel var1) {
      if (!var1.getServer().getGameRules().getBoolean(GameRules.RULE_SPAWNER_BLOCKS_ENABLED)) {
         return false;
      } else if (this.overridePeacefulAndMobSpawnRule) {
         return true;
      } else {
         return var1.getDifficulty() == Difficulty.PEACEFUL ? false : var1.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
      }
   }

   public Optional<UUID> spawnMob(ServerLevel var1, BlockPos var2) {
      RandomSource var3 = var1.getRandom();
      SpawnData var4 = this.data.getOrCreateNextSpawnData(this, var1.getRandom());
      ProblemReporter.ScopedCollector var5 = new ProblemReporter.ScopedCollector(() -> {
         return "spawner@" + String.valueOf(var2);
      }, LOGGER);

      Optional var15;
      label110: {
         Optional var20;
         label109: {
            Optional var16;
            label108: {
               Optional var21;
               label107: {
                  label106: {
                     Optional var18;
                     label105: {
                        label104: {
                           label103: {
                              try {
                                 label124: {
                                    ValueInput var6 = TagValueInput.create(var5, var1.registryAccess(), (CompoundTag)var4.entityToSpawn());
                                    Optional var7 = EntityType.by(var6);
                                    if (var7.isEmpty()) {
                                       var15 = Optional.empty();
                                       break label110;
                                    }

                                    Vec3 var8 = (Vec3)var6.read("Pos", Vec3.CODEC).orElseGet(() -> {
                                       TrialSpawnerConfig var3x = this.activeConfig();
                                       return new Vec3((double)var2.getX() + (var3.nextDouble() - var3.nextDouble()) * (double)var3x.spawnRange() + 0.5D, (double)(var2.getY() + var3.nextInt(3) - 1), (double)var2.getZ() + (var3.nextDouble() - var3.nextDouble()) * (double)var3x.spawnRange() + 0.5D);
                                    });
                                    if (!var1.noCollision(((EntityType)var7.get()).getSpawnAABB(var8.x, var8.y, var8.z))) {
                                       var16 = Optional.empty();
                                       break label108;
                                    }

                                    if (!inLineOfSight(var1, var2.getCenter(), var8)) {
                                       var16 = Optional.empty();
                                       break label106;
                                    }

                                    BlockPos var9 = BlockPos.containing(var8);
                                    if (!SpawnPlacements.checkSpawnRules((EntityType)var7.get(), var1, EntitySpawnReason.TRIAL_SPAWNER, var9, var1.getRandom())) {
                                       var18 = Optional.empty();
                                       break label105;
                                    }

                                    if (var4.getCustomSpawnRules().isPresent()) {
                                       SpawnData.CustomSpawnRules var10 = (SpawnData.CustomSpawnRules)var4.getCustomSpawnRules().get();
                                       if (!var10.isValidPosition(var9, var1)) {
                                          var20 = Optional.empty();
                                          break label104;
                                       }
                                    }

                                    Entity var17 = EntityType.loadEntityRecursive((ValueInput)var6, var1, EntitySpawnReason.TRIAL_SPAWNER, (var2x) -> {
                                       var2x.snapTo(var8.x, var8.y, var8.z, var3.nextFloat() * 360.0F, 0.0F);
                                       return var2x;
                                    });
                                    if (var17 == null) {
                                       var20 = Optional.empty();
                                       break label103;
                                    }

                                    if (var17 instanceof Mob) {
                                       Mob var11 = (Mob)var17;
                                       if (!var11.checkSpawnObstruction(var1)) {
                                          var21 = Optional.empty();
                                          break label124;
                                       }

                                       boolean var12 = var4.getEntityToSpawn().size() == 1 && var4.getEntityToSpawn().getString("id").isPresent();
                                       if (var12) {
                                          var11.finalizeSpawn(var1, var1.getCurrentDifficultyAt(var11.blockPosition()), EntitySpawnReason.TRIAL_SPAWNER, (SpawnGroupData)null);
                                       }

                                       var11.setPersistenceRequired();
                                       Optional var10000 = var4.getEquipment();
                                       Objects.requireNonNull(var11);
                                       var10000.ifPresent(var11::equip);
                                    }

                                    if (!var1.tryAddFreshEntityWithPassengers(var17)) {
                                       var20 = Optional.empty();
                                       break label109;
                                    }

                                    TrialSpawner.FlameParticle var19 = this.isOminous ? TrialSpawner.FlameParticle.OMINOUS : TrialSpawner.FlameParticle.NORMAL;
                                    var1.levelEvent(3011, var2, var19.encode());
                                    var1.levelEvent(3012, var9, var19.encode());
                                    var1.gameEvent(var17, GameEvent.ENTITY_PLACE, var9);
                                    var21 = Optional.of(var17.getUUID());
                                    break label107;
                                 }
                              } catch (Throwable var14) {
                                 try {
                                    var5.close();
                                 } catch (Throwable var13) {
                                    var14.addSuppressed(var13);
                                 }

                                 throw var14;
                              }

                              var5.close();
                              return var21;
                           }

                           var5.close();
                           return var20;
                        }

                        var5.close();
                        return var20;
                     }

                     var5.close();
                     return var18;
                  }

                  var5.close();
                  return var16;
               }

               var5.close();
               return var21;
            }

            var5.close();
            return var16;
         }

         var5.close();
         return var20;
      }

      var5.close();
      return var15;
   }

   public void ejectReward(ServerLevel var1, BlockPos var2, ResourceKey<LootTable> var3) {
      LootTable var4 = var1.getServer().reloadableRegistries().getLootTable(var3);
      LootParams var5 = (new LootParams.Builder(var1)).create(LootContextParamSets.EMPTY);
      ObjectArrayList var6 = var4.getRandomItems(var5);
      if (!var6.isEmpty()) {
         ObjectListIterator var7 = var6.iterator();

         while(var7.hasNext()) {
            ItemStack var8 = (ItemStack)var7.next();
            DefaultDispenseItemBehavior.spawnItem(var1, var8, 2, Direction.UP, Vec3.atBottomCenterOf(var2).relative(Direction.UP, 1.2D));
         }

         var1.levelEvent(3014, var2, 0);
      }

   }

   public void tickClient(Level var1, BlockPos var2, boolean var3) {
      TrialSpawnerState var4 = this.getState();
      var4.emitParticles(var1, var2, var3);
      if (var4.hasSpinningMob()) {
         double var5 = (double)Math.max(0L, this.data.nextMobSpawnsAt - var1.getGameTime());
         this.data.oSpin = this.data.spin;
         this.data.spin = (this.data.spin + var4.spinningMobSpeed() / (var5 + 200.0D)) % 360.0D;
      }

      if (var4.isCapableOfSpawning()) {
         RandomSource var7 = var1.getRandom();
         if (var7.nextFloat() <= 0.02F) {
            SoundEvent var6 = var3 ? SoundEvents.TRIAL_SPAWNER_AMBIENT_OMINOUS : SoundEvents.TRIAL_SPAWNER_AMBIENT;
            var1.playLocalSound(var2, var6, SoundSource.BLOCKS, var7.nextFloat() * 0.25F + 0.75F, var7.nextFloat() + 0.5F, false);
         }
      }

   }

   public void tickServer(ServerLevel var1, BlockPos var2, boolean var3) {
      this.isOminous = var3;
      TrialSpawnerState var4 = this.getState();
      if (this.data.currentMobs.removeIf((var2x) -> {
         return shouldMobBeUntracked(var1, var2, var2x);
      })) {
         this.data.nextMobSpawnsAt = var1.getGameTime() + (long)this.activeConfig().ticksBetweenSpawn();
      }

      TrialSpawnerState var5 = var4.tickAndGetNext(var2, this, var1);
      if (var5 != var4) {
         this.setState(var1, var5);
      }

   }

   private static boolean shouldMobBeUntracked(ServerLevel var0, BlockPos var1, UUID var2) {
      Entity var3 = var0.getEntity(var2);
      return var3 == null || !var3.isAlive() || !var3.level().dimension().equals(var0.dimension()) || var3.blockPosition().distSqr(var1) > (double)MAX_MOB_TRACKING_DISTANCE_SQR;
   }

   private static boolean inLineOfSight(Level var0, Vec3 var1, Vec3 var2) {
      BlockHitResult var3 = var0.clip(new ClipContext(var2, var1, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
      return var3.getBlockPos().equals(BlockPos.containing(var1)) || var3.getType() == HitResult.Type.MISS;
   }

   public static void addSpawnParticles(Level var0, BlockPos var1, RandomSource var2, SimpleParticleType var3) {
      for(int var4 = 0; var4 < 20; ++var4) {
         double var5 = (double)var1.getX() + 0.5D + (var2.nextDouble() - 0.5D) * 2.0D;
         double var7 = (double)var1.getY() + 0.5D + (var2.nextDouble() - 0.5D) * 2.0D;
         double var9 = (double)var1.getZ() + 0.5D + (var2.nextDouble() - 0.5D) * 2.0D;
         var0.addParticle(ParticleTypes.SMOKE, var5, var7, var9, 0.0D, 0.0D, 0.0D);
         var0.addParticle(var3, var5, var7, var9, 0.0D, 0.0D, 0.0D);
      }

   }

   public static void addBecomeOminousParticles(Level var0, BlockPos var1, RandomSource var2) {
      for(int var3 = 0; var3 < 20; ++var3) {
         double var4 = (double)var1.getX() + 0.5D + (var2.nextDouble() - 0.5D) * 2.0D;
         double var6 = (double)var1.getY() + 0.5D + (var2.nextDouble() - 0.5D) * 2.0D;
         double var8 = (double)var1.getZ() + 0.5D + (var2.nextDouble() - 0.5D) * 2.0D;
         double var10 = var2.nextGaussian() * 0.02D;
         double var12 = var2.nextGaussian() * 0.02D;
         double var14 = var2.nextGaussian() * 0.02D;
         var0.addParticle(ParticleTypes.TRIAL_OMEN, var4, var6, var8, var10, var12, var14);
         var0.addParticle(ParticleTypes.SOUL_FIRE_FLAME, var4, var6, var8, var10, var12, var14);
      }

   }

   public static void addDetectPlayerParticles(Level var0, BlockPos var1, RandomSource var2, int var3, ParticleOptions var4) {
      for(int var5 = 0; var5 < 30 + Math.min(var3, 10) * 5; ++var5) {
         double var6 = (double)(2.0F * var2.nextFloat() - 1.0F) * 0.65D;
         double var8 = (double)(2.0F * var2.nextFloat() - 1.0F) * 0.65D;
         double var10 = (double)var1.getX() + 0.5D + var6;
         double var12 = (double)var1.getY() + 0.1D + (double)var2.nextFloat() * 0.8D;
         double var14 = (double)var1.getZ() + 0.5D + var8;
         var0.addParticle(var4, var10, var12, var14, 0.0D, 0.0D, 0.0D);
      }

   }

   public static void addEjectItemParticles(Level var0, BlockPos var1, RandomSource var2) {
      for(int var3 = 0; var3 < 20; ++var3) {
         double var4 = (double)var1.getX() + 0.4D + var2.nextDouble() * 0.2D;
         double var6 = (double)var1.getY() + 0.4D + var2.nextDouble() * 0.2D;
         double var8 = (double)var1.getZ() + 0.4D + var2.nextDouble() * 0.2D;
         double var10 = var2.nextGaussian() * 0.02D;
         double var12 = var2.nextGaussian() * 0.02D;
         double var14 = var2.nextGaussian() * 0.02D;
         var0.addParticle(ParticleTypes.SMALL_FLAME, var4, var6, var8, var10, var12, var14 * 0.25D);
         var0.addParticle(ParticleTypes.SMOKE, var4, var6, var8, var10, var12, var14);
      }

   }

   public void overrideEntityToSpawn(EntityType<?> var1, Level var2) {
      this.data.reset();
      this.config = this.config.overrideEntity(var1);
      this.setState(var2, TrialSpawnerState.INACTIVE);
   }

   /** @deprecated */
   @Deprecated(
      forRemoval = true
   )
   @VisibleForTesting
   public void setPlayerDetector(PlayerDetector var1) {
      this.playerDetector = var1;
   }

   /** @deprecated */
   @Deprecated(
      forRemoval = true
   )
   @VisibleForTesting
   public void overridePeacefulAndMobSpawnRule() {
      this.overridePeacefulAndMobSpawnRule = true;
   }

   public static record FullConfig(Holder<TrialSpawnerConfig> normal, Holder<TrialSpawnerConfig> ominous, int targetCooldownLength, int requiredPlayerRange) {
      final Holder<TrialSpawnerConfig> normal;
      final Holder<TrialSpawnerConfig> ominous;
      final int targetCooldownLength;
      final int requiredPlayerRange;
      public static final MapCodec<TrialSpawner.FullConfig> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(TrialSpawnerConfig.CODEC.optionalFieldOf("normal_config", Holder.direct(TrialSpawnerConfig.DEFAULT)).forGetter(TrialSpawner.FullConfig::normal), TrialSpawnerConfig.CODEC.optionalFieldOf("ominous_config", Holder.direct(TrialSpawnerConfig.DEFAULT)).forGetter(TrialSpawner.FullConfig::ominous), ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("target_cooldown_length", 36000).forGetter(TrialSpawner.FullConfig::targetCooldownLength), Codec.intRange(1, 128).optionalFieldOf("required_player_range", 14).forGetter(TrialSpawner.FullConfig::requiredPlayerRange)).apply(var0, TrialSpawner.FullConfig::new);
      });
      public static final TrialSpawner.FullConfig DEFAULT;

      public FullConfig(Holder<TrialSpawnerConfig> param1, Holder<TrialSpawnerConfig> param2, int param3, int param4) {
         super();
         this.normal = var1;
         this.ominous = var2;
         this.targetCooldownLength = var3;
         this.requiredPlayerRange = var4;
      }

      public TrialSpawner.FullConfig overrideEntity(EntityType<?> var1) {
         return new TrialSpawner.FullConfig(Holder.direct(((TrialSpawnerConfig)this.normal.value()).withSpawning(var1)), Holder.direct(((TrialSpawnerConfig)this.ominous.value()).withSpawning(var1)), this.targetCooldownLength, this.requiredPlayerRange);
      }

      public Holder<TrialSpawnerConfig> normal() {
         return this.normal;
      }

      public Holder<TrialSpawnerConfig> ominous() {
         return this.ominous;
      }

      public int targetCooldownLength() {
         return this.targetCooldownLength;
      }

      public int requiredPlayerRange() {
         return this.requiredPlayerRange;
      }

      static {
         DEFAULT = new TrialSpawner.FullConfig(Holder.direct(TrialSpawnerConfig.DEFAULT), Holder.direct(TrialSpawnerConfig.DEFAULT), 36000, 14);
      }
   }

   public interface StateAccessor {
      void setState(Level var1, TrialSpawnerState var2);

      TrialSpawnerState getState();

      void markUpdated();
   }

   public static enum FlameParticle {
      NORMAL(ParticleTypes.FLAME),
      OMINOUS(ParticleTypes.SOUL_FIRE_FLAME);

      public final SimpleParticleType particleType;

      private FlameParticle(final SimpleParticleType param3) {
         this.particleType = var3;
      }

      public static TrialSpawner.FlameParticle decode(int var0) {
         TrialSpawner.FlameParticle[] var1 = values();
         return var0 <= var1.length && var0 >= 0 ? var1[var0] : NORMAL;
      }

      public int encode() {
         return this.ordinal();
      }

      // $FF: synthetic method
      private static TrialSpawner.FlameParticle[] $values() {
         return new TrialSpawner.FlameParticle[]{NORMAL, OMINOUS};
      }
   }
}
