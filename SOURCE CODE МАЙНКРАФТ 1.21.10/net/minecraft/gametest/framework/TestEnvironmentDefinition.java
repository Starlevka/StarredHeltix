package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;

public interface TestEnvironmentDefinition {
   Codec<TestEnvironmentDefinition> DIRECT_CODEC = BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE.byNameCodec().dispatch(TestEnvironmentDefinition::codec, (var0) -> {
      return var0;
   });
   Codec<Holder<TestEnvironmentDefinition>> CODEC = RegistryFileCodec.create(Registries.TEST_ENVIRONMENT, DIRECT_CODEC);

   static MapCodec<? extends TestEnvironmentDefinition> bootstrap(Registry<MapCodec<? extends TestEnvironmentDefinition>> var0) {
      Registry.register(var0, (String)"all_of", TestEnvironmentDefinition.AllOf.CODEC);
      Registry.register(var0, (String)"game_rules", TestEnvironmentDefinition.SetGameRules.CODEC);
      Registry.register(var0, (String)"time_of_day", TestEnvironmentDefinition.TimeOfDay.CODEC);
      Registry.register(var0, (String)"weather", TestEnvironmentDefinition.Weather.CODEC);
      return (MapCodec)Registry.register(var0, (String)"function", TestEnvironmentDefinition.Functions.CODEC);
   }

   void setup(ServerLevel var1);

   default void teardown(ServerLevel var1) {
   }

   MapCodec<? extends TestEnvironmentDefinition> codec();

