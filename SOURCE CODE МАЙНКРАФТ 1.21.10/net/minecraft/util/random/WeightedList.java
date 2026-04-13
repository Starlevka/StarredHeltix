package net.minecraft.util.random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

public final class WeightedList<E> {
   private static final int FLAT_THRESHOLD = 64;
   private final int totalWeight;
   private final List<Weighted<E>> items;
   @Nullable
   private final WeightedList.Selector<E> selector;

   WeightedList(List<? extends Weighted<E>> var1) {
      super();
      this.items = List.copyOf(var1);
      this.totalWeight = WeightedRandom.getTotalWeight(var1, Weighted::weight);
      if (this.totalWeight == 0) {
         this.selector = null;
      } else if (this.totalWeight < 64) {
         this.selector = new WeightedList.Flat(this.items, this.totalWeight);
      } else {
         this.selector = new WeightedList.Compact(this.items);
      }

   }

   public static <E> WeightedList<E> of() {
      return new WeightedList(List.of());
   }

   public static <E> WeightedList<E> of(E var0) {
      return new WeightedList(List.of(new Weighted(var0, 1)));
   }

   @SafeVarargs
   public static <E> WeightedList<E> of(Weighted<E>... var0) {
      return new WeightedList(List.of(var0));
   }

   public static <E> WeightedList<E> of(List<Weighted<E>> var0) {
      return new WeightedList(var0);
   }

   public static <E> WeightedList.Builder<E> builder() {
      return new WeightedList.Builder();
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }

   public <T> WeightedList<T> map(Function<E, T> var1) {
      return new WeightedList(Lists.transform(this.items, (var1x) -> {
         return var1x.map(var1);
      }));
   }

   public Optional<E> getRandom(RandomSource var1) {
      if (this.selector == null) {
         return Optional.empty();
      } else {
         int var2 = var1.nextInt(this.totalWeight);
         return Optional.of(this.selector.get(var2));
      }
   }

   public E getRandomOrThrow(RandomSource var1) {
      if (this.selector == null) {
         throw new IllegalStateException("Weighted list has no elements");
      } else {
         int var2 = var1.nextInt(this.totalWeight);
         return this.selector.get(var2);
      }
   }

   public List<Weighted<E>> unwrap() {
      return this.items;
   }

   public static <E> Codec<WeightedList<E>> codec(Codec<E> var0) {
      return Weighted.codec(var0).listOf().xmap(WeightedList::of, WeightedList::unwrap);
   }

   public static <E> Codec<WeightedList<E>> codec(MapCodec<E> var0) {
      return Weighted.codec(var0).listOf().xmap(WeightedList::of, WeightedList::unwrap);
   }

   public static <E> Codec<WeightedList<E>> nonEmptyCodec(Codec<E> var0) {
      return ExtraCodecs.nonEmptyList(Weighted.codec(var0).listOf()).xmap(WeightedList::of, WeightedList::unwrap);
   }

   public static <E> Codec<WeightedList<E>> nonEmptyCodec(MapCodec<E> var0) {
      return ExtraCodecs.nonEmptyList(Weighted.codec(var0).listOf()).xmap(WeightedList::of, WeightedList::unwrap);
   }

   public static <E, B extends ByteBuf> StreamCodec<B, WeightedList<E>> streamCodec(StreamCodec<B, E> var0) {
      return Weighted.streamCodec(var0).apply(ByteBufCodecs.list()).map(WeightedList::of, WeightedList::unwrap);
   }

   public boolean contains(E var1) {
      Iterator var2 = this.items.iterator();

      Weighted var3;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         var3 = (Weighted)var2.next();
      } while(!var3.value().equals(var1));

      return true;
   }

   public boolean equals(@Nullable Object var1) {
      if (this == var1) {
         return true;
      } else if (!(var1 instanceof WeightedList)) {
         return false;
      } else {
         WeightedList var2 = (WeightedList)var1;
         return this.totalWeight == var2.totalWeight && Objects.equals(this.items, var2.items);
      }
   }

   public int hashCode() {
      int var1 = this.totalWeight;
      var1 = 31 * var1 + this.items.hashCode();
      return var1;
   }

   private interface Selector<E> {
      E get(int var1);
   }

   static class Flat<E> implements WeightedList.Selector<E> {
      private final Object[] entries;

      Flat(List<Weighted<E>> var1, int var2) {
         super();
         this.entries = new Object[var2];
         int var3 = 0;

         int var6;
         for(Iterator var4 = var1.iterator(); var4.hasNext(); var3 += var6) {
            Weighted var5 = (Weighted)var4.next();
            var6 = var5.weight();
            Arrays.fill(this.entries, var3, var3 + var6, var5.value());
         }

      }

      public E get(int var1) {
         return this.entries[var1];
      }
   }

   static class Compact<E> implements WeightedList.Selector<E> {
      private final Weighted<?>[] entries;

      Compact(List<Weighted<E>> var1) {
         super();
         this.entries = (Weighted[])var1.toArray((var0) -> {
            return new Weighted[var0];
         });
      }

      public E get(int var1) {
         Weighted[] var2 = this.entries;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Weighted var5 = var2[var4];
            var1 -= var5.weight();
            if (var1 < 0) {
               return var5.value();
            }
         }

         throw new IllegalStateException(var1 + " exceeded total weight");
      }
   }

   public static class Builder<E> {
      private final com.google.common.collect.ImmutableList.Builder<Weighted<E>> result = ImmutableList.builder();

      public Builder() {
         super();
      }

      public WeightedList.Builder<E> add(E var1) {
         return this.add(var1, 1);
      }

      public WeightedList.Builder<E> add(E var1, int var2) {
         this.result.add(new Weighted(var1, var2));
         return this;
      }

      public WeightedList<E> build() {
         return new WeightedList(this.result.build());
      }
   }
}
