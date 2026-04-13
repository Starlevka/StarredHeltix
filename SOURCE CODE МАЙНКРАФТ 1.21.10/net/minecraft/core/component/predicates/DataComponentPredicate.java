package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public interface DataComponentPredicate {
   Codec<Map<DataComponentPredicate.Type<?>, DataComponentPredicate>> CODEC = Codec.dispatchedMap(BuiltInRegistries.DATA_COMPONENT_PREDICATE_TYPE.byNameCodec(), DataComponentPredicate.Type::codec);
   StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<?>> SINGLE_STREAM_CODEC = ByteBufCodecs.registry(Registries.DATA_COMPONENT_PREDICATE_TYPE).dispatch(DataComponentPredicate.Single::type, DataComponentPredicate.Type::singleStreamCodec);
   StreamCodec<RegistryFriendlyByteBuf, Map<DataComponentPredicate.Type<?>, DataComponentPredicate>> STREAM_CODEC = SINGLE_STREAM_CODEC.apply(ByteBufCodecs.list(64)).map((var0) -> {
      return (Map)var0.stream().collect(Collectors.toMap(DataComponentPredicate.Single::type, DataComponentPredicate.Single::predicate));
   }, (var0) -> {
      return var0.entrySet().stream().map(DataComponentPredicate.Single::fromEntry).toList();
   });

   static MapCodec<DataComponentPredicate.Single<?>> singleCodec(String var0) {
      return BuiltInRegistries.DATA_COMPONENT_PREDICATE_TYPE.byNameCodec().dispatchMap(var0, DataComponentPredicate.Single::type, DataComponentPredicate.Type::wrappedCodec);
   }

   boolean matches(DataComponentGetter var1);

   public static record Single<T extends DataComponentPredicate>(DataComponentPredicate.Type<T> type, T predicate) {
      public Single(DataComponentPredicate.Type<T> param1, T param2) {
         super();
         this.type = var1;
         this.predicate = var2;
      }

      private static <T extends DataComponentPredicate> DataComponentPredicate.Single<T> fromEntry(Entry<DataComponentPredicate.Type<?>, T> var0) {
         return new DataComponentPredicate.Single((DataComponentPredicate.Type)var0.getKey(), (DataComponentPredicate)var0.getValue());
      }

      public DataComponentPredicate.Type<T> type() {
         return this.type;
      }

      public T predicate() {
         return this.predicate;
      }
   }

   public static final class Type<T extends DataComponentPredicate> {
      private final Codec<T> codec;
      private final MapCodec<DataComponentPredicate.Single<T>> wrappedCodec;
      private final StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<T>> singleStreamCodec;

      public Type(Codec<T> var1) {
         super();
         this.codec = var1;
         this.wrappedCodec = RecordCodecBuilder.mapCodec((var2) -> {
            return var2.group(var1.fieldOf("value").forGetter(DataComponentPredicate.Single::predicate)).apply(var2, (var1x) -> {
               return new DataComponentPredicate.Single(this, var1x);
            });
         });
         this.singleStreamCodec = ByteBufCodecs.fromCodecWithRegistries(var1).map((var1x) -> {
            return new DataComponentPredicate.Single(this, var1x);
         }, DataComponentPredicate.Single::predicate);
      }

      public Codec<T> codec() {
         return this.codec;
      }

      public MapCodec<DataComponentPredicate.Single<T>> wrappedCodec() {
         return this.wrappedCodec;
      }

      public StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<T>> singleStreamCodec() {
         return this.singleStreamCodec;
      }
   }
}
