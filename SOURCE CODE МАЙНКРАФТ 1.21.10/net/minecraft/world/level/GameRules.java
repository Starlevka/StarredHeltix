package net.minecraft.world.level;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicLike;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.slf4j.Logger;

public class GameRules {
   public static final int DEFAULT_RANDOM_TICK_SPEED = 3;
   static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<GameRules.Key<?>, GameRules.Type<?>> GAME_RULE_TYPES = Maps.newTreeMap(Comparator.comparing((var0) -> {
      return var0.id;
   }));
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DOFIRETICK;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_ALLOWFIRETICKAWAYFROMPLAYERS;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_MOBGRIEFING;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_KEEPINVENTORY;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DOMOBSPAWNING;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DOMOBLOOT;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_PROJECTILESCANBREAKBLOCKS;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DOBLOCKDROPS;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DOENTITYDROPS;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_COMMANDBLOCKOUTPUT;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_NATURAL_REGENERATION;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DAYLIGHT;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_LOGADMINCOMMANDS;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_SHOWDEATHMESSAGES;
   public static final GameRules.Key<GameRules.IntegerValue> RULE_RANDOMTICKING;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_SENDCOMMANDFEEDBACK;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_REDUCEDDEBUGINFO;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_SPECTATORSGENERATECHUNKS;
   public static final GameRules.Key<GameRules.IntegerValue> RULE_SPAWN_RADIUS;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DISABLE_PLAYER_MOVEMENT_CHECK;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DISABLE_ELYTRA_MOVEMENT_CHECK;
   public static final GameRules.Key<GameRules.IntegerValue> RULE_MAX_ENTITY_CRAMMING;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_WEATHER_CYCLE;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_LIMITED_CRAFTING;
   public static final GameRules.Key<GameRules.IntegerValue> RULE_MAX_COMMAND_CHAIN_LENGTH;
   public static final GameRules.Key<GameRules.IntegerValue> RULE_MAX_COMMAND_FORK_COUNT;
   public static final GameRules.Key<GameRules.IntegerValue> RULE_COMMAND_MODIFICATION_BLOCK_LIMIT;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_ANNOUNCE_ADVANCEMENTS;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DISABLE_RAIDS;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DOINSOMNIA;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_IMMEDIATE_RESPAWN;
   public static final GameRules.Key<GameRules.IntegerValue> RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY;
   public static final GameRules.Key<GameRules.IntegerValue> RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DROWNING_DAMAGE;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_FALL_DAMAGE;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_FIRE_DAMAGE;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_FREEZE_DAMAGE;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_PATROL_SPAWNING;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_TRADER_SPAWNING;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_WARDEN_SPAWNING;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_FORGIVE_DEAD_PLAYERS;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_UNIVERSAL_ANGER;
   public static final GameRules.Key<GameRules.IntegerValue> RULE_PLAYERS_SLEEPING_PERCENTAGE;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_BLOCK_EXPLOSION_DROP_DECAY;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_MOB_EXPLOSION_DROP_DECAY;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_TNT_EXPLOSION_DROP_DECAY;
   public static final GameRules.Key<GameRules.IntegerValue> RULE_SNOW_ACCUMULATION_HEIGHT;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_WATER_SOURCE_CONVERSION;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_LAVA_SOURCE_CONVERSION;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_GLOBAL_SOUND_EVENTS;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_VINES_SPREAD;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_ENDER_PEARLS_VANISH_ON_DEATH;
   public static final GameRules.Key<GameRules.IntegerValue> RULE_MINECART_MAX_SPEED;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_TNT_EXPLODES;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_LOCATOR_BAR;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_PVP;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_ALLOW_NETHER;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_SPAWN_MONSTERS;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_COMMAND_BLOCKS_ENABLED;
   public static final GameRules.Key<GameRules.BooleanValue> RULE_SPAWNER_BLOCKS_ENABLED;
   private final Map<GameRules.Key<?>, GameRules.Value<?>> rules;
   private final FeatureFlagSet enabledFeatures;

