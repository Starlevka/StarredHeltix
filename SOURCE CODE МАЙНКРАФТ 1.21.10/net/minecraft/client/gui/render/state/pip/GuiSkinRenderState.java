package net.minecraft.client.gui.render.state.pip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.resources.ResourceLocation;

public record GuiSkinRenderState(PlayerModel playerModel, ResourceLocation texture, float rotationX, float rotationY, float pivotY, int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
   public GuiSkinRenderState(PlayerModel var1, ResourceLocation var2, float var3, float var4, float var5, int var6, int var7, int var8, int var9, float var10, @Nullable ScreenRectangle var11) {
      this(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, PictureInPictureRenderState.getBounds(var6, var7, var8, var9, var11));
   }

   public GuiSkinRenderState(PlayerModel param1, ResourceLocation param2, float param3, float param4, float param5, int param6, int param7, int param8, int param9, float param10, @Nullable ScreenRectangle param11, @Nullable ScreenRectangle param12) {
      super();
      this.playerModel = var1;
      this.texture = var2;
      this.rotationX = var3;
      this.rotationY = var4;
      this.pivotY = var5;
      this.x0 = var6;
      this.y0 = var7;
      this.x1 = var8;
      this.y1 = var9;
      this.scale = var10;
      this.scissorArea = var11;
      this.bounds = var12;
   }

   public PlayerModel playerModel() {
      return this.playerModel;
   }

   public ResourceLocation texture() {
      return this.texture;
   }

   public float rotationX() {
      return this.rotationX;
   }

   public float rotationY() {
      return this.rotationY;
   }

   public float pivotY() {
      return this.pivotY;
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
