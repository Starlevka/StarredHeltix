package net.minecraft.gametest.framework;

import java.util.Collection;
import net.minecraft.core.Holder;

public record GameTestBatch(int index, Collection<GameTestInfo> gameTestInfos, Holder<TestEnvironmentDefinition> environment) {
   public GameTestBatch(int param1, Collection<GameTestInfo> param2, Holder<TestEnvironmentDefinition> param3) {
      super();
      if (var2.isEmpty()) {
         throw new IllegalArgumentException("A GameTestBatch must include at least one GameTestInfo!");
      } else {
         this.index = var1;
         this.gameTestInfos = var2;
         this.environment = var3;
      }
   }

   public int index() {
      return this.index;
   }

   public Collection<GameTestInfo> gameTestInfos() {
      return this.gameTestInfos;
   }

   public Holder<TestEnvironmentDefinition> environment() {
      return this.environment;
   }
}
