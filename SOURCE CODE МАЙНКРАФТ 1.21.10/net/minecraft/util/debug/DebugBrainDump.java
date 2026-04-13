package net.minecraft.util.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;

public record DebugBrainDump(String name, String profession, int xp, float health, float maxHealth, String inventory, boolean wantsGolem, int angerLevel, List<String> activities, List<String> behaviors, List<String> memories, List<String> gossips, Set<BlockPos> pois, Set<BlockPos> potentialPois) {
   public static final StreamCodec<FriendlyByteBuf, DebugBrainDump> STREAM_CODEC = StreamCodec.of((var0, var1) -> {
      var1.write(var0);
   }, DebugBrainDump::new);

   public DebugBrainDump(FriendlyByteBuf var1) {
      this(var1.readUtf(), var1.readUtf(), var1.readInt(), var1.readFloat(), var1.readFloat(), var1.readUtf(), var1.readBoolean(), var1.readInt(), var1.readList(FriendlyByteBuf::readUtf), var1.readList(FriendlyByteBuf::readUtf), var1.readList(FriendlyByteBuf::readUtf), var1.readList(FriendlyByteBuf::readUtf), (Set)var1.readCollection(HashSet::new, BlockPos.STREAM_CODEC), (Set)var1.readCollection(HashSet::new, BlockPos.STREAM_CODEC));
   }

   public DebugBrainDump(String param1, String param2, int param3, float param4, float param5, String param6, boolean param7, int param8, List<String> param9, List<String> param10, List<String> param11, List<String> param12, Set<BlockPos> param13, Set<BlockPos> param14) {
      super();
      this.name = var1;
      this.profession = var2;
      this.xp = var3;
      this.health = var4;
      this.maxHealth = var5;
      this.inventory = var6;
      this.wantsGolem = var7;
      this.angerLevel = var8;
      this.activities = var9;
      this.behaviors = var10;
      this.memories = var11;
      this.gossips = var12;
      this.pois = var13;
      this.potentialPois = var14;
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeUtf(this.name);
      var1.writeUtf(this.profession);
      var1.writeInt(this.xp);
      var1.writeFloat(this.health);
      var1.writeFloat(this.maxHealth);
      var1.writeUtf(this.inventory);
      var1.writeBoolean(this.wantsGolem);
      var1.writeInt(this.angerLevel);
      var1.writeCollection(this.activities, FriendlyByteBuf::writeUtf);
      var1.writeCollection(this.behaviors, FriendlyByteBuf::writeUtf);
      var1.writeCollection(this.memories, FriendlyByteBuf::writeUtf);
      var1.writeCollection(this.gossips, FriendlyByteBuf::writeUtf);
      var1.writeCollection(this.pois, BlockPos.STREAM_CODEC);
      var1.writeCollection(this.potentialPois, BlockPos.STREAM_CODEC);
   }

   public static DebugBrainDump takeBrainDump(ServerLevel var0, LivingEntity var1) {
      String var2 = DebugEntityNameGenerator.getEntityName((Entity)var1);
      String var3;
      int var4;
      if (var1 instanceof Villager) {
         Villager var5 = (Villager)var1;
         var3 = var5.getVillagerData().profession().getRegisteredName();
         var4 = var5.getVillagerXp();
      } else {
         var3 = "";
         var4 = 0;
      }

      float var20 = var1.getHealth();
      float var6 = var1.getMaxHealth();
      Brain var7 = var1.getBrain();
      long var8 = var1.level().getGameTime();
      String var10;
      if (var1 instanceof InventoryCarrier) {
         InventoryCarrier var11 = (InventoryCarrier)var1;
         SimpleContainer var12 = var11.getInventory();
         var10 = var12.isEmpty() ? "" : var12.toString();
      } else {
         var10 = "";
      }

      boolean var10000;
      label36: {
         if (var1 instanceof Villager) {
            Villager var22 = (Villager)var1;
            if (var22.wantsToSpawnGolem(var8)) {
               var10000 = true;
               break label36;
            }
         }

         var10000 = false;
      }

      boolean var21 = var10000;
      int var25;
      if (var1 instanceof Warden) {
         Warden var13 = (Warden)var1;
         var25 = var13.getClientAngerLevel();
      } else {
         var25 = -1;
      }

      int var23 = var25;
      List var24 = var7.getActiveActivities().stream().map(Activity::getName).toList();
      List var14 = var7.getRunningBehaviors().stream().map(BehaviorControl::debugString).toList();
      List var15 = getMemoryDescriptions(var0, var1, var8).map((var0x) -> {
         return StringUtil.truncateStringIfNecessary(var0x, 255, true);
      }).toList();
      Set var16 = getKnownBlockPositions(var7, MemoryModuleType.JOB_SITE, MemoryModuleType.HOME, MemoryModuleType.MEETING_POINT);
      Set var17 = getKnownBlockPositions(var7, MemoryModuleType.POTENTIAL_JOB_SITE);
      List var26;
      if (var1 instanceof Villager) {
         Villager var19 = (Villager)var1;
         var26 = getVillagerGossips(var19);
      } else {
         var26 = List.of();
      }

      List var18 = var26;
      return new DebugBrainDump(var2, var3, var4, var20, var6, var10, var21, var23, var24, var14, var15, var18, var16, var17);
   }

