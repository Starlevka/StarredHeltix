package net.minecraft.server.packs.resources;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@FunctionalInterface
public interface PreparableReloadListener {
   CompletableFuture<Void> reload(PreparableReloadListener.SharedState var1, Executor var2, PreparableReloadListener.PreparationBarrier var3, Executor var4);

   default void prepareSharedState(PreparableReloadListener.SharedState var1) {
   }

   default String getName() {
      return this.getClass().getSimpleName();
   }

   public static final class SharedState {
      private final ResourceManager manager;
      private final Map<PreparableReloadListener.StateKey<?>, Object> state = new IdentityHashMap();

      public SharedState(ResourceManager var1) {
         super();
         this.manager = var1;
      }

      public ResourceManager resourceManager() {
         return this.manager;
      }

      public <T> void set(PreparableReloadListener.StateKey<T> var1, T var2) {
         this.state.put(var1, var2);
      }

      public <T> T get(PreparableReloadListener.StateKey<T> var1) {
         return Objects.requireNonNull(this.state.get(var1));
      }
   }

   public static final class StateKey<T> {
      public StateKey() {
         super();
      }
   }

   @FunctionalInterface
   public interface PreparationBarrier {
      <T> CompletableFuture<T> wait(T var1);
   }
}
