package net.minecraft.server.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public record BooleanInput(Component label, boolean initial, String onTrue, String onFalse) implements InputControl {
   public static final MapCodec<BooleanInput> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(ComponentSerialization.CODEC.fieldOf("label").forGetter(BooleanInput::label), Codec.BOOL.optionalFieldOf("initial", false).forGetter(BooleanInput::initial), Codec.STRING.optionalFieldOf("on_true", "true").forGetter(BooleanInput::onTrue), Codec.STRING.optionalFieldOf("on_false", "false").forGetter(BooleanInput::onFalse)).apply(var0, BooleanInput::new);
   });

   public BooleanInput(Component param1, boolean param2, String param3, String param4) {
      super();
      this.label = var1;
      this.initial = var2;
      this.onTrue = var3;
      this.onFalse = var4;
   }

   public MapCodec<BooleanInput> mapCodec() {
      return MAP_CODEC;
   }

   public Component label() {
      return this.label;
   }

   public boolean initial() {
      return this.initial;
   }

   public String onTrue() {
      return this.onTrue;
   }

   public String onFalse() {
      return this.onFalse;
   }
}
