package net.minecraft.client.gui.screens.recipebook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

public class RecipeCollection {
   public static final RecipeCollection EMPTY = new RecipeCollection(List.of());
   private final List<RecipeDisplayEntry> entries;
   private final Set<RecipeDisplayId> craftable = new HashSet();
   private final Set<RecipeDisplayId> selected = new HashSet();

   public RecipeCollection(List<RecipeDisplayEntry> var1) {
      super();
      this.entries = var1;
   }

   public void selectRecipes(StackedItemContents var1, Predicate<RecipeDisplay> var2) {
      Iterator var3 = this.entries.iterator();

      while(true) {
         while(var3.hasNext()) {
            RecipeDisplayEntry var4 = (RecipeDisplayEntry)var3.next();
            boolean var5 = var2.test(var4.display());
            if (var5) {
               this.selected.add(var4.id());
            } else {
               this.selected.remove(var4.id());
            }

            if (var5 && var4.canCraft(var1)) {
               this.craftable.add(var4.id());
            } else {
               this.craftable.remove(var4.id());
            }
         }

         return;
      }
   }

   public boolean isCraftable(RecipeDisplayId var1) {
      return this.craftable.contains(var1);
   }

   public boolean hasCraftable() {
      return !this.craftable.isEmpty();
   }

   public boolean hasAnySelected() {
      return !this.selected.isEmpty();
   }

   public List<RecipeDisplayEntry> getRecipes() {
      return this.entries;
   }

   public List<RecipeDisplayEntry> getSelectedRecipes(RecipeCollection.CraftableStatus var1) {
      Predicate var10000;
      Set var6;
      switch(var1.ordinal()) {
      case 0:
         var6 = this.selected;
         Objects.requireNonNull(var6);
         var10000 = var6::contains;
         break;
      case 1:
         var6 = this.craftable;
         Objects.requireNonNull(var6);
         var10000 = var6::contains;
         break;
      case 2:
         var10000 = (var1x) -> {
            return this.selected.contains(var1x) && !this.craftable.contains(var1x);
         };
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      Predicate var2 = var10000;
      ArrayList var3 = new ArrayList();
      Iterator var4 = this.entries.iterator();

      while(var4.hasNext()) {
         RecipeDisplayEntry var5 = (RecipeDisplayEntry)var4.next();
         if (var2.test(var5.id())) {
            var3.add(var5);
         }
      }

      return var3;
   }

   public static enum CraftableStatus {
      ANY,
      CRAFTABLE,
      NOT_CRAFTABLE;

      private CraftableStatus() {
      }

      // $FF: synthetic method
      private static RecipeCollection.CraftableStatus[] $values() {
         return new RecipeCollection.CraftableStatus[]{ANY, CRAFTABLE, NOT_CRAFTABLE};
      }
   }
}