   @SafeVarargs
   private static Set<BlockPos> getKnownBlockPositions(Brain<?> var0, MemoryModuleType<GlobalPos>... var1) {
      Stream var10000 = Stream.of(var1);
      Objects.requireNonNull(var0);
      var10000 = var10000.filter(var0::hasMemoryValue);
      Objects.requireNonNull(var0);
      return (Set)var10000.map(var0::getMemory).flatMap(Optional::stream).map(GlobalPos::pos).collect(Collectors.toSet());
   }

   private static List<String> getVillagerGossips(Villager var0) {
      ArrayList var1 = new ArrayList();
      var0.getGossips().getGossipEntries().forEach((var1x, var2) -> {
         String var3 = DebugEntityNameGenerator.getEntityName(var1x);
         var2.forEach((var2x, var3x) -> {
            var1.add(var3 + ": " + String.valueOf(var2x) + ": " + var3x);
         });
      });
      return var1;
   }

   private static Stream<String> getMemoryDescriptions(ServerLevel var0, LivingEntity var1, long var2) {
      return var1.getBrain().getMemories().entrySet().stream().map((var3) -> {
         MemoryModuleType var4 = (MemoryModuleType)var3.getKey();
         Optional var5 = (Optional)var3.getValue();
         return getMemoryDescription(var0, var2, var4, var5);
      }).sorted();
   }

   private static String getMemoryDescription(ServerLevel var0, long var1, MemoryModuleType<?> var3, Optional<? extends ExpirableValue<?>> var4) {
      String var10000;
      String var5;
      if (var4.isPresent()) {
         ExpirableValue var6 = (ExpirableValue)var4.get();
         Object var7 = var6.getValue();
         if (var3 == MemoryModuleType.HEARD_BELL_TIME) {
            long var8 = var1 - (Long)var7;
            var5 = var8 + " ticks ago";
         } else if (var6.canExpire()) {
            var10000 = getShortDescription(var0, var7);
            var5 = var10000 + " (ttl: " + var6.getTimeToLive() + ")";
         } else {
            var5 = getShortDescription(var0, var7);
         }
      } else {
         var5 = "-";
      }

      var10000 = BuiltInRegistries.MEMORY_MODULE_TYPE.getKey(var3).getPath();
      return var10000 + ": " + var5;
   }

   private static String getShortDescription(ServerLevel var0, @Nullable Object var1) {
      byte var3 = 0;
      String var10000;
      switch(((Class)var1).typeSwitch<invokedynamic>(var1, var3)) {
      case -1:
         var10000 = "-";
         break;
      case 0:
         UUID var4 = (UUID)var1;
         var10000 = getShortDescription(var0, var0.getEntity(var4));
         break;
      case 1:
         Entity var5 = (Entity)var1;
         var10000 = DebugEntityNameGenerator.getEntityName(var5);
         break;
      case 2:
         WalkTarget var6 = (WalkTarget)var1;
         var10000 = getShortDescription(var0, var6.getTarget());
         break;
      case 3:
         EntityTracker var7 = (EntityTracker)var1;
         var10000 = getShortDescription(var0, var7.getEntity());
         break;
      case 4:
         GlobalPos var8 = (GlobalPos)var1;
         var10000 = getShortDescription(var0, var8.pos());
         break;
      case 5:
         BlockPosTracker var9 = (BlockPosTracker)var1;
         var10000 = getShortDescription(var0, var9.currentBlockPosition());
         break;
      case 6:
         DamageSource var10 = (DamageSource)var1;
         Entity var12 = var10.getEntity();
         var10000 = var12 == null ? var1.toString() : getShortDescription(var0, var12);
         break;
      case 7:
         Collection var11 = (Collection)var1;
         var10000 = "[" + (String)var11.stream().map((var1x) -> {
            return getShortDescription(var0, var1x);
         }).collect(Collectors.joining(", ")) + "]";
         break;
      default:
         var10000 = var1.toString();
      }

      return var10000;
   }

   public boolean hasPoi(BlockPos var1) {
      return this.pois.contains(var1);
   }

   public boolean hasPotentialPoi(BlockPos var1) {
      return this.potentialPois.contains(var1);
   }

   public String name() {
      return this.name;
   }

   public String profession() {
      return this.profession;
   }

   public int xp() {
      return this.xp;
   }

   public float health() {
      return this.health;
   }

   public float maxHealth() {
      return this.maxHealth;
   }

   public String inventory() {
      return this.inventory;
   }

   public boolean wantsGolem() {
      return this.wantsGolem;
   }

   public int angerLevel() {
      return this.angerLevel;
   }

   public List<String> activities() {
      return this.activities;
   }

   public List<String> behaviors() {
      return this.behaviors;
   }

   public List<String> memories() {
      return this.memories;
   }

   public List<String> gossips() {
      return this.gossips;
   }

   public Set<BlockPos> pois() {
      return this.pois;
   }

   public Set<BlockPos> potentialPois() {
      return this.potentialPois;
   }
}
