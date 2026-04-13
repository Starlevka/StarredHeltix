package net.minecraft.server.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.Locale;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.ParamInfo;
import net.minecraft.server.jsonrpc.api.ResultInfo;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.EncodeJsonRpcException;
import net.minecraft.server.jsonrpc.methods.IllegalMethodDefinitionException;
import net.minecraft.server.jsonrpc.methods.InvalidParameterJsonRpcException;

public interface IncomingRpcMethod {
   MethodInfo info();

   IncomingRpcMethod.Attributes attributes();

   JsonElement apply(MinecraftApi var1, @Nullable JsonElement var2, ClientInfo var3);

   static <Result> IncomingRpcMethod.IncomingRpcMethodBuilder<IncomingRpcMethod.ParameterlessMethod<Result>> method(IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> var0, Codec<Result> var1) {
      return new IncomingRpcMethod.IncomingRpcMethodBuilder((var2, var3) -> {
         if (var2.params().isPresent()) {
            throw new IllegalMethodDefinitionException("Method defined as not having parameters but is describing them");
         } else if (var2.result().isEmpty()) {
            throw new IllegalMethodDefinitionException("Method lacks result");
         } else {
            return new IncomingRpcMethod.ParameterlessMethod(var2, var3, var1, var0);
         }
      });
   }

   static <Params, Result> IncomingRpcMethod.IncomingRpcMethodBuilder<IncomingRpcMethod.Method<Params, Result>> method(IncomingRpcMethod.RpcMethodFunction<Params, Result> var0, Codec<Params> var1, Codec<Result> var2) {
      return new IncomingRpcMethod.IncomingRpcMethodBuilder((var3, var4) -> {
         if (var3.params().isEmpty()) {
            throw new IllegalMethodDefinitionException("Method defined as having parameters without describing them");
         } else if (var3.result().isEmpty()) {
            throw new IllegalMethodDefinitionException("Method lacks result");
         } else {
            return new IncomingRpcMethod.Method(var3, var4, var1, var2, var0);
         }
      });
   }

   static <Result> IncomingRpcMethod.IncomingRpcMethodBuilder<IncomingRpcMethod.ParameterlessMethod<Result>> method(Function<MinecraftApi, Result> var0, Codec<Result> var1) {
      return new IncomingRpcMethod.IncomingRpcMethodBuilder((var2, var3) -> {
         if (var2.params().isPresent()) {
            throw new IllegalMethodDefinitionException("Method defined as not having parameters but is describing them");
         } else if (var2.result().isEmpty()) {
            throw new IllegalMethodDefinitionException("Method lacks result");
         } else {
            return new IncomingRpcMethod.ParameterlessMethod(var2, var3, var1, (var1x, var2x) -> {
               return var0.apply(var1x);
            });
         }
      });
   }

   public static class IncomingRpcMethodBuilder<T extends IncomingRpcMethod> {
      private final IncomingRpcMethod.Factory<T> method;
      private String description = "";
      @Nullable
      private ParamInfo paramInfo;
      @Nullable
      private ResultInfo resultInfo;
      private boolean discoverable = true;
      private boolean runOnMainThread = true;

