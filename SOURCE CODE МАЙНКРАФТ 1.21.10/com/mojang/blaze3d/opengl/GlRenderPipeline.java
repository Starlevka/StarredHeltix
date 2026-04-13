package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;

public record GlRenderPipeline(RenderPipeline info, GlProgram program) implements CompiledRenderPipeline {
   public GlRenderPipeline(RenderPipeline param1, GlProgram param2) {
      super();
      this.info = var1;
      this.program = var2;
   }

   public boolean isValid() {
      return this.program != GlProgram.INVALID_PROGRAM;
   }

   public RenderPipeline info() {
      return this.info;
   }

   public GlProgram program() {
      return this.program;
   }
}
