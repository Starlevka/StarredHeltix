package net.minecraft.world.level.saveddata;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.util.datafix.DataFixTypes;

public record SavedDataType<T extends SavedData>(String id, Function<SavedData.Context, T> constructor, Function<SavedData.Context, Codec<T>> codec, DataFixTypes dataFixType) {
   public SavedDataType(String var1, Supplier<T> var2, Codec<T> var3, DataFixTypes var4) {
      this(var1, (var1x) -> {
         return (SavedData)var2.get();
      }, (var1x) -> {
         return var3;
      }, var4);
   }

   public SavedDataType(String param1, Function<SavedData.Context, T> param2, Function<SavedData.Context, Codec<T>> param3, DataFixTypes param4) {
      super();
      this.id = var1;
      this.constructor = var2;
      this.codec = var3;
      this.dataFixType = var4;
   }

   public boolean equals(Object var1) {
      boolean var10000;
      if (var1 instanceof SavedDataType) {
         SavedDataType var2 = (SavedDataType)var1;
         if (this.id.equals(var2.id)) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }

   public int hashCode() {
      return this.id.hashCode();
   }

   public String toString() {
      return "SavedDataType[" + this.id + "]";
   }

   public String id() {
      return this.id;
   }

   public Function<SavedData.Context, T> constructor() {
      return this.constructor;
   }

   public Function<SavedData.Context, Codec<T>> codec() {
      return this.codec;
   }

   public DataFixTypes dataFixType() {
      return this.dataFixType;
   }
}
