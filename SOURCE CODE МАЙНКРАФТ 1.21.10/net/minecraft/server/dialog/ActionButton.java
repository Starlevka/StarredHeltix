package net.minecraft.server.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.server.dialog.action.Action;

public record ActionButton(CommonButtonData button, Optional<Action> action) {
   public static final Codec<ActionButton> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(CommonButtonData.MAP_CODEC.forGetter(ActionButton::button), Action.CODEC.optionalFieldOf("action").forGetter(ActionButton::action)).apply(var0, ActionButton::new);
   });

   public ActionButton(CommonButtonData param1, Optional<Action> param2) {
      super();
      this.button = var1;
      this.action = var2;
   }

   public CommonButtonData button() {
      return this.button;
   }

   public Optional<Action> action() {
      return this.action;
   }
}
