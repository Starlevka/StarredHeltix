package net.minecraft.server.dialog.action;

import com.mojang.serialization.MapCodec;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.network.chat.ClickEvent;

public record StaticAction(ClickEvent value) implements Action {
   public static final Map<ClickEvent.Action, MapCodec<StaticAction>> WRAPPED_CODECS = (Map)Util.make(() -> {
      EnumMap var0 = new EnumMap(ClickEvent.Action.class);
      ClickEvent.Action[] var1 = (ClickEvent.Action[])ClickEvent.Action.class.getEnumConstants();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ClickEvent.Action var4 = var1[var3];
         if (var4.isAllowedFromServer()) {
            MapCodec var5 = var4.valueCodec();
            var0.put(var4, var5.xmap(StaticAction::new, StaticAction::value));
         }
      }

      return Collections.unmodifiableMap(var0);
   });

   public StaticAction(ClickEvent param1) {
      super();
      this.value = var1;
   }

   public MapCodec<StaticAction> codec() {
      return (MapCodec)WRAPPED_CODECS.get(this.value.action());
   }

   public Optional<ClickEvent> createAction(Map<String, Action.ValueGetter> var1) {
      return Optional.of(this.value);
   }

   public ClickEvent value() {
      return this.value;
   }
}
