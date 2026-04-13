package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.TextureFormat;

public interface Uniform extends AutoCloseable {
   default void close() {
   }

   public static record Sampler(int location, int samplerIndex) implements Uniform {
      public Sampler(int param1, int param2) {
         super();
         this.location = var1;
         this.samplerIndex = var2;
      }

      public int location() {
         return this.location;
      }

      public int samplerIndex() {
         return this.samplerIndex;
      }
   }

   public static record Utb(int location, int samplerIndex, TextureFormat format, int texture) implements Uniform {
      public Utb(int var1, int var2, TextureFormat var3) {
         this(var1, var2, var3, GlStateManager._genTexture());
      }

      public Utb(int param1, int param2, TextureFormat param3, int param4) {
         super();
         this.location = var1;
         this.samplerIndex = var2;
         this.format = var3;
         this.texture = var4;
      }

      public void close() {
         GlStateManager._deleteTexture(this.texture);
      }

      public int location() {
         return this.location;
      }

      public int samplerIndex() {
         return this.samplerIndex;
      }

      public TextureFormat format() {
         return this.format;
      }

      public int texture() {
         return this.texture;
      }
   }

   public static record Ubo(int blockBinding) implements Uniform {
      public Ubo(int param1) {
         super();
         this.blockBinding = var1;
      }

      public int blockBinding() {
         return this.blockBinding;
      }
   }
}
