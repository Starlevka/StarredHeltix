package net.minecraft.advancements;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CacheableFunction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record AdvancementRewards(int experience, List<ResourceKey<LootTable>> loot, List<ResourceKey<Recipe<?>>> recipes, Optional<CacheableFunction> function) {
   public static final Codec<AdvancementRewards> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(Codec.INT.optionalFieldOf("experience", 0).forGetter(AdvancementRewards::experience), LootTable.KEY_CODEC.listOf().optionalFieldOf("loot", List.of()).forGetter(AdvancementRewards::loot), Recipe.KEY_CODEC.listOf().optionalFieldOf("recipes", List.of()).forGetter(AdvancementRewards::recipes), CacheableFunction.CODEC.optionalFieldOf("function").forGetter(AdvancementRewards::function)).apply(var0, AdvancementRewards::new);
   });
   public static final AdvancementRewards EMPTY = new AdvancementRewards(0, List.of(), List.of(), Optional.empty());

   public AdvancementRewards(int param1, List<ResourceKey<LootTable>> param2, List<ResourceKey<Recipe<?>>> param3, Optional<CacheableFunction> param4) {
      super();
      this.experience = var1;
      this.loot = var2;
      this.recipes = var3;
      this.function = var4;
   }

   public void grant(ServerPlayer var1) {
      var1.giveExperiencePoints(this.experience);
      ServerLevel var2 = var1.level();
      MinecraftServer var3 = var2.getServer();
      LootParams var4 = (new LootParams.Builder(var2)).withParameter(LootContextParams.THIS_ENTITY, var1).withParameter(LootContextParams.ORIGIN, var1.position()).create(LootContextParamSets.ADVANCEMENT_REWARD);
      boolean var5 = false;
      Iterator var6 = this.loot.iterator();

      while(var6.hasNext()) {
         ResourceKey var7 = (ResourceKey)var6.next();
         ObjectListIterator var8 = var3.reloadableRegistries().getLootTable(var7).getRandomItems(var4).iterator();

         while(var8.hasNext()) {
            ItemStack var9 = (ItemStack)var8.next();
            if (var1.addItem(var9)) {
               var2.playSound((Entity)null, var1.getX(), var1.getY(), var1.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((var1.getRandom().nextFloat() - var1.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
               var5 = true;
            } else {
               ItemEntity var10 = var1.drop(var9, false);
               if (var10 != null) {
                  var10.setNoPickUpDelay();
                  var10.setTarget(var1.getUUID());
               }
            }
         }
      }

      if (var5) {
         var1.containerMenu.broadcastChanges();
      }

      if (!this.recipes.isEmpty()) {
         var1.awardRecipesByKey(this.recipes);
      }

      this.function.flatMap((var1x) -> {
         return var1x.get(var3.getFunctions());
      }).ifPresent((var2x) -> {
         var3.getFunctions().execute(var2x, var1.createCommandSourceStack().withSuppressedOutput().withPermission(2));
      });
   }

   public int experience() {
      return this.experience;
   }

   public List<ResourceKey<LootTable>> loot() {
      return this.loot;
   }

   public List<ResourceKey<Recipe<?>>> recipes() {
      return this.recipes;
   }

   public Optional<CacheableFunction> function() {
      return this.function;
   }

   public static class Builder {
      private int experience;
      private final com.google.common.collect.ImmutableList.Builder<ResourceKey<LootTable>> loot = ImmutableList.builder();
      private final com.google.common.collect.ImmutableList.Builder<ResourceKey<Recipe<?>>> recipes = ImmutableList.builder();
      private Optional<ResourceLocation> function = Optional.empty();

      public Builder() {
         super();
      }

      public static AdvancementRewards.Builder experience(int var0) {
         return (new AdvancementRewards.Builder()).addExperience(var0);
      }

      public AdvancementRewards.Builder addExperience(int var1) {
         this.experience += var1;
         return this;
      }

      public static AdvancementRewards.Builder loot(ResourceKey<LootTable> var0) {
         return (new AdvancementRewards.Builder()).addLootTable(var0);
      }

      public AdvancementRewards.Builder addLootTable(ResourceKey<LootTable> var1) {
         this.loot.add(var1);
         return this;
      }

      public static AdvancementRewards.Builder recipe(ResourceKey<Recipe<?>> var0) {
         return (new AdvancementRewards.Builder()).addRecipe(var0);
      }

      public AdvancementRewards.Builder addRecipe(ResourceKey<Recipe<?>> var1) {
         this.recipes.add(var1);
         return this;
      }

      public static AdvancementRewards.Builder function(ResourceLocation var0) {
         return (new AdvancementRewards.Builder()).runs(var0);
      }

      public AdvancementRewards.Builder runs(ResourceLocation var1) {
         this.function = Optional.of(var1);
         return this;
      }

      public AdvancementRewards build() {
         return new AdvancementRewards(this.experience, this.loot.build(), this.recipes.build(), this.function.map(CacheableFunction::new));
      }
   }
}
