package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;

public class OperatorService {
   public OperatorService() {
      super();
   }

   public static List<OperatorService.OperatorDto> get(MinecraftApi var0) {
      return var0.operatorListService().getEntries().stream().filter((var0x) -> {
         return var0x.getUser() != null;
      }).map(OperatorService.OperatorDto::from).toList();
   }

   public static List<OperatorService.OperatorDto> clear(MinecraftApi var0, ClientInfo var1) {
      var0.operatorListService().clear(var1);
      return get(var0);
   }

   public static List<OperatorService.OperatorDto> remove(MinecraftApi var0, List<PlayerDto> var1, ClientInfo var2) {
      List var3 = var1.stream().map((var1x) -> {
         return var0.playerListService().getUser(var1x.id(), var1x.name());
      }).toList();
      Iterator var4 = ((List)Util.sequence(var3).join()).iterator();

      while(var4.hasNext()) {
         Optional var5 = (Optional)var4.next();
         var5.ifPresent((var2x) -> {
            var0.operatorListService().deop(var2x, var2);
         });
      }

      return get(var0);
   }

   public static List<OperatorService.OperatorDto> add(MinecraftApi var0, List<OperatorService.OperatorDto> var1, ClientInfo var2) {
      List var3 = var1.stream().map((var1x) -> {
         return var0.playerListService().getUser(var1x.player().id(), var1x.player().name()).thenApply((var1) -> {
            return var1.map((var1xx) -> {
               return new OperatorService.Op(var1xx, var1x.permissionLevel(), var1x.bypassesPlayerLimit());
            });
         });
      }).toList();
      Iterator var4 = ((List)Util.sequence(var3).join()).iterator();

      while(var4.hasNext()) {
         Optional var5 = (Optional)var4.next();
         var5.ifPresent((var2x) -> {
            var0.operatorListService().op(var2x.user(), var2x.permissionLevel(), var2x.bypassesPlayerLimit(), var2);
         });
      }

      return get(var0);
   }

   public static List<OperatorService.OperatorDto> set(MinecraftApi var0, List<OperatorService.OperatorDto> var1, ClientInfo var2) {
      List var3 = var1.stream().map((var1x) -> {
         return var0.playerListService().getUser(var1x.player().id(), var1x.player().name()).thenApply((var1) -> {
            return var1.map((var1xx) -> {
               return new OperatorService.Op(var1xx, var1x.permissionLevel(), var1x.bypassesPlayerLimit());
            });
         });
      }).toList();
      Set var4 = (Set)((List)Util.sequence(var3).join()).stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
      Set var5 = (Set)var0.operatorListService().getEntries().stream().filter((var0x) -> {
         return var0x.getUser() != null;
      }).map((var0x) -> {
         return new OperatorService.Op((NameAndId)var0x.getUser(), Optional.of(var0x.getLevel()), Optional.of(var0x.getBypassesPlayerLimit()));
      }).collect(Collectors.toSet());
      var5.stream().filter((var1x) -> {
         return !var4.contains(var1x);
      }).forEach((var2x) -> {
         var0.operatorListService().deop(var2x.user(), var2);
      });
      var4.stream().filter((var1x) -> {
         return !var5.contains(var1x);
      }).forEach((var2x) -> {
         var0.operatorListService().op(var2x.user(), var2x.permissionLevel(), var2x.bypassesPlayerLimit(), var2);
      });
      return get(var0);
   }

   static record Op(NameAndId user, Optional<Integer> permissionLevel, Optional<Boolean> bypassesPlayerLimit) {
      Op(NameAndId param1, Optional<Integer> param2, Optional<Boolean> param3) {
         super();
         this.user = var1;
         this.permissionLevel = var2;
         this.bypassesPlayerLimit = var3;
      }

      public NameAndId user() {
         return this.user;
      }

      public Optional<Integer> permissionLevel() {
         return this.permissionLevel;
      }

      public Optional<Boolean> bypassesPlayerLimit() {
         return this.bypassesPlayerLimit;
      }
   }

   public static record OperatorDto(PlayerDto player, Optional<Integer> permissionLevel, Optional<Boolean> bypassesPlayerLimit) {
      public static final MapCodec<OperatorService.OperatorDto> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(PlayerDto.CODEC.codec().fieldOf("player").forGetter(OperatorService.OperatorDto::player), Codec.INT.optionalFieldOf("permissionLevel").forGetter(OperatorService.OperatorDto::permissionLevel), Codec.BOOL.optionalFieldOf("bypassesPlayerLimit").forGetter(OperatorService.OperatorDto::bypassesPlayerLimit)).apply(var0, OperatorService.OperatorDto::new);
      });

      public OperatorDto(PlayerDto param1, Optional<Integer> param2, Optional<Boolean> param3) {
         super();
         this.player = var1;
         this.permissionLevel = var2;
         this.bypassesPlayerLimit = var3;
      }

      public static OperatorService.OperatorDto from(ServerOpListEntry var0) {
         return new OperatorService.OperatorDto(PlayerDto.from((NameAndId)Objects.requireNonNull((NameAndId)var0.getUser())), Optional.of(var0.getLevel()), Optional.of(var0.getBypassesPlayerLimit()));
      }

      public PlayerDto player() {
         return this.player;
      }

      public Optional<Integer> permissionLevel() {
         return this.permissionLevel;
      }

      public Optional<Boolean> bypassesPlayerLimit() {
         return this.bypassesPlayerLimit;
      }
   }
}
