package com.mojang.blaze3d;

import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix4f;

public enum ProjectionType {
   PERSPECTIVE(VertexSorting.DISTANCE_TO_ORIGIN, (var0, var1) -> {
      var0.scale(1.0F - var1 / 4096.0F);
   }),
   ORTHOGRAPHIC(VertexSorting.ORTHOGRAPHIC_Z, (var0, var1) -> {
      var0.translate(0.0F, 0.0F, var1 / 512.0F);
   });

   private final VertexSorting vertexSorting;
   private final ProjectionType.LayeringTransform layeringTransform;

   private ProjectionType(final VertexSorting param3, final ProjectionType.LayeringTransform param4) {
      this.vertexSorting = var3;
      this.layeringTransform = var4;
   }

   public VertexSorting vertexSorting() {
      return this.vertexSorting;
   }

   public void applyLayeringTransform(Matrix4f var1, float var2) {
      this.layeringTransform.apply(var1, var2);
   }

   // $FF: synthetic method
   private static ProjectionType[] $values() {
      return new ProjectionType[]{PERSPECTIVE, ORTHOGRAPHIC};
   }

   @FunctionalInterface
   private interface LayeringTransform {
      void apply(Matrix4f var1, float var2);
   }
}
