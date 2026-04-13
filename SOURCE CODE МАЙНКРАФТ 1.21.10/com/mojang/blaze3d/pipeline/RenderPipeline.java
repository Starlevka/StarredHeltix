package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.LogicOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.ResourceLocation;

@DontObfuscate
public class RenderPipeline {
   private final ResourceLocation location;
   private final ResourceLocation vertexShader;
   private final ResourceLocation fragmentShader;
   private final ShaderDefines shaderDefines;
   private final List<String> samplers;
   private final List<RenderPipeline.UniformDescription> uniforms;
   private final DepthTestFunction depthTestFunction;
   private final PolygonMode polygonMode;
   private final boolean cull;
   private final LogicOp colorLogic;
   private final Optional<BlendFunction> blendFunction;
   private final boolean writeColor;
   private final boolean writeAlpha;
   private final boolean writeDepth;
   private final VertexFormat vertexFormat;
   private final VertexFormat.Mode vertexFormatMode;
   private final float depthBiasScaleFactor;
   private final float depthBiasConstant;
   private final int sortKey;
   private static int sortKeySeed;

   protected RenderPipeline(ResourceLocation var1, ResourceLocation var2, ResourceLocation var3, ShaderDefines var4, List<String> var5, List<RenderPipeline.UniformDescription> var6, Optional<BlendFunction> var7, DepthTestFunction var8, PolygonMode var9, boolean var10, boolean var11, boolean var12, boolean var13, LogicOp var14, VertexFormat var15, VertexFormat.Mode var16, float var17, float var18, int var19) {
      super();
      this.location = var1;
      this.vertexShader = var2;
      this.fragmentShader = var3;
      this.shaderDefines = var4;
      this.samplers = var5;
      this.uniforms = var6;
      this.depthTestFunction = var8;
      this.polygonMode = var9;
      this.cull = var10;
      this.blendFunction = var7;
      this.writeColor = var11;
      this.writeAlpha = var12;
      this.writeDepth = var13;
      this.colorLogic = var14;
      this.vertexFormat = var15;
      this.vertexFormatMode = var16;
      this.depthBiasScaleFactor = var17;
      this.depthBiasConstant = var18;
      this.sortKey = var19;
   }

   public int getSortKey() {
      return SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER ? super.hashCode() * (sortKeySeed + 1) : this.sortKey;
   }

   public static void updateSortKeySeed() {
      sortKeySeed = Math.round(100000.0F * (float)Math.random());
   }

   public String toString() {
      return this.location.toString();
   }

   public DepthTestFunction getDepthTestFunction() {
      return this.depthTestFunction;
   }

   public PolygonMode getPolygonMode() {
      return this.polygonMode;
   }

   public boolean isCull() {
      return this.cull;
   }

   public LogicOp getColorLogic() {
      return this.colorLogic;
   }

   public Optional<BlendFunction> getBlendFunction() {
      return this.blendFunction;
   }

   public boolean isWriteColor() {
      return this.writeColor;
   }

   public boolean isWriteAlpha() {
      return this.writeAlpha;
   }

   public boolean isWriteDepth() {
      return this.writeDepth;
   }

   public float getDepthBiasScaleFactor() {
      return this.depthBiasScaleFactor;
   }

   public float getDepthBiasConstant() {
      return this.depthBiasConstant;
   }

   public ResourceLocation getLocation() {
      return this.location;
   }

   public VertexFormat getVertexFormat() {
      return this.vertexFormat;
   }

   public VertexFormat.Mode getVertexFormatMode() {
      return this.vertexFormatMode;
   }

   public ResourceLocation getVertexShader() {
      return this.vertexShader;
   }

   public ResourceLocation getFragmentShader() {
      return this.fragmentShader;
   }

   public ShaderDefines getShaderDefines() {
      return this.shaderDefines;
   }

   public List<String> getSamplers() {
      return this.samplers;
   }

   public List<RenderPipeline.UniformDescription> getUniforms() {
      return this.uniforms;
   }

