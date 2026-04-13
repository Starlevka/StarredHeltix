package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.util.ExtraCodecs;

public class BanlistService {
   private static final String BAN_SOURCE = "Management server";

   public BanlistService() {
      super();
   }

   public static List<BanlistService.UserBanDto> get(MinecraftApi var0) {
      return var0.banListService().getUserBanEntries().stream().filter((var0x) -> {
         return var0x.getUser() != null;
      }).map(BanlistService.UserBan::from).map(BanlistService.UserBanDto::from).toList();
   }

   public static List<BanlistService.UserBanDto> add(MinecraftApi var0, List<BanlistService.UserBanDto> var1, ClientInfo var2) {
      List var3 = var1.stream().map((var1x) -> {
         return var0.playerListService().getUser(var1x.player().id(), var1x.player().name()).thenApply((var1) -> {
            Objects.requireNonNull(var1x);
            return var1.map(var1x::toUserBan);
         });
      }).toList();
      Iterator var4 = ((List)Util.sequence(var3).join()).iterator();

      while(var4.hasNext()) {
         Optional var5 = (Optional)var4.next();
         if (!var5.isEmpty()) {
            BanlistService.UserBan var6 = (BanlistService.UserBan)var5.get();
            var0.banListService().addUserBan(var6.toBanEntry(), var2);
            ServerPlayer var7 = var0.playerListService().getPlayer(((BanlistService.UserBan)var5.get()).player().id());
            if (var7 != null) {
               var7.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
            }
         }
      }

      return get(var0);
   }

   public static List<BanlistService.UserBanDto> clear(MinecraftApi var0, ClientInfo var1) {
      var0.banListService().clearUserBans(var1);
      return get(var0);
   }

   public static List<BanlistService.UserBanDto> remove(MinecraftApi var0, List<PlayerDto> var1, ClientInfo var2) {
      List var3 = var1.stream().map((var1x) -> {
         return var0.playerListService().getUser(var1x.id(), var1x.name());
      }).toList();
      Iterator var4 = ((List)Util.sequence(var3).join()).iterator();

      while(var4.hasNext()) {
         Optional var5 = (Optional)var4.next();
         if (!var5.isEmpty()) {
            var0.banListService().removeUserBan((NameAndId)var5.get(), var2);
         }
      }

      return get(var0);
   }

   public static List<BanlistService.UserBanDto> set(MinecraftApi var0, List<BanlistService.UserBanDto> var1, ClientInfo var2) {
      List var3 = var1.stream().map((var1x) -> {
         return var0.playerListService().getUser(var1x.player().id(), var1x.player().name()).thenApply((var1) -> {
            Objects.requireNonNull(var1x);
            return var1.map(var1x::toUserBan);
         });
      }).toList();
      Set var4 = (Set)((List)Util.sequence(var3).join()).stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
      Set var5 = (Set)var0.banListService().getUserBanEntries().stream().filter((var0x) -> {
         return var0x.getUser() != null;
      }).map(BanlistService.UserBan::from).collect(Collectors.toSet());
      var5.stream().filter((var1x) -> {
         return !var4.contains(var1x);
      }).forEach((var2x) -> {
         var0.banListService().removeUserBan(var2x.player(), var2);
      });
      var4.stream().filter((var1x) -> {
         return !var5.contains(var1x);
      }).forEach((var2x) -> {
         var0.banListService().addUserBan(var2x.toBanEntry(), var2);
         ServerPlayer var3 = var0.playerListService().getPlayer(var2x.player().id());
         if (var3 != null) {
            var3.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
         }

      });
      return get(var0);
   }

   private static record UserBan(NameAndId player, @Nullable String reason, String source, Optional<Instant> expires) {
      UserBan(NameAndId param1, @Nullable String param2, String param3, Optional<Instant> param4) {
         super();
         this.player = var1;
         this.reason = var2;
         this.source = var3;
         this.expires = var4;
      }

      static BanlistService.UserBan from(UserBanListEntry var0) {
         return new BanlistService.UserBan((NameAndId)Objects.requireNonNull((NameAndId)var0.getUser()), var0.getReason(), var0.getSource(), Optional.ofNullable(var0.getExpires()).map(Date::toInstant));
      }

      UserBanListEntry toBanEntry() {
         return new UserBanListEntry(new NameAndId(this.player().id(), this.player().name()), (Date)null, this.source(), (Date)this.expires().map(Date::from).orElse((Object)null), this.reason());
      }

      public NameAndId player() {
         return this.player;
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

   public static record UserBanDto(PlayerDto player, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
      public static final MapCodec<BanlistService.UserBanDto> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(PlayerDto.CODEC.codec().fieldOf("player").forGetter(BanlistService.UserBanDto::player), Codec.STRING.optionalFieldOf("reason").forGetter(BanlistService.UserBanDto::reason), Codec.STRING.optionalFieldOf("source").forGetter(BanlistService.UserBanDto::source), ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(BanlistService.UserBanDto::expires)).apply(var0, BanlistService.UserBanDto::new);
      });

      public UserBanDto(PlayerDto param1, Optional<String> param2, Optional<String> param3, Optional<Instant> param4) {
         super();
         this.player = var1;
         this.reason = var2;
         this.source = var3;
         this.expires = var4;
      }

      private static BanlistService.UserBanDto from(BanlistService.UserBan var0) {
         return new BanlistService.UserBanDto(PlayerDto.from(var0.player()), Optional.ofNullable(var0.reason()), Optional.of(var0.source()), var0.expires());
      }

      public static BanlistService.UserBanDto from(UserBanListEntry var0) {
         return from(BanlistService.UserBan.from(var0));
      }

      private BanlistService.UserBan toUserBan(NameAndId var1) {
         return new BanlistService.UserBan(var1, (String)this.reason().orElse((Object)null), (String)this.source().orElse("Management server"), this.expires());
      }

      public PlayerDto player() {
         return this.player;
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
