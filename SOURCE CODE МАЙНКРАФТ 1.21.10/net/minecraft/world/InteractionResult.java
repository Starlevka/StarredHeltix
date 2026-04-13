package net.minecraft.world;

import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;

public interface InteractionResult {
   InteractionResult.Success SUCCESS = new InteractionResult.Success(InteractionResult.SwingSource.CLIENT, InteractionResult.ItemContext.DEFAULT);
   InteractionResult.Success SUCCESS_SERVER = new InteractionResult.Success(InteractionResult.SwingSource.SERVER, InteractionResult.ItemContext.DEFAULT);
   InteractionResult.Success CONSUME = new InteractionResult.Success(InteractionResult.SwingSource.NONE, InteractionResult.ItemContext.DEFAULT);
   InteractionResult.Fail FAIL = new InteractionResult.Fail();
   InteractionResult.Pass PASS = new InteractionResult.Pass();
   InteractionResult.TryEmptyHandInteraction TRY_WITH_EMPTY_HAND = new InteractionResult.TryEmptyHandInteraction();

   default boolean consumesAction() {
      return false;
   }

   public static record Success(InteractionResult.SwingSource swingSource, InteractionResult.ItemContext itemContext) implements InteractionResult {
      public Success(InteractionResult.SwingSource param1, InteractionResult.ItemContext param2) {
         super();
         this.swingSource = var1;
         this.itemContext = var2;
      }

      public boolean consumesAction() {
         return true;
      }

      public InteractionResult.Success heldItemTransformedTo(ItemStack var1) {
         return new InteractionResult.Success(this.swingSource, new InteractionResult.ItemContext(true, var1));
      }

      public InteractionResult.Success withoutItem() {
         return new InteractionResult.Success(this.swingSource, InteractionResult.ItemContext.NONE);
      }

      public boolean wasItemInteraction() {
         return this.itemContext.wasItemInteraction;
      }

      @Nullable
      public ItemStack heldItemTransformedTo() {
         return this.itemContext.heldItemTransformedTo;
      }

      public InteractionResult.SwingSource swingSource() {
         return this.swingSource;
      }

      public InteractionResult.ItemContext itemContext() {
         return this.itemContext;
      }
   }

   public static enum SwingSource {
      NONE,
      CLIENT,
      SERVER;

      private SwingSource() {
      }

      // $FF: synthetic method
      private static InteractionResult.SwingSource[] $values() {
         return new InteractionResult.SwingSource[]{NONE, CLIENT, SERVER};
      }
   }

   public static record ItemContext(boolean wasItemInteraction, @Nullable ItemStack heldItemTransformedTo) {
      final boolean wasItemInteraction;
      @Nullable
      final ItemStack heldItemTransformedTo;
      static InteractionResult.ItemContext NONE = new InteractionResult.ItemContext(false, (ItemStack)null);
      static InteractionResult.ItemContext DEFAULT = new InteractionResult.ItemContext(true, (ItemStack)null);

      public ItemContext(boolean param1, @Nullable ItemStack param2) {
         super();
         this.wasItemInteraction = var1;
         this.heldItemTransformedTo = var2;
      }

      public boolean wasItemInteraction() {
         return this.wasItemInteraction;
      }

      @Nullable
      public ItemStack heldItemTransformedTo() {
         return this.heldItemTransformedTo;
      }
   }

   public static record Fail() implements InteractionResult {
      public Fail() {
         super();
      }
   }

   public static record Pass() implements InteractionResult {
      public Pass() {
         super();
      }
   }

   public static record TryEmptyHandInteraction() implements InteractionResult {
      public TryEmptyHandInteraction() {
         super();
      }
   }
}
