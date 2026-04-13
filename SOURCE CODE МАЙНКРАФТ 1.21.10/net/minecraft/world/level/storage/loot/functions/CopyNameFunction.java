package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyNameFunction extends LootItemConditionalFunction {
   private static final ExtraCodecs.LateBoundIdMapper<String, CopyNameFunction.Source> SOURCES = new ExtraCodecs.LateBoundIdMapper();
   public static final MapCodec<CopyNameFunction> CODEC;
   private final CopyNameFunction.Source source;

   private CopyNameFunction(List<LootItemCondition> var1, CopyNameFunction.Source var2) {
      super(var1);
      this.source = var2;
   }

   public LootItemFunctionType<CopyNameFunction> getType() {
      return LootItemFunctions.COPY_NAME;
   }

   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(this.source.param);
   }

   public ItemStack run(ItemStack var1, LootContext var2) {
      Object var3 = var2.getOptionalParameter(this.source.param);
      if (var3 instanceof Nameable) {
         Nameable var4 = (Nameable)var3;
         var1.set(DataComponents.CUSTOM_NAME, var4.getCustomName());
      }

      return var1;
   }

   public static LootItemConditionalFunction.Builder<?> copyName(CopyNameFunction.Source var0) {
      return simpleBuilder((var1) -> {
         return new CopyNameFunction(var1, var0);
      });
   }

   static {
      LootContext.EntityTarget[] var0 = LootContext.EntityTarget.values();
      int var1 = var0.length;

      int var2;
      for(var2 = 0; var2 < var1; ++var2) {
         LootContext.EntityTarget var3 = var0[var2];
         SOURCES.put(var3.getSerializedName(), new CopyNameFunction.Source(var3.getParam()));
      }

      LootContext.BlockEntityTarget[] var4 = LootContext.BlockEntityTarget.values();
      var1 = var4.length;

      for(var2 = 0; var2 < var1; ++var2) {
         LootContext.BlockEntityTarget var5 = var4[var2];
         SOURCES.put(var5.getSerializedName(), new CopyNameFunction.Source(var5.getParam()));
      }

      CODEC = RecordCodecBuilder.mapCodec((var0x) -> {
         return commonFields(var0x).and(SOURCES.codec(Codec.STRING).fieldOf("source").forGetter((var0xx) -> {
            return var0xx.source;
         })).apply(var0x, CopyNameFunction::new);
      });
   }

   public static record Source(ContextKey<?> param) {
      final ContextKey<?> param;

      public Source(ContextKey<?> param1) {
         super();
         this.param = var1;
      }

      public ContextKey<?> param() {
         return this.param;
      }
   }
}