   public static record AllOf(List<Holder<TestEnvironmentDefinition>> definitions) implements TestEnvironmentDefinition {
      public static final MapCodec<TestEnvironmentDefinition.AllOf> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(TestEnvironmentDefinition.CODEC.listOf().fieldOf("definitions").forGetter(TestEnvironmentDefinition.AllOf::definitions)).apply(var0, TestEnvironmentDefinition.AllOf::new);
      });

      public AllOf(TestEnvironmentDefinition... var1) {
         this(Arrays.stream(var1).map(Holder::direct).toList());
      }

      public AllOf(List<Holder<TestEnvironmentDefinition>> param1) {
         super();
         this.definitions = var1;
      }

      public void setup(ServerLevel var1) {
         this.definitions.forEach((var1x) -> {
            ((TestEnvironmentDefinition)var1x.value()).setup(var1);
         });
      }

      public void teardown(ServerLevel var1) {
         this.definitions.forEach((var1x) -> {
            ((TestEnvironmentDefinition)var1x.value()).teardown(var1);
         });
      }

      public MapCodec<TestEnvironmentDefinition.AllOf> codec() {
         return CODEC;
      }

      public List<Holder<TestEnvironmentDefinition>> definitions() {
         return this.definitions;
      }
   }

   public static record SetGameRules(List<TestEnvironmentDefinition.SetGameRules.Entry<Boolean, GameRules.BooleanValue>> boolRules, List<TestEnvironmentDefinition.SetGameRules.Entry<Integer, GameRules.IntegerValue>> intRules) implements TestEnvironmentDefinition {
      public static final MapCodec<TestEnvironmentDefinition.SetGameRules> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(TestEnvironmentDefinition.SetGameRules.Entry.codec(GameRules.BooleanValue.class, Codec.BOOL).listOf().fieldOf("bool_rules").forGetter(TestEnvironmentDefinition.SetGameRules::boolRules), TestEnvironmentDefinition.SetGameRules.Entry.codec(GameRules.IntegerValue.class, Codec.INT).listOf().fieldOf("int_rules").forGetter(TestEnvironmentDefinition.SetGameRules::intRules)).apply(var0, TestEnvironmentDefinition.SetGameRules::new);
      });

      public SetGameRules(List<TestEnvironmentDefinition.SetGameRules.Entry<Boolean, GameRules.BooleanValue>> param1, List<TestEnvironmentDefinition.SetGameRules.Entry<Integer, GameRules.IntegerValue>> param2) {
         super();
         this.boolRules = var1;
         this.intRules = var2;
      }

      public void setup(ServerLevel var1) {
         GameRules var2 = var1.getGameRules();
         MinecraftServer var3 = var1.getServer();
         Iterator var4 = this.boolRules.iterator();

         TestEnvironmentDefinition.SetGameRules.Entry var5;
         while(var4.hasNext()) {
            var5 = (TestEnvironmentDefinition.SetGameRules.Entry)var4.next();
            ((GameRules.BooleanValue)var2.getRule(var5.key())).set((Boolean)var5.value(), var3);
         }

         var4 = this.intRules.iterator();

         while(var4.hasNext()) {
            var5 = (TestEnvironmentDefinition.SetGameRules.Entry)var4.next();
            ((GameRules.IntegerValue)var2.getRule(var5.key())).set((Integer)var5.value(), var3);
         }

      }

      public void teardown(ServerLevel var1) {
         GameRules var2 = var1.getGameRules();
         MinecraftServer var3 = var1.getServer();
         Iterator var4 = this.boolRules.iterator();

         TestEnvironmentDefinition.SetGameRules.Entry var5;
         while(var4.hasNext()) {
            var5 = (TestEnvironmentDefinition.SetGameRules.Entry)var4.next();
            ((GameRules.BooleanValue)var2.getRule(var5.key())).setFrom((GameRules.BooleanValue)GameRules.getType(var5.key()).createRule(), var3);
         }

         var4 = this.intRules.iterator();

         while(var4.hasNext()) {
            var5 = (TestEnvironmentDefinition.SetGameRules.Entry)var4.next();
            ((GameRules.IntegerValue)var2.getRule(var5.key())).setFrom((GameRules.IntegerValue)GameRules.getType(var5.key()).createRule(), var3);
         }

      }

      public MapCodec<TestEnvironmentDefinition.SetGameRules> codec() {
         return CODEC;
      }

      public static <S, T extends GameRules.Value<T>> TestEnvironmentDefinition.SetGameRules.Entry<S, T> entry(GameRules.Key<T> var0, S var1) {
         return new TestEnvironmentDefinition.SetGameRules.Entry(var0, var1);
      }

      public List<TestEnvironmentDefinition.SetGameRules.Entry<Boolean, GameRules.BooleanValue>> boolRules() {
         return this.boolRules;
      }

      public List<TestEnvironmentDefinition.SetGameRules.Entry<Integer, GameRules.IntegerValue>> intRules() {
         return this.intRules;
      }

      public static record Entry<S, T extends GameRules.Value<T>>(GameRules.Key<T> key, S value) {
         public Entry(GameRules.Key<T> param1, S param2) {
            super();
            this.key = var1;
            this.value = var2;
         }

         public static <S, T extends GameRules.Value<T>> Codec<TestEnvironmentDefinition.SetGameRules.Entry<S, T>> codec(Class<T> var0, Codec<S> var1) {
            return RecordCodecBuilder.create((var2) -> {
               return var2.group(GameRules.keyCodec(var0).fieldOf("rule").forGetter(TestEnvironmentDefinition.SetGameRules.Entry::key), var1.fieldOf("value").forGetter(TestEnvironmentDefinition.SetGameRules.Entry::value)).apply(var2, TestEnvironmentDefinition.SetGameRules.Entry::new);
            });
         }

         public GameRules.Key<T> key() {
            return this.key;
         }

         public S value() {
            return this.value;
         }
      }
   }

   public static record TimeOfDay(int time) implements TestEnvironmentDefinition {
      public static final MapCodec<TestEnvironmentDefinition.TimeOfDay> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ExtraCodecs.NON_NEGATIVE_INT.fieldOf("time").forGetter(TestEnvironmentDefinition.TimeOfDay::time)).apply(var0, TestEnvironmentDefinition.TimeOfDay::new);
      });

      public TimeOfDay(int param1) {
         super();
         this.time = var1;
      }

      public void setup(ServerLevel var1) {
         var1.setDayTime((long)this.time);
      }

      public MapCodec<TestEnvironmentDefinition.TimeOfDay> codec() {
         return CODEC;
      }

      public int time() {
         return this.time;
      }
   }

   public static record Weather(TestEnvironmentDefinition.Weather.Type weather) implements TestEnvironmentDefinition {
      public static final MapCodec<TestEnvironmentDefinition.Weather> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(TestEnvironmentDefinition.Weather.Type.CODEC.fieldOf("weather").forGetter(TestEnvironmentDefinition.Weather::weather)).apply(var0, TestEnvironmentDefinition.Weather::new);
      });

      public Weather(TestEnvironmentDefinition.Weather.Type param1) {
         super();
         this.weather = var1;
      }

      public void setup(ServerLevel var1) {
         this.weather.apply(var1);
      }

      public void teardown(ServerLevel var1) {
         var1.resetWeatherCycle();
      }

      public MapCodec<TestEnvironmentDefinition.Weather> codec() {
         return CODEC;
      }

      public TestEnvironmentDefinition.Weather.Type weather() {
         return this.weather;
      }

      public static enum Type implements StringRepresentable {
         CLEAR("clear", 100000, 0, false, false),
         RAIN("rain", 0, 100000, true, false),
         THUNDER("thunder", 0, 100000, true, true);

         public static final Codec<TestEnvironmentDefinition.Weather.Type> CODEC = StringRepresentable.fromEnum(TestEnvironmentDefinition.Weather.Type::values);
         private final String id;
         private final int clearTime;
         private final int rainTime;
         private final boolean raining;
         private final boolean thundering;

         private Type(final String param3, final int param4, final int param5, final boolean param6, final boolean param7) {
            this.id = var3;
            this.clearTime = var4;
            this.rainTime = var5;
            this.raining = var6;
            this.thundering = var7;
         }

         void apply(ServerLevel var1) {
            var1.setWeatherParameters(this.clearTime, this.rainTime, this.raining, this.thundering);
         }

         public String getSerializedName() {
            return this.id;
         }

         // $FF: synthetic method
         private static TestEnvironmentDefinition.Weather.Type[] $values() {
            return new TestEnvironmentDefinition.Weather.Type[]{CLEAR, RAIN, THUNDER};
         }
      }
   }

   public static record Functions(Optional<ResourceLocation> setupFunction, Optional<ResourceLocation> teardownFunction) implements TestEnvironmentDefinition {
      private static final Logger LOGGER = LogUtils.getLogger();
      public static final MapCodec<TestEnvironmentDefinition.Functions> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ResourceLocation.CODEC.optionalFieldOf("setup").forGetter(TestEnvironmentDefinition.Functions::setupFunction), ResourceLocation.CODEC.optionalFieldOf("teardown").forGetter(TestEnvironmentDefinition.Functions::teardownFunction)).apply(var0, TestEnvironmentDefinition.Functions::new);
      });

      public Functions(Optional<ResourceLocation> param1, Optional<ResourceLocation> param2) {
         super();
         this.setupFunction = var1;
         this.teardownFunction = var2;
      }

      public void setup(ServerLevel var1) {
         this.setupFunction.ifPresent((var1x) -> {
            run(var1, var1x);
         });
      }

      public void teardown(ServerLevel var1) {
         this.teardownFunction.ifPresent((var1x) -> {
            run(var1, var1x);
         });
      }

      private static void run(ServerLevel var0, ResourceLocation var1) {
         MinecraftServer var2 = var0.getServer();
         ServerFunctionManager var3 = var2.getFunctions();
         Optional var4 = var3.get(var1);
         if (var4.isPresent()) {
            CommandSourceStack var5 = var2.createCommandSourceStack().withPermission(2).withSuppressedOutput().withLevel(var0);
            var3.execute((CommandFunction)var4.get(), var5);
         } else {
            LOGGER.error("Test Batch failed for non-existent function {}", var1);
         }

      }

      public MapCodec<TestEnvironmentDefinition.Functions> codec() {
         return CODEC;
      }

      public Optional<ResourceLocation> setupFunction() {
         return this.setupFunction;
      }

      public Optional<ResourceLocation> teardownFunction() {
         return this.teardownFunction;
      }
   }
}
