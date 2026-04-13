package net.minecraft.world.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

public class ItemCombinerMenuSlotDefinition {
   private final List<ItemCombinerMenuSlotDefinition.SlotDefinition> slots;
   private final ItemCombinerMenuSlotDefinition.SlotDefinition resultSlot;

   ItemCombinerMenuSlotDefinition(List<ItemCombinerMenuSlotDefinition.SlotDefinition> var1, ItemCombinerMenuSlotDefinition.SlotDefinition var2) {
      super();
      if (!var1.isEmpty() && !var2.equals(ItemCombinerMenuSlotDefinition.SlotDefinition.EMPTY)) {
         this.slots = var1;
         this.resultSlot = var2;
      } else {
         throw new IllegalArgumentException("Need to define both inputSlots and resultSlot");
      }
   }

   public static ItemCombinerMenuSlotDefinition.Builder create() {
      return new ItemCombinerMenuSlotDefinition.Builder();
   }

   public ItemCombinerMenuSlotDefinition.SlotDefinition getSlot(int var1) {
      return (ItemCombinerMenuSlotDefinition.SlotDefinition)this.slots.get(var1);
   }

   public ItemCombinerMenuSlotDefinition.SlotDefinition getResultSlot() {
      return this.resultSlot;
   }

   public List<ItemCombinerMenuSlotDefinition.SlotDefinition> getSlots() {
      return this.slots;
   }

   public int getNumOfInputSlots() {
      return this.slots.size();
   }

   public int getResultSlotIndex() {
      return this.getNumOfInputSlots();
   }

   public static record SlotDefinition(int slotIndex, int x, int y, Predicate<ItemStack> mayPlace) {
      final int slotIndex;
      static final ItemCombinerMenuSlotDefinition.SlotDefinition EMPTY = new ItemCombinerMenuSlotDefinition.SlotDefinition(0, 0, 0, (var0) -> {
         return true;
      });

      public SlotDefinition(int param1, int param2, int param3, Predicate<ItemStack> param4) {
         super();
         this.slotIndex = var1;
         this.x = var2;
         this.y = var3;
         this.mayPlace = var4;
      }

      public int slotIndex() {
         return this.slotIndex;
      }

      public int x() {
         return this.x;
      }

      public int y() {
         return this.y;
      }

      public Predicate<ItemStack> mayPlace() {
         return this.mayPlace;
      }
   }

   public static class Builder {
      private final List<ItemCombinerMenuSlotDefinition.SlotDefinition> inputSlots = new ArrayList();
      private ItemCombinerMenuSlotDefinition.SlotDefinition resultSlot;

      public Builder() {
         super();
         this.resultSlot = ItemCombinerMenuSlotDefinition.SlotDefinition.EMPTY;
      }

      public ItemCombinerMenuSlotDefinition.Builder withSlot(int var1, int var2, int var3, Predicate<ItemStack> var4) {
         this.inputSlots.add(new ItemCombinerMenuSlotDefinition.SlotDefinition(var1, var2, var3, var4));
         return this;
      }

      public ItemCombinerMenuSlotDefinition.Builder withResultSlot(int var1, int var2, int var3) {
         this.resultSlot = new ItemCombinerMenuSlotDefinition.SlotDefinition(var1, var2, var3, (var0) -> {
            return false;
         });
         return this;
      }

      public ItemCombinerMenuSlotDefinition build() {
         int var1 = this.inputSlots.size();

         for(int var2 = 0; var2 < var1; ++var2) {
            ItemCombinerMenuSlotDefinition.SlotDefinition var3 = (ItemCombinerMenuSlotDefinition.SlotDefinition)this.inputSlots.get(var2);
            if (var3.slotIndex != var2) {
               throw new IllegalArgumentException("Expected input slots to have continous indexes");
            }
         }

         if (this.resultSlot.slotIndex != var1) {
            throw new IllegalArgumentException("Expected result slot index to follow last input slot");
         } else {
            return new ItemCombinerMenuSlotDefinition(this.inputSlots, this.resultSlot);
         }
      }
   }
}
