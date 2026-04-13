package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.world.item.ItemStack;

public class EntityEquipment {
   public static final Codec<EntityEquipment> CODEC;
   private final EnumMap<EquipmentSlot, ItemStack> items;

   private EntityEquipment(EnumMap<EquipmentSlot, ItemStack> var1) {
      super();
      this.items = var1;
   }

   public EntityEquipment() {
      this(new EnumMap(EquipmentSlot.class));
   }

   public ItemStack set(EquipmentSlot var1, ItemStack var2) {
      return (ItemStack)Objects.requireNonNullElse((ItemStack)this.items.put(var1, var2), ItemStack.EMPTY);
   }

   public ItemStack get(EquipmentSlot var1) {
      return (ItemStack)this.items.getOrDefault(var1, ItemStack.EMPTY);
   }

   public boolean isEmpty() {
      Iterator var1 = this.items.values().iterator();

      ItemStack var2;
      do {
         if (!var1.hasNext()) {
            return true;
         }

         var2 = (ItemStack)var1.next();
      } while(var2.isEmpty());

      return false;
   }

   public void tick(Entity var1) {
      Iterator var2 = this.items.entrySet().iterator();

      while(var2.hasNext()) {
         Entry var3 = (Entry)var2.next();
         ItemStack var4 = (ItemStack)var3.getValue();
         if (!var4.isEmpty()) {
            var4.inventoryTick(var1.level(), var1, (EquipmentSlot)var3.getKey());
         }
      }

   }

   public void setAll(EntityEquipment var1) {
      this.items.clear();
      this.items.putAll(var1.items);
   }

   public void dropAll(LivingEntity var1) {
      Iterator var2 = this.items.values().iterator();

      while(var2.hasNext()) {
         ItemStack var3 = (ItemStack)var2.next();
         var1.drop(var3, true, false);
      }

      this.clear();
   }

   public void clear() {
      this.items.replaceAll((var0, var1) -> {
         return ItemStack.EMPTY;
      });
   }

   static {
      CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ItemStack.CODEC).xmap((var0) -> {
         EnumMap var1 = new EnumMap(EquipmentSlot.class);
         var1.putAll(var0);
         return new EntityEquipment(var1);
      }, (var0) -> {
         EnumMap var1 = new EnumMap(var0.items);
         var1.values().removeIf(ItemStack::isEmpty);
         return var1;
      });
   }
}
