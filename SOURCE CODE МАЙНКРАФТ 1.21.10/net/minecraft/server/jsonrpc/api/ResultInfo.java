package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ResultInfo(String name, Schema schema) {
   public static final MapCodec<ResultInfo> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(Codec.STRING.fieldOf("name").forGetter(ResultInfo::name), Schema.CODEC.fieldOf("schema").forGetter(ResultInfo::schema)).apply(var0, ResultInfo::new);
   });

   public ResultInfo(String param1, Schema param2) {
      super();
      this.name = var1;
      this.schema = var2;
   }

   public String name() {
      return this.name;
   }

   public Schema schema() {
      return this.schema;
   }
}
