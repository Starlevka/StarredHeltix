package net.minecraft.client.input;

public record MouseButtonEvent(double x, double y, MouseButtonInfo buttonInfo) implements InputWithModifiers {
   public MouseButtonEvent(double param1, double param3, MouseButtonInfo param5) {
      super();
      this.x = var1;
      this.y = var3;
      this.buttonInfo = var5;
   }

   public int input() {
      return this.button();
   }

   public int button() {
      return this.buttonInfo().button();
   }

   public int modifiers() {
      return this.buttonInfo().modifiers();
   }

   public double x() {
      return this.x;
   }

   public double y() {
      return this.y;
   }

   public MouseButtonInfo buttonInfo() {
      return this.buttonInfo;
   }
}
