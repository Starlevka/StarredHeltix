package net.minecraft.server.jsonrpc;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.ParamInfo;
import net.minecraft.server.jsonrpc.api.ResultInfo;
import net.minecraft.server.jsonrpc.methods.IllegalMethodDefinitionException;

public interface OutgoingRpcMethod<Params, Result> {
   String NOTIFICATION_PREFIX = "notification/";

   MethodInfo info();

   OutgoingRpcMethod.Attributes attributes();

   @Nullable
   default JsonElement encodeParams(Params var1) {
      return null;
   }

   @Nullable
   default Result decodeResult(JsonElement var1) {
      return null;
   }

   static OutgoingRpcMethod.OutgoingRpcMethodBuilder<OutgoingRpcMethod.ParmeterlessNotification> notification() {
      return new OutgoingRpcMethod.OutgoingRpcMethodBuilder((var0, var1) -> {
         if (var0.params().isPresent()) {
            throw new IllegalMethodDefinitionException("Method defined as not having parameters but is describing them");
         } else if (var0.result().isPresent()) {
            throw new IllegalMethodDefinitionException("Method defined as not having result but is describing it");
         } else {
            return new OutgoingRpcMethod.ParmeterlessNotification(var0, var1);
         }
      });
   }

   static <Params> OutgoingRpcMethod.OutgoingRpcMethodBuilder<OutgoingRpcMethod.Notification<Params>> notification(Codec<Params> var0) {
      return new OutgoingRpcMethod.OutgoingRpcMethodBuilder((var1, var2) -> {
         if (var1.params().isEmpty()) {
            throw new IllegalMethodDefinitionException("Method defined as having parameters without describing them");
         } else if (var1.result().isPresent()) {
            throw new IllegalMethodDefinitionException("Method defined as not having result but is describing it");
         } else {
            return new OutgoingRpcMethod.Notification(var1, var2, var0);
         }
      });
   }

   static <Result> OutgoingRpcMethod.OutgoingRpcMethodBuilder<OutgoingRpcMethod.ParameterlessMethod<Result>> request(Codec<Result> var0) {
      return new OutgoingRpcMethod.OutgoingRpcMethodBuilder((var1, var2) -> {
         if (var1.params().isPresent()) {
            throw new IllegalMethodDefinitionException("Method defined as not having parameters but is describing them");
         } else if (var1.result().isEmpty()) {
            throw new IllegalMethodDefinitionException("Method lacks result");
         } else {
            return new OutgoingRpcMethod.ParameterlessMethod(var1, var2, var0);
         }
      });
   }

   static <Params, Result> OutgoingRpcMethod.OutgoingRpcMethodBuilder<OutgoingRpcMethod.Method<Params, Result>> request(Codec<Params> var0, Codec<Result> var1) {
      return new OutgoingRpcMethod.OutgoingRpcMethodBuilder((var2, var3) -> {
         if (var2.params().isEmpty()) {
            throw new IllegalMethodDefinitionException("Method defined as having parameters without describing them");
         } else if (var2.result().isEmpty()) {
            throw new IllegalMethodDefinitionException("Method lacks result");
         } else {
            return new OutgoingRpcMethod.Method(var2, var3, var0, var1);
         }
      });
   }

   public static class OutgoingRpcMethodBuilder<T extends OutgoingRpcMethod<?, ?>> {
      public static final OutgoingRpcMethod.Attributes DEFAULT_ATTRIBUTES = new OutgoingRpcMethod.Attributes(true);
      private final OutgoingRpcMethod.Factory<T> method;
      private String description = "";
      @Nullable
      private ParamInfo paramInfo;
      @Nullable
      private ResultInfo resultInfo;

      public OutgoingRpcMethodBuilder(OutgoingRpcMethod.Factory<T> var1) {
         super();
         this.method = var1;
      }

      public OutgoingRpcMethod.OutgoingRpcMethodBuilder<T> description(String var1) {
         this.description = var1;
         return this;
      }

      public OutgoingRpcMethod.OutgoingRpcMethodBuilder<T> response(ResultInfo var1) {
         this.resultInfo = var1;
         return this;
      }

      public OutgoingRpcMethod.OutgoingRpcMethodBuilder<T> param(ParamInfo var1) {
         this.paramInfo = var1;
         return this;
      }

