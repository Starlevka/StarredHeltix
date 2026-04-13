package net.minecraft.client.renderer;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.datafixers.util.Pair;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Map.Entry;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.system.MemoryStack;

public class PostPass implements AutoCloseable {
   private static final int UBO_SIZE_PER_SAMPLER = (new Std140SizeCalculator()).putVec2().get();
   private final String name;
   private final RenderPipeline pipeline;
   private final ResourceLocation outputTargetId;
   private final Map<String, GpuBuffer> customUniforms = new HashMap();
   private final MappableRingBuffer infoUbo;
   private final List<PostPass.Input> inputs;

   public PostPass(RenderPipeline var1, ResourceLocation var2, Map<String, List<UniformValue>> var3, List<PostPass.Input> var4) {
      super();
      this.pipeline = var1;
      this.name = var1.getLocation().toString();
      this.outputTargetId = var2;
      this.inputs = var4;
      Iterator var5 = var3.entrySet().iterator();

      while(true) {
         Entry var6;
         List var7;
         do {
            if (!var5.hasNext()) {
               this.infoUbo = new MappableRingBuffer(() -> {
                  return this.name + " SamplerInfo";
               }, 130, (var4.size() + 1) * UBO_SIZE_PER_SAMPLER);
               return;
            }

            var6 = (Entry)var5.next();
            var7 = (List)var6.getValue();
         } while(var7.isEmpty());

         Std140SizeCalculator var8 = new Std140SizeCalculator();
         Iterator var9 = var7.iterator();

         while(var9.hasNext()) {
            UniformValue var10 = (UniformValue)var9.next();
            var10.addSize(var8);
         }

         int var16 = var8.get();
         MemoryStack var17 = MemoryStack.stackPush();

         try {
            Std140Builder var11 = Std140Builder.onStack(var17, var16);
            Iterator var12 = var7.iterator();

            while(true) {
               if (!var12.hasNext()) {
                  this.customUniforms.put((String)var6.getKey(), RenderSystem.getDevice().createBuffer(() -> {
                     String var10000 = this.name;
                     return var10000 + " / " + (String)var6.getKey();
                  }, 128, var11.get()));
                  break;
               }

               UniformValue var13 = (UniformValue)var12.next();
               var13.writeTo(var11);
            }
         } catch (Throwable var15) {
            if (var17 != null) {
               try {
                  var17.close();
               } catch (Throwable var14) {
                  var15.addSuppressed(var14);
               }
            }

            throw var15;
         }

         if (var17 != null) {
            var17.close();
         }
      }
   }

   public void addToFrame(FrameGraphBuilder var1, Map<ResourceLocation, ResourceHandle<RenderTarget>> var2, GpuBufferSlice var3) {
      FramePass var4 = var1.addPass(this.name);
      Iterator var5 = this.inputs.iterator();

      while(var5.hasNext()) {
         PostPass.Input var6 = (PostPass.Input)var5.next();
         var6.addToPass(var4, var2);
      }

      ResourceHandle var7 = (ResourceHandle)var2.computeIfPresent(this.outputTargetId, (var1x, var2x) -> {
         return var4.readsAndWrites(var2x);
      });
      if (var7 == null) {
         throw new IllegalStateException("Missing handle for target " + String.valueOf(this.outputTargetId));
      } else {
         var4.executes(() -> {
            RenderTarget var4 = (RenderTarget)var7.get();
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(var3, ProjectionType.ORTHOGRAPHIC);
            CommandEncoder var5 = RenderSystem.getDevice().createCommandEncoder();
            List var6 = this.inputs.stream().map((var1) -> {
               return Pair.of(var1.samplerName(), var1.texture(var2));
            }).toList();
            GpuBuffer.MappedView var7x = var5.mapBuffer(this.infoUbo.currentBuffer(), false, true);

            try {
               Std140Builder var8 = Std140Builder.intoBuffer(var7x.data());
               var8.putVec2((float)var4.width, (float)var4.height);
               Iterator var9 = var6.iterator();

               while(var9.hasNext()) {
                  Pair var10 = (Pair)var9.next();
                  var8.putVec2((float)((GpuTextureView)var10.getSecond()).getWidth(0), (float)((GpuTextureView)var10.getSecond()).getHeight(0));
               }
            } catch (Throwable var14) {
               if (var7x != null) {
                  try {
                     var7x.close();
                  } catch (Throwable var12) {
                     var14.addSuppressed(var12);
                  }
               }

               throw var14;
            }

            if (var7x != null) {
               var7x.close();
            }

            RenderPass var15 = var5.createRenderPass(() -> {
               return "Post pass " + this.name;
            }, var4.getColorTextureView(), OptionalInt.empty(), var4.useDepth ? var4.getDepthTextureView() : null, OptionalDouble.empty());

            try {
               var15.setPipeline(this.pipeline);
               RenderSystem.bindDefaultUniforms(var15);
               var15.setUniform("SamplerInfo", this.infoUbo.currentBuffer());
               Iterator var17 = this.customUniforms.entrySet().iterator();

               while(var17.hasNext()) {
                  Entry var19 = (Entry)var17.next();
                  var15.setUniform((String)var19.getKey(), (GpuBuffer)var19.getValue());
               }

               var17 = var6.iterator();

               while(true) {
                  if (!var17.hasNext()) {
                     var15.draw(0, 3);
                     break;
                  }

                  Pair var20 = (Pair)var17.next();
                  var15.bindSampler((String)var20.getFirst() + "Sampler", (GpuTextureView)var20.getSecond());
               }
            } catch (Throwable var13) {
               if (var15 != null) {
                  try {
                     var15.close();
                  } catch (Throwable var11) {
                     var13.addSuppressed(var11);
                  }
               }

               throw var13;
            }

            if (var15 != null) {
               var15.close();
            }

            this.infoUbo.rotate();
            RenderSystem.restoreProjectionMatrix();
            Iterator var16 = this.inputs.iterator();

            while(var16.hasNext()) {
               PostPass.Input var18 = (PostPass.Input)var16.next();
               var18.cleanup(var2);
            }

         });
      }
   }

