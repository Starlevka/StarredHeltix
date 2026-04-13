package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyComponentsFunction extends LootItemConditionalFunction {
   private static final ExtraCodecs.LateBoundIdMapper<String, CopyComponentsFunction.Source<?>> SOURCES = new ExtraCodecs.LateBoundIdMapper();
   public static final MapCodec<CopyComponentsFunction> CODEC;
   private final CopyComponentsFunction.Source<?> source;
   private final Optional<List<DataComponentType<?>>> include;
   private final Optional<List<DataComponentType<?>>> exclude;
   private final Predicate<DataComponentType<?>> bakedPredicate;

   CopyComponentsFunction(List<LootItemCondition> var1, CopyComponentsFunction.Source<?> var2, Optional<List<DataComponentType<?>>> var3, Optional<List<DataComponentType<?>>> var4) {
      super(var1);
      this.source = var2;
      this.include = var3.map(List::copyOf);
      this.exclude = var4.map(List::copyOf);
      ArrayList var5 = new ArrayList(2);
      var4.ifPresent((var1x) -> {
         var5.add((var1) -> {
            return !var1x.contains(var1);
         });
      });
      var3.ifPresent((var1x) -> {
         Objects.requireNonNull(var1x);
         var5.add(var1x::contains);
      });
      this.bakedPredicate = Util.allOf((List)var5);
   }

   public LootItemFunctionType<CopyComponentsFunction> getType() {
      return LootItemFunctions.COPY_COMPONENTS;
   }

   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(this.source.contextParam());
   }

   public ItemStack run(ItemStack var1, LootContext var2) {
      DataComponentGetter var3 = this.source.get(var2);
      if (var3 != null) {
         if (var3 instanceof DataComponentMap) {
            DataComponentMap var4 = (DataComponentMap)var3;
            var1.applyComponents(var4.filter(this.bakedPredicate));
         } else {
            Collection var5 = (Collection)this.exclude.orElse(List.of());
            ((Stream)this.include.map(Collection::stream).orElse(BuiltInRegistries.DATA_COMPONENT_TYPE.listElements().map(Holder::value))).forEach((var3x) -> {
               if (!var5.contains(var3x)) {
                  TypedDataComponent var4 = var3.getTyped(var3x);
                  if (var4 != null) {
                     var1.set(var4);
                  }

               }
            });
         }
      }

      return var1;
   }

   public static CopyComponentsFunction.Builder copyComponentsFromEntity(ContextKey<? extends Entity> var0) {
      return new CopyComponentsFunction.Builder(new CopyComponentsFunction.EntitySource(var0));
   }

   public static CopyComponentsFunction.Builder copyComponentsFromBlockEntity(ContextKey<? extends BlockEntity> var0) {
      return new CopyComponentsFunction.Builder(new CopyComponentsFunction.BlockEntitySource(var0));
   }

   static {
      LootContext.EntityTarget[] var0 = LootContext.EntityTarget.values();
      int var1 = var0.length;

      int var2;
      for(var2 = 0; var2 < var1; ++var2) {
         LootContext.EntityTarget var3 = var0[var2];
         SOURCES.put(var3.getSerializedName(), new CopyComponentsFunction.EntitySource(var3.getParam()));
      }

      LootContext.BlockEntityTarget[] var4 = LootContext.BlockEntityTarget.values();
      var1 = var4.length;

      for(var2 = 0; var2 < var1; ++var2) {
         LootContext.BlockEntityTarget var6 = var4[var2];
         SOURCES.put(var6.getSerializedName(), new CopyComponentsFunction.BlockEntitySource(var6.getParam()));
      }

      LootContext.ItemStackTarget[] var5 = LootContext.ItemStackTarget.values();
      var1 = var5.length;

      for(var2 = 0; var2 < var1; ++var2) {
         LootContext.ItemStackTarget var7 = var5[var2];
         SOURCES.put(var7.getSerializedName(), new CopyComponentsFunction.ItemStackSource(var7.getParam()));
      }

      CODEC = RecordCodecBuilder.mapCodec((var0x) -> {
         return commonFields(var0x).and(var0x.group(SOURCES.codec(Codec.STRING).fieldOf("source").forGetter((var0xx) -> {
            return var0xx.source;
         }), DataComponentType.CODEC.listOf().optionalFieldOf("include").forGetter((var0xx) -> {
            return var0xx.include;
         }), DataComponentType.CODEC.listOf().optionalFieldOf("exclude").forGetter((var0xx) -> {
            return var0xx.exclude;
         }))).apply(var0x, CopyComponentsFunction::new);
      });
   }

   public interface Source<T> {
      ContextKey<? extends T> contextParam();

      DataComponentGetter get(T var1);

      @Nullable
      default DataComponentGetter get(LootContext var1) {
         Object var2 = var1.getOptionalParameter(this.contextParam());
         return var2 != null ? this.get(var2) : null;
      }
   }

   public static class Builder extends LootItemConditionalFunction.Builder<CopyComponentsFunction.Builder> {
      private final CopyComponentsFunction.Source<?> source;
      private Optional<com.google.common.collect.ImmutableList.Builder<DataComponentType<?>>> include = Optional.empty();
      private Optional<com.google.common.collect.ImmutableList.Builder<DataComponentType<?>>> exclude = Optional.empty();

      Builder(CopyComponentsFunction.Source<?> var1) {
         super();
         this.source = var1;
      }

      public CopyComponentsFunction.Builder include(DataComponentType<?> var1) {
         if (this.include.isEmpty()) {
            this.include = Optional.of(ImmutableList.builder());
         }

         ((com.google.common.collect.ImmutableList.Builder)this.include.get()).add(var1);
         return this;
      }

      public CopyComponentsFunction.Builder exclude(DataComponentType<?> var1) {
         if (this.exclude.isEmpty()) {
            this.exclude = Optional.of(ImmutableList.builder());
         }

         ((com.google.common.collect.ImmutableList.Builder)this.exclude.get()).add(var1);
         return this;
      }

      protected CopyComponentsFunction.Builder getThis() {
         return this;
      }

      public LootItemFunction build() {
         return new CopyComponentsFunction(this.getConditions(), this.source, this.include.map(com.google.common.collect.ImmutableList.Builder::build), this.exclude.map(com.google.common.collect.ImmutableList.Builder::build));
      }

      // $FF: synthetic method
      protected LootItemConditionalFunction.Builder getThis() {
         return this.getThis();
      }
   }

   private static record EntitySource(ContextKey<? extends Entity> contextParam) implements CopyComponentsFunction.Source<Entity> {
      EntitySource(ContextKey<? extends Entity> param1) {
         super();
         this.contextParam = var1;
      }

      public DataComponentGetter get(Entity var1) {
         return var1;
      }

      public ContextKey<? extends Entity> contextParam() {
         return this.contextParam;
      }
   }

   private static record BlockEntitySource(ContextKey<? extends BlockEntity> contextParam) implements CopyComponentsFunction.Source<BlockEntity> {
      BlockEntitySource(ContextKey<? extends BlockEntity> param1) {
         super();
         this.contextParam = var1;
      }

      public DataComponentGetter get(BlockEntity var1) {
         return var1.collectComponents();
      }

      public ContextKey<? extends BlockEntity> contextParam() {
         return this.contextParam;
      }
   }

   private static record ItemStackSource(ContextKey<? extends ItemStack> contextParam) implements CopyComponentsFunction.Source<ItemStack> {
      ItemStackSource(ContextKey<? extends ItemStack> param1) {
         super();
         this.contextParam = var1;
      }

      public DataComponentGetter get(ItemStack var1) {
         return var1.getComponents();
      }

      public ContextKey<? extends ItemStack> contextParam() {
         return this.contextParam;
      }
   }
}
