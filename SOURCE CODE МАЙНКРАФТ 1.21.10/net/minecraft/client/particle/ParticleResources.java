package net.minecraft.client.particle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class ParticleResources implements PreparableReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particles");
   private final Map<ResourceLocation, ParticleResources.MutableSpriteSet> spriteSets = Maps.newHashMap();
   private final Int2ObjectMap<ParticleProvider<?>> providers = new Int2ObjectOpenHashMap();
   @Nullable
   private Runnable onReload;

   public ParticleResources() {
      super();
      this.registerProviders();
   }

   public void onReload(Runnable var1) {
      this.onReload = var1;
   }

   private void registerProviders() {
      this.register(ParticleTypes.ANGRY_VILLAGER, (ParticleResources.SpriteParticleRegistration)(HeartParticle.AngryVillagerProvider::new));
      this.register(ParticleTypes.BLOCK_MARKER, (ParticleProvider)(new BlockMarker.Provider()));
      this.register(ParticleTypes.BLOCK, (ParticleProvider)(new TerrainParticle.Provider()));
      this.register(ParticleTypes.BUBBLE, (ParticleResources.SpriteParticleRegistration)(BubbleParticle.Provider::new));
      this.register(ParticleTypes.BUBBLE_COLUMN_UP, (ParticleResources.SpriteParticleRegistration)(BubbleColumnUpParticle.Provider::new));
      this.register(ParticleTypes.BUBBLE_POP, (ParticleResources.SpriteParticleRegistration)(BubblePopParticle.Provider::new));
      this.register(ParticleTypes.CAMPFIRE_COSY_SMOKE, (ParticleResources.SpriteParticleRegistration)(CampfireSmokeParticle.CosyProvider::new));
      this.register(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, (ParticleResources.SpriteParticleRegistration)(CampfireSmokeParticle.SignalProvider::new));
      this.register(ParticleTypes.CLOUD, (ParticleResources.SpriteParticleRegistration)(PlayerCloudParticle.Provider::new));
      this.register(ParticleTypes.COMPOSTER, (ParticleResources.SpriteParticleRegistration)(SuspendedTownParticle.ComposterFillProvider::new));
      this.register(ParticleTypes.COPPER_FIRE_FLAME, (ParticleResources.SpriteParticleRegistration)(FlameParticle.Provider::new));
      this.register(ParticleTypes.CRIT, (ParticleResources.SpriteParticleRegistration)(CritParticle.Provider::new));
      this.register(ParticleTypes.CURRENT_DOWN, (ParticleResources.SpriteParticleRegistration)(WaterCurrentDownParticle.Provider::new));
      this.register(ParticleTypes.DAMAGE_INDICATOR, (ParticleResources.SpriteParticleRegistration)(CritParticle.DamageIndicatorProvider::new));
      this.register(ParticleTypes.DRAGON_BREATH, DragonBreathParticle.Provider::new);
      this.register(ParticleTypes.DOLPHIN, (ParticleResources.SpriteParticleRegistration)(SuspendedTownParticle.DolphinSpeedProvider::new));
      this.register(ParticleTypes.DRIPPING_LAVA, (ParticleResources.SpriteParticleRegistration)(DripParticle.LavaHangProvider::new));
      this.register(ParticleTypes.FALLING_LAVA, (ParticleResources.SpriteParticleRegistration)(DripParticle.LavaFallProvider::new));
      this.register(ParticleTypes.LANDING_LAVA, (ParticleResources.SpriteParticleRegistration)(DripParticle.LavaLandProvider::new));
      this.register(ParticleTypes.DRIPPING_WATER, (ParticleResources.SpriteParticleRegistration)(DripParticle.WaterHangProvider::new));
      this.register(ParticleTypes.FALLING_WATER, (ParticleResources.SpriteParticleRegistration)(DripParticle.WaterFallProvider::new));
      this.register(ParticleTypes.DUST, DustParticle.Provider::new);
      this.register(ParticleTypes.DUST_COLOR_TRANSITION, DustColorTransitionParticle.Provider::new);
      this.register(ParticleTypes.EFFECT, SpellParticle.InstantProvider::new);
      this.register(ParticleTypes.ELDER_GUARDIAN, (ParticleProvider)(new ElderGuardianParticle.Provider()));
      this.register(ParticleTypes.ENCHANTED_HIT, (ParticleResources.SpriteParticleRegistration)(CritParticle.MagicProvider::new));
      this.register(ParticleTypes.ENCHANT, (ParticleResources.SpriteParticleRegistration)(FlyTowardsPositionParticle.EnchantProvider::new));
      this.register(ParticleTypes.END_ROD, (ParticleResources.SpriteParticleRegistration)(EndRodParticle.Provider::new));
      this.register(ParticleTypes.ENTITY_EFFECT, SpellParticle.MobEffectProvider::new);
      this.register(ParticleTypes.EXPLOSION_EMITTER, (ParticleProvider)(new HugeExplosionSeedParticle.Provider()));
      this.register(ParticleTypes.EXPLOSION, (ParticleResources.SpriteParticleRegistration)(HugeExplosionParticle.Provider::new));
      this.register(ParticleTypes.SONIC_BOOM, (ParticleResources.SpriteParticleRegistration)(SonicBoomParticle.Provider::new));
      this.register(ParticleTypes.FALLING_DUST, FallingDustParticle.Provider::new);
      this.register(ParticleTypes.GUST, (ParticleResources.SpriteParticleRegistration)(GustParticle.Provider::new));
      this.register(ParticleTypes.SMALL_GUST, (ParticleResources.SpriteParticleRegistration)(GustParticle.SmallProvider::new));
      this.register(ParticleTypes.GUST_EMITTER_LARGE, (ParticleProvider)(new GustSeedParticle.Provider(3.0D, 7, 0)));
      this.register(ParticleTypes.GUST_EMITTER_SMALL, (ParticleProvider)(new GustSeedParticle.Provider(1.0D, 3, 2)));
      this.register(ParticleTypes.FIREWORK, (ParticleResources.SpriteParticleRegistration)(FireworkParticles.SparkProvider::new));
      this.register(ParticleTypes.FISHING, (ParticleResources.SpriteParticleRegistration)(WakeParticle.Provider::new));
      this.register(ParticleTypes.FLAME, (ParticleResources.SpriteParticleRegistration)(FlameParticle.Provider::new));
      this.register(ParticleTypes.INFESTED, (ParticleResources.SpriteParticleRegistration)(SpellParticle.Provider::new));
      this.register(ParticleTypes.SCULK_SOUL, (ParticleResources.SpriteParticleRegistration)(SoulParticle.EmissiveProvider::new));
      this.register(ParticleTypes.SCULK_CHARGE, SculkChargeParticle.Provider::new);
      this.register(ParticleTypes.SCULK_CHARGE_POP, (ParticleResources.SpriteParticleRegistration)(SculkChargePopParticle.Provider::new));
      this.register(ParticleTypes.SOUL, (ParticleResources.SpriteParticleRegistration)(SoulParticle.Provider::new));
      this.register(ParticleTypes.SOUL_FIRE_FLAME, (ParticleResources.SpriteParticleRegistration)(FlameParticle.Provider::new));
      this.register(ParticleTypes.FLASH, FireworkParticles.FlashProvider::new);
      this.register(ParticleTypes.HAPPY_VILLAGER, (ParticleResources.SpriteParticleRegistration)(SuspendedTownParticle.HappyVillagerProvider::new));
      this.register(ParticleTypes.HEART, (ParticleResources.SpriteParticleRegistration)(HeartParticle.Provider::new));
      this.register(ParticleTypes.INSTANT_EFFECT, SpellParticle.InstantProvider::new);
      this.register(ParticleTypes.ITEM, (ParticleProvider)(new BreakingItemParticle.Provider()));
      this.register(ParticleTypes.ITEM_SLIME, (ParticleProvider)(new BreakingItemParticle.SlimeProvider()));
      this.register(ParticleTypes.ITEM_COBWEB, (ParticleProvider)(new BreakingItemParticle.CobwebProvider()));
      this.register(ParticleTypes.ITEM_SNOWBALL, (ParticleProvider)(new BreakingItemParticle.SnowballProvider()));
      this.register(ParticleTypes.LARGE_SMOKE, (ParticleResources.SpriteParticleRegistration)(LargeSmokeParticle.Provider::new));
      this.register(ParticleTypes.LAVA, (ParticleResources.SpriteParticleRegistration)(LavaParticle.Provider::new));
      this.register(ParticleTypes.MYCELIUM, (ParticleResources.SpriteParticleRegistration)(SuspendedTownParticle.Provider::new));
      this.register(ParticleTypes.NAUTILUS, (ParticleResources.SpriteParticleRegistration)(FlyTowardsPositionParticle.NautilusProvider::new));
      this.register(ParticleTypes.NOTE, (ParticleResources.SpriteParticleRegistration)(NoteParticle.Provider::new));
      this.register(ParticleTypes.POOF, (ParticleResources.SpriteParticleRegistration)(ExplodeParticle.Provider::new));
      this.register(ParticleTypes.PORTAL, (ParticleResources.SpriteParticleRegistration)(PortalParticle.Provider::new));
      this.register(ParticleTypes.RAIN, (ParticleResources.SpriteParticleRegistration)(WaterDropParticle.Provider::new));
      this.register(ParticleTypes.SMOKE, (ParticleResources.SpriteParticleRegistration)(SmokeParticle.Provider::new));
      this.register(ParticleTypes.WHITE_SMOKE, (ParticleResources.SpriteParticleRegistration)(WhiteSmokeParticle.Provider::new));
      this.register(ParticleTypes.SNEEZE, (ParticleResources.SpriteParticleRegistration)(PlayerCloudParticle.SneezeProvider::new));
      this.register(ParticleTypes.SNOWFLAKE, (ParticleResources.SpriteParticleRegistration)(SnowflakeParticle.Provider::new));
      this.register(ParticleTypes.SPIT, (ParticleResources.SpriteParticleRegistration)(SpitParticle.Provider::new));
      this.register(ParticleTypes.SWEEP_ATTACK, (ParticleResources.SpriteParticleRegistration)(AttackSweepParticle.Provider::new));
      this.register(ParticleTypes.TOTEM_OF_UNDYING, (ParticleResources.SpriteParticleRegistration)(TotemParticle.Provider::new));
      this.register(ParticleTypes.SQUID_INK, (ParticleResources.SpriteParticleRegistration)(SquidInkParticle.Provider::new));
      this.register(ParticleTypes.UNDERWATER, (ParticleResources.SpriteParticleRegistration)(SuspendedParticle.UnderwaterProvider::new));
      this.register(ParticleTypes.SPLASH, (ParticleResources.SpriteParticleRegistration)(SplashParticle.Provider::new));
      this.register(ParticleTypes.WITCH, (ParticleResources.SpriteParticleRegistration)(SpellParticle.WitchProvider::new));
      this.register(ParticleTypes.DRIPPING_HONEY, (ParticleResources.SpriteParticleRegistration)(DripParticle.HoneyHangProvider::new));
      this.register(ParticleTypes.FALLING_HONEY, (ParticleResources.SpriteParticleRegistration)(DripParticle.HoneyFallProvider::new));
      this.register(ParticleTypes.LANDING_HONEY, (ParticleResources.SpriteParticleRegistration)(DripParticle.HoneyLandProvider::new));
      this.register(ParticleTypes.FALLING_NECTAR, (ParticleResources.SpriteParticleRegistration)(DripParticle.NectarFallProvider::new));
      this.register(ParticleTypes.FALLING_SPORE_BLOSSOM, (ParticleResources.SpriteParticleRegistration)(DripParticle.SporeBlossomFallProvider::new));
      this.register(ParticleTypes.SPORE_BLOSSOM_AIR, (ParticleResources.SpriteParticleRegistration)(SuspendedParticle.SporeBlossomAirProvider::new));
      this.register(ParticleTypes.ASH, (ParticleResources.SpriteParticleRegistration)(AshParticle.Provider::new));
      this.register(ParticleTypes.CRIMSON_SPORE, (ParticleResources.SpriteParticleRegistration)(SuspendedParticle.CrimsonSporeProvider::new));
      this.register(ParticleTypes.WARPED_SPORE, (ParticleResources.SpriteParticleRegistration)(SuspendedParticle.WarpedSporeProvider::new));
      this.register(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, (ParticleResources.SpriteParticleRegistration)(DripParticle.ObsidianTearHangProvider::new));
      this.register(ParticleTypes.FALLING_OBSIDIAN_TEAR, (ParticleResources.SpriteParticleRegistration)(DripParticle.ObsidianTearFallProvider::new));
      this.register(ParticleTypes.LANDING_OBSIDIAN_TEAR, (ParticleResources.SpriteParticleRegistration)(DripParticle.ObsidianTearLandProvider::new));
      this.register(ParticleTypes.REVERSE_PORTAL, (ParticleResources.SpriteParticleRegistration)(ReversePortalParticle.ReversePortalProvider::new));
      this.register(ParticleTypes.WHITE_ASH, (ParticleResources.SpriteParticleRegistration)(WhiteAshParticle.Provider::new));
      this.register(ParticleTypes.SMALL_FLAME, (ParticleResources.SpriteParticleRegistration)(FlameParticle.SmallFlameProvider::new));
      this.register(ParticleTypes.DRIPPING_DRIPSTONE_WATER, (ParticleResources.SpriteParticleRegistration)(DripParticle.DripstoneWaterHangProvider::new));
      this.register(ParticleTypes.FALLING_DRIPSTONE_WATER, (ParticleResources.SpriteParticleRegistration)(DripParticle.DripstoneWaterFallProvider::new));
      this.register(ParticleTypes.CHERRY_LEAVES, (ParticleResources.SpriteParticleRegistration)(FallingLeavesParticle.CherryProvider::new));
      this.register(ParticleTypes.PALE_OAK_LEAVES, (ParticleResources.SpriteParticleRegistration)(FallingLeavesParticle.PaleOakProvider::new));
      this.register(ParticleTypes.TINTED_LEAVES, FallingLeavesParticle.TintedLeavesProvider::new);
      this.register(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, (ParticleResources.SpriteParticleRegistration)(DripParticle.DripstoneLavaHangProvider::new));
      this.register(ParticleTypes.FALLING_DRIPSTONE_LAVA, (ParticleResources.SpriteParticleRegistration)(DripParticle.DripstoneLavaFallProvider::new));
      this.register(ParticleTypes.VIBRATION, VibrationSignalParticle.Provider::new);
      this.register(ParticleTypes.TRAIL, TrailParticle.Provider::new);
      this.register(ParticleTypes.GLOW_SQUID_INK, (ParticleResources.SpriteParticleRegistration)(SquidInkParticle.GlowInkProvider::new));
      this.register(ParticleTypes.GLOW, (ParticleResources.SpriteParticleRegistration)(GlowParticle.GlowSquidProvider::new));
      this.register(ParticleTypes.WAX_ON, (ParticleResources.SpriteParticleRegistration)(GlowParticle.WaxOnProvider::new));
      this.register(ParticleTypes.WAX_OFF, (ParticleResources.SpriteParticleRegistration)(GlowParticle.WaxOffProvider::new));
      this.register(ParticleTypes.ELECTRIC_SPARK, (ParticleResources.SpriteParticleRegistration)(GlowParticle.ElectricSparkProvider::new));
      this.register(ParticleTypes.SCRAPE, (ParticleResources.SpriteParticleRegistration)(GlowParticle.ScrapeProvider::new));
      this.register(ParticleTypes.SHRIEK, ShriekParticle.Provider::new);
      this.register(ParticleTypes.EGG_CRACK, (ParticleResources.SpriteParticleRegistration)(SuspendedTownParticle.EggCrackProvider::new));
      this.register(ParticleTypes.DUST_PLUME, (ParticleResources.SpriteParticleRegistration)(DustPlumeParticle.Provider::new));
      this.register(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER, (ParticleResources.SpriteParticleRegistration)(TrialSpawnerDetectionParticle.Provider::new));
      this.register(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS, (ParticleResources.SpriteParticleRegistration)(TrialSpawnerDetectionParticle.Provider::new));
      this.register(ParticleTypes.VAULT_CONNECTION, (ParticleResources.SpriteParticleRegistration)(FlyTowardsPositionParticle.VaultConnectionProvider::new));
      this.register(ParticleTypes.DUST_PILLAR, (ParticleProvider)(new TerrainParticle.DustPillarProvider()));
      this.register(ParticleTypes.RAID_OMEN, (ParticleResources.SpriteParticleRegistration)(SpellParticle.Provider::new));
      this.register(ParticleTypes.TRIAL_OMEN, (ParticleResources.SpriteParticleRegistration)(SpellParticle.Provider::new));
      this.register(ParticleTypes.OMINOUS_SPAWNING, (ParticleResources.SpriteParticleRegistration)(FlyStraightTowardsParticle.OminousSpawnProvider::new));
      this.register(ParticleTypes.BLOCK_CRUMBLE, (ParticleProvider)(new TerrainParticle.CrumblingProvider()));
      this.register(ParticleTypes.FIREFLY, (ParticleResources.SpriteParticleRegistration)(FireflyParticle.FireflyProvider::new));
   }

   private <T extends ParticleOptions> void register(ParticleType<T> var1, ParticleProvider<T> var2) {
      this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(var1), var2);
   }

   private <T extends ParticleOptions> void register(ParticleType<T> var1, ParticleResources.SpriteParticleRegistration<T> var2) {
      ParticleResources.MutableSpriteSet var3 = new ParticleResources.MutableSpriteSet();
      this.spriteSets.put(BuiltInRegistries.PARTICLE_TYPE.getKey(var1), var3);
      this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(var1), var2.create(var3));
   }

   public CompletableFuture<Void> reload(PreparableReloadListener.SharedState var1, Executor var2, PreparableReloadListener.PreparationBarrier var3, Executor var4) {
      ResourceManager var5 = var1.resourceManager();
      CompletableFuture var6 = CompletableFuture.supplyAsync(() -> {
         return PARTICLE_LISTER.listMatchingResources(var5);
      }, var2).thenCompose((var2x) -> {
         ArrayList var3 = new ArrayList(var2x.size());
         var2x.forEach((var3x, var4) -> {
            ResourceLocation var5 = PARTICLE_LISTER.fileToId(var3x);
            var3.add(CompletableFuture.supplyAsync(() -> {
               record 1ParticleDefinition(ResourceLocation id, Optional<List<ResourceLocation>> sprites) {
                  _ParticleDefinition/* $FF was: 1ParticleDefinition*/(ResourceLocation param1, Optional<List<ResourceLocation>> param2) {
                     super();
                     this.id = var1;
                     this.sprites = var2;
                  }

                  public ResourceLocation id() {
                     return this.id;
                  }

                  public Optional<List<ResourceLocation>> sprites() {
                     return this.sprites;
                  }
               }

               return new 1ParticleDefinition(var5, this.loadParticleDescription(var5, var4));
            }, var2));
         });
         return Util.sequence(var3);
      });
      CompletableFuture var7 = ((AtlasManager.PendingStitchResults)var1.get(AtlasManager.PENDING_STITCH)).get(AtlasIds.PARTICLES);
      CompletableFuture var10000 = CompletableFuture.allOf(var6, var7);
      Objects.requireNonNull(var3);
      return var10000.thenCompose(var3::wait).thenAcceptAsync((var3x) -> {
         if (this.onReload != null) {
            this.onReload.run();
         }

         ProfilerFiller var4 = Profiler.get();
         var4.push("upload");
         SpriteLoader.Preparations var5 = (SpriteLoader.Preparations)var7.join();
         var4.popPush("bindSpriteSets");
         HashSet var6x = new HashSet();
         TextureAtlasSprite var7x = var5.missing();
         ((List)var6.join()).forEach((var4x) -> {
            Optional var5x = var4x.sprites();
            if (!var5x.isEmpty()) {
               ArrayList var6 = new ArrayList();
               Iterator var7 = ((List)var5x.get()).iterator();

               while(var7.hasNext()) {
                  ResourceLocation var8 = (ResourceLocation)var7.next();
                  TextureAtlasSprite var9 = var5.getSprite(var8);
                  if (var9 == null) {
                     var6x.add(var8);
                     var6.add(var7x);
                  } else {
                     var6.add(var9);
                  }
               }

               if (var6.isEmpty()) {
                  var6.add(var7x);
               }

               ((ParticleResources.MutableSpriteSet)this.spriteSets.get(var4x.id())).rebind(var6);
            }
         });
         if (!var6x.isEmpty()) {
            LOGGER.warn("Missing particle sprites: {}", var6x.stream().sorted().map(ResourceLocation::toString).collect(Collectors.joining(",")));
         }

         var4.pop();
      }, var4);
   }

   private Optional<List<ResourceLocation>> loadParticleDescription(ResourceLocation var1, Resource var2) {
      if (!this.spriteSets.containsKey(var1)) {
         LOGGER.debug("Redundant texture list for particle: {}", var1);
         return Optional.empty();
      } else {
         try {
            BufferedReader var3 = var2.openAsReader();

            Optional var5;
            try {
               ParticleDescription var4 = ParticleDescription.fromJson(GsonHelper.parse((Reader)var3));
               var5 = Optional.of(var4.getTextures());
            } catch (Throwable var7) {
               if (var3 != null) {
                  try {
                     var3.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (var3 != null) {
               var3.close();
            }

            return var5;
         } catch (IOException var8) {
            throw new IllegalStateException("Failed to load description for particle " + String.valueOf(var1), var8);
         }
      }
   }

   public Int2ObjectMap<ParticleProvider<?>> getProviders() {
      return this.providers;
   }

   @FunctionalInterface
   private interface SpriteParticleRegistration<T extends ParticleOptions> {
      ParticleProvider<T> create(SpriteSet var1);
   }

   static class MutableSpriteSet implements SpriteSet {
      private List<TextureAtlasSprite> sprites;

      MutableSpriteSet() {
         super();
      }

      public TextureAtlasSprite get(int var1, int var2) {
         return (TextureAtlasSprite)this.sprites.get(var1 * (this.sprites.size() - 1) / var2);
      }

      public TextureAtlasSprite get(RandomSource var1) {
         return (TextureAtlasSprite)this.sprites.get(var1.nextInt(this.sprites.size()));
      }

      public TextureAtlasSprite first() {
         return (TextureAtlasSprite)this.sprites.getFirst();
      }

      public void rebind(List<TextureAtlasSprite> var1) {
         this.sprites = ImmutableList.copyOf(var1);
      }
   }
}
