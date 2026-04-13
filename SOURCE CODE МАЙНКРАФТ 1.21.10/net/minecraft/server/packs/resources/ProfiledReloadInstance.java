package net.minecraft.server.packs.resources;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.Util;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class ProfiledReloadInstance extends SimpleReloadInstance<ProfiledReloadInstance.State> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Stopwatch total = Stopwatch.createUnstarted();

   public static ReloadInstance of(ResourceManager var0, List<PreparableReloadListener> var1, Executor var2, Executor var3, CompletableFuture<Unit> var4) {
      ProfiledReloadInstance var5 = new ProfiledReloadInstance(var1);
      var5.startTasks(var2, var3, var0, var1, (var1x, var2x, var3x, var4x, var5x) -> {
         AtomicLong var6 = new AtomicLong();
         AtomicLong var7 = new AtomicLong();
         AtomicLong var8 = new AtomicLong();
         AtomicLong var9 = new AtomicLong();
         CompletableFuture var10 = var3x.reload(var1x, profiledExecutor(var4x, var6, var7, var3x.getName()), var2x, profiledExecutor(var5x, var8, var9, var3x.getName()));
         return var10.thenApplyAsync((var5) -> {
            LOGGER.debug("Finished reloading {}", var3x.getName());
            return new ProfiledReloadInstance.State(var3x.getName(), var6, var7, var8, var9);
         }, var3);
      }, var4);
      return var5;
   }

   private ProfiledReloadInstance(List<PreparableReloadListener> var1) {
      super(var1);
      this.total.start();
   }

   protected CompletableFuture<List<ProfiledReloadInstance.State>> prepareTasks(Executor var1, Executor var2, ResourceManager var3, List<PreparableReloadListener> var4, SimpleReloadInstance.StateFactory<ProfiledReloadInstance.State> var5, CompletableFuture<?> var6) {
      return super.prepareTasks(var1, var2, var3, var4, var5, var6).thenApplyAsync(this::finish, var2);
   }

   private static Executor profiledExecutor(Executor var0, AtomicLong var1, AtomicLong var2, String var3) {
      return (var4) -> {
         var0.execute(() -> {
            ProfilerFiller var4x = Profiler.get();
            var4x.push(var3);
            long var5 = Util.getNanos();
            var4.run();
            var1.addAndGet(Util.getNanos() - var5);
            var2.incrementAndGet();
            var4x.pop();
         });
      };
   }

   private List<ProfiledReloadInstance.State> finish(List<ProfiledReloadInstance.State> var1) {
      this.total.stop();
      long var2 = 0L;
      LOGGER.info("Resource reload finished after {} ms", this.total.elapsed(TimeUnit.MILLISECONDS));

      long var10;
      for(Iterator var4 = var1.iterator(); var4.hasNext(); var2 += var10) {
         ProfiledReloadInstance.State var5 = (ProfiledReloadInstance.State)var4.next();
         long var6 = TimeUnit.NANOSECONDS.toMillis(var5.preparationNanos.get());
         long var8 = var5.preparationCount.get();
         var10 = TimeUnit.NANOSECONDS.toMillis(var5.reloadNanos.get());
         long var12 = var5.reloadCount.get();
         long var14 = var6 + var10;
         long var16 = var8 + var12;
         String var18 = var5.name;
         LOGGER.info("{} took approximately {} tasks/{} ms ({} tasks/{} ms preparing, {} tasks/{} ms applying)", new Object[]{var18, var16, var14, var8, var6, var12, var10});
      }

      LOGGER.info("Total blocking time: {} ms", var2);
      return var1;
   }

   public static record State(String name, AtomicLong preparationNanos, AtomicLong preparationCount, AtomicLong reloadNanos, AtomicLong reloadCount) {
      final String name;
      final AtomicLong preparationNanos;
      final AtomicLong preparationCount;
      final AtomicLong reloadNanos;
      final AtomicLong reloadCount;

      public State(String param1, AtomicLong param2, AtomicLong param3, AtomicLong param4, AtomicLong param5) {
         super();
         this.name = var1;
         this.preparationNanos = var2;
         this.preparationCount = var3;
         this.reloadNanos = var4;
         this.reloadCount = var5;
      }

      public String name() {
         return this.name;
      }

      public AtomicLong preparationNanos() {
         return this.preparationNanos;
      }

      public AtomicLong preparationCount() {
         return this.preparationCount;
      }

      public AtomicLong reloadNanos() {
         return this.reloadNanos;
      }

      public AtomicLong reloadCount() {
         return this.reloadCount;
      }
   }
}
