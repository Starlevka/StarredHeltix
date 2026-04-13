package net.minecraft.network.protocol.game;

import com.google.common.base.MoreObjects;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;

public class ClientboundPlayerInfoUpdatePacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPlayerInfoUpdatePacket> STREAM_CODEC = Packet.codec(ClientboundPlayerInfoUpdatePacket::write, ClientboundPlayerInfoUpdatePacket::new);
   private final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions;
   private final List<ClientboundPlayerInfoUpdatePacket.Entry> entries;

   public ClientboundPlayerInfoUpdatePacket(EnumSet<ClientboundPlayerInfoUpdatePacket.Action> var1, Collection<ServerPlayer> var2) {
      super();
      this.actions = var1;
      this.entries = var2.stream().map(ClientboundPlayerInfoUpdatePacket.Entry::new).toList();
   }

   public ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action var1, ServerPlayer var2) {
      super();
      this.actions = EnumSet.of(var1);
      this.entries = List.of(new ClientboundPlayerInfoUpdatePacket.Entry(var2));
   }

   public static ClientboundPlayerInfoUpdatePacket createPlayerInitializing(Collection<ServerPlayer> var0) {
      EnumSet var1 = EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER);
      return new ClientboundPlayerInfoUpdatePacket(var1, var0);
   }

   private ClientboundPlayerInfoUpdatePacket(RegistryFriendlyByteBuf var1) {
      super();
      this.actions = var1.readEnumSet(ClientboundPlayerInfoUpdatePacket.Action.class);
      this.entries = var1.readList((var1x) -> {
         ClientboundPlayerInfoUpdatePacket.EntryBuilder var2 = new ClientboundPlayerInfoUpdatePacket.EntryBuilder(var1x.readUUID());
         Iterator var3 = this.actions.iterator();

         while(var3.hasNext()) {
            ClientboundPlayerInfoUpdatePacket.Action var4 = (ClientboundPlayerInfoUpdatePacket.Action)var3.next();
            var4.reader.read(var2, (RegistryFriendlyByteBuf)var1x);
         }

         return var2.build();
      });
   }

   private void write(RegistryFriendlyByteBuf var1) {
      var1.writeEnumSet(this.actions, ClientboundPlayerInfoUpdatePacket.Action.class);
      var1.writeCollection(this.entries, (var1x, var2) -> {
         var1x.writeUUID(var2.profileId());
         Iterator var3 = this.actions.iterator();

         while(var3.hasNext()) {
            ClientboundPlayerInfoUpdatePacket.Action var4 = (ClientboundPlayerInfoUpdatePacket.Action)var3.next();
            var4.writer.write((RegistryFriendlyByteBuf)var1x, var2);
         }

      });
   }

   public PacketType<ClientboundPlayerInfoUpdatePacket> type() {
      return GamePacketTypes.CLIENTBOUND_PLAYER_INFO_UPDATE;
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handlePlayerInfoUpdate(this);
   }

   public EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions() {
      return this.actions;
   }

   public List<ClientboundPlayerInfoUpdatePacket.Entry> entries() {
      return this.entries;
   }

   public List<ClientboundPlayerInfoUpdatePacket.Entry> newEntries() {
      return this.actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER) ? this.entries : List.of();
   }

   public String toString() {
      return MoreObjects.toStringHelper(this).add("actions", this.actions).add("entries", this.entries).toString();
   }

   public static record Entry(UUID profileId, @Nullable GameProfile profile, boolean listed, int latency, GameType gameMode, @Nullable Component displayName, boolean showHat, int listOrder, @Nullable RemoteChatSession.Data chatSession) {
      final boolean showHat;
      final int listOrder;
      @Nullable
      final RemoteChatSession.Data chatSession;

      Entry(ServerPlayer var1) {
         this(var1.getUUID(), var1.getGameProfile(), true, var1.connection.latency(), var1.gameMode(), var1.getTabListDisplayName(), var1.isModelPartShown(PlayerModelPart.HAT), var1.getTabListOrder(), (RemoteChatSession.Data)Optionull.map(var1.getChatSession(), RemoteChatSession::asData));
      }

      public Entry(UUID param1, @Nullable GameProfile param2, boolean param3, int param4, GameType param5, @Nullable Component param6, boolean param7, int param8, @Nullable RemoteChatSession.Data param9) {
         super();
         this.profileId = var1;
         this.profile = var2;
         this.listed = var3;
         this.latency = var4;
         this.gameMode = var5;
         this.displayName = var6;
         this.showHat = var7;
         this.listOrder = var8;
         this.chatSession = var9;
      }

      public UUID profileId() {
         return this.profileId;
      }

      @Nullable
      public GameProfile profile() {
         return this.profile;
      }

      public boolean listed() {
         return this.listed;
      }

      public int latency() {
         return this.latency;
      }

      public GameType gameMode() {
         return this.gameMode;
      }

      @Nullable
      public Component displayName() {
         return this.displayName;
      }

      public boolean showHat() {
         return this.showHat;
      }

      public int listOrder() {
         return this.listOrder;
      }

      @Nullable
      public RemoteChatSession.Data chatSession() {
         return this.chatSession;
      }
   }

   public static enum Action {
      ADD_PLAYER((var0, var1) -> {
         String var2 = (String)ByteBufCodecs.PLAYER_NAME.decode(var1);
         PropertyMap var3 = (PropertyMap)ByteBufCodecs.GAME_PROFILE_PROPERTIES.decode(var1);
         var0.profile = new GameProfile(var0.profileId, var2, var3);
      }, (var0, var1) -> {
         GameProfile var2 = (GameProfile)Objects.requireNonNull(var1.profile());
         ByteBufCodecs.PLAYER_NAME.encode(var0, var2.name());
         ByteBufCodecs.GAME_PROFILE_PROPERTIES.encode(var0, var2.properties());
      }),
      INITIALIZE_CHAT((var0, var1) -> {
         var0.chatSession = (RemoteChatSession.Data)var1.readNullable(RemoteChatSession.Data::read);
      }, (var0, var1) -> {
         var0.writeNullable(var1.chatSession, RemoteChatSession.Data::write);
      }),
      UPDATE_GAME_MODE((var0, var1) -> {
         var0.gameMode = GameType.byId(var1.readVarInt());
      }, (var0, var1) -> {
         var0.writeVarInt(var1.gameMode().getId());
      }),
      UPDATE_LISTED((var0, var1) -> {
         var0.listed = var1.readBoolean();
      }, (var0, var1) -> {
         var0.writeBoolean(var1.listed());
      }),
      UPDATE_LATENCY((var0, var1) -> {
         var0.latency = var1.readVarInt();
      }, (var0, var1) -> {
         var0.writeVarInt(var1.latency());
      }),
      UPDATE_DISPLAY_NAME((var0, var1) -> {
         var0.displayName = (Component)FriendlyByteBuf.readNullable(var1, ComponentSerialization.TRUSTED_STREAM_CODEC);
      }, (var0, var1) -> {
         FriendlyByteBuf.writeNullable(var0, var1.displayName(), ComponentSerialization.TRUSTED_STREAM_CODEC);
      }),
      UPDATE_LIST_ORDER((var0, var1) -> {
         var0.listOrder = var1.readVarInt();
      }, (var0, var1) -> {
         var0.writeVarInt(var1.listOrder);
      }),
      UPDATE_HAT((var0, var1) -> {
         var0.showHat = var1.readBoolean();
      }, (var0, var1) -> {
         var0.writeBoolean(var1.showHat);
      });

      final ClientboundPlayerInfoUpdatePacket.Action.Reader reader;
      final ClientboundPlayerInfoUpdatePacket.Action.Writer writer;

      private Action(final ClientboundPlayerInfoUpdatePacket.Action.Reader param3, final ClientboundPlayerInfoUpdatePacket.Action.Writer param4) {
         this.reader = var3;
         this.writer = var4;
      }

      // $FF: synthetic method
      private static ClientboundPlayerInfoUpdatePacket.Action[] $values() {
         return new ClientboundPlayerInfoUpdatePacket.Action[]{ADD_PLAYER, INITIALIZE_CHAT, UPDATE_GAME_MODE, UPDATE_LISTED, UPDATE_LATENCY, UPDATE_DISPLAY_NAME, UPDATE_LIST_ORDER, UPDATE_HAT};
      }

      public interface Reader {
         void read(ClientboundPlayerInfoUpdatePacket.EntryBuilder var1, RegistryFriendlyByteBuf var2);
      }

      public interface Writer {
         void write(RegistryFriendlyByteBuf var1, ClientboundPlayerInfoUpdatePacket.Entry var2);
      }
   }

   private static class EntryBuilder {
      final UUID profileId;
      @Nullable
      GameProfile profile;
      boolean listed;
      int latency;
      GameType gameMode;
      @Nullable
      Component displayName;
      boolean showHat;
      int listOrder;
      @Nullable
      RemoteChatSession.Data chatSession;

      EntryBuilder(UUID var1) {
         super();
         this.gameMode = GameType.DEFAULT_MODE;
         this.profileId = var1;
      }

      ClientboundPlayerInfoUpdatePacket.Entry build() {
         return new ClientboundPlayerInfoUpdatePacket.Entry(this.profileId, this.profile, this.listed, this.latency, this.gameMode, this.displayName, this.showHat, this.listOrder, this.chatSession);
      }
   }
}