   public static <T extends GameRules.Value<T>> GameRules.Type<T> getType(GameRules.Key<T> var0) {
      return (GameRules.Type)GAME_RULE_TYPES.get(var0);
   }

   public static <T extends GameRules.Value<T>> Codec<GameRules.Key<T>> keyCodec(Class<T> var0) {
      return Codec.STRING.comapFlatMap((var1) -> {
         return (DataResult)GAME_RULE_TYPES.entrySet().stream().filter((var1x) -> {
            return ((GameRules.Type)var1x.getValue()).valueClass == var0;
         }).map(Entry::getKey).filter((var1x) -> {
            return var1x.getId().equals(var1);
         }).map((var0x) -> {
            return var0x;
         }).findFirst().map(DataResult::success).orElseGet(() -> {
            return DataResult.error(() -> {
               return "Invalid game rule ID for type: " + var1;
            });
         });
      }, GameRules.Key::getId);
   }

   private static <T extends GameRules.Value<T>> GameRules.Key<T> register(String var0, GameRules.Category var1, GameRules.Type<T> var2) {
      GameRules.Key var3 = new GameRules.Key(var0, var1);
      GameRules.Type var4 = (GameRules.Type)GAME_RULE_TYPES.put(var3, var2);
      if (var4 != null) {
         throw new IllegalStateException("Duplicate game rule registration for " + var0);
      } else {
         return var3;
      }
   }

   public GameRules(FeatureFlagSet var1, DynamicLike<?> var2) {
      this(var1);
      this.loadFromTag(var2);
   }

   public GameRules(FeatureFlagSet var1) {
      this((Map)availableRules(var1).collect(ImmutableMap.toImmutableMap(Entry::getKey, (var0) -> {
         return ((GameRules.Type)var0.getValue()).createRule();
      })), var1);
   }

   public static Stream<Entry<GameRules.Key<?>, GameRules.Type<?>>> availableRules(FeatureFlagSet var0) {
      return GAME_RULE_TYPES.entrySet().stream().filter((var1) -> {
         return ((GameRules.Type)var1.getValue()).requiredFeatures.isSubsetOf(var0);
      });
   }

   private GameRules(Map<GameRules.Key<?>, GameRules.Value<?>> var1, FeatureFlagSet var2) {
      super();
      this.rules = var1;
      this.enabledFeatures = var2;
   }

   public <T extends GameRules.Value<T>> T getRule(GameRules.Key<T> var1) {
      GameRules.Value var2 = (GameRules.Value)this.rules.get(var1);
      if (var2 == null) {
         throw new IllegalArgumentException("Tried to access invalid game rule");
      } else {
         return var2;
      }
   }

   public CompoundTag createTag() {
      CompoundTag var1 = new CompoundTag();
      this.rules.forEach((var1x, var2) -> {
         var1.putString(var1x.id, var2.serialize());
      });
      return var1;
   }

   private void loadFromTag(DynamicLike<?> var1) {
      this.rules.forEach((var1x, var2) -> {
         DataResult var10000 = var1.get(var1x.id).asString();
         Objects.requireNonNull(var2);
         var10000.ifSuccess(var2::deserialize);
      });
   }

   public GameRules copy(FeatureFlagSet var1) {
      return new GameRules((Map)availableRules(var1).collect(ImmutableMap.toImmutableMap(Entry::getKey, (var1x) -> {
         return this.rules.containsKey(var1x.getKey()) ? ((GameRules.Value)this.rules.get(var1x.getKey())).copy() : ((GameRules.Type)var1x.getValue()).createRule();
      })), var1);
   }

   public void visitGameRuleTypes(GameRules.GameRuleTypeVisitor var1) {
      GAME_RULE_TYPES.forEach((var2, var3) -> {
         this.callVisitorCap(var1, var2, var3);
      });
   }

