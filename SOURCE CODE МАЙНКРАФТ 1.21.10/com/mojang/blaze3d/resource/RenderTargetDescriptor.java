package com.mojang.blaze3d.resource;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;

public record RenderTargetDescriptor(int width, int height, boolean useDepth, int clearColor) implements ResourceDescriptor<RenderTarget> {
   public RenderTargetDescriptor(int param1, int param2, boolean param3, int param4) {
      super();
      this.width = var1;
      this.height = var2;
      this.useDepth = var3;
      this.clearColor = var4;
   }

   public RenderTarget allocate() {
      return new TextureTarget((String)null, this.width, this.height, this.useDepth);
   }

   public void prepare(RenderTarget var1) {
      if (this.useDepth) {
         RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(var1.getColorTexture(), this.clearColor, var1.getDepthTexture(), 1.0D);
      } else {
         RenderSystem.getDevice().createCommandEncoder().clearColorTexture(var1.getColorTexture(), this.clearColor);
      }

   }

   public void free(RenderTarget var1) {
      var1.destroyBuffers();
   }

   public boolean canUsePhysicalResource(ResourceDescriptor<?> var1) {
      if (!(var1 instanceof RenderTargetDescriptor)) {
         return false;
      } else {
         RenderTargetDescriptor var2 = (RenderTargetDescriptor)var1;
         return this.width == var2.width && this.height == var2.height && this.useDepth == var2.useDepth;
      }
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   public boolean useDepth() {
      return this.useDepth;
   }

   public int clearColor() {
      return this.clearColor;
   }

   // $FF: synthetic method
   public void free(final Object param1) {
      this.free((RenderTarget)var1);
   }

   // $FF: synthetic method
   public void prepare(final Object param1) {
      this.prepare((RenderTarget)var1);
   }

   // $FF: synthetic method
   public Object allocate() {
      return this.allocate();
   }
}
