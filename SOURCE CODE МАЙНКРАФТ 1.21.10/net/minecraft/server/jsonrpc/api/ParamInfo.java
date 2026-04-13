package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ParamInfo(String name, Schema schema, boolean required) {
   public static final MapCodec<ParamInfo> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(Codec.STRING.fieldOf("name").forGetter(ParamInfo::name), Schema.CODEC.fieldOf("schema").forGetter(ParamInfo::schema), Codec.BOOL.fieldOf("required").forGetter(ParamInfo::required)).apply(var0, ParamInfo::new);
   });

   public ParamInfo(String var1, Schema var2) {
      this(var1, var2, true);
   }

   public ParamInfo(String param1, Schema param2, boolean param3) {
      super();
      this.name = var1;
      this.schema = var2;
      this.required = var3;
   }

   public String name() {
      return this.name;
   }

   public Schema schema() {
      return this.schema;
   }

   public boolean required() {
      return this.required;
   }
}