   private <T extends GameRules.Value<T>> void callVisitorCap(GameRules.GameRuleTypeVisitor var1, GameRules.Key<?> var2, GameRules.Type<?> var3) {
      if (var3.requiredFeatures.isSubsetOf(this.enabledFeatures)) {
         var1.visit(var2, var3);
         var3.callVisitor(var1, var2);
      }

   }

   public void assignFrom(GameRules var1, @Nullable MinecraftServer var2) {
      var1.rules.keySet().forEach((var3) -> {
         this.assignCap(var3, var1, var2);
      });
   }

   private <T extends GameRules.Value<T>> void assignCap(GameRules.Key<T> var1, GameRules var2, @Nullable MinecraftServer var3) {
      GameRules.Value var4 = var2.getRule(var1);
      this.getRule(var1).setFrom(var4, var3);
   }

   public boolean getBoolean(GameRules.Key<GameRules.BooleanValue> var1) {
      return ((GameRules.BooleanValue)this.getRule(var1)).get();
   }

   public int getInt(GameRules.Key<GameRules.IntegerValue> var1) {
      return ((GameRules.IntegerValue)this.getRule(var1)).get();
   }

   static {
      RULE_DOFIRETICK = register("doFireTick", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
      RULE_ALLOWFIRETICKAWAYFROMPLAYERS = register("allowFireTicksAwayFromPlayer", GameRules.Category.UPDATES, GameRules.BooleanValue.create(false));
      RULE_MOBGRIEFING = register("mobGriefing", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
      RULE_KEEPINVENTORY = register("keepInventory", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
      RULE_DOMOBSPAWNING = register("doMobSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
      RULE_DOMOBLOOT = register("doMobLoot", GameRules.Category.DROPS, GameRules.BooleanValue.create(true));
      RULE_PROJECTILESCANBREAKBLOCKS = register("projectilesCanBreakBlocks", GameRules.Category.DROPS, GameRules.BooleanValue.create(true));
      RULE_DOBLOCKDROPS = register("doTileDrops", GameRules.Category.DROPS, GameRules.BooleanValue.create(true));
      RULE_DOENTITYDROPS = register("doEntityDrops", GameRules.Category.DROPS, GameRules.BooleanValue.create(true));
      RULE_COMMANDBLOCKOUTPUT = register("commandBlockOutput", GameRules.Category.CHAT, GameRules.BooleanValue.create(true));
      RULE_NATURAL_REGENERATION = register("naturalRegeneration", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
      RULE_DAYLIGHT = register("doDaylightCycle", GameRules.Category.UPDATES, GameRules.BooleanValue.create(!SharedConstants.DEBUG_WORLD_RECREATE));
      RULE_LOGADMINCOMMANDS = register("logAdminCommands", GameRules.Category.CHAT, GameRules.BooleanValue.create(true));
      RULE_SHOWDEATHMESSAGES = register("showDeathMessages", GameRules.Category.CHAT, GameRules.BooleanValue.create(true));
      RULE_RANDOMTICKING = register("randomTickSpeed", GameRules.Category.UPDATES, GameRules.IntegerValue.create(3));
      RULE_SENDCOMMANDFEEDBACK = register("sendCommandFeedback", GameRules.Category.CHAT, GameRules.BooleanValue.create(true));
      RULE_REDUCEDDEBUGINFO = register("reducedDebugInfo", GameRules.Category.MISC, GameRules.BooleanValue.create(false, (var0, var1) -> {
         int var2 = var1.get() ? 22 : 23;
         Iterator var3 = var0.getPlayerList().getPlayers().iterator();

         while(var3.hasNext()) {
            ServerPlayer var4 = (ServerPlayer)var3.next();
            var4.connection.send(new ClientboundEntityEventPacket(var4, (byte)var2));
         }

      }));
      RULE_SPECTATORSGENERATECHUNKS = register("spectatorsGenerateChunks", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
      RULE_SPAWN_RADIUS = register("spawnRadius", GameRules.Category.PLAYER, GameRules.IntegerValue.create(10));
      RULE_DISABLE_PLAYER_MOVEMENT_CHECK = register("disablePlayerMovementCheck", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
      RULE_DISABLE_ELYTRA_MOVEMENT_CHECK = register("disableElytraMovementCheck", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
      RULE_MAX_ENTITY_CRAMMING = register("maxEntityCramming", GameRules.Category.MOBS, GameRules.IntegerValue.create(24));
      RULE_WEATHER_CYCLE = register("doWeatherCycle", GameRules.Category.UPDATES, GameRules.BooleanValue.create(!SharedConstants.DEBUG_WORLD_RECREATE));
      RULE_LIMITED_CRAFTING = register("doLimitedCrafting", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false, (var0, var1) -> {
         Iterator var2 = var0.getPlayerList().getPlayers().iterator();

         while(var2.hasNext()) {
            ServerPlayer var3 = (ServerPlayer)var2.next();
            var3.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.LIMITED_CRAFTING, var1.get() ? 1.0F : 0.0F));
         }

      }));
      RULE_MAX_COMMAND_CHAIN_LENGTH = register("maxCommandChainLength", GameRules.Category.MISC, GameRules.IntegerValue.create(65536));
      RULE_MAX_COMMAND_FORK_COUNT = register("maxCommandForkCount", GameRules.Category.MISC, GameRules.IntegerValue.create(65536));
      RULE_COMMAND_MODIFICATION_BLOCK_LIMIT = register("commandModificationBlockLimit", GameRules.Category.MISC, GameRules.IntegerValue.create(32768));
      RULE_ANNOUNCE_ADVANCEMENTS = register("announceAdvancements", GameRules.Category.CHAT, GameRules.BooleanValue.create(true));
      RULE_DISABLE_RAIDS = register("disableRaids", GameRules.Category.MOBS, GameRules.BooleanValue.create(false));
      RULE_DOINSOMNIA = register("doInsomnia", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
      RULE_DO_IMMEDIATE_RESPAWN = register("doImmediateRespawn", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false, (var0, var1) -> {
         Iterator var2 = var0.getPlayerList().getPlayers().iterator();

         while(var2.hasNext()) {
            ServerPlayer var3 = (ServerPlayer)var2.next();
            var3.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.IMMEDIATE_RESPAWN, var1.get() ? 1.0F : 0.0F));
         }

      }));
      RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY = register("playersNetherPortalDefaultDelay", GameRules.Category.PLAYER, GameRules.IntegerValue.create(80));
      RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY = register("playersNetherPortalCreativeDelay", GameRules.Category.PLAYER, GameRules.IntegerValue.create(0));
      RULE_DROWNING_DAMAGE = register("drowningDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
      RULE_FALL_DAMAGE = register("fallDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
      RULE_FIRE_DAMAGE = register("fireDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
      RULE_FREEZE_DAMAGE = register("freezeDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
      RULE_DO_PATROL_SPAWNING = register("doPatrolSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
      RULE_DO_TRADER_SPAWNING = register("doTraderSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
      RULE_DO_WARDEN_SPAWNING = register("doWardenSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
      RULE_FORGIVE_DEAD_PLAYERS = register("forgiveDeadPlayers", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
      RULE_UNIVERSAL_ANGER = register("universalAnger", GameRules.Category.MOBS, GameRules.BooleanValue.create(false));
      RULE_PLAYERS_SLEEPING_PERCENTAGE = register("playersSleepingPercentage", GameRules.Category.PLAYER, GameRules.IntegerValue.create(100));
      RULE_BLOCK_EXPLOSION_DROP_DECAY = register("blockExplosionDropDecay", GameRules.Category.DROPS, GameRules.BooleanValue.create(true));
      RULE_MOB_EXPLOSION_DROP_DECAY = register("mobExplosionDropDecay", GameRules.Category.DROPS, GameRules.BooleanValue.create(true));
      RULE_TNT_EXPLOSION_DROP_DECAY = register("tntExplosionDropDecay", GameRules.Category.DROPS, GameRules.BooleanValue.create(false));
      RULE_SNOW_ACCUMULATION_HEIGHT = register("snowAccumulationHeight", GameRules.Category.UPDATES, GameRules.IntegerValue.create(1));
      RULE_WATER_SOURCE_CONVERSION = register("waterSourceConversion", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
      RULE_LAVA_SOURCE_CONVERSION = register("lavaSourceConversion", GameRules.Category.UPDATES, GameRules.BooleanValue.create(false));
      RULE_GLOBAL_SOUND_EVENTS = register("globalSoundEvents", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
      RULE_DO_VINES_SPREAD = register("doVinesSpread", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
      RULE_ENDER_PEARLS_VANISH_ON_DEATH = register("enderPearlsVanishOnDeath", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
      RULE_MINECART_MAX_SPEED = register("minecartMaxSpeed", GameRules.Category.MISC, GameRules.IntegerValue.create(8, 1, 1000, FeatureFlagSet.of(FeatureFlags.MINECART_IMPROVEMENTS), (var0, var1) -> {
      }));
      RULE_TNT_EXPLODES = register("tntExplodes", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
      RULE_LOCATOR_BAR = register("locatorBar", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true, (var0, var1) -> {
         var0.getAllLevels().forEach((var1x) -> {
            ServerWaypointManager var2 = var1x.getWaypointManager();
            if (var1.get()) {
               List var10000 = var1x.players();
               Objects.requireNonNull(var2);
               var10000.forEach(var2::updatePlayer);
            } else {
               var2.breakAllConnections();
            }

         });
      }));
      RULE_PVP = register("pvp", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
      RULE_ALLOW_NETHER = register("allowEnteringNetherUsingPortals", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
      RULE_SPAWN_MONSTERS = register("spawnMonsters", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true, (var0, var1) -> {
         var0.updateMobSpawningFlags();
      }));
      RULE_COMMAND_BLOCKS_ENABLED = register("commandBlocksEnabled", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
      RULE_SPAWNER_BLOCKS_ENABLED = register("spawnerBlocksEnabled", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
   }

   public static class Type<T extends GameRules.Value<T>> {
      final Supplier<ArgumentType<?>> argument;
      private final Function<GameRules.Type<T>, T> constructor;
      final BiConsumer<MinecraftServer, T> callback;
      private final GameRules.VisitorCaller<T> visitorCaller;
      final Class<T> valueClass;
      final FeatureFlagSet requiredFeatures;

      Type(Supplier<ArgumentType<?>> var1, Function<GameRules.Type<T>, T> var2, BiConsumer<MinecraftServer, T> var3, GameRules.VisitorCaller<T> var4, Class<T> var5, FeatureFlagSet var6) {
         super();
         this.argument = var1;
         this.constructor = var2;
         this.callback = var3;
         this.visitorCaller = var4;
         this.valueClass = var5;
         this.requiredFeatures = var6;
      }

      public RequiredArgumentBuilder<CommandSourceStack, ?> createArgument(String var1) {
         return Commands.argument(var1, (ArgumentType)this.argument.get());
      }

      public T createRule() {
         return (GameRules.Value)this.constructor.apply(this);
      }

      public void callVisitor(GameRules.GameRuleTypeVisitor var1, GameRules.Key<T> var2) {
         this.visitorCaller.call(var1, var2, this);
      }

      public FeatureFlagSet requiredFeatures() {
         return this.requiredFeatures;
      }
   }

   public static final class Key<T extends GameRules.Value<T>> {
      final String id;
      private final GameRules.Category category;

      public Key(String var1, GameRules.Category var2) {
         super();
         this.id = var1;
         this.category = var2;
      }

      public String toString() {
         return this.id;
      }

      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else {
            return var1 instanceof GameRules.Key && ((GameRules.Key)var1).id.equals(this.id);
         }
      }

      public int hashCode() {
         return this.id.hashCode();
      }

      public String getId() {
         return this.id;
      }

      public String getDescriptionId() {
         return "gamerule." + this.id;
      }

      public GameRules.Category getCategory() {
         return this.category;
      }
   }

   public static enum Category {
      PLAYER("gamerule.category.player"),
      MOBS("gamerule.category.mobs"),
      SPAWNING("gamerule.category.spawning"),
      DROPS("gamerule.category.drops"),
      UPDATES("gamerule.category.updates"),
      CHAT("gamerule.category.chat"),
      MISC("gamerule.category.misc");

      private final String descriptionId;

      private Category(final String param3) {
         this.descriptionId = var3;
      }

      public String getDescriptionId() {
         return this.descriptionId;
      }

      // $FF: synthetic method
      private static GameRules.Category[] $values() {
         return new GameRules.Category[]{PLAYER, MOBS, SPAWNING, DROPS, UPDATES, CHAT, MISC};
      }
   }

   public abstract static class Value<T extends GameRules.Value<T>> {
      protected final GameRules.Type<T> type;

      public Value(GameRules.Type<T> var1) {
         super();
         this.type = var1;
      }

      protected abstract void updateFromArgument(CommandContext<CommandSourceStack> var1, String var2);

      public void setFromArgument(CommandContext<CommandSourceStack> var1, String var2) {
         this.updateFromArgument(var1, var2);
         this.onChanged(((CommandSourceStack)var1.getSource()).getServer());
      }

      protected void onChanged(@Nullable MinecraftServer var1) {
         if (var1 != null) {
            this.type.callback.accept(var1, this.getSelf());
         }

      }

      protected abstract void deserialize(String var1);

      public abstract String serialize();

      public String toString() {
         return this.serialize();
      }

      public abstract int getCommandResult();

      protected abstract T getSelf();

      protected abstract T copy();

      public abstract void setFrom(T var1, @Nullable MinecraftServer var2);
   }

   public interface GameRuleTypeVisitor {
      default <T extends GameRules.Value<T>> void visit(GameRules.Key<T> var1, GameRules.Type<T> var2) {
      }

      default void visitBoolean(GameRules.Key<GameRules.BooleanValue> var1, GameRules.Type<GameRules.BooleanValue> var2) {
      }

      default void visitInteger(GameRules.Key<GameRules.IntegerValue> var1, GameRules.Type<GameRules.IntegerValue> var2) {
      }
   }

   public static class BooleanValue extends GameRules.Value<GameRules.BooleanValue> {
      private boolean value;

      private static GameRules.Type<GameRules.BooleanValue> create(boolean var0, BiConsumer<MinecraftServer, GameRules.BooleanValue> var1, FeatureFlagSet var2) {
         return new GameRules.Type(BoolArgumentType::bool, (var1x) -> {
            return new GameRules.BooleanValue(var1x, var0);
         }, var1, GameRules.GameRuleTypeVisitor::visitBoolean, GameRules.BooleanValue.class, var2);
      }

      static GameRules.Type<GameRules.BooleanValue> create(boolean var0, BiConsumer<MinecraftServer, GameRules.BooleanValue> var1) {
         return new GameRules.Type(BoolArgumentType::bool, (var1x) -> {
            return new GameRules.BooleanValue(var1x, var0);
         }, var1, GameRules.GameRuleTypeVisitor::visitBoolean, GameRules.BooleanValue.class, FeatureFlagSet.of());
      }

      public static GameRules.Type<GameRules.BooleanValue> create(boolean var0) {
         return create(var0, (var0x, var1) -> {
         });
      }

      public BooleanValue(GameRules.Type<GameRules.BooleanValue> var1, boolean var2) {
         super(var1);
         this.value = var2;
      }

      protected void updateFromArgument(CommandContext<CommandSourceStack> var1, String var2) {
         this.value = BoolArgumentType.getBool(var1, var2);
      }

      public boolean get() {
         return this.value;
      }

      public void set(boolean var1, @Nullable MinecraftServer var2) {
         this.value = var1;
         this.onChanged(var2);
      }

      public String serialize() {
         return Boolean.toString(this.value);
      }

      protected void deserialize(String var1) {
         this.value = Boolean.parseBoolean(var1);
      }

      public int getCommandResult() {
         return this.value ? 1 : 0;
      }

      protected GameRules.BooleanValue getSelf() {
         return this;
      }

      protected GameRules.BooleanValue copy() {
         return new GameRules.BooleanValue(this.type, this.value);
      }

      public void setFrom(GameRules.BooleanValue var1, @Nullable MinecraftServer var2) {
         this.value = var1.value;
         this.onChanged(var2);
      }

      // $FF: synthetic method
      protected GameRules.Value copy() {
         return this.copy();
      }

      // $FF: synthetic method
      protected GameRules.Value getSelf() {
         return this.getSelf();
      }
   }

   public static class IntegerValue extends GameRules.Value<GameRules.IntegerValue> {
      private int value;

      private static GameRules.Type<GameRules.IntegerValue> create(int var0, BiConsumer<MinecraftServer, GameRules.IntegerValue> var1) {
         return new GameRules.Type(IntegerArgumentType::integer, (var1x) -> {
            return new GameRules.IntegerValue(var1x, var0);
         }, var1, GameRules.GameRuleTypeVisitor::visitInteger, GameRules.IntegerValue.class, FeatureFlagSet.of());
      }

      static GameRules.Type<GameRules.IntegerValue> create(int var0, int var1, int var2, FeatureFlagSet var3, BiConsumer<MinecraftServer, GameRules.IntegerValue> var4) {
         return new GameRules.Type(() -> {
            return IntegerArgumentType.integer(var1, var2);
         }, (var1x) -> {
            return new GameRules.IntegerValue(var1x, var0);
         }, var4, GameRules.GameRuleTypeVisitor::visitInteger, GameRules.IntegerValue.class, var3);
      }

      public static GameRules.Type<GameRules.IntegerValue> create(int var0) {
         return create(var0, (var0x, var1) -> {
         });
      }

      public IntegerValue(GameRules.Type<GameRules.IntegerValue> var1, int var2) {
         super(var1);
         this.value = var2;
      }

      protected void updateFromArgument(CommandContext<CommandSourceStack> var1, String var2) {
         this.value = IntegerArgumentType.getInteger(var1, var2);
      }

      public int get() {
         return this.value;
      }

      public void set(int var1, @Nullable MinecraftServer var2) {
         this.value = var1;
         this.onChanged(var2);
      }

      public String serialize() {
         return Integer.toString(this.value);
      }

      protected void deserialize(String var1) {
         this.value = safeParse(var1);
      }

      public boolean tryDeserialize(String var1) {
         try {
            StringReader var2 = new StringReader(var1);
            this.value = (Integer)((ArgumentType)this.type.argument.get()).parse(var2);
            return !var2.canRead();
         } catch (CommandSyntaxException var3) {
            return false;
         }
      }

      private static int safeParse(String var0) {
         if (!var0.isEmpty()) {
            try {
               return Integer.parseInt(var0);
            } catch (NumberFormatException var2) {
               GameRules.LOGGER.warn("Failed to parse integer {}", var0);
            }
         }

         return 0;
      }

      public int getCommandResult() {
         return this.value;
      }

      protected GameRules.IntegerValue getSelf() {
         return this;
      }

      protected GameRules.IntegerValue copy() {
         return new GameRules.IntegerValue(this.type, this.value);
      }

      public void setFrom(GameRules.IntegerValue var1, @Nullable MinecraftServer var2) {
         this.value = var1.value;
         this.onChanged(var2);
      }

      // $FF: synthetic method
      protected GameRules.Value copy() {
         return this.copy();
      }

      // $FF: synthetic method
      protected GameRules.Value getSelf() {
         return this.getSelf();
      }
   }

   private interface VisitorCaller<T extends GameRules.Value<T>> {
      void call(GameRules.GameRuleTypeVisitor var1, GameRules.Key<T> var2, GameRules.Type<T> var3);
   }
}
