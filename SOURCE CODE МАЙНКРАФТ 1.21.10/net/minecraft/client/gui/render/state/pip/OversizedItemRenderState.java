package net.minecraft.client.gui.render.state.pip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import org.joml.Matrix3x2f;

public record OversizedItemRenderState(GuiItemRenderState guiItemRenderState, int x0, int y0, int x1, int y1) implements PictureInPictureRenderState {
   public OversizedItemRenderState(GuiItemRenderState param1, int param2, int param3, int param4, int param5) {
      super();
      this.guiItemRenderState = var1;
      this.x0 = var2;
      this.y0 = var3;
      this.x1 = var4;
      this.y1 = var5;
   }

   public float scale() {
      return 16.0F;
   }

   public Matrix3x2f pose() {
      return this.guiItemRenderState.pose();
   }

   @Nullable
   public ScreenRectangle scissorArea() {
      return this.guiItemRenderState.scissorArea();
   }

   @Nullable
   public ScreenRectangle bounds() {
      return this.guiItemRenderState.bounds();
   }

   public GuiItemRenderState guiItemRenderState() {
      return this.guiItemRenderState;
   }

   public int x0() {
      return this.x0;
   }

   public int y0() {
      return this.y0;
   }

   public int x1() {
      return this.x1;
   }

   public int y1() {
      return this.y1;
   }
}
