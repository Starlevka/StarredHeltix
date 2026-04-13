package net.minecraft.server.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.ExtraCodecs;

public record SingleOptionInput(int width, List<SingleOptionInput.Entry> entries, Component label, boolean labelVisible) implements InputControl {
   public static final MapCodec<SingleOptionInput> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(Dialog.WIDTH_CODEC.optionalFieldOf("width", 200).forGetter(SingleOptionInput::width), ExtraCodecs.nonEmptyList(SingleOptionInput.Entry.CODEC.listOf()).fieldOf("options").forGetter(SingleOptionInput::entries), ComponentSerialization.CODEC.fieldOf("label").forGetter(SingleOptionInput::label), Codec.BOOL.optionalFieldOf("label_visible", true).forGetter(SingleOptionInput::labelVisible)).apply(var0, SingleOptionInput::new);
   }).validate((var0) -> {
      long var1 = var0.entries.stream().filter(SingleOptionInput.Entry::initial).count();
      return var1 > 1L ? DataResult.error(() -> {
         return "Multiple initial values";
      }) : DataResult.success(var0);
   });

   public SingleOptionInput(int param1, List<SingleOptionInput.Entry> param2, Component param3, boolean param4) {
      super();
      this.width = var1;
      this.entries = var2;
      this.label = var3;
      this.labelVisible = var4;
   }

   public MapCodec<SingleOptionInput> mapCodec() {
      return MAP_CODEC;
   }

   public Optional<SingleOptionInput.Entry> initial() {
      return this.entries.stream().filter(SingleOptionInput.Entry::initial).findFirst();
   }

   public int width() {
      return this.width;
   }

   public List<SingleOptionInput.Entry> entries() {
      return this.entries;
   }

   public Component label() {
      return this.label;
   }

   public boolean labelVisible() {
      return this.labelVisible;
   }

   public static record Entry(String id, Optional<Component> display, boolean initial) {
      public static final Codec<SingleOptionInput.Entry> FULL_CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(Codec.STRING.fieldOf("id").forGetter(SingleOptionInput.Entry::id), ComponentSerialization.CODEC.optionalFieldOf("display").forGetter(SingleOptionInput.Entry::display), Codec.BOOL.optionalFieldOf("initial", false).forGetter(SingleOptionInput.Entry::initial)).apply(var0, SingleOptionInput.Entry::new);
      });
      public static final Codec<SingleOptionInput.Entry> CODEC;

      public Entry(String param1, Optional<Component> param2, boolean param3) {
         super();
         this.id = var1;
         this.display = var2;
         this.initial = var3;
      }

      public Component displayOrDefault() {
         return (Component)this.display.orElseGet(() -> {
            return Component.literal(this.id);
         });
      }

      public String id() {
         return this.id;
      }

      public Optional<Component> display() {
         return this.display;
      }

      public boolean initial() {
         return this.initial;
      }

      static {
         CODEC = Codec.withAlternative(FULL_CODEC, Codec.STRING, (var0) -> {
            return new SingleOptionInput.Entry(var0, Optional.empty(), false);
         });
      }
   }
}
