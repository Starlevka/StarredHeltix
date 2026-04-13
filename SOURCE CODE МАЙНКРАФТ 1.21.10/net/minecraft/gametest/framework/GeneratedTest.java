package net.minecraft.gametest.framework;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public record GeneratedTest(Map<ResourceLocation, TestData<ResourceKey<TestEnvironmentDefinition>>> tests, ResourceKey<Consumer<GameTestHelper>> functionKey, Consumer<GameTestHelper> function) {
   public GeneratedTest(Map<ResourceLocation, TestData<ResourceKey<TestEnvironmentDefinition>>> var1, ResourceLocation var2, Consumer<GameTestHelper> var3) {
      this(var1, ResourceKey.create(Registries.TEST_FUNCTION, var2), var3);
   }

   public GeneratedTest(ResourceLocation var1, TestData<ResourceKey<TestEnvironmentDefinition>> var2, Consumer<GameTestHelper> var3) {
      this(Map.of(var1, var2), var1, var3);
   }

   public GeneratedTest(Map<ResourceLocation, TestData<ResourceKey<TestEnvironmentDefinition>>> param1, ResourceKey<Consumer<GameTestHelper>> param2, Consumer<GameTestHelper> param3) {
      super();
      this.tests = var1;
      this.functionKey = var2;
      this.function = var3;
   }

   public Map<ResourceLocation, TestData<ResourceKey<TestEnvironmentDefinition>>> tests() {
      return this.tests;
   }

   public ResourceKey<Consumer<GameTestHelper>> functionKey() {
      return this.functionKey;
   }

   public Consumer<GameTestHelper> function() {
      return this.function;
   }
}
