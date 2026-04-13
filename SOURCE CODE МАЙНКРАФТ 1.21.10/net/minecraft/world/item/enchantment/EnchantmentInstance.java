package net.minecraft.world.item.enchantment;

import net.minecraft.core.Holder;

public record EnchantmentInstance(Holder<Enchantment> enchantment, int level) {
   public EnchantmentInstance(Holder<Enchantment> param1, int param2) {
      super();
      this.enchantment = var1;
      this.level = var2;
   }

   public int weight() {
      return ((Enchantment)this.enchantment().value()).getWeight();
   }

   public Holder<Enchantment> enchantment() {
      return this.enchantment;
   }

   public int level() {
      return this.level;
   }
}
