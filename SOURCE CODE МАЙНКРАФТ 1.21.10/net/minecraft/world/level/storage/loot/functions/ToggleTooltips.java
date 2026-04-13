package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ToggleTooltips extends LootItemConditionalFunction {
   public static final MapCodec<ToggleTooltips> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return commonFields(var0).and(Codec.unboundedMap(DataComponentType.CODEC, Codec.BOOL).fieldOf("toggles").forGetter((var0x) -> {
         return var0x.values;
      })).apply(var0, ToggleTooltips::new);
   });
   private final Map<DataComponentType<?>, Boolean> values;

   private ToggleTooltips(List<LootItemCondition> var1, Map<DataComponentType<?>, Boolean> var2) {
      super(var1);
      this.values = var2;
   }

   protected ItemStack run(ItemStack var1, LootContext var2) {
      var1.update(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT, (var1x) -> {
         Entry var3;
         boolean var4;
         for(Iterator var2 = this.values.entrySet().iterator(); var2.hasNext(); var1x = var1x.withHidden((DataComponentType)var3.getKey(), !var4)) {
            var3 = (Entry)var2.next();
            var4 = (Boolean)var3.getValue();
         }

         return var1x;
      });
      return var1;
   }

   public LootItemFunctionType<ToggleTooltips> getType() {
      return LootItemFunctions.TOGGLE_TOOLTIPS;
   }
}
