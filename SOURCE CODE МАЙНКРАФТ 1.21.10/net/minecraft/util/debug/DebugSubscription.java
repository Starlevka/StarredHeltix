package net.minecraft.util.debug;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class DebugSubscription<T> {
   public static final int DOES_NOT_EXPIRE = 0;
   @Nullable
   final StreamCodec<? super RegistryFriendlyByteBuf, T> valueStreamCodec;
   private final int expireAfterTicks;

   public DebugSubscription(@Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> var1, int var2) {
      super();
      this.valueStreamCodec = var1;
      this.expireAfterTicks = var2;
   }

   public DebugSubscription(@Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> var1) {
      this(var1, 0);
   }

   public DebugSubscription.Update<T> packUpdate(@Nullable T var1) {
      return new DebugSubscription.Update(this, Optional.ofNullable(var1));
   }

   public DebugSubscription.Update<T> emptyUpdate() {
      return new DebugSubscription.Update(this, Optional.empty());
   }

   public DebugSubscription.Event<T> packEvent(T var1) {
      return new DebugSubscription.Event(this, var1);
   }

   public String toString() {
      return Util.getRegisteredName(BuiltInRegistries.DEBUG_SUBSCRIPTION, this);
   }

   @Nullable
   public StreamCodec<? super RegistryFriendlyByteBuf, T> valueStreamCodec() {
      return this.valueStreamCodec;
   }

   public int expireAfterTicks() {
      return this.expireAfterTicks;
   }

   public static record Update<T>(DebugSubscription<T> subscription, Optional<T> value) {
      public static final StreamCodec<RegistryFriendlyByteBuf, DebugSubscription.Update<?>> STREAM_CODEC;

      public Update(DebugSubscription<T> param1, Optional<T> param2) {
         super();
         this.subscription = var1;
         this.value = var2;
      }

      private static <T> StreamCodec<? super RegistryFriendlyByteBuf, DebugSubscription.Update<T>> streamCodec(DebugSubscription<T> var0) {
         return ByteBufCodecs.optional((StreamCodec)Objects.requireNonNull(var0.valueStreamCodec)).map((var1) -> {
            return new DebugSubscription.Update(var0, var1);
         }, DebugSubscription.Update::value);
      }

      public DebugSubscription<T> subscription() {
         return this.subscription;
      }

      public Optional<T> value() {
         return this.value;
      }

      static {
         STREAM_CODEC = ByteBufCodecs.registry(Registries.DEBUG_SUBSCRIPTION).dispatch(DebugSubscription.Update::subscription, DebugSubscription.Update::streamCodec);
      }
   }

   public static record Event<T>(DebugSubscription<T> subscription, T value) {
      public static final StreamCodec<RegistryFriendlyByteBuf, DebugSubscription.Event<?>> STREAM_CODEC;

      public Event(DebugSubscription<T> param1, T param2) {
         super();
         this.subscription = var1;
         this.value = var2;
      }

      private static <T> StreamCodec<? super RegistryFriendlyByteBuf, DebugSubscription.Event<T>> streamCodec(DebugSubscription<T> var0) {
         return ((StreamCodec)Objects.requireNonNull(var0.valueStreamCodec)).map((var1) -> {
            return new DebugSubscription.Event(var0, var1);
         }, DebugSubscription.Event::value);
      }

      public DebugSubscription<T> subscription() {
         return this.subscription;
      }

      public T value() {
         return this.value;
      }

      static {
         STREAM_CODEC = ByteBufCodecs.registry(Registries.DEBUG_SUBSCRIPTION).dispatch(DebugSubscription.Event::subscription, DebugSubscription.Event::streamCodec);
      }
   }
}
