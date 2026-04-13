package net.minecraft.server.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.dialog.body.DialogBody;

public record CommonDialogData(Component title, Optional<Component> externalTitle, boolean canCloseWithEscape, boolean pause, DialogAction afterAction, List<DialogBody> body, List<Input> inputs) {
   public static final MapCodec<CommonDialogData> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(ComponentSerialization.CODEC.fieldOf("title").forGetter(CommonDialogData::title), ComponentSerialization.CODEC.optionalFieldOf("external_title").forGetter(CommonDialogData::externalTitle), Codec.BOOL.optionalFieldOf("can_close_with_escape", true).forGetter(CommonDialogData::canCloseWithEscape), Codec.BOOL.optionalFieldOf("pause", true).forGetter(CommonDialogData::pause), DialogAction.CODEC.optionalFieldOf("after_action", DialogAction.CLOSE).forGetter(CommonDialogData::afterAction), DialogBody.COMPACT_LIST_CODEC.optionalFieldOf("body", List.of()).forGetter(CommonDialogData::body), Input.CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(CommonDialogData::inputs)).apply(var0, CommonDialogData::new);
   }).validate((var0) -> {
      return var0.pause && !var0.afterAction.willUnpause() ? DataResult.error(() -> {
         return "Dialogs that pause the game must use after_action values that unpause it after user action!";
      }) : DataResult.success(var0);
   });

   public CommonDialogData(Component param1, Optional<Component> param2, boolean param3, boolean param4, DialogAction param5, List<DialogBody> param6, List<Input> param7) {
      super();
      this.title = var1;
      this.externalTitle = var2;
      this.canCloseWithEscape = var3;
      this.pause = var4;
      this.afterAction = var5;
      this.body = var6;
      this.inputs = var7;
   }

   public Component computeExternalTitle() {
      return (Component)this.externalTitle.orElse(this.title);
   }

   public Component title() {
      return this.title;
   }

   public Optional<Component> externalTitle() {
      return this.externalTitle;
   }

   public boolean canCloseWithEscape() {
      return this.canCloseWithEscape;
   }

   public boolean pause() {
      return this.pause;
   }

   public DialogAction afterAction() {
      return this.afterAction;
   }

   public List<DialogBody> body() {
      return this.body;
   }

   public List<Input> inputs() {
      return this.inputs;
   }
}
