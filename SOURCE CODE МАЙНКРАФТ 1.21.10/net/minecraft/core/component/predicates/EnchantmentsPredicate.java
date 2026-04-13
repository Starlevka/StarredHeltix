package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public abstract class EnchantmentsPredicate implements SingleComponentItemPredicate<ItemEnchantments> {
   private final List<EnchantmentPredicate> enchantments;

   protected EnchantmentsPredicate(List<EnchantmentPredicate> var1) {
      super();
      this.enchantments = var1;
   }

   public static <T extends EnchantmentsPredicate> Codec<T> codec(Function<List<EnchantmentPredicate>, T> var0) {
      return EnchantmentPredicate.CODEC.listOf().xmap(var0, EnchantmentsPredicate::enchantments);
   }

   protected List<EnchantmentPredicate> enchantments() {
      return this.enchantments;
   }

   public boolean matches(ItemEnchantments var1) {
      Iterator var2 = this.enchantments.iterator();

      EnchantmentPredicate var3;
      do {
         if (!var2.hasNext()) {
            return true;
         }

         var3 = (EnchantmentPredicate)var2.next();
      } while(var3.containedIn(var1));

      return false;
   }

   public static EnchantmentsPredicate.Enchantments enchantments(List<EnchantmentPredicate> var0) {
      return new EnchantmentsPredicate.Enchantments(var0);
   }

   public static EnchantmentsPredicate.StoredEnchantments storedEnchantments(List<EnchantmentPredicate> var0) {
      return new EnchantmentsPredicate.StoredEnchantments(var0);
   }

   public static class Enchantments extends EnchantmentsPredicate {
      public static final Codec<EnchantmentsPredicate.Enchantments> CODEC = codec(EnchantmentsPredicate.Enchantments::new);

      protected Enchantments(List<EnchantmentPredicate> var1) {
         super(var1);
      }

      public DataComponentType<ItemEnchantments> componentType() {
         return DataComponents.ENCHANTMENTS;
      }
   }

   public static class StoredEnchantments extends EnchantmentsPredicate {
      public static final Codec<EnchantmentsPredicate.StoredEnchantments> CODEC = codec(EnchantmentsPredicate.StoredEnchantments::new);

      protected StoredEnchantments(List<EnchantmentPredicate> var1) {
         super(var1);
      }

      public DataComponentType<ItemEnchantments> componentType() {
         return DataComponents.STORED_ENCHANTMENTS;
      }
   }
}
