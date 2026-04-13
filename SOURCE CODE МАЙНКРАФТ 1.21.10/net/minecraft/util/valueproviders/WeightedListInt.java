package net.minecraft.util.valueproviders;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;

public class WeightedListInt extends IntProvider {
   public static final MapCodec<WeightedListInt> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(WeightedList.nonEmptyCodec(IntProvider.CODEC).fieldOf("distribution").forGetter((var0x) -> {
         return var0x.distribution;
      })).apply(var0, WeightedListInt::new);
   });
   private final WeightedList<IntProvider> distribution;
   private final int minValue;
   private final int maxValue;

   public WeightedListInt(WeightedList<IntProvider> var1) {
      super();
      this.distribution = var1;
      int var2 = 2147483647;
      int var3 = -2147483648;

      int var7;
      for(Iterator var4 = var1.unwrap().iterator(); var4.hasNext(); var3 = Math.max(var3, var7)) {
         Weighted var5 = (Weighted)var4.next();
         int var6 = ((IntProvider)var5.value()).getMinValue();
         var7 = ((IntProvider)var5.value()).getMaxValue();
         var2 = Math.min(var2, var6);
      }

      this.minValue = var2;
      this.maxValue = var3;
   }

   public int sample(RandomSource var1) {
      return ((IntProvider)this.distribution.getRandomOrThrow(var1)).sample(var1);
   }

   public int getMinValue() {
      return this.minValue;
   }

   public int getMaxValue() {
      return this.maxValue;
   }

   public IntProviderType<?> getType() {
      return IntProviderType.WEIGHTED_LIST;
   }
}
