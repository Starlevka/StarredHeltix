package net.minecraft.client.input;

public record KeyEvent(int key, int scancode, int modifiers) implements InputWithModifiers {
   public KeyEvent(int param1, int param2, int param3) {
      super();
      this.key = var1;
      this.scancode = var2;
      this.modifiers = var3;
   }

   public int input() {
      return this.key;
   }

   public int key() {
      return this.key;
   }

   public int scancode() {
      return this.scancode;
   }

   public int modifiers() {
      return this.modifiers;
   }
}
