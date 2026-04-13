package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import java.util.Iterator;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;

public class DialogCommand {
   public DialogCommand() {
      super();
   }

   public static void register(CommandDispatcher<CommandSourceStack> var0, CommandBuildContext var1) {
      var0.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("dialog").requires(Commands.hasPermission(2))).then(Commands.literal("show").then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("dialog", ResourceOrIdArgument.dialog(var1)).executes((var0x) -> {
         return showDialog((CommandSourceStack)var0x.getSource(), EntityArgument.getPlayers(var0x, "targets"), ResourceOrIdArgument.getDialog(var0x, "dialog"));
      }))))).then(Commands.literal("clear").then(Commands.argument("targets", EntityArgument.players()).executes((var0x) -> {
         return clearDialog((CommandSourceStack)var0x.getSource(), EntityArgument.getPlayers(var0x, "targets"));
      }))));
   }

   private static int showDialog(CommandSourceStack var0, Collection<ServerPlayer> var1, Holder<Dialog> var2) {
      Iterator var3 = var1.iterator();

      while(var3.hasNext()) {
         ServerPlayer var4 = (ServerPlayer)var3.next();
         var4.openDialog(var2);
      }

      if (var1.size() == 1) {
         var0.sendSuccess(() -> {
            return Component.translatable("commands.dialog.show.single", ((ServerPlayer)var1.iterator().next()).getDisplayName());
         }, true);
      } else {
         var0.sendSuccess(() -> {
            return Component.translatable("commands.dialog.show.multiple", var1.size());
         }, true);
      }

      return var1.size();
   }

   private static int clearDialog(CommandSourceStack var0, Collection<ServerPlayer> var1) {
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         ServerPlayer var3 = (ServerPlayer)var2.next();
         var3.connection.send(ClientboundClearDialogPacket.INSTANCE);
      }

      if (var1.size() == 1) {
         var0.sendSuccess(() -> {
            return Component.translatable("commands.dialog.clear.single", ((ServerPlayer)var1.iterator().next()).getDisplayName());
         }, true);
      } else {
         var0.sendSuccess(() -> {
            return Component.translatable("commands.dialog.clear.multiple", var1.size());
         }, true);
      }

      return var1.size();
   }
}
