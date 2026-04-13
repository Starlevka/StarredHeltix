package net.minecraft.util.parsing.packrat;

import java.util.ArrayList;
import java.util.List;

public interface Term<S> {
   boolean parse(ParseState<S> var1, Scope var2, Control var3);

   static <S, T> Term<S> marker(Atom<T> var0, T var1) {
      return new Term.Marker(var0, var1);
   }

   @SafeVarargs
   static <S> Term<S> sequence(Term<S>... var0) {
      return new Term.Sequence(var0);
   }

   @SafeVarargs
   static <S> Term<S> alternative(Term<S>... var0) {
      return new Term.Alternative(var0);
   }

   static <S> Term<S> optional(Term<S> var0) {
      return new Term.Maybe(var0);
   }

   static <S, T> Term<S> repeated(NamedRule<S, T> var0, Atom<List<T>> var1) {
      return repeated(var0, var1, 0);
   }

   static <S, T> Term<S> repeated(NamedRule<S, T> var0, Atom<List<T>> var1, int var2) {
      return new Term.Repeated(var0, var1, var2);
   }

   static <S, T> Term<S> repeatedWithTrailingSeparator(NamedRule<S, T> var0, Atom<List<T>> var1, Term<S> var2) {
      return repeatedWithTrailingSeparator(var0, var1, var2, 0);
   }

   static <S, T> Term<S> repeatedWithTrailingSeparator(NamedRule<S, T> var0, Atom<List<T>> var1, Term<S> var2, int var3) {
      return new Term.RepeatedWithSeparator(var0, var1, var2, var3, true);
   }

   static <S, T> Term<S> repeatedWithoutTrailingSeparator(NamedRule<S, T> var0, Atom<List<T>> var1, Term<S> var2) {
      return repeatedWithoutTrailingSeparator(var0, var1, var2, 0);
   }

   static <S, T> Term<S> repeatedWithoutTrailingSeparator(NamedRule<S, T> var0, Atom<List<T>> var1, Term<S> var2, int var3) {
      return new Term.RepeatedWithSeparator(var0, var1, var2, var3, false);
   }

   static <S> Term<S> positiveLookahead(Term<S> var0) {
      return new Term.LookAhead(var0, true);
   }

   static <S> Term<S> negativeLookahead(Term<S> var0) {
      return new Term.LookAhead(var0, false);
   }

   static <S> Term<S> cut() {
      return new Term<S>() {
         public boolean parse(ParseState<S> var1, Scope var2, Control var3) {
            var3.cut();
            return true;
         }

         public String toString() {
            return "\u2191";
         }
      };
   }

   static <S> Term<S> empty() {
      return new Term<S>() {
         public boolean parse(ParseState<S> var1, Scope var2, Control var3) {
            return true;
         }

         public String toString() {
            return "\u03b5";
         }
      };
   }

   static <S> Term<S> fail(final Object var0) {
      return new Term<S>() {
         public boolean parse(ParseState<S> var1, Scope var2, Control var3) {
            var1.errorCollector().store(var1.mark(), var0);
            return false;
         }

         public String toString() {
            return "fail";
         }
      };
   }

   public static record Marker<S, T>(Atom<T> name, T value) implements Term<S> {
      public Marker(Atom<T> param1, T param2) {
         super();
         this.name = var1;
         this.value = var2;
      }

      public boolean parse(ParseState<S> var1, Scope var2, Control var3) {
         var2.put(this.name, this.value);
         return true;
      }

      public Atom<T> name() {
         return this.name;
      }

      public T value() {
         return this.value;
      }
   }

   public static record Sequence<S>(Term<S>[] elements) implements Term<S> {
      public Sequence(Term<S>[] param1) {
         super();
         this.elements = var1;
      }

      public boolean parse(ParseState<S> var1, Scope var2, Control var3) {
         int var4 = var1.mark();
         Term[] var5 = this.elements;
         int var6 = var5.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            Term var8 = var5[var7];
            if (!var8.parse(var1, var2, var3)) {
               var1.restore(var4);
               return false;
            }
         }

         return true;
      }

