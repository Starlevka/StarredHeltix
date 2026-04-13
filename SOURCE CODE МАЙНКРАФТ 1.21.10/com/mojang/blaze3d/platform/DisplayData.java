package com.mojang.blaze3d.platform;

import java.util.OptionalInt;

public record DisplayData(int width, int height, OptionalInt fullscreenWidth, OptionalInt fullscreenHeight, boolean isFullscreen) {
   public DisplayData(int param1, int param2, OptionalInt param3, OptionalInt param4, boolean param5) {
      super();
      this.width = var1;
      this.height = var2;
      this.fullscreenWidth = var3;
      this.fullscreenHeight = var4;
      this.isFullscreen = var5;
   }

   public DisplayData withSize(int var1, int var2) {
      return new DisplayData(var1, var2, this.fullscreenWidth, this.fullscreenHeight, this.isFullscreen);
   }

   public DisplayData withFullscreen(boolean var1) {
      return new DisplayData(this.width, this.height, this.fullscreenWidth, this.fullscreenHeight, var1);
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   public OptionalInt fullscreenWidth() {
      return this.fullscreenWidth;
   }

   public OptionalInt fullscreenHeight() {
      return this.fullscreenHeight;
   }

   public boolean isFullscreen() {
      return this.isFullscreen;
   }
}
