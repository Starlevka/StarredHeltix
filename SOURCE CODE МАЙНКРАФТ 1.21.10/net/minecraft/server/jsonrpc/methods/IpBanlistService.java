package net.minecraft.server.jsonrpc.methods;

import com.google.common.net.InetAddresses;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.util.ExtraCodecs;

public class IpBanlistService {
   private static final String BAN_SOURCE = "Management server";

   public IpBanlistService() {
      super();
   }

   public static List<IpBanlistService.IpBanDto> get(MinecraftApi var0) {
      return var0.banListService().getIpBanEntries().stream().map(IpBanlistService.IpBan::from).map(IpBanlistService.IpBanDto::from).toList();
   }

   public static List<IpBanlistService.IpBanDto> add(MinecraftApi var0, List<IpBanlistService.IncomingIpBanDto> var1, ClientInfo var2) {
      var1.stream().map((var2x) -> {
         return banIp(var0, var2x, var2);
      }).flatMap(Collection::stream).forEach((var0x) -> {
         var0x.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned"));
      });
      return get(var0);
   }

   private static List<ServerPlayer> banIp(MinecraftApi var0, IpBanlistService.IncomingIpBanDto var1, ClientInfo var2) {
      IpBanlistService.IpBan var3 = var1.toIpBan();
      if (var3 != null) {
         return banIp(var0, var3, var2);
      } else {
         if (var1.player().isPresent()) {
            Optional var4 = var0.playerListService().getPlayer(((PlayerDto)var1.player().get()).id(), ((PlayerDto)var1.player().get()).name());
            if (var4.isPresent()) {
               return banIp(var0, var1.toIpBan((ServerPlayer)var4.get()), var2);
            }
         }

         return List.of();
      }
   }

   private static List<ServerPlayer> banIp(MinecraftApi var0, IpBanlistService.IpBan var1, ClientInfo var2) {
      var0.banListService().addIpBan(var1.toIpBanEntry(), var2);
      return var0.playerListService().getPlayersWithAddress(var1.ip());
   }

   public static List<IpBanlistService.IpBanDto> clear(MinecraftApi var0, ClientInfo var1) {
      var0.banListService().clearIpBans(var1);
      return get(var0);
   }

   public static List<IpBanlistService.IpBanDto> remove(MinecraftApi var0, List<String> var1, ClientInfo var2) {
      var1.forEach((var2x) -> {
         var0.banListService().removeIpBan(var2x, var2);
      });
      return get(var0);
   }

   public static List<IpBanlistService.IpBanDto> set(MinecraftApi var0, List<IpBanlistService.IpBanDto> var1, ClientInfo var2) {
      Set var3 = (Set)var1.stream().filter((var0x) -> {
         return InetAddresses.isInetAddress(var0x.ip());
      }).map(IpBanlistService.IpBanDto::toIpBan).collect(Collectors.toSet());
      Set var4 = (Set)var0.banListService().getIpBanEntries().stream().map(IpBanlistService.IpBan::from).collect(Collectors.toSet());
      var4.stream().filter((var1x) -> {
         return !var3.contains(var1x);
      }).forEach((var2x) -> {
         var0.banListService().removeIpBan(var2x.ip(), var2);
      });
      var3.stream().filter((var1x) -> {
         return !var4.contains(var1x);
      }).forEach((var2x) -> {
         var0.banListService().addIpBan(var2x.toIpBanEntry(), var2);
      });
      var3.stream().filter((var1x) -> {
         return !var4.contains(var1x);
      }).flatMap((var1x) -> {
         return var0.playerListService().getPlayersWithAddress(var1x.ip()).stream();
      }).forEach((var0x) -> {
         var0x.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned"));
      });
      return get(var0);
   }