      public Term<S>[] elements() {
         return this.elements;
      }
   }

   public static record Alternative<S>(Term<S>[] elements) implements Term<S> {
      public Alternative(Term<S>[] param1) {
         super();
         this.elements = var1;
      }

      public boolean parse(ParseState<S> var1, Scope var2, Control var3) {
         Control var4 = var1.acquireControl();

         boolean var14;
         try {
            int var5 = var1.mark();
            var2.splitFrame();
            Term[] var6 = this.elements;
            int var7 = var6.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               Term var9 = var6[var8];
               if (var9.parse(var1, var2, var4)) {
                  var2.mergeFrame();
                  boolean var10 = true;
                  return var10;
               }

               var2.clearFrameValues();
               var1.restore(var5);
               if (var4.hasCut()) {
                  break;
               }
            }

            var2.popFrame();
            var14 = false;
         } finally {
            var1.releaseControl();
         }

         return var14;
      }

      public Term<S>[] elements() {
         return this.elements;
      }
   }

   public static record Maybe<S>(Term<S> term) implements Term<S> {
      public Maybe(Term<S> param1) {
         super();
         this.term = var1;
      }

      public boolean parse(ParseState<S> var1, Scope var2, Control var3) {
         int var4 = var1.mark();
         if (!this.term.parse(var1, var2, var3)) {
            var1.restore(var4);
         }

         return true;
      }

      public Term<S> term() {
         return this.term;
      }
   }

   public static record Repeated<S, T>(NamedRule<S, T> element, Atom<List<T>> listName, int minRepetitions) implements Term<S> {
      public Repeated(NamedRule<S, T> param1, Atom<List<T>> param2, int param3) {
         super();
         this.element = var1;
         this.listName = var2;
         this.minRepetitions = var3;
      }

      public boolean parse(ParseState<S> var1, Scope var2, Control var3) {
         int var4 = var1.mark();
         ArrayList var5 = new ArrayList(this.minRepetitions);

         while(true) {
            int var6 = var1.mark();
            Object var7 = var1.parse(this.element);
            if (var7 == null) {
               var1.restore(var6);
               if (var5.size() < this.minRepetitions) {
                  var1.restore(var4);
                  return false;
               } else {
                  var2.put(this.listName, var5);
                  return true;
               }
            }

            var5.add(var7);
         }
      }

      public NamedRule<S, T> element() {
         return this.element;
      }

      public Atom<List<T>> listName() {
         return this.listName;
      }

      public int minRepetitions() {
         return this.minRepetitions;
      }
   }

   public static record RepeatedWithSeparator<S, T>(NamedRule<S, T> element, Atom<List<T>> listName, Term<S> separator, int minRepetitions, boolean allowTrailingSeparator) implements Term<S> {
      public RepeatedWithSeparator(NamedRule<S, T> param1, Atom<List<T>> param2, Term<S> param3, int param4, boolean param5) {
         super();
         this.element = var1;
         this.listName = var2;
         this.separator = var3;
         this.minRepetitions = var4;
         this.allowTrailingSeparator = var5;
      }

      public boolean parse(ParseState<S> var1, Scope var2, Control var3) {
         int var4 = var1.mark();
         ArrayList var5 = new ArrayList(this.minRepetitions);
         boolean var6 = true;

         while(true) {
            int var7 = var1.mark();
            if (!var6 && !this.separator.parse(var1, var2, var3)) {
               var1.restore(var7);
               break;
            }

            int var8 = var1.mark();
            Object var9 = var1.parse(this.element);
            if (var9 == null) {
               if (var6) {
                  var1.restore(var8);
               } else {
                  if (!this.allowTrailingSeparator) {
                     var1.restore(var4);
                     return false;
                  }

                  var1.restore(var8);
               }
               break;
            }

            var5.add(var9);
            var6 = false;
         }

         if (var5.size() < this.minRepetitions) {
            var1.restore(var4);
            return false;
         } else {
            var2.put(this.listName, var5);
            return true;
         }
      }

      public NamedRule<S, T> element() {
         return this.element;
      }

      public Atom<List<T>> listName() {
         return this.listName;
      }

      public Term<S> separator() {
         return this.separator;
      }

      public int minRepetitions() {
         return this.minRepetitions;
      }

      public boolean allowTrailingSeparator() {
         return this.allowTrailingSeparator;
      }
   }

   public static record LookAhead<S>(Term<S> term, boolean positive) implements Term<S> {
      public LookAhead(Term<S> param1, boolean param2) {
         super();
         this.term = var1;
         this.positive = var2;
      }

      public boolean parse(ParseState<S> var1, Scope var2, Control var3) {
         int var4 = var1.mark();
         boolean var5 = this.term.parse(var1.silent(), var2, var3);
         var1.restore(var4);
         return this.positive == var5;
      }

      public Term<S> term() {
         return this.term;
      }

      public boolean positive() {
         return this.positive;
      }
   }
}
