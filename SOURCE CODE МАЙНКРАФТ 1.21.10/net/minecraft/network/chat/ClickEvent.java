package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

public interface ClickEvent {
   Codec<ClickEvent> CODEC = ClickEvent.Action.CODEC.dispatch("action", ClickEvent::action, (var0) -> {
      return var0.codec;
   });

   ClickEvent.Action action();

   public static enum Action implements StringRepresentable {
      OPEN_URL("open_url", true, ClickEvent.OpenUrl.CODEC),
      OPEN_FILE("open_file", false, ClickEvent.OpenFile.CODEC),
      RUN_COMMAND("run_command", true, ClickEvent.RunCommand.CODEC),
      SUGGEST_COMMAND("suggest_command", true, ClickEvent.SuggestCommand.CODEC),
      SHOW_DIALOG("show_dialog", true, ClickEvent.ShowDialog.CODEC),
      CHANGE_PAGE("change_page", true, ClickEvent.ChangePage.CODEC),
      COPY_TO_CLIPBOARD("copy_to_clipboard", true, ClickEvent.CopyToClipboard.CODEC),
      CUSTOM("custom", true, ClickEvent.Custom.CODEC);

      public static final Codec<ClickEvent.Action> UNSAFE_CODEC = StringRepresentable.fromEnum(ClickEvent.Action::values);
      public static final Codec<ClickEvent.Action> CODEC = UNSAFE_CODEC.validate(ClickEvent.Action::filterForSerialization);
      private final boolean allowFromServer;
      private final String name;
      final MapCodec<? extends ClickEvent> codec;

      private Action(final String param3, final boolean param4, final MapCodec<? extends ClickEvent> param5) {
         this.name = var3;
         this.allowFromServer = var4;
         this.codec = var5;
      }

      public boolean isAllowedFromServer() {
         return this.allowFromServer;
      }

      public String getSerializedName() {
         return this.name;
      }

      public MapCodec<? extends ClickEvent> valueCodec() {
         return this.codec;
      }

      public static DataResult<ClickEvent.Action> filterForSerialization(ClickEvent.Action var0) {
         return !var0.isAllowedFromServer() ? DataResult.error(() -> {
            return "Click event type not allowed: " + String.valueOf(var0);
         }) : DataResult.success(var0, Lifecycle.stable());
      }

      // $FF: synthetic method
      private static ClickEvent.Action[] $values() {
         return new ClickEvent.Action[]{OPEN_URL, OPEN_FILE, RUN_COMMAND, SUGGEST_COMMAND, SHOW_DIALOG, CHANGE_PAGE, COPY_TO_CLIPBOARD, CUSTOM};
      }
   }

   public static record Custom(ResourceLocation id, Optional<Tag> payload) implements ClickEvent {
      public static final MapCodec<ClickEvent.Custom> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ResourceLocation.CODEC.fieldOf("id").forGetter(ClickEvent.Custom::id), ExtraCodecs.NBT.optionalFieldOf("payload").forGetter(ClickEvent.Custom::payload)).apply(var0, ClickEvent.Custom::new);
      });

      public Custom(ResourceLocation param1, Optional<Tag> param2) {
         super();
         this.id = var1;
         this.payload = var2;
      }

      public ClickEvent.Action action() {
         return ClickEvent.Action.CUSTOM;
      }

      public ResourceLocation id() {
         return this.id;
      }

      public Optional<Tag> payload() {
         return this.payload;
      }
   }

   public static record CopyToClipboard(String value) implements ClickEvent {
      public static final MapCodec<ClickEvent.CopyToClipboard> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.STRING.fieldOf("value").forGetter(ClickEvent.CopyToClipboard::value)).apply(var0, ClickEvent.CopyToClipboard::new);
      });

      public CopyToClipboard(String param1) {
         super();
         this.value = var1;
      }

      public ClickEvent.Action action() {
         return ClickEvent.Action.COPY_TO_CLIPBOARD;
      }

      public String value() {
         return this.value;
      }
   }

   public static record ChangePage(int page) implements ClickEvent {
      public static final MapCodec<ClickEvent.ChangePage> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ExtraCodecs.POSITIVE_INT.fieldOf("page").forGetter(ClickEvent.ChangePage::page)).apply(var0, ClickEvent.ChangePage::new);
      });

      public ChangePage(int param1) {
         super();
         this.page = var1;
      }

      public ClickEvent.Action action() {
         return ClickEvent.Action.CHANGE_PAGE;
      }

      public int page() {
         return this.page;
      }
   }

   public static record ShowDialog(Holder<Dialog> dialog) implements ClickEvent {
      public static final MapCodec<ClickEvent.ShowDialog> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Dialog.CODEC.fieldOf("dialog").forGetter(ClickEvent.ShowDialog::dialog)).apply(var0, ClickEvent.ShowDialog::new);
      });

      public ShowDialog(Holder<Dialog> param1) {
         super();
         this.dialog = var1;
      }

      public ClickEvent.Action action() {
         return ClickEvent.Action.SHOW_DIALOG;
      }

      public Holder<Dialog> dialog() {
         return this.dialog;
      }
   }

   public static record SuggestCommand(String command) implements ClickEvent {
      public static final MapCodec<ClickEvent.SuggestCommand> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ExtraCodecs.CHAT_STRING.fieldOf("command").forGetter(ClickEvent.SuggestCommand::command)).apply(var0, ClickEvent.SuggestCommand::new);
      });

      public SuggestCommand(String param1) {
         super();
         this.command = var1;
      }

      public ClickEvent.Action action() {
         return ClickEvent.Action.SUGGEST_COMMAND;
      }

      public String command() {
         return this.command;
      }
   }

   public static record RunCommand(String command) implements ClickEvent {
      public static final MapCodec<ClickEvent.RunCommand> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ExtraCodecs.CHAT_STRING.fieldOf("command").forGetter(ClickEvent.RunCommand::command)).apply(var0, ClickEvent.RunCommand::new);
      });

      public RunCommand(String param1) {
         super();
         this.command = var1;
      }

      public ClickEvent.Action action() {
         return ClickEvent.Action.RUN_COMMAND;
      }

      public String command() {
         return this.command;
      }
   }

   public static record OpenFile(String path) implements ClickEvent {
      public static final MapCodec<ClickEvent.OpenFile> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.STRING.fieldOf("path").forGetter(ClickEvent.OpenFile::path)).apply(var0, ClickEvent.OpenFile::new);
      });

      public OpenFile(File var1) {
         this(var1.toString());
      }

      public OpenFile(Path var1) {
         this(var1.toFile());
      }

      public OpenFile(String param1) {
         super();
         this.path = var1;
      }

      public File file() {
         return new File(this.path);
      }

      public ClickEvent.Action action() {
         return ClickEvent.Action.OPEN_FILE;
      }

      public String path() {
         return this.path;
      }
   }

   public static record OpenUrl(URI uri) implements ClickEvent {
      public static final MapCodec<ClickEvent.OpenUrl> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ExtraCodecs.UNTRUSTED_URI.fieldOf("url").forGetter(ClickEvent.OpenUrl::uri)).apply(var0, ClickEvent.OpenUrl::new);
      });

      public OpenUrl(URI param1) {
         super();
         this.uri = var1;
      }

      public ClickEvent.Action action() {
         return ClickEvent.Action.OPEN_URL;
      }

      public URI uri() {
         return this.uri;
      }
   }
}
