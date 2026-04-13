package net.minecraft.util.parsing.packrat;

import javax.annotation.Nullable;
import net.minecraft.Util;

public abstract class CachedParseState<S> implements ParseState<S> {
   private CachedParseState.PositionCache[] positionCache = new CachedParseState.PositionCache[256];
   private final ErrorCollector<S> errorCollector;
   private final Scope scope = new Scope();
   private CachedParseState.SimpleControl[] controlCache = new CachedParseState.SimpleControl[16];
   private int nextControlToReturn;
   private final CachedParseState<S>.Silent silent = new CachedParseState.Silent();

   protected CachedParseState(ErrorCollector<S> var1) {
      super();
      this.errorCollector = var1;
   }

   public Scope scope() {
      return this.scope;
   }

   public ErrorCollector<S> errorCollector() {
      return this.errorCollector;
   }

   @Nullable
   public <T> T parse(NamedRule<S, T> var1) {
      int var2 = this.mark();
      CachedParseState.PositionCache var3 = this.getCacheForPosition(var2);
      int var4 = var3.findKeyIndex(var1.name());
      if (var4 != -1) {
         CachedParseState.CacheEntry var5 = var3.getValue(var4);
         if (var5 != null) {
            if (var5 == CachedParseState.CacheEntry.NEGATIVE) {
               return null;
            }

            this.restore(var5.markAfterParse);
            return var5.value;
         }
      } else {
         var4 = var3.allocateNewEntry(var1.name());
      }

      Object var8 = var1.value().parse(this);
      CachedParseState.CacheEntry var6;
      if (var8 == null) {
         var6 = CachedParseState.CacheEntry.negativeEntry();
      } else {
         int var7 = this.mark();
         var6 = new CachedParseState.CacheEntry(var8, var7);
      }

      var3.setValue(var4, var6);
      return var8;
   }

   private CachedParseState.PositionCache getCacheForPosition(int var1) {
      int var2 = this.positionCache.length;
      if (var1 >= var2) {
         int var3 = Util.growByHalf(var2, var1 + 1);
         CachedParseState.PositionCache[] var4 = new CachedParseState.PositionCache[var3];
         System.arraycopy(this.positionCache, 0, var4, 0, var2);
         this.positionCache = var4;
      }

      CachedParseState.PositionCache var5 = this.positionCache[var1];
      if (var5 == null) {
         var5 = new CachedParseState.PositionCache();
         this.positionCache[var1] = var5;
      }

      return var5;
   }

   public Control acquireControl() {
      int var1 = this.controlCache.length;
      int var2;
      if (this.nextControlToReturn >= var1) {
         var2 = Util.growByHalf(var1, this.nextControlToReturn + 1);
         CachedParseState.SimpleControl[] var3 = new CachedParseState.SimpleControl[var2];
         System.arraycopy(this.controlCache, 0, var3, 0, var1);
         this.controlCache = var3;
      }

      var2 = this.nextControlToReturn++;
      CachedParseState.SimpleControl var4 = this.controlCache[var2];
      if (var4 == null) {
         var4 = new CachedParseState.SimpleControl();
         this.controlCache[var2] = var4;
      } else {
         var4.reset();
      }

      return var4;
   }

   public void releaseControl() {
      --this.nextControlToReturn;
   }

   public ParseState<S> silent() {
      return this.silent;
   }

   static class PositionCache {
      public static final int ENTRY_STRIDE = 2;
      private static final int NOT_FOUND = -1;
      private Object[] atomCache = new Object[16];
      private int nextKey;

      PositionCache() {
         super();
      }

      public int findKeyIndex(Atom<?> var1) {
         for(int var2 = 0; var2 < this.nextKey; var2 += 2) {
            if (this.atomCache[var2] == var1) {
               return var2;
            }
         }

         return -1;
      }

      public int allocateNewEntry(Atom<?> var1) {
         int var2 = this.nextKey;
         this.nextKey += 2;
         int var3 = var2 + 1;
         int var4 = this.atomCache.length;
         if (var3 >= var4) {
            int var5 = Util.growByHalf(var4, var3 + 1);
            Object[] var6 = new Object[var5];
            System.arraycopy(this.atomCache, 0, var6, 0, var4);
            this.atomCache = var6;
         }

         this.atomCache[var2] = var1;
         return var2;
      }

      @Nullable
      public <T> CachedParseState.CacheEntry<T> getValue(int var1) {
         return (CachedParseState.CacheEntry)this.atomCache[var1 + 1];
      }

      public void setValue(int var1, CachedParseState.CacheEntry<?> var2) {
         this.atomCache[var1 + 1] = var2;
      }
   }

   static class SimpleControl implements Control {
      private boolean hasCut;

      SimpleControl() {
         super();
      }

      public void cut() {
         this.hasCut = true;
      }

      public boolean hasCut() {
         return this.hasCut;
      }

      public void reset() {
         this.hasCut = false;
      }
   }

   class Silent implements ParseState<S> {
      private final ErrorCollector<S> silentCollector = new ErrorCollector.Nop();

      Silent() {
         super();
      }

      public ErrorCollector<S> errorCollector() {
         return this.silentCollector;
      }

      public Scope scope() {
         return CachedParseState.this.scope();
      }

      @Nullable
      public <T> T parse(NamedRule<S, T> var1) {
         return CachedParseState.this.parse(var1);
      }

      public S input() {
         return CachedParseState.this.input();
      }

      public int mark() {
         return CachedParseState.this.mark();
      }

      public void restore(int var1) {
         CachedParseState.this.restore(var1);
      }

      public Control acquireControl() {
         return CachedParseState.this.acquireControl();
      }

      public void releaseControl() {
         CachedParseState.this.releaseControl();
      }

      public ParseState<S> silent() {
         return this;
      }
   }

   private static record CacheEntry<T>(@Nullable T value, int markAfterParse) {
      @Nullable
      final T value;
      final int markAfterParse;
      public static final CachedParseState.CacheEntry<?> NEGATIVE = new CachedParseState.CacheEntry((Object)null, -1);

      CacheEntry(@Nullable T param1, int param2) {
         super();
         this.value = var1;
         this.markAfterParse = var2;
      }

      public static <T> CachedParseState.CacheEntry<T> negativeEntry() {
         return NEGATIVE;
      }

      @Nullable
      public T value() {
         return this.value;
      }

      public int markAfterParse() {
         return this.markAfterParse;
      }
   }
}
