package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;

public class ServerStateService {
   public ServerStateService() {
      super();
   }

   public static ServerStateService.ServerState status(MinecraftApi var0) {
      return !var0.serverStateService().isReady() ? ServerStateService.ServerState.NOT_STARTED : new ServerStateService.ServerState(true, PlayerService.get(var0), ServerStatus.Version.current());
   }

   public static boolean save(MinecraftApi var0, boolean var1, ClientInfo var2) {
      return var0.serverStateService().saveEverything(true, var1, true, var2);
   }

   public static boolean stop(MinecraftApi var0, ClientInfo var1) {
      var0.submit(() -> {
         var0.serverStateService().halt(false, var1);
      });
      return true;
   }

   public static boolean systemMessage(MinecraftApi var0, ServerStateService.SystemMessage var1, ClientInfo var2) {
      Component var3 = (Component)var1.message().asComponent().orElse((Object)null);
      if (var3 == null) {
         return false;
      } else if (var1.receivingPlayers().isPresent()) {
         if (((List)var1.receivingPlayers().get()).isEmpty()) {
            return false;
         } else {
            Iterator var4 = ((List)var1.receivingPlayers().get()).iterator();

            while(true) {
               ServerPlayer var6;
               while(true) {
                  if (!var4.hasNext()) {
                     return true;
                  }

                  PlayerDto var5 = (PlayerDto)var4.next();
                  if (var5.id().isPresent()) {
                     var6 = var0.playerListService().getPlayer((UUID)var5.id().get());
                     break;
                  }

                  if (var5.name().isPresent()) {
                     var6 = var0.playerListService().getPlayerByName((String)var5.name().get());
                     break;
                  }
               }

               if (var6 != null) {
                  var6.sendSystemMessage(var3, var1.overlay());
               }
            }
         }
      } else {
         var0.serverStateService().broadcastSystemMessage(var3, var1.overlay(), var2);
         return true;
      }
   }

   public static record ServerState(boolean started, List<PlayerDto> players, ServerStatus.Version version) {
      public static final Codec<ServerStateService.ServerState> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(Codec.BOOL.fieldOf("started").forGetter(ServerStateService.ServerState::started), PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("players", List.of()).forGetter(ServerStateService.ServerState::players), ServerStatus.Version.CODEC.fieldOf("version").forGetter(ServerStateService.ServerState::version)).apply(var0, ServerStateService.ServerState::new);
      });
      public static final ServerStateService.ServerState NOT_STARTED = new ServerStateService.ServerState(false, List.of(), ServerStatus.Version.current());

      public ServerState(boolean param1, List<PlayerDto> param2, ServerStatus.Version param3) {
         super();
         this.started = var1;
         this.players = var2;
         this.version = var3;
      }

      public boolean started() {
         return this.started;
      }

      public List<PlayerDto> players() {
         return this.players;
      }

      public ServerStatus.Version version() {
         return this.version;
      }
   }

   public static record SystemMessage(Message message, boolean overlay, Optional<List<PlayerDto>> receivingPlayers) {
      public static final Codec<ServerStateService.SystemMessage> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(Message.CODEC.fieldOf("message").forGetter(ServerStateService.SystemMessage::message), Codec.BOOL.fieldOf("overlay").forGetter(ServerStateService.SystemMessage::overlay), PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("receivingPlayers").forGetter(ServerStateService.SystemMessage::receivingPlayers)).apply(var0, ServerStateService.SystemMessage::new);
      });

      public SystemMessage(Message param1, boolean param2, Optional<List<PlayerDto>> param3) {
         super();
         this.message = var1;
         this.overlay = var2;
         this.receivingPlayers = var3;
      }

      public Message message() {
         return this.message;
      }

      public boolean overlay() {
         return this.overlay;
      }

      public Optional<List<PlayerDto>> receivingPlayers() {
         return this.receivingPlayers;
      }
   }
}
