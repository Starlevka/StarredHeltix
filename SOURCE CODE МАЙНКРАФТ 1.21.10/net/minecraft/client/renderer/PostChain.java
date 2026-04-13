package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Sets.SetView;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.shaders.UniformType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public class PostChain implements AutoCloseable {
   public static final ResourceLocation MAIN_TARGET_ID = ResourceLocation.withDefaultNamespace("main");
   private final List<PostPass> passes;
   private final Map<ResourceLocation, PostChainConfig.InternalTarget> internalTargets;
   private final Set<ResourceLocation> externalTargets;
   private final Map<ResourceLocation, RenderTarget> persistentTargets = new HashMap();
   private final CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer;

   private PostChain(List<PostPass> var1, Map<ResourceLocation, PostChainConfig.InternalTarget> var2, Set<ResourceLocation> var3, CachedOrthoProjectionMatrixBuffer var4) {
      super();
      this.passes = var1;
      this.internalTargets = var2;
      this.externalTargets = var3;
      this.projectionMatrixBuffer = var4;
   }

   public static PostChain load(PostChainConfig var0, TextureManager var1, Set<ResourceLocation> var2, ResourceLocation var3, CachedOrthoProjectionMatrixBuffer var4) throws ShaderManager.CompilationException {
      Stream var5 = var0.passes().stream().flatMap(PostChainConfig.Pass::referencedTargets);
      Set var6 = (Set)var5.filter((var1x) -> {
         return !var0.internalTargets().containsKey(var1x);
      }).collect(Collectors.toSet());
      SetView var7 = Sets.difference(var6, var2);
      if (!var7.isEmpty()) {
         throw new ShaderManager.CompilationException("Referenced external targets are not available in this context: " + String.valueOf(var7));
      } else {
         Builder var8 = ImmutableList.builder();

         for(int var9 = 0; var9 < var0.passes().size(); ++var9) {
            PostChainConfig.Pass var10 = (PostChainConfig.Pass)var0.passes().get(var9);
            var8.add(createPass(var1, var10, var3.withSuffix("/" + var9)));
         }

         return new PostChain(var8.build(), var0.internalTargets(), var6, var4);
      }
   }

   private static PostPass createPass(TextureManager var0, PostChainConfig.Pass var1, ResourceLocation var2) throws ShaderManager.CompilationException {
      RenderPipeline.Builder var3 = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET).withFragmentShader(var1.fragmentShaderId()).withVertexShader(var1.vertexShaderId()).withLocation(var2);
      Iterator var4 = var1.inputs().iterator();

      while(var4.hasNext()) {
         PostChainConfig.Input var5 = (PostChainConfig.Input)var4.next();
         var3.withSampler(var5.samplerName() + "Sampler");
      }

      var3.withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER);
      var4 = var1.uniforms().keySet().iterator();

      while(var4.hasNext()) {
         String var32 = (String)var4.next();
         var3.withUniform(var32, UniformType.UNIFORM_BUFFER);
      }

      RenderPipeline var31 = var3.build();
      ArrayList var33 = new ArrayList();
      Iterator var6 = var1.inputs().iterator();

      while(var6.hasNext()) {
         PostChainConfig.Input var7 = (PostChainConfig.Input)var6.next();
         Objects.requireNonNull(var7);
         byte var9 = 0;
         boolean var10001;
         Throwable var35;
         String var37;
         ResourceLocation var40;
         boolean var43;
         switch(var7.typeSwitch<invokedynamic>(var7, var9)) {
         case 0:
            PostChainConfig.TextureInput var10 = (PostChainConfig.TextureInput)var7;
            PostChainConfig.TextureInput var44 = var10;

            try {
               var37 = var44.samplerName();
            } catch (Throwable var30) {
               var35 = var30;
               var10001 = false;
               break;
            }

            String var36 = var37;
            String var11 = var36;
            var44 = var10;

            try {
               var40 = var44.location();
            } catch (Throwable var29) {
               var35 = var29;
               var10001 = false;
               break;
            }

            ResourceLocation var38 = var40;
            ResourceLocation var12 = var38;
            var44 = var10;

            int var47;
            try {
               var47 = var44.width();
            } catch (Throwable var28) {
               var35 = var28;
               var10001 = false;
               break;
            }

            int var39 = var47;
            int var13 = var39;
            var44 = var10;

            try {
               var47 = var44.height();
            } catch (Throwable var27) {
               var35 = var27;
               var10001 = false;
               break;
            }

            var39 = var47;
            int var14 = var39;
            var44 = var10;

            try {
               var43 = var44.bilinear();
            } catch (Throwable var26) {
               var35 = var26;
               var10001 = false;
               break;
            }

            boolean var41 = var43;
            boolean var15 = var41;
            AbstractTexture var42 = var0.getTexture(var12.withPath((var0x) -> {
               return "textures/effect/" + var0x + ".png";
            }));
            var42.setFilter(var15, false);
            var33.add(new PostPass.TextureInput(var11, var42, var13, var14));
            continue;
         case 1:
            PostChainConfig.TargetInput var16 = (PostChainConfig.TargetInput)var7;
            PostChainConfig.TargetInput var10000 = var16;

            try {
               var37 = var10000.samplerName();
            } catch (Throwable var25) {
               var35 = var25;
               var10001 = false;
               break;
            }

            String var21 = var37;
            String var17 = var21;
            var10000 = var16;

            try {
               var40 = var10000.targetId();
            } catch (Throwable var24) {
               var35 = var24;
               var10001 = false;
               break;
            }

            ResourceLocation var45 = var40;
            ResourceLocation var18 = var45;
            var10000 = var16;

            try {
               var43 = var10000.useDepthBuffer();
            } catch (Throwable var23) {
               var35 = var23;
               var10001 = false;
               break;
            }

            boolean var46 = var43;
            boolean var19 = var46;
            var10000 = var16;

            try {
               var43 = var10000.bilinear();
            } catch (Throwable var22) {
               var35 = var22;
               var10001 = false;
               break;
            }

            var46 = var43;
            var33.add(new PostPass.TargetInput(var17, var18, var19, var46));
            continue;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         Throwable var34 = var35;
         throw new MatchException(var34.toString(), var34);
      }

      return new PostPass(var31, var1.outputTarget(), var1.uniforms(), var33);
   }

   public void addToFrame(FrameGraphBuilder var1, int var2, int var3, PostChain.TargetBundle var4) {
      GpuBufferSlice var5 = this.projectionMatrixBuffer.getBuffer((float)var2, (float)var3);
      HashMap var6 = new HashMap(this.internalTargets.size() + this.externalTargets.size());
      Iterator var7 = this.externalTargets.iterator();

      ResourceLocation var8;
      while(var7.hasNext()) {
         var8 = (ResourceLocation)var7.next();
         var6.put(var8, var4.getOrThrow(var8));
      }

      var7 = this.internalTargets.entrySet().iterator();

      while(var7.hasNext()) {
         Entry var13 = (Entry)var7.next();
         ResourceLocation var9 = (ResourceLocation)var13.getKey();
         PostChainConfig.InternalTarget var10 = (PostChainConfig.InternalTarget)var13.getValue();
         RenderTargetDescriptor var11 = new RenderTargetDescriptor((Integer)var10.width().orElse(var2), (Integer)var10.height().orElse(var3), true, var10.clearColor());
         if (var10.persistent()) {
            RenderTarget var12 = this.getOrCreatePersistentTarget(var9, var11);
            var6.put(var9, var1.importExternal(var9.toString(), var12));
         } else {
            var6.put(var9, var1.createInternal(var9.toString(), var11));
         }
      }

      var7 = this.passes.iterator();

      while(var7.hasNext()) {
         PostPass var14 = (PostPass)var7.next();
         var14.addToFrame(var1, var6, var5);
      }

      var7 = this.externalTargets.iterator();

      while(var7.hasNext()) {
         var8 = (ResourceLocation)var7.next();
         var4.replace(var8, (ResourceHandle)var6.get(var8));
      }

   }

   /** @deprecated */
   @Deprecated
   public void process(RenderTarget var1, GraphicsResourceAllocator var2) {
      FrameGraphBuilder var3 = new FrameGraphBuilder();
      PostChain.TargetBundle var4 = PostChain.TargetBundle.of(MAIN_TARGET_ID, var3.importExternal("main", var1));
      this.addToFrame(var3, var1.width, var1.height, var4);
      var3.execute(var2);
   }

   private RenderTarget getOrCreatePersistentTarget(ResourceLocation var1, RenderTargetDescriptor var2) {
      RenderTarget var3 = (RenderTarget)this.persistentTargets.get(var1);
      if (var3 == null || var3.width != var2.width() || var3.height != var2.height()) {
         if (var3 != null) {
            var3.destroyBuffers();
         }

         var3 = var2.allocate();
         var2.prepare(var3);
         this.persistentTargets.put(var1, var3);
      }

      return var3;
   }

   public void close() {
      this.persistentTargets.values().forEach(RenderTarget::destroyBuffers);
      this.persistentTargets.clear();
      Iterator var1 = this.passes.iterator();

      while(var1.hasNext()) {
         PostPass var2 = (PostPass)var1.next();
         var2.close();
      }

   }

   public interface TargetBundle {
      static PostChain.TargetBundle of(final ResourceLocation var0, final ResourceHandle<RenderTarget> var1) {
         return new PostChain.TargetBundle() {
            private ResourceHandle<RenderTarget> handle = var1;

            public void replace(ResourceLocation var1x, ResourceHandle<RenderTarget> var2) {
               if (var1x.equals(var0)) {
                  this.handle = var2;
               } else {
                  throw new IllegalArgumentException("No target with id " + String.valueOf(var1x));
               }
            }

            @Nullable
            public ResourceHandle<RenderTarget> get(ResourceLocation var1x) {
               return var1x.equals(var0) ? this.handle : null;
            }
         };
      }

      void replace(ResourceLocation var1, ResourceHandle<RenderTarget> var2);

      @Nullable
      ResourceHandle<RenderTarget> get(ResourceLocation var1);

      default ResourceHandle<RenderTarget> getOrThrow(ResourceLocation var1) {
         ResourceHandle var2 = this.get(var1);
         if (var2 == null) {
            throw new IllegalArgumentException("Missing target with id " + String.valueOf(var1));
         } else {
            return var2;
         }
      }
   }
}
