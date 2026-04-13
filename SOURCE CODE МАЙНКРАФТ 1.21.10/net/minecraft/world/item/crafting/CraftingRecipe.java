package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface CraftingRecipe extends Recipe<CraftingInput> {
   default RecipeType<CraftingRecipe> getType() {
      return RecipeType.CRAFTING;
   }

   RecipeSerializer<? extends CraftingRecipe> getSerializer();

   CraftingBookCategory category();

   default NonNullList<ItemStack> getRemainingItems(CraftingInput var1) {
      return defaultCraftingReminder(var1);
   }

   static NonNullList<ItemStack> defaultCraftingReminder(CraftingInput var0) {
      NonNullList var1 = NonNullList.withSize(var0.size(), ItemStack.EMPTY);

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         Item var3 = var0.getItem(var2).getItem();
         var1.set(var2, var3.getCraftingRemainder());
      }

      return var1;
   }

   default RecipeBookCategory recipeBookCategory() {
      RecipeBookCategory var10000;
      switch(this.category()) {
      case BUILDING:
         var10000 = RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
         break;
      case EQUIPMENT:
         var10000 = RecipeBookCategories.CRAFTING_EQUIPMENT;
         break;
      case REDSTONE:
         var10000 = RecipeBookCategories.CRAFTING_REDSTONE;
         break;
      case MISC:
         var10000 = RecipeBookCategories.CRAFTING_MISC;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }
}
