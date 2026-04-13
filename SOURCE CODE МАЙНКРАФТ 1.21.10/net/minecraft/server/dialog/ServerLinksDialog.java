package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;

public record ServerLinksDialog(CommonDialogData common, Optional<ActionButton> exitAction, int columns, int buttonWidth) implements ButtonListDialog {
   public static final MapCodec<ServerLinksDialog> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(CommonDialogData.MAP_CODEC.forGetter(ServerLinksDialog::common), ActionButton.CODEC.optionalFieldOf("exit_action").forGetter(ServerLinksDialog::exitAction), ExtraCodecs.POSITIVE_INT.optionalFieldOf("columns", 2).forGetter(ServerLinksDialog::columns), WIDTH_CODEC.optionalFieldOf("button_width", 150).forGetter(ServerLinksDialog::buttonWidth)).apply(var0, ServerLinksDialog::new);
   });

   public ServerLinksDialog(CommonDialogData param1, Optional<ActionButton> param2, int param3, int param4) {
      super();
      this.common = var1;
      this.exitAction = var2;
      this.columns = var3;
      this.buttonWidth = var4;
   }

   public MapCodec<ServerLinksDialog> codec() {
      return MAP_CODEC;
   }

   public CommonDialogData common() {
      return this.common;
   }

   public Optional<ActionButton> exitAction() {
      return this.exitAction;
   }

   public int columns() {
      return this.columns;
   }

   public int buttonWidth() {
      return this.buttonWidth;
   }
}
