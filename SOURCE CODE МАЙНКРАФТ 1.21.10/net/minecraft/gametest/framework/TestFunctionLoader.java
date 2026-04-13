package net.minecraft.gametest.framework;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public abstract class TestFunctionLoader {
   private static final List<TestFunctionLoader> loaders = new ArrayList();

   public TestFunctionLoader() {
      super();
   }

   public static void registerLoader(TestFunctionLoader var0) {
      loaders.add(var0);
   }

   public static void runLoaders(Registry<Consumer<GameTestHelper>> var0) {
      Iterator var1 = loaders.iterator();

      while(var1.hasNext()) {
         TestFunctionLoader var2 = (TestFunctionLoader)var1.next();
         var2.load((var1x, var2x) -> {
            Registry.register(var0, (ResourceKey)var1x, var2x);
         });
      }

   }

   public abstract void load(BiConsumer<ResourceKey<Consumer<GameTestHelper>>, Consumer<GameTestHelper>> var1);
}
