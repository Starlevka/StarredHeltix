package net.minecraft.client.gui.render.state.pip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.BannerFlagModel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public record GuiBannerResultRenderState(BannerFlagModel flag, DyeColor baseColor, BannerPatternLayers resultBannerPatterns, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
   public GuiBannerResultRenderState(BannerFlagModel var1, DyeColor var2, BannerPatternLayers var3, int var4, int var5, int var6, int var7, @Nullable ScreenRectangle var8) {
      this(var1, var2, var3, var4, var5, var6, var7, var8, PictureInPictureRenderState.getBounds(var4, var5, var6, var7, var8));
   }

   public GuiBannerResultRenderState(BannerFlagModel param1, DyeColor param2, BannerPatternLayers param3, int param4, int param5, int param6, int param7, @Nullable ScreenRectangle param8, @Nullable ScreenRectangle param9) {
      super();
      this.flag = var1;
      this.baseColor = var2;
      this.resultBannerPatterns = var3;
      this.x0 = var4;
      this.y0 = var5;
      this.x1 = var6;
      this.y1 = var7;
      this.scissorArea = var8;
      this.bounds = var9;
   }

   public float scale() {
      return 16.0F;
   }

   public BannerFlagModel flag() {
      return this.flag;
   }

   public DyeColor baseColor() {
      return this.baseColor;
   }

   public BannerPatternLayers resultBannerPatterns() {
      return this.resultBannerPatterns;
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
