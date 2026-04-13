package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class MinecraftServerStateServiceImpl implements MinecraftServerStateService {
   private final DedicatedServer server;
   private final JsonRpcLogger jsonrpcLogger;

   public MinecraftServerStateServiceImpl(DedicatedServer var1, JsonRpcLogger var2) {
      super();
      this.server = var1;
      this.jsonrpcLogger = var2;
   }

   public boolean isReady() {
      return this.server.isReady();
   }

   public boolean saveEverything(boolean var1, boolean var2, boolean var3, ClientInfo var4) {
      this.jsonrpcLogger.log(var4, "Save everything. SuppressLogs: {}, flush: {}, force: {}", var1, var2, var3);
      return this.server.saveEverything(var1, var2, var3);
   }

   public void halt(boolean var1, ClientInfo var2) {
      this.jsonrpcLogger.log(var2, "Halt server. WaitForShutdown: {}", var1);
      this.server.halt(var1);
   }

   public void sendSystemMessage(Component var1, ClientInfo var2) {
      this.jsonrpcLogger.log(var2, "Send system message: '{}'", var1.getString());
      this.server.sendSystemMessage(var1);
   }

   public void sendSystemMessage(Component var1, boolean var2, Collection<ServerPlayer> var3, ClientInfo var4) {
      List var5 = var3.stream().map(Player::getPlainTextName).toList();
      this.jsonrpcLogger.log(var4, "Send system message to '{}' players (overlay: {}): '{}'", var5.size(), var2, var1.getString());
      Iterator var6 = var3.iterator();

      while(var6.hasNext()) {
         ServerPlayer var7 = (ServerPlayer)var6.next();
         if (var2) {
            var7.sendSystemMessage(var1, true);
         } else {
            var7.sendSystemMessage(var1);
         }
      }

   }

   public void broadcastSystemMessage(Component var1, boolean var2, ClientInfo var3) {
      this.jsonrpcLogger.log(var3, "Broadcast system message (overlay: {}): '{}'", var2, var1.getString());
      Iterator var4 = this.server.getPlayerList().getPlayers().iterator();

      while(var4.hasNext()) {
         ServerPlayer var5 = (ServerPlayer)var4.next();
         if (var2) {
            var5.sendSystemMessage(var1, true);
         } else {
            var5.sendSystemMessage(var1);
         }
      }

   }
}
