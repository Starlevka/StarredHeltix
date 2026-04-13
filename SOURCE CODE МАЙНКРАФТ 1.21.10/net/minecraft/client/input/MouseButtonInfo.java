package net.minecraft.client.input;

public record MouseButtonInfo(int button, int modifiers) implements InputWithModifiers {
   public MouseButtonInfo(int param1, int param2) {
      super();
      this.button = var1;
      this.modifiers = var2;
   }

   public int input() {
      return this.button;
   }

   public int button() {
      return this.button;
   }

   public int modifiers() {
      return this.modifiers;
   }
}
