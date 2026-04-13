package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class TradeRebalanceEnchantmentTagsProvider extends KeyTagProvider<Enchantment> {
   public TradeRebalanceEnchantmentTagsProvider(PackOutput var1, CompletableFuture<HolderLookup.Provider> var2) {
      super(var1, Registries.ENCHANTMENT, var2);
   }

   protected void addTags(HolderLookup.Provider var1) {
      this.tag(EnchantmentTags.TRADES_DESERT_COMMON).add((Object[])(Enchantments.FIRE_PROTECTION, Enchantments.THORNS, Enchantments.INFINITY));
      this.tag(EnchantmentTags.TRADES_JUNGLE_COMMON).add((Object[])(Enchantments.FEATHER_FALLING, Enchantments.PROJECTILE_PROTECTION, Enchantments.POWER));
      this.tag(EnchantmentTags.TRADES_PLAINS_COMMON).add((Object[])(Enchantments.PUNCH, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS));
      this.tag(EnchantmentTags.TRADES_SAVANNA_COMMON).add((Object[])(Enchantments.KNOCKBACK, Enchantments.BINDING_CURSE, Enchantments.SWEEPING_EDGE));
      this.tag(EnchantmentTags.TRADES_SNOW_COMMON).add((Object[])(Enchantments.AQUA_AFFINITY, Enchantments.LOOTING, Enchantments.FROST_WALKER));
      this.tag(EnchantmentTags.TRADES_SWAMP_COMMON).add((Object[])(Enchantments.DEPTH_STRIDER, Enchantments.RESPIRATION, Enchantments.VANISHING_CURSE));
      this.tag(EnchantmentTags.TRADES_TAIGA_COMMON).add((Object[])(Enchantments.BLAST_PROTECTION, Enchantments.FIRE_ASPECT, Enchantments.FLAME));
      this.tag(EnchantmentTags.TRADES_DESERT_SPECIAL).add((Object)Enchantments.EFFICIENCY);
      this.tag(EnchantmentTags.TRADES_JUNGLE_SPECIAL).add((Object)Enchantments.UNBREAKING);
      this.tag(EnchantmentTags.TRADES_PLAINS_SPECIAL).add((Object)Enchantments.PROTECTION);
      this.tag(EnchantmentTags.TRADES_SAVANNA_SPECIAL).add((Object)Enchantments.SHARPNESS);
      this.tag(EnchantmentTags.TRADES_SNOW_SPECIAL).add((Object)Enchantments.SILK_TOUCH);
      this.tag(EnchantmentTags.TRADES_SWAMP_SPECIAL).add((Object)Enchantments.MENDING);
      this.tag(EnchantmentTags.TRADES_TAIGA_SPECIAL).add((Object)Enchantments.FORTUNE);
   }
}
