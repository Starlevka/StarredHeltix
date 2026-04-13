package net.minecraft.stats;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import org.slf4j.Logger;

public class ServerRecipeBook extends RecipeBook {
   public static final String RECIPE_BOOK_TAG = "recipeBook";
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ServerRecipeBook.DisplayResolver displayResolver;
   @VisibleForTesting
   protected final Set<ResourceKey<Recipe<?>>> known = Sets.newIdentityHashSet();
   @VisibleForTesting
   protected final Set<ResourceKey<Recipe<?>>> highlight = Sets.newIdentityHashSet();

   public ServerRecipeBook(ServerRecipeBook.DisplayResolver var1) {
      super();
      this.displayResolver = var1;
   }

   public void add(ResourceKey<Recipe<?>> var1) {
      this.known.add(var1);
   }

   public boolean contains(ResourceKey<Recipe<?>> var1) {
      return this.known.contains(var1);
   }

   public void remove(ResourceKey<Recipe<?>> var1) {
      this.known.remove(var1);
      this.highlight.remove(var1);
   }

   public void removeHighlight(ResourceKey<Recipe<?>> var1) {
      this.highlight.remove(var1);
   }

   private void addHighlight(ResourceKey<Recipe<?>> var1) {
      this.highlight.add(var1);
   }

   public int addRecipes(Collection<RecipeHolder<?>> var1, ServerPlayer var2) {
      ArrayList var3 = new ArrayList();
      Iterator var4 = var1.iterator();

      while(var4.hasNext()) {
         RecipeHolder var5 = (RecipeHolder)var4.next();
         ResourceKey var6 = var5.id();
         if (!this.known.contains(var6) && !var5.value().isSpecial()) {
            this.add(var6);
            this.addHighlight(var6);
            this.displayResolver.displaysForRecipe(var6, (var2x) -> {
               var3.add(new ClientboundRecipeBookAddPacket.Entry(var2x, var5.value().showNotification(), true));
            });
            CriteriaTriggers.RECIPE_UNLOCKED.trigger(var2, var5);
         }
      }

      if (!var3.isEmpty()) {
         var2.connection.send(new ClientboundRecipeBookAddPacket(var3, false));
      }

      return var3.size();
   }

   public int removeRecipes(Collection<RecipeHolder<?>> var1, ServerPlayer var2) {
      ArrayList var3 = Lists.newArrayList();
      Iterator var4 = var1.iterator();

      while(var4.hasNext()) {
         RecipeHolder var5 = (RecipeHolder)var4.next();
         ResourceKey var6 = var5.id();
         if (this.known.contains(var6)) {
            this.remove(var6);
            this.displayResolver.displaysForRecipe(var6, (var1x) -> {
               var3.add(var1x.id());
            });
         }
      }

      if (!var3.isEmpty()) {
         var2.connection.send(new ClientboundRecipeBookRemovePacket(var3));
      }

      return var3.size();
   }

   private void loadRecipes(List<ResourceKey<Recipe<?>>> var1, Consumer<ResourceKey<Recipe<?>>> var2, Predicate<ResourceKey<Recipe<?>>> var3) {
      Iterator var4 = var1.iterator();

      while(var4.hasNext()) {
         ResourceKey var5 = (ResourceKey)var4.next();
         if (!var3.test(var5)) {
            LOGGER.error("Tried to load unrecognized recipe: {} removed now.", var5);
         } else {
            var2.accept(var5);
         }
      }

   }

   public void sendInitialRecipeBook(ServerPlayer var1) {
      var1.connection.send(new ClientboundRecipeBookSettingsPacket(this.getBookSettings().copy()));
      ArrayList var2 = new ArrayList(this.known.size());
      Iterator var3 = this.known.iterator();

      while(var3.hasNext()) {
         ResourceKey var4 = (ResourceKey)var3.next();
         this.displayResolver.displaysForRecipe(var4, (var3x) -> {
            var2.add(new ClientboundRecipeBookAddPacket.Entry(var3x, false, this.highlight.contains(var4)));
         });
      }

      var1.connection.send(new ClientboundRecipeBookAddPacket(var2, true));
   }

   public void copyOverData(ServerRecipeBook var1) {
      this.apply(var1.pack());
   }

   public ServerRecipeBook.Packed pack() {
      return new ServerRecipeBook.Packed(this.bookSettings.copy(), List.copyOf(this.known), List.copyOf(this.highlight));
   }

   private void apply(ServerRecipeBook.Packed var1) {
      this.known.clear();
      this.highlight.clear();
      this.bookSettings.replaceFrom(var1.settings);
      this.known.addAll(var1.known);
      this.highlight.addAll(var1.highlight);
   }

   public void loadUntrusted(ServerRecipeBook.Packed var1, Predicate<ResourceKey<Recipe<?>>> var2) {
      this.bookSettings.replaceFrom(var1.settings);
      List var10001 = var1.known;
      Set var10002 = this.known;
      Objects.requireNonNull(var10002);
      this.loadRecipes(var10001, var10002::add, var2);
      var10001 = var1.highlight;
      var10002 = this.highlight;
      Objects.requireNonNull(var10002);
      this.loadRecipes(var10001, var10002::add, var2);
   }

   @FunctionalInterface
   public interface DisplayResolver {
      void displaysForRecipe(ResourceKey<Recipe<?>> var1, Consumer<RecipeDisplayEntry> var2);
   }

   public static record Packed(RecipeBookSettings settings, List<ResourceKey<Recipe<?>>> known, List<ResourceKey<Recipe<?>>> highlight) {
      final RecipeBookSettings settings;
      final List<ResourceKey<Recipe<?>>> known;
      final List<ResourceKey<Recipe<?>>> highlight;
      public static final Codec<ServerRecipeBook.Packed> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(RecipeBookSettings.MAP_CODEC.forGetter(ServerRecipeBook.Packed::settings), Recipe.KEY_CODEC.listOf().fieldOf("recipes").forGetter(ServerRecipeBook.Packed::known), Recipe.KEY_CODEC.listOf().fieldOf("toBeDisplayed").forGetter(ServerRecipeBook.Packed::highlight)).apply(var0, ServerRecipeBook.Packed::new);
      });

      public Packed(RecipeBookSettings param1, List<ResourceKey<Recipe<?>>> param2, List<ResourceKey<Recipe<?>>> param3) {
         super();
         this.settings = var1;
         this.known = var2;
         this.highlight = var3;
      }

      public RecipeBookSettings settings() {
         return this.settings;
      }

      public List<ResourceKey<Recipe<?>>> known() {
         return this.known;
      }

      public List<ResourceKey<Recipe<?>>> highlight() {
         return this.highlight;
      }
   }
}