   public static record IncomingIpBanDto(Optional<PlayerDto> player, Optional<String> ip, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
      public static final MapCodec<IpBanlistService.IncomingIpBanDto> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(PlayerDto.CODEC.codec().optionalFieldOf("player").forGetter(IpBanlistService.IncomingIpBanDto::player), Codec.STRING.optionalFieldOf("ip").forGetter(IpBanlistService.IncomingIpBanDto::ip), Codec.STRING.optionalFieldOf("reason").forGetter(IpBanlistService.IncomingIpBanDto::reason), Codec.STRING.optionalFieldOf("source").forGetter(IpBanlistService.IncomingIpBanDto::source), ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(IpBanlistService.IncomingIpBanDto::expires)).apply(var0, IpBanlistService.IncomingIpBanDto::new);
      });

      public IncomingIpBanDto(Optional<PlayerDto> param1, Optional<String> param2, Optional<String> param3, Optional<String> param4, Optional<Instant> param5) {
         super();
         this.player = var1;
         this.ip = var2;
         this.reason = var3;
         this.source = var4;
         this.expires = var5;
      }

      IpBanlistService.IpBan toIpBan(ServerPlayer var1) {
         return new IpBanlistService.IpBan(var1.getIpAddress(), (String)this.reason().orElse((Object)null), (String)this.source().orElse("Management server"), this.expires());
      }

      @Nullable
      IpBanlistService.IpBan toIpBan() {
         return !this.ip().isEmpty() && InetAddresses.isInetAddress((String)this.ip().get()) ? new IpBanlistService.IpBan((String)this.ip().get(), (String)this.reason().orElse((Object)null), (String)this.source().orElse("Management server"), this.expires()) : null;
      }

      public Optional<PlayerDto> player() {
         return this.player;
      }

      public Optional<String> ip() {
         return this.ip;
      }

      public Optional<String> reason() {
         return this.reason;
      }

      public Optional<String> source() {
         return this.source;
      }

      public Optional<Instant> expires() {
         return this.expires;
      }
   }

   private static record IpBan(String ip, @Nullable String reason, String source, Optional<Instant> expires) {
      IpBan(String param1, @Nullable String param2, String param3, Optional<Instant> param4) {
         super();
         this.ip = var1;
         this.reason = var2;
         this.source = var3;
         this.expires = var4;
      }

      static IpBanlistService.IpBan from(IpBanListEntry var0) {
         return new IpBanlistService.IpBan((String)Objects.requireNonNull((String)var0.getUser()), var0.getReason(), var0.getSource(), Optional.ofNullable(var0.getExpires()).map(Date::toInstant));
      }

      IpBanListEntry toIpBanEntry() {
         return new IpBanListEntry(this.ip(), (Date)null, this.source(), (Date)this.expires().map(Date::from).orElse((Object)null), this.reason());
      }

      public String ip() {
         return this.ip;
      }

      @Nullable
      public String reason() {
         return this.reason;
      }

      public String source() {
         return this.source;
      }

      public Optional<Instant> expires() {
         return this.expires;
      }
   }

   public static record IpBanDto(String ip, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
      public static final MapCodec<IpBanlistService.IpBanDto> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.STRING.fieldOf("ip").forGetter(IpBanlistService.IpBanDto::ip), Codec.STRING.optionalFieldOf("reason").forGetter(IpBanlistService.IpBanDto::reason), Codec.STRING.optionalFieldOf("source").forGetter(IpBanlistService.IpBanDto::source), ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(IpBanlistService.IpBanDto::expires)).apply(var0, IpBanlistService.IpBanDto::new);
      });

      public IpBanDto(String param1, Optional<String> param2, Optional<String> param3, Optional<Instant> param4) {
         super();
         this.ip = var1;
         this.reason = var2;
         this.source = var3;
         this.expires = var4;
      }

      private static IpBanlistService.IpBanDto from(IpBanlistService.IpBan var0) {
         return new IpBanlistService.IpBanDto(var0.ip(), Optional.ofNullable(var0.reason()), Optional.of(var0.source()), var0.expires());
      }

      public static IpBanlistService.IpBanDto from(IpBanListEntry var0) {
         return from(IpBanlistService.IpBan.from(var0));
      }

      private IpBanlistService.IpBan toIpBan() {
         return new IpBanlistService.IpBan(this.ip(), (String)this.reason().orElse((Object)null), (String)this.source().orElse("Management server"), this.expires());
      }

      public String ip() {
         return this.ip;
      }

      public Optional<String> reason() {
         return this.reason;
      }

      public Optional<String> source() {
         return this.source;
      }

      public Optional<Instant> expires() {
         return this.expires;
      }
   }
}