   public void close() {
      Iterator var1 = this.customUniforms.values().iterator();

      while(var1.hasNext()) {
         GpuBuffer var2 = (GpuBuffer)var1.next();
         var2.close();
      }

      this.infoUbo.close();
   }

   public interface Input {
      void addToPass(FramePass var1, Map<ResourceLocation, ResourceHandle<RenderTarget>> var2);

      default void cleanup(Map<ResourceLocation, ResourceHandle<RenderTarget>> var1) {
      }

      GpuTextureView texture(Map<ResourceLocation, ResourceHandle<RenderTarget>> var1);

      String samplerName();
   }

   public static record TargetInput(String samplerName, ResourceLocation targetId, boolean depthBuffer, boolean bilinear) implements PostPass.Input {
      public TargetInput(String param1, ResourceLocation param2, boolean param3, boolean param4) {
         super();
         this.samplerName = var1;
         this.targetId = var2;
         this.depthBuffer = var3;
         this.bilinear = var4;
      }

      private ResourceHandle<RenderTarget> getHandle(Map<ResourceLocation, ResourceHandle<RenderTarget>> var1) {
         ResourceHandle var2 = (ResourceHandle)var1.get(this.targetId);
         if (var2 == null) {
            throw new IllegalStateException("Missing handle for target " + String.valueOf(this.targetId));
         } else {
            return var2;
         }
      }

      public void addToPass(FramePass var1, Map<ResourceLocation, ResourceHandle<RenderTarget>> var2) {
         var1.reads(this.getHandle(var2));
      }

      public void cleanup(Map<ResourceLocation, ResourceHandle<RenderTarget>> var1) {
         if (this.bilinear) {
            ((RenderTarget)this.getHandle(var1).get()).setFilterMode(FilterMode.NEAREST);
         }

      }

      public GpuTextureView texture(Map<ResourceLocation, ResourceHandle<RenderTarget>> var1) {
         ResourceHandle var2 = this.getHandle(var1);
         RenderTarget var3 = (RenderTarget)var2.get();
         var3.setFilterMode(this.bilinear ? FilterMode.LINEAR : FilterMode.NEAREST);
         GpuTextureView var4 = this.depthBuffer ? var3.getDepthTextureView() : var3.getColorTextureView();
         if (var4 == null) {
            String var10002 = this.depthBuffer ? "depth" : "color";
            throw new IllegalStateException("Missing " + var10002 + "texture for target " + String.valueOf(this.targetId));
         } else {
            return var4;
         }
      }

      public String samplerName() {
         return this.samplerName;
      }

      public ResourceLocation targetId() {
         return this.targetId;
      }

      public boolean depthBuffer() {
         return this.depthBuffer;
      }

      public boolean bilinear() {
         return this.bilinear;
      }
   }

   public static record TextureInput(String samplerName, AbstractTexture texture, int width, int height) implements PostPass.Input {
      public TextureInput(String param1, AbstractTexture param2, int param3, int param4) {
         super();
         this.samplerName = var1;
         this.texture = var2;
         this.width = var3;
         this.height = var4;
      }

      public void addToPass(FramePass var1, Map<ResourceLocation, ResourceHandle<RenderTarget>> var2) {
      }

      public GpuTextureView texture(Map<ResourceLocation, ResourceHandle<RenderTarget>> var1) {
         return this.texture.getTextureView();
      }

      public String samplerName() {
         return this.samplerName;
      }

      public AbstractTexture texture() {
         return this.texture;
      }

      public int width() {
         return this.width;
      }

      public int height() {
         return this.height;
      }
   }
}