      private T build() {
         MethodInfo var1 = new MethodInfo(this.description, this.paramInfo, this.resultInfo);
         return this.method.create(var1, DEFAULT_ATTRIBUTES);
      }

      public Holder.Reference<T> register(String var1) {
         return this.register(ResourceLocation.withDefaultNamespace("notification/" + var1));
      }

      private Holder.Reference<T> register(ResourceLocation var1) {
         return Registry.registerForHolder(BuiltInRegistries.OUTGOING_RPC_METHOD, (ResourceLocation)var1, this.build());
      }
   }

   @FunctionalInterface
   public interface Factory<T extends OutgoingRpcMethod<?, ?>> {
      T create(MethodInfo var1, OutgoingRpcMethod.Attributes var2);
   }

   public static record Method<Params, Result>(MethodInfo info, OutgoingRpcMethod.Attributes attributes, Codec<Params> paramsCodec, Codec<Result> resultCodec) implements OutgoingRpcMethod<Params, Result> {
      public Method(MethodInfo param1, OutgoingRpcMethod.Attributes param2, Codec<Params> param3, Codec<Result> param4) {
         super();
         this.info = var1;
         this.attributes = var2;
         this.paramsCodec = var3;
         this.resultCodec = var4;
      }

      @Nullable
      public JsonElement encodeParams(Params var1) {
         return (JsonElement)this.paramsCodec.encodeStart(JsonOps.INSTANCE, var1).getOrThrow();
      }

      public Result decodeResult(JsonElement var1) {
         return this.resultCodec.parse(JsonOps.INSTANCE, var1).getOrThrow();
      }

      public MethodInfo info() {
         return this.info;
      }

      public OutgoingRpcMethod.Attributes attributes() {
         return this.attributes;
      }

      public Codec<Params> paramsCodec() {
         return this.paramsCodec;
      }

      public Codec<Result> resultCodec() {
         return this.resultCodec;
      }
   }

   public static record Attributes(boolean discoverable) {
      public Attributes(boolean param1) {
         super();
         this.discoverable = var1;
      }

      public boolean discoverable() {
         return this.discoverable;
      }
   }

   public static record ParameterlessMethod<Result>(MethodInfo info, OutgoingRpcMethod.Attributes attributes, Codec<Result> resultCodec) implements OutgoingRpcMethod<Void, Result> {
      public ParameterlessMethod(MethodInfo param1, OutgoingRpcMethod.Attributes param2, Codec<Result> param3) {
         super();
         this.info = var1;
         this.attributes = var2;
         this.resultCodec = var3;
      }

      public Result decodeResult(JsonElement var1) {
         return this.resultCodec.parse(JsonOps.INSTANCE, var1).getOrThrow();
      }

      public MethodInfo info() {
         return this.info;
      }

      public OutgoingRpcMethod.Attributes attributes() {
         return this.attributes;
      }

      public Codec<Result> resultCodec() {
         return this.resultCodec;
      }
   }

   public static record Notification<Params>(MethodInfo info, OutgoingRpcMethod.Attributes attributes, Codec<Params> paramsCodec) implements OutgoingRpcMethod<Params, Void> {
      public Notification(MethodInfo param1, OutgoingRpcMethod.Attributes param2, Codec<Params> param3) {
         super();
         this.info = var1;
         this.attributes = var2;
         this.paramsCodec = var3;
      }

      @Nullable
      public JsonElement encodeParams(Params var1) {
         return (JsonElement)this.paramsCodec.encodeStart(JsonOps.INSTANCE, var1).getOrThrow();
      }

      public MethodInfo info() {
         return this.info;
      }

      public OutgoingRpcMethod.Attributes attributes() {
         return this.attributes;
      }

      public Codec<Params> paramsCodec() {
         return this.paramsCodec;
      }
   }

   public static record ParmeterlessNotification(MethodInfo info, OutgoingRpcMethod.Attributes attributes) implements OutgoingRpcMethod<Void, Void> {
      public ParmeterlessNotification(MethodInfo param1, OutgoingRpcMethod.Attributes param2) {
         super();
         this.info = var1;
         this.attributes = var2;
      }

      public MethodInfo info() {
         return this.info;
      }

      public OutgoingRpcMethod.Attributes attributes() {
         return this.attributes;
      }
   }
}
