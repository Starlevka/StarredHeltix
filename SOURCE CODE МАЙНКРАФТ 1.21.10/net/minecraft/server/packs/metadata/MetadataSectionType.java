package net.minecraft.server.packs.metadata;

import com.mojang.serialization.Codec;
import java.util.Optional;

public record MetadataSectionType<T>(String name, Codec<T> codec) {
   public MetadataSectionType(String param1, Codec<T> param2) {
      super();
      this.name = var1;
      this.codec = var2;
   }

   public MetadataSectionType.WithValue<T> withValue(T var1) {
      return new MetadataSectionType.WithValue(this, var1);
   }

   public String name() {
      return this.name;
   }

   public Codec<T> codec() {
      return this.codec;
   }

   public static record WithValue<T>(MetadataSectionType<T> type, T value) {
      public WithValue(MetadataSectionType<T> param1, T param2) {
         super();
         this.type = var1;
         this.value = var2;
      }

      public <U> Optional<U> unwrapToType(MetadataSectionType<U> var1) {
         return var1 == this.type ? Optional.of(this.value) : Optional.empty();
      }

      public MetadataSectionType<T> type() {
         return this.type;
      }

      public T value() {
         return this.value;
      }
   }
}
