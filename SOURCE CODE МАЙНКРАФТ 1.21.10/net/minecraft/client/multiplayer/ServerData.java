package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.PngInfo;
import org.slf4j.Logger;

public class ServerData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_ICON_SIZE = 1024;
   public String name;
   public String ip;
   public Component status;
   public Component motd;
   @Nullable
   public ServerStatus.Players players;
   public long ping;
   public int protocol = SharedConstants.getCurrentVersion().protocolVersion();
   public Component version = Component.literal(SharedConstants.getCurrentVersion().name());
   public List<Component> playerList = Collections.emptyList();
   private ServerData.ServerPackStatus packStatus;
   @Nullable
   private byte[] iconBytes;
   private ServerData.Type type;
   private int acceptedCodeOfConduct;
   private ServerData.State state;

   public ServerData(String var1, String var2, ServerData.Type var3) {
      super();
      this.packStatus = ServerData.ServerPackStatus.PROMPT;
      this.state = ServerData.State.INITIAL;
      this.name = var1;
      this.ip = var2;
      this.type = var3;
   }

   public CompoundTag write() {
      CompoundTag var1 = new CompoundTag();
      var1.putString("name", this.name);
      var1.putString("ip", this.ip);
      var1.storeNullable("icon", ExtraCodecs.BASE64_STRING, this.iconBytes);
      var1.store(ServerData.ServerPackStatus.FIELD_CODEC, this.packStatus);
      if (this.acceptedCodeOfConduct != 0) {
         var1.putInt("acceptedCodeOfConduct", this.acceptedCodeOfConduct);
      }

      return var1;
   }

   public ServerData.ServerPackStatus getResourcePackStatus() {
      return this.packStatus;
   }

   public void setResourcePackStatus(ServerData.ServerPackStatus var1) {
      this.packStatus = var1;
   }

   public static ServerData read(CompoundTag var0) {
      ServerData var1 = new ServerData(var0.getStringOr("name", ""), var0.getStringOr("ip", ""), ServerData.Type.OTHER);
      var1.setIconBytes((byte[])var0.read("icon", ExtraCodecs.BASE64_STRING).orElse((Object)null));
      var1.setResourcePackStatus((ServerData.ServerPackStatus)var0.read(ServerData.ServerPackStatus.FIELD_CODEC).orElse(ServerData.ServerPackStatus.PROMPT));
      var1.acceptedCodeOfConduct = var0.getIntOr("acceptedCodeOfConduct", 0);
      return var1;
   }

   @Nullable
   public byte[] getIconBytes() {
      return this.iconBytes;
   }

   public void setIconBytes(@Nullable byte[] var1) {
      this.iconBytes = var1;
   }

   public boolean isLan() {
      return this.type == ServerData.Type.LAN;
   }

   public boolean isRealm() {
      return this.type == ServerData.Type.REALM;
   }

   public ServerData.Type type() {
      return this.type;
   }

   public boolean hasAcceptedCodeOfConduct(String var1) {
      return this.acceptedCodeOfConduct == var1.hashCode();
   }

   public void acceptCodeOfConduct(String var1) {
      this.acceptedCodeOfConduct = var1.hashCode();
   }

   public void clearCodeOfConduct() {
      this.acceptedCodeOfConduct = 0;
   }

   public void copyNameIconFrom(ServerData var1) {
      this.ip = var1.ip;
      this.name = var1.name;
      this.iconBytes = var1.iconBytes;
   }

   public void copyFrom(ServerData var1) {
      this.copyNameIconFrom(var1);
      this.setResourcePackStatus(var1.getResourcePackStatus());
      this.type = var1.type;
   }

   public ServerData.State state() {
      return this.state;
   }

   public void setState(ServerData.State var1) {
      this.state = var1;
   }

   @Nullable
   public static byte[] validateIcon(@Nullable byte[] var0) {
      if (var0 != null) {
         try {
            PngInfo var1 = PngInfo.fromBytes(var0);
            if (var1.width() <= 1024 && var1.height() <= 1024) {
               return var0;
            }
         } catch (IOException var2) {
            LOGGER.warn("Failed to decode server icon", var2);
         }
      }

      return null;
   }

   public static enum ServerPackStatus {
      ENABLED("enabled"),
      DISABLED("disabled"),
      PROMPT("prompt");

      public static final MapCodec<ServerData.ServerPackStatus> FIELD_CODEC = Codec.BOOL.optionalFieldOf("acceptTextures").xmap((var0) -> {
         return (ServerData.ServerPackStatus)var0.map((var0x) -> {
            return var0x ? ENABLED : DISABLED;
         }).orElse(PROMPT);
      }, (var0) -> {
         Optional var10000;
         switch(var0.ordinal()) {
         case 0:
            var10000 = Optional.of(true);
            break;
         case 1:
            var10000 = Optional.of(false);
            break;
         case 2:
            var10000 = Optional.empty();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      });
      private final Component name;

      private ServerPackStatus(final String param3) {
         this.name = Component.translatable("manageServer.resourcePack." + var3);
      }

      public Component getName() {
         return this.name;
      }

      // $FF: synthetic method
      private static ServerData.ServerPackStatus[] $values() {
         return new ServerData.ServerPackStatus[]{ENABLED, DISABLED, PROMPT};
      }
   }

   public static enum State {
      INITIAL,
      PINGING,
      UNREACHABLE,
      INCOMPATIBLE,
      SUCCESSFUL;

      private State() {
      }

      // $FF: synthetic method
      private static ServerData.State[] $values() {
         return new ServerData.State[]{INITIAL, PINGING, UNREACHABLE, INCOMPATIBLE, SUCCESSFUL};
      }
   }

   public static enum Type {
      LAN,
      REALM,
      OTHER;

      private Type() {
      }

      // $FF: synthetic method
      private static ServerData.Type[] $values() {
         return new ServerData.Type[]{LAN, REALM, OTHER};
      }
   }
}
