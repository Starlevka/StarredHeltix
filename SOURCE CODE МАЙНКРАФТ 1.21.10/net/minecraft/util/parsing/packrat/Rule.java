package net.minecraft.util.parsing.packrat;

import javax.annotation.Nullable;

public interface Rule<S, T> {
   @Nullable
   T parse(ParseState<S> var1);

   static <S, T> Rule<S, T> fromTerm(Term<S> var0, Rule.RuleAction<S, T> var1) {
      return new Rule.WrappedTerm(var1, var0);
   }

   static <S, T> Rule<S, T> fromTerm(Term<S> var0, Rule.SimpleRuleAction<S, T> var1) {
      return new Rule.WrappedTerm(var1, var0);
   }

   public static record WrappedTerm<S, T>(Rule.RuleAction<S, T> action, Term<S> child) implements Rule<S, T> {
      public WrappedTerm(Rule.RuleAction<S, T> param1, Term<S> param2) {
         super();
         this.action = var1;
         this.child = var2;
      }

      @Nullable
      public T parse(ParseState<S> var1) {
         Scope var2 = var1.scope();
         var2.pushFrame();

         Object var3;
         try {
            if (this.child.parse(var1, var2, Control.UNBOUND)) {
               var3 = this.action.run(var1);
               return var3;
            }

            var3 = null;
         } finally {
            var2.popFrame();
         }

         return var3;
      }

      public Rule.RuleAction<S, T> action() {
         return this.action;
      }

      public Term<S> child() {
         return this.child;
      }
   }

   @FunctionalInterface
   public interface RuleAction<S, T> {
      @Nullable
      T run(ParseState<S> var1);
   }

   @FunctionalInterface
   public interface SimpleRuleAction<S, T> extends Rule.RuleAction<S, T> {
      T run(Scope var1);

      default T run(ParseState<S> var1) {
         return this.run(var1.scope());
      }
   }
}
