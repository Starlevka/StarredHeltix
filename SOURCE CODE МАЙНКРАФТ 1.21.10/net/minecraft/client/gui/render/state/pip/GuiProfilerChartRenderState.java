package net.minecraft.client.gui.render.state.pip;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.profiling.ResultField;

public record GuiProfilerChartRenderState(List<ResultField> chartData, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
   public GuiProfilerChartRenderState(List<ResultField> var1, int var2, int var3, int var4, int var5, @Nullable ScreenRectangle var6) {
      this(var1, var2, var3, var4, var5, var6, PictureInPictureRenderState.getBounds(var2, var3, var4, var5, var6));
   }

   public GuiProfilerChartRenderState(List<ResultField> param1, int param2, int param3, int param4, int param5, @Nullable ScreenRectangle param6, @Nullable ScreenRectangle param7) {
      super();
      this.chartData = var1;
      this.x0 = var2;
      this.y0 = var3;
      this.x1 = var4;
      this.y1 = var5;
      this.scissorArea = var6;
      this.bounds = var7;
   }

   public float scale() {
      return 1.0F;
   }

   public List<ResultField> chartData() {
      return this.chartData;
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

   @Nullable
   public ScreenRectangle scissorArea() {
      return this.scissorArea;
   }

   @Nullable
   public ScreenRectangle bounds() {
      return this.bounds;
   }
}