   public boolean wantsDepthTexture() {
      return this.depthTestFunction != DepthTestFunction.NO_DEPTH_TEST || this.depthBiasConstant != 0.0F || this.depthBiasScaleFactor != 0.0F || this.writeDepth;
   }

   public static RenderPipeline.Builder builder(RenderPipeline.Snippet... var0) {
      RenderPipeline.Builder var1 = new RenderPipeline.Builder();
      RenderPipeline.Snippet[] var2 = var0;
      int var3 = var0.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         RenderPipeline.Snippet var5 = var2[var4];
         var1.withSnippet(var5);
      }

      return var1;
   }

   @DontObfuscate
   public static class Builder {
      private static int nextPipelineSortKey;
      private Optional<ResourceLocation> location = Optional.empty();
      private Optional<ResourceLocation> fragmentShader = Optional.empty();
      private Optional<ResourceLocation> vertexShader = Optional.empty();
      private Optional<ShaderDefines.Builder> definesBuilder = Optional.empty();
      private Optional<List<String>> samplers = Optional.empty();
      private Optional<List<RenderPipeline.UniformDescription>> uniforms = Optional.empty();
      private Optional<DepthTestFunction> depthTestFunction = Optional.empty();
      private Optional<PolygonMode> polygonMode = Optional.empty();
      private Optional<Boolean> cull = Optional.empty();
      private Optional<Boolean> writeColor = Optional.empty();
      private Optional<Boolean> writeAlpha = Optional.empty();
      private Optional<Boolean> writeDepth = Optional.empty();
      private Optional<LogicOp> colorLogic = Optional.empty();
      private Optional<BlendFunction> blendFunction = Optional.empty();
      private Optional<VertexFormat> vertexFormat = Optional.empty();
      private Optional<VertexFormat.Mode> vertexFormatMode = Optional.empty();
      private float depthBiasScaleFactor;
      private float depthBiasConstant;

      Builder() {
         super();
      }

      public RenderPipeline.Builder withLocation(String var1) {
         this.location = Optional.of(ResourceLocation.withDefaultNamespace(var1));
         return this;
      }

      public RenderPipeline.Builder withLocation(ResourceLocation var1) {
         this.location = Optional.of(var1);
         return this;
      }

      public RenderPipeline.Builder withFragmentShader(String var1) {
         this.fragmentShader = Optional.of(ResourceLocation.withDefaultNamespace(var1));
         return this;
      }

      public RenderPipeline.Builder withFragmentShader(ResourceLocation var1) {
         this.fragmentShader = Optional.of(var1);
         return this;
      }

      public RenderPipeline.Builder withVertexShader(String var1) {
         this.vertexShader = Optional.of(ResourceLocation.withDefaultNamespace(var1));
         return this;
      }

      public RenderPipeline.Builder withVertexShader(ResourceLocation var1) {
         this.vertexShader = Optional.of(var1);
         return this;
      }

      public RenderPipeline.Builder withShaderDefine(String var1) {
         if (this.definesBuilder.isEmpty()) {
            this.definesBuilder = Optional.of(ShaderDefines.builder());
         }

         ((ShaderDefines.Builder)this.definesBuilder.get()).define(var1);
         return this;
      }

      public RenderPipeline.Builder withShaderDefine(String var1, int var2) {
         if (this.definesBuilder.isEmpty()) {
            this.definesBuilder = Optional.of(ShaderDefines.builder());
         }

         ((ShaderDefines.Builder)this.definesBuilder.get()).define(var1, var2);
         return this;
      }

      public RenderPipeline.Builder withShaderDefine(String var1, float var2) {
         if (this.definesBuilder.isEmpty()) {
            this.definesBuilder = Optional.of(ShaderDefines.builder());
         }

         ((ShaderDefines.Builder)this.definesBuilder.get()).define(var1, var2);
         return this;
      }

      public RenderPipeline.Builder withSampler(String var1) {
         if (this.samplers.isEmpty()) {
            this.samplers = Optional.of(new ArrayList());
         }

         ((List)this.samplers.get()).add(var1);
         return this;
      }

      public RenderPipeline.Builder withUniform(String var1, UniformType var2) {
         if (this.uniforms.isEmpty()) {
            this.uniforms = Optional.of(new ArrayList());
         }

         if (var2 == UniformType.TEXEL_BUFFER) {
            throw new IllegalArgumentException("Cannot use texel buffer without specifying texture format");
         } else {
            ((List)this.uniforms.get()).add(new RenderPipeline.UniformDescription(var1, var2));
            return this;
         }
      }

      public RenderPipeline.Builder withUniform(String var1, UniformType var2, TextureFormat var3) {
         if (this.uniforms.isEmpty()) {
            this.uniforms = Optional.of(new ArrayList());
         }

         if (var2 != UniformType.TEXEL_BUFFER) {
            throw new IllegalArgumentException("Only texel buffer can specify texture format");
         } else {
            ((List)this.uniforms.get()).add(new RenderPipeline.UniformDescription(var1, var3));
            return this;
         }
      }

      public RenderPipeline.Builder withDepthTestFunction(DepthTestFunction var1) {
         this.depthTestFunction = Optional.of(var1);
         return this;
      }

      public RenderPipeline.Builder withPolygonMode(PolygonMode var1) {
         this.polygonMode = Optional.of(var1);
         return this;
      }

      public RenderPipeline.Builder withCull(boolean var1) {
         this.cull = Optional.of(var1);
         return this;
      }

      public RenderPipeline.Builder withBlend(BlendFunction var1) {
         this.blendFunction = Optional.of(var1);
         return this;
      }

      public RenderPipeline.Builder withoutBlend() {
         this.blendFunction = Optional.empty();
         return this;
      }

      public RenderPipeline.Builder withColorWrite(boolean var1) {
         this.writeColor = Optional.of(var1);
         this.writeAlpha = Optional.of(var1);
         return this;
      }

      public RenderPipeline.Builder withColorWrite(boolean var1, boolean var2) {
         this.writeColor = Optional.of(var1);
         this.writeAlpha = Optional.of(var2);
         return this;
      }

      public RenderPipeline.Builder withDepthWrite(boolean var1) {
         this.writeDepth = Optional.of(var1);
         return this;
      }

      /** @deprecated */
      @Deprecated
      public RenderPipeline.Builder withColorLogic(LogicOp var1) {
         this.colorLogic = Optional.of(var1);
         return this;
      }

      public RenderPipeline.Builder withVertexFormat(VertexFormat var1, VertexFormat.Mode var2) {
         this.vertexFormat = Optional.of(var1);
         this.vertexFormatMode = Optional.of(var2);
         return this;
      }

      public RenderPipeline.Builder withDepthBias(float var1, float var2) {
         this.depthBiasScaleFactor = var1;
         this.depthBiasConstant = var2;
         return this;
      }

      void withSnippet(RenderPipeline.Snippet var1) {
         if (var1.vertexShader.isPresent()) {
            this.vertexShader = var1.vertexShader;
         }

         if (var1.fragmentShader.isPresent()) {
            this.fragmentShader = var1.fragmentShader;
         }

         if (var1.shaderDefines.isPresent()) {
            if (this.definesBuilder.isEmpty()) {
               this.definesBuilder = Optional.of(ShaderDefines.builder());
            }

            ShaderDefines var2 = (ShaderDefines)var1.shaderDefines.get();
            Iterator var3 = var2.values().entrySet().iterator();

            while(var3.hasNext()) {
               Entry var4 = (Entry)var3.next();
               ((ShaderDefines.Builder)this.definesBuilder.get()).define((String)var4.getKey(), (String)var4.getValue());
            }

            var3 = var2.flags().iterator();

            while(var3.hasNext()) {
               String var5 = (String)var3.next();
               ((ShaderDefines.Builder)this.definesBuilder.get()).define(var5);
            }
         }

         var1.samplers.ifPresent((var1x) -> {
            if (this.samplers.isPresent()) {
               ((List)this.samplers.get()).addAll(var1x);
            } else {
               this.samplers = Optional.of(new ArrayList(var1x));
            }

         });
         var1.uniforms.ifPresent((var1x) -> {
            if (this.uniforms.isPresent()) {
               ((List)this.uniforms.get()).addAll(var1x);
            } else {
               this.uniforms = Optional.of(new ArrayList(var1x));
            }

         });
         if (var1.depthTestFunction.isPresent()) {
            this.depthTestFunction = var1.depthTestFunction;
         }

         if (var1.cull.isPresent()) {
            this.cull = var1.cull;
         }

         if (var1.writeColor.isPresent()) {
            this.writeColor = var1.writeColor;
         }

         if (var1.writeAlpha.isPresent()) {
            this.writeAlpha = var1.writeAlpha;
         }

         if (var1.writeDepth.isPresent()) {
            this.writeDepth = var1.writeDepth;
         }

         if (var1.colorLogic.isPresent()) {
            this.colorLogic = var1.colorLogic;
         }

         if (var1.blendFunction.isPresent()) {
            this.blendFunction = var1.blendFunction;
         }

         if (var1.vertexFormat.isPresent()) {
            this.vertexFormat = var1.vertexFormat;
         }

         if (var1.vertexFormatMode.isPresent()) {
            this.vertexFormatMode = var1.vertexFormatMode;
         }

      }

      public RenderPipeline.Snippet buildSnippet() {
         return new RenderPipeline.Snippet(this.vertexShader, this.fragmentShader, this.definesBuilder.map(ShaderDefines.Builder::build), this.samplers.map(Collections::unmodifiableList), this.uniforms.map(Collections::unmodifiableList), this.blendFunction, this.depthTestFunction, this.polygonMode, this.cull, this.writeColor, this.writeAlpha, this.writeDepth, this.colorLogic, this.vertexFormat, this.vertexFormatMode);
      }

      public RenderPipeline build() {
         if (this.location.isEmpty()) {
            throw new IllegalStateException("Missing location");
         } else if (this.vertexShader.isEmpty()) {
            throw new IllegalStateException("Missing vertex shader");
         } else if (this.fragmentShader.isEmpty()) {
            throw new IllegalStateException("Missing fragment shader");
         } else if (this.vertexFormat.isEmpty()) {
            throw new IllegalStateException("Missing vertex buffer format");
         } else if (this.vertexFormatMode.isEmpty()) {
            throw new IllegalStateException("Missing vertex mode");
         } else {
            return new RenderPipeline((ResourceLocation)this.location.get(), (ResourceLocation)this.vertexShader.get(), (ResourceLocation)this.fragmentShader.get(), ((ShaderDefines.Builder)this.definesBuilder.orElse(ShaderDefines.builder())).build(), List.copyOf((Collection)this.samplers.orElse(new ArrayList())), (List)this.uniforms.orElse(Collections.emptyList()), this.blendFunction, (DepthTestFunction)this.depthTestFunction.orElse(DepthTestFunction.LEQUAL_DEPTH_TEST), (PolygonMode)this.polygonMode.orElse(PolygonMode.FILL), (Boolean)this.cull.orElse(true), (Boolean)this.writeColor.orElse(true), (Boolean)this.writeAlpha.orElse(true), (Boolean)this.writeDepth.orElse(true), (LogicOp)this.colorLogic.orElse(LogicOp.NONE), (VertexFormat)this.vertexFormat.get(), (VertexFormat.Mode)this.vertexFormatMode.get(), this.depthBiasScaleFactor, this.depthBiasConstant, nextPipelineSortKey++);
         }
      }
   }

   @DontObfuscate
   public static record Snippet(Optional<ResourceLocation> vertexShader, Optional<ResourceLocation> fragmentShader, Optional<ShaderDefines> shaderDefines, Optional<List<String>> samplers, Optional<List<RenderPipeline.UniformDescription>> uniforms, Optional<BlendFunction> blendFunction, Optional<DepthTestFunction> depthTestFunction, Optional<PolygonMode> polygonMode, Optional<Boolean> cull, Optional<Boolean> writeColor, Optional<Boolean> writeAlpha, Optional<Boolean> writeDepth, Optional<LogicOp> colorLogic, Optional<VertexFormat> vertexFormat, Optional<VertexFormat.Mode> vertexFormatMode) {
      final Optional<ResourceLocation> vertexShader;
      final Optional<ResourceLocation> fragmentShader;
      final Optional<ShaderDefines> shaderDefines;
      final Optional<List<String>> samplers;
      final Optional<List<RenderPipeline.UniformDescription>> uniforms;
      final Optional<BlendFunction> blendFunction;
      final Optional<DepthTestFunction> depthTestFunction;
      final Optional<Boolean> cull;
      final Optional<Boolean> writeColor;
      final Optional<Boolean> writeAlpha;
      final Optional<Boolean> writeDepth;
      final Optional<LogicOp> colorLogic;
      final Optional<VertexFormat> vertexFormat;
      final Optional<VertexFormat.Mode> vertexFormatMode;

      public Snippet(Optional<ResourceLocation> param1, Optional<ResourceLocation> param2, Optional<ShaderDefines> param3, Optional<List<String>> param4, Optional<List<RenderPipeline.UniformDescription>> param5, Optional<BlendFunction> param6, Optional<DepthTestFunction> param7, Optional<PolygonMode> param8, Optional<Boolean> param9, Optional<Boolean> param10, Optional<Boolean> param11, Optional<Boolean> param12, Optional<LogicOp> param13, Optional<VertexFormat> param14, Optional<VertexFormat.Mode> param15) {
         super();
         this.vertexShader = var1;
         this.fragmentShader = var2;
         this.shaderDefines = var3;
         this.samplers = var4;
         this.uniforms = var5;
         this.blendFunction = var6;
         this.depthTestFunction = var7;
         this.polygonMode = var8;
         this.cull = var9;
         this.writeColor = var10;
         this.writeAlpha = var11;
         this.writeDepth = var12;
         this.colorLogic = var13;
         this.vertexFormat = var14;
         this.vertexFormatMode = var15;
      }

      public Optional<ResourceLocation> vertexShader() {
         return this.vertexShader;
      }

      public Optional<ResourceLocation> fragmentShader() {
         return this.fragmentShader;
      }

      public Optional<ShaderDefines> shaderDefines() {
         return this.shaderDefines;
      }

      public Optional<List<String>> samplers() {
         return this.samplers;
      }

      public Optional<List<RenderPipeline.UniformDescription>> uniforms() {
         return this.uniforms;
      }

      public Optional<BlendFunction> blendFunction() {
         return this.blendFunction;
      }

      public Optional<DepthTestFunction> depthTestFunction() {
         return this.depthTestFunction;
      }

      public Optional<PolygonMode> polygonMode() {
         return this.polygonMode;
      }

      public Optional<Boolean> cull() {
         return this.cull;
      }

      public Optional<Boolean> writeColor() {
         return this.writeColor;
      }

      public Optional<Boolean> writeAlpha() {
         return this.writeAlpha;
      }

      public Optional<Boolean> writeDepth() {
         return this.writeDepth;
      }

      public Optional<LogicOp> colorLogic() {
         return this.colorLogic;
      }

      public Optional<VertexFormat> vertexFormat() {
         return this.vertexFormat;
      }

      public Optional<VertexFormat.Mode> vertexFormatMode() {
         return this.vertexFormatMode;
      }
   }

   @DontObfuscate
   public static record UniformDescription(String name, UniformType type, @Nullable TextureFormat textureFormat) {
      public UniformDescription(String var1, UniformType var2) {
         this(var1, var2, (TextureFormat)null);
         if (var2 == UniformType.TEXEL_BUFFER) {
            throw new IllegalArgumentException("Texel buffer needs a texture format");
         }
      }

      public UniformDescription(String var1, TextureFormat var2) {
         this(var1, UniformType.TEXEL_BUFFER, var2);
      }

      public UniformDescription(String param1, UniformType param2, @Nullable TextureFormat param3) {
         super();
         this.name = var1;
         this.type = var2;
         this.textureFormat = var3;
      }

      public String name() {
         return this.name;
      }

      public UniformType type() {
         return this.type;
      }

      @Nullable
      public TextureFormat textureFormat() {
         return this.textureFormat;
      }
   }
}
