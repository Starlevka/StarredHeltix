package net.minecraft.server.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.ExtraCodecs;

public record TextInput(int width, Component label, boolean labelVisible, String initial, int maxLength, Optional<TextInput.MultilineOptions> multiline) implements InputControl {
   public static final MapCodec<TextInput> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(Dialog.WIDTH_CODEC.optionalFieldOf("width", 200).forGetter(TextInput::width), ComponentSerialization.CODEC.fieldOf("label").forGetter(TextInput::label), Codec.BOOL.optionalFieldOf("label_visible", true).forGetter(TextInput::labelVisible), Codec.STRING.optionalFieldOf("initial", "").forGetter(TextInput::initial), ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_length", 32).forGetter(TextInput::maxLength), TextInput.MultilineOptions.CODEC.optionalFieldOf("multiline").forGetter(TextInput::multiline)).apply(var0, TextInput::new);
   }).validate((var0) -> {
      return var0.initial.length() > var0.maxLength() ? DataResult.error(() -> {
         return "Default text length exceeds allowed size";
      }) : DataResult.success(var0);
   });

   public TextInput(int param1, Component param2, boolean param3, String param4, int param5, Optional<TextInput.MultilineOptions> param6) {
      super();
      this.width = var1;
      this.label = var2;
      this.labelVisible = var3;
      this.initial = var4;
      this.maxLength = var5;
      this.multiline = var6;
   }

   public MapCodec<TextInput> mapCodec() {
      return MAP_CODEC;
   }

   public int width() {
      return this.width;
   }

   public Component label() {
      return this.label;
   }

   public boolean labelVisible() {
      return this.labelVisible;
   }

   public String initial() {
      return this.initial;
   }

   public int maxLength() {
      return this.maxLength;
   }

   public Optional<TextInput.MultilineOptions> multiline() {
      return this.multiline;
   }

   public static record MultilineOptions(Optional<Integer> maxLines, Optional<Integer> height) {
      public static final int MAX_HEIGHT = 512;
      public static final Codec<TextInput.MultilineOptions> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_lines").forGetter(TextInput.MultilineOptions::maxLines), ExtraCodecs.intRange(1, 512).optionalFieldOf("height").forGetter(TextInput.MultilineOptions::height)).apply(var0, TextInput.MultilineOptions::new);
      });

      public MultilineOptions(Optional<Integer> param1, Optional<Integer> param2) {
         super();
         this.maxLines = var1;
         this.height = var2;
      }

      public Optional<Integer> maxLines() {
         return this.maxLines;
      }

      public Optional<Integer> height() {
         return this.height;
      }
   }
}
