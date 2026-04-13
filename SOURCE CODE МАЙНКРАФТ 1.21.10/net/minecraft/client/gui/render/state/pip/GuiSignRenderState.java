package net.minecraft.client.gui.render.state.pip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.Model;
import net.minecraft.world.level.block.state.properties.WoodType;

public record GuiSignRenderState(Model.Simple signModel, WoodType woodType, int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
   public GuiSignRenderState(Model.Simple var1, WoodType var2, int var3, int var4, int var5, int var6, float var7, @Nullable ScreenRectangle var8) {
      this(var1, var2, var3, var4, var5, var6, var7, var8, PictureInPictureRenderState.getBounds(var3, var4, var5, var6, var8));
   }

   public GuiSignRenderState(Model.Simple param1, WoodType param2, int param3, int param4, int param5, int param6, float param7, @Nullable ScreenRectangle param8, @Nullable ScreenRectangle param9) {
      super();
      this.signModel = var1;
      this.woodType = var2;
      this.x0 = var3;
      this.y0 = var4;
      this.x1 = var5;
      this.y1 = var6;
      this.scale = var7;
      this.scissorArea = var8;
      this.bounds = var9;
   }

   public Model.Simple signModel() {
      return this.signModel;
   }

   public WoodType woodType() {
      return this.woodType;
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

   public float scale() {
      return this.scale;
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
