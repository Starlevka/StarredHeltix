package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.GameRules;

public class GameRulesService {
   public GameRulesService() {
      super();
   }

   public static List<GameRulesService.TypedRule> get(MinecraftApi var0) {
      List var1 = var0.gameRuleService().getAvailableGameRules().map(Entry::getKey).toList();
      ArrayList var2 = new ArrayList();
      Iterator var3 = var1.iterator();

      while(var3.hasNext()) {
         GameRules.Key var4 = (GameRules.Key)var3.next();
         GameRules.Value var5 = var0.gameRuleService().getRule(var4);
         var2.add(getTypedRule(var0, var4.getId(), var5));
      }

      return var2;
   }

   public static GameRulesService.TypedRule getTypedRule(MinecraftApi var0, String var1, GameRules.Value<?> var2) {
      return var0.gameRuleService().getTypedRule(var1, var2);
   }

   public static GameRulesService.TypedRule update(MinecraftApi var0, GameRulesService.UntypedRule var1, ClientInfo var2) {
      return var0.gameRuleService().updateGameRule(var1, var2);
   }

   public static record TypedRule(String key, String value, GameRulesService.RuleType type) {
      public static final MapCodec<GameRulesService.TypedRule> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.STRING.fieldOf("key").forGetter(GameRulesService.TypedRule::key), Codec.STRING.fieldOf("value").forGetter(GameRulesService.TypedRule::value), StringRepresentable.fromEnum(GameRulesService.RuleType::values).fieldOf("type").forGetter(GameRulesService.TypedRule::type)).apply(var0, GameRulesService.TypedRule::new);
      });

      public TypedRule(String param1, String param2, GameRulesService.RuleType param3) {
         super();
         this.key = var1;
         this.value = var2;
         this.type = var3;
      }

      public String key() {
         return this.key;
      }

      public String value() {
         return this.value;
      }

      public GameRulesService.RuleType type() {
         return this.type;
      }
   }

   public static record UntypedRule(String key, String value) {
      public static final MapCodec<GameRulesService.UntypedRule> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.STRING.fieldOf("key").forGetter(GameRulesService.UntypedRule::key), Codec.STRING.fieldOf("value").forGetter(GameRulesService.UntypedRule::value)).apply(var0, GameRulesService.UntypedRule::new);
      });

      public UntypedRule(String param1, String param2) {
         super();
         this.key = var1;
         this.value = var2;
      }

      public String key() {
         return this.key;
      }

      public String value() {
         return this.value;
      }
   }

   public static enum RuleType implements StringRepresentable {
      INT("integer"),
      BOOL("boolean");

      private final String name;

      private RuleType(final String param3) {
         this.name = var3;
      }

      public String getSerializedName() {
         return this.name;
      }

      // $FF: synthetic method
      private static GameRulesService.RuleType[] $values() {
         return new GameRulesService.RuleType[]{INT, BOOL};
      }
   }
}
