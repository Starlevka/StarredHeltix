package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public record MethodInfo(String description, Optional<ParamInfo> params, Optional<ResultInfo> result) {
   public static final Codec<Optional<ParamInfo>> PARAMS_CODEC;
   public static final MapCodec<MethodInfo> MAP_CODEC;

   public MethodInfo(String var1, @Nullable ParamInfo var2, @Nullable ResultInfo var3) {
      this(var1, Optional.ofNullable(var2), Optional.ofNullable(var3));
   }

   public MethodInfo(String param1, Optional<ParamInfo> param2, Optional<ResultInfo> param3) {
      super();
      this.description = var1;
      this.params = var2;
      this.result = var3;
   }

   public MethodInfo.Named named(ResourceLocation var1) {
      return new MethodInfo.Named(var1, this);
   }

   public String description() {
      return this.description;
   }

   public Optional<ParamInfo> params() {
      return this.params;
   }

   public Optional<ResultInfo> result() {
      return this.result;
   }

   static {
      PARAMS_CODEC = ParamInfo.CODEC.codec().listOf().xmap((var0) -> {
         return var0.stream().findAny();
      }, (var0) -> {
         return (List)var0.map(List::of).orElse(List.of());
      });
      MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.STRING.fieldOf("description").forGetter(MethodInfo::description), PARAMS_CODEC.fieldOf("params").forGetter(MethodInfo::params), ResultInfo.CODEC.codec().optionalFieldOf("result").forGetter(MethodInfo::result)).apply(var0, MethodInfo::new);
      });
   }

   public static record Named(ResourceLocation name, MethodInfo contents) {
      public static final Codec<MethodInfo.Named> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(ResourceLocation.CODEC.fieldOf("name").forGetter(MethodInfo.Named::name), MethodInfo.MAP_CODEC.forGetter(MethodInfo.Named::contents)).apply(var0, MethodInfo.Named::new);
      });

      public Named(ResourceLocation param1, MethodInfo param2) {
         super();
         this.name = var1;
         this.contents = var2;
      }

      public ResourceLocation name() {
         return this.name;
      }

      public MethodInfo contents() {
         return this.contents;
      }
   }
}
