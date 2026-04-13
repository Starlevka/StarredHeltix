package net.minecraft.client.gui.render.state.pip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.BookModel;
import net.minecraft.resources.ResourceLocation;

public record GuiBookModelRenderState(BookModel bookModel, ResourceLocation texture, float open, float flip, int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
   public GuiBookModelRenderState(BookModel var1, ResourceLocation var2, float var3, float var4, int var5, int var6, int var7, int var8, float var9, @Nullable ScreenRectangle var10) {
      this(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, PictureInPictureRenderState.getBounds(var5, var6, var7, var8, var10));
   }

   public GuiBookModelRenderState(BookModel param1, ResourceLocation param2, float param3, float param4, int param5, int param6, int param7, int param8, float param9, @Nullable ScreenRectangle param10, @Nullable ScreenRectangle param11) {
      super();
      this.bookModel = var1;
      this.texture = var2;
      this.open = var3;
      this.flip = var4;
      this.x0 = var5;
      this.y0 = var6;
      this.x1 = var7;
      this.y1 = var8;
      this.scale = var9;
      this.scissorArea = var10;
      this.bounds = var11;
   }

   public BookModel bookModel() {
      return this.bookModel;
   }

   public ResourceLocation texture() {
      return this.texture;
   }

   public float open() {
      return this.open;
   }

   public float flip() {
      return this.flip;
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