      public IncomingRpcMethodBuilder(IncomingRpcMethod.Factory<T> var1) {
         super();
         this.method = var1;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<T> description(String var1) {
         this.description = var1;
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<T> response(ResultInfo var1) {
         this.resultInfo = var1;
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<T> param(ParamInfo var1) {
         this.paramInfo = var1;
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<T> undiscoverable() {
         this.discoverable = false;
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<T> notOnMainThread() {
         this.runOnMainThread = false;
         return this;
      }

      public T build() {
         MethodInfo var1 = new MethodInfo(this.description, this.paramInfo, this.resultInfo);
         return this.method.create(var1, new IncomingRpcMethod.Attributes(this.runOnMainThread, this.discoverable));
      }

      public T register(Registry<IncomingRpcMethod> var1, String var2) {
         return this.register(var1, ResourceLocation.withDefaultNamespace(var2));
      }

      private T register(Registry<IncomingRpcMethod> var1, ResourceLocation var2) {
         return (IncomingRpcMethod)Registry.register(var1, (ResourceLocation)var2, this.build());
      }
   }

   @FunctionalInterface
   public interface ParameterlessRpcMethodFunction<Result> {
      Result apply(MinecraftApi var1, ClientInfo var2);
   }

   @FunctionalInterface
   public interface Factory<T extends IncomingRpcMethod> {
      T create(MethodInfo var1, IncomingRpcMethod.Attributes var2);
   }

   @FunctionalInterface
   public interface RpcMethodFunction<Params, Result> {
      Result apply(MinecraftApi var1, Params var2, ClientInfo var3);
   }

   public static record ParameterlessMethod<Result>(MethodInfo info, IncomingRpcMethod.Attributes attributes, Codec<Result> resultCodec, IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> supplier) implements IncomingRpcMethod {
      public ParameterlessMethod(MethodInfo param1, IncomingRpcMethod.Attributes param2, Codec<Result> param3, IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> param4) {
         super();
         this.info = var1;
         this.attributes = var2;
         this.resultCodec = var3;
         this.supplier = var4;
      }

      public JsonElement apply(MinecraftApi var1, @Nullable JsonElement var2, ClientInfo var3) {
         if (var2 == null || var2.isJsonArray() && var2.getAsJsonArray().isEmpty()) {
            if (this.info.params().isPresent()) {
               throw new IllegalArgumentException("Method defined as not having parameters but is describing them");
            } else {
               Object var4 = this.supplier.apply(var1, var3);
               return (JsonElement)this.resultCodec.encodeStart(JsonOps.INSTANCE, var4).getOrThrow(InvalidParameterJsonRpcException::new);
            }
         } else {
            throw new InvalidParameterJsonRpcException("Expected no params, or an empty array");
         }
      }

      public MethodInfo info() {
         return this.info;
      }

      public IncomingRpcMethod.Attributes attributes() {
         return this.attributes;
      }

      public Codec<Result> resultCodec() {
         return this.resultCodec;
      }

      public IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> supplier() {
         return this.supplier;
      }
   }

   public static record Attributes(boolean runOnMainThread, boolean discoverable) {
      public Attributes(boolean param1, boolean param2) {
         super();
         this.runOnMainThread = var1;
         this.discoverable = var2;
      }

      public boolean runOnMainThread() {
         return this.runOnMainThread;
      }

      public boolean discoverable() {
         return this.discoverable;
      }
   }

   public static record Method<Params, Result>(MethodInfo info, IncomingRpcMethod.Attributes attributes, Codec<Params> paramsCodec, Codec<Result> resultCodec, IncomingRpcMethod.RpcMethodFunction<Params, Result> function) implements IncomingRpcMethod {
      public Method(MethodInfo param1, IncomingRpcMethod.Attributes param2, Codec<Params> param3, Codec<Result> param4, IncomingRpcMethod.RpcMethodFunction<Params, Result> param5) {
         super();
         this.info = var1;
         this.attributes = var2;
         this.paramsCodec = var3;
         this.resultCodec = var4;
         this.function = var5;
      }

      public JsonElement apply(MinecraftApi var1, @Nullable JsonElement var2, ClientInfo var3) {
         if (var2 != null && (var2.isJsonArray() || var2.isJsonObject())) {
            if (this.info.params().isEmpty()) {
               throw new IllegalArgumentException("Method defined as having parameters without describing them");
            } else {
               JsonElement var4;
               if (var2.isJsonObject()) {
                  String var5 = ((ParamInfo)this.info.params().get()).name();
                  JsonElement var6 = var2.getAsJsonObject().get(var5);
                  if (var6 == null) {
                     throw new InvalidParameterJsonRpcException(String.format(Locale.ROOT, "Params passed by-name, but expected param [%s] does not exist", var5));
                  }

                  var4 = var6;
               } else {
                  JsonArray var7 = var2.getAsJsonArray();
                  if (var7.isEmpty() || var7.size() > 1) {
                     throw new InvalidParameterJsonRpcException("Expected exactly one element in the params array");
                  }

                  var4 = var7.get(0);
               }

               Object var8 = this.paramsCodec.parse(JsonOps.INSTANCE, var4).getOrThrow(InvalidParameterJsonRpcException::new);
               Object var9 = this.function.apply(var1, var8, var3);
               return (JsonElement)this.resultCodec.encodeStart(JsonOps.INSTANCE, var9).getOrThrow(EncodeJsonRpcException::new);
            }
         } else {
            throw new InvalidParameterJsonRpcException("Expected params as array or named");
         }
      }

      public MethodInfo info() {
         return this.info;
      }

      public IncomingRpcMethod.Attributes attributes() {
         return this.attributes;
      }

      public Codec<Params> paramsCodec() {
         return this.paramsCodec;
      }

      public Codec<Result> resultCodec() {
         return this.resultCodec;
      }

      public IncomingRpcMethod.RpcMethodFunction<Params, Result> function() {
         return this.function;
      }
   }
}
