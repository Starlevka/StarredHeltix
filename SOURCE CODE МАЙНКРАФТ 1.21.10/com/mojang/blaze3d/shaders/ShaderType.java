package com.mojang.blaze3d.shaders;

import com.mojang.blaze3d.DontObfuscate;
import javax.annotation.Nullable;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;

@DontObfuscate
public enum ShaderType {
   VERTEX("vertex", ".vsh"),
   FRAGMENT("fragment", ".fsh");

   private static final ShaderType[] TYPES = values();
   private final String name;
   private final String extension;

   private ShaderType(final String param3, final String param4) {
      this.name = var3;
      this.extension = var4;
   }

   @Nullable
   public static ShaderType byLocation(ResourceLocation var0) {
      ShaderType[] var1 = TYPES;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ShaderType var4 = var1[var3];
         if (var0.getPath().endsWith(var4.extension)) {
            return var4;
         }
      }

      return null;
   }

   public String getName() {
      return this.name;
   }

   public FileToIdConverter idConverter() {
      return new FileToIdConverter("shaders", this.extension);
   }

   // $FF: synthetic method
   private static ShaderType[] $values() {
      return new ShaderType[]{VERTEX, FRAGMENT};
   }
}
