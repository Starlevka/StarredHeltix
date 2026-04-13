package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class SkyRenderer implements AutoCloseable {
   private static final ResourceLocation SUN_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
   private static final ResourceLocation END_LIGHT_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/end_flash.png");
   private static final ResourceLocation MOON_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
   private static final ResourceLocation END_SKY_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/end_sky.png");
   private static final float SKY_DISC_RADIUS = 512.0F;
   private static final int SKY_VERTICES = 10;
   private static final int STAR_COUNT = 1500;
   private static final float SUN_SIZE = 30.0F;
   private static final float SUN_HEIGHT = 100.0F;
   private static final float MOON_SIZE = 20.0F;
   private static final float MOON_HEIGHT = 100.0F;
   private static final int SUNRISE_STEPS = 16;
   private static final int END_SKY_QUAD_COUNT = 6;
   private static final float END_FLASH_HEIGHT = 100.0F;
   private static final float END_FLASH_SCALE = 60.0F;
   private final GpuBuffer starBuffer;
   private final RenderSystem.AutoStorageIndexBuffer starIndices;
   private final GpuBuffer topSkyBuffer;
   private final GpuBuffer bottomSkyBuffer;
   private final GpuBuffer endSkyBuffer;
   private final GpuBuffer sunBuffer;
   private final GpuBuffer moonBuffer;
   private final GpuBuffer sunriseBuffer;
   private final GpuBuffer endFlashBuffer;
   private final RenderSystem.AutoStorageIndexBuffer quadIndices;
   @Nullable
   private AbstractTexture sunTexture;
   @Nullable
   private AbstractTexture moonTexture;
   @Nullable
   private AbstractTexture endSkyTexture;
   @Nullable
   private AbstractTexture endFlashTexture;
   private int starIndexCount;

   public SkyRenderer() {
      super();
      this.starIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
      this.quadIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
      this.starBuffer = this.buildStars();
      this.endSkyBuffer = buildEndSky();
      this.endFlashBuffer = this.buildEndFlashQuad();
      this.sunBuffer = this.buildSunQuad();
      this.moonBuffer = this.buildMoonPhases();
      this.sunriseBuffer = this.buildSunriseFan();
      ByteBufferBuilder var1 = ByteBufferBuilder.exactlySized(10 * DefaultVertexFormat.POSITION.getVertexSize());

      try {
         BufferBuilder var2 = new BufferBuilder(var1, VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
         this.buildSkyDisc(var2, 16.0F);
         MeshData var3 = var2.buildOrThrow();

         try {
            this.topSkyBuffer = RenderSystem.getDevice().createBuffer(() -> {
               return "Top sky vertex buffer";
            }, 32, var3.vertexBuffer());
         } catch (Throwable var9) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var7) {
                  var9.addSuppressed(var7);
               }
            }

            throw var9;
         }

         if (var3 != null) {
            var3.close();
         }

         var2 = new BufferBuilder(var1, VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
         this.buildSkyDisc(var2, -16.0F);
         var3 = var2.buildOrThrow();

         try {
            this.bottomSkyBuffer = RenderSystem.getDevice().createBuffer(() -> {
               return "Bottom sky vertex buffer";
            }, 32, var3.vertexBuffer());
         } catch (Throwable var10) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var10.addSuppressed(var8);
               }
            }

            throw var10;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Throwable var11) {
         if (var1 != null) {
            try {
               var1.close();
            } catch (Throwable var6) {
               var11.addSuppressed(var6);
            }
         }

         throw var11;
      }

      if (var1 != null) {
         var1.close();
      }

   }

   protected void initTextures() {
      this.endSkyTexture = this.getTexture(END_SKY_LOCATION);
      this.endFlashTexture = this.getTexture(END_LIGHT_LOCATION);
      this.sunTexture = this.getTexture(SUN_LOCATION);
      this.moonTexture = this.getTexture(MOON_LOCATION);
   }

   private AbstractTexture getTexture(ResourceLocation var1) {
      TextureManager var2 = Minecraft.getInstance().getTextureManager();
      AbstractTexture var3 = var2.getTexture(var1);
      var3.setUseMipmaps(false);
      return var3;
   }

   private GpuBuffer buildSunriseFan() {
      boolean var1 = true;
      int var2 = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
      ByteBufferBuilder var3 = ByteBufferBuilder.exactlySized(18 * var2);

      GpuBuffer var16;
      try {
         BufferBuilder var4 = new BufferBuilder(var3, VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
         int var5 = ARGB.white(1.0F);
         int var6 = ARGB.white(0.0F);
         var4.addVertex(0.0F, 100.0F, 0.0F).setColor(var5);

         for(int var7 = 0; var7 <= 16; ++var7) {
            float var8 = (float)var7 * 6.2831855F / 16.0F;
            float var9 = Mth.sin(var8);
            float var10 = Mth.cos(var8);
            var4.addVertex(var9 * 120.0F, var10 * 120.0F, -var10 * 40.0F).setColor(var6);
         }

         MeshData var15 = var4.buildOrThrow();

         try {
            var16 = RenderSystem.getDevice().createBuffer(() -> {
               return "Sunrise/Sunset fan";
            }, 32, var15.vertexBuffer());
         } catch (Throwable var13) {
            if (var15 != null) {
               try {
                  var15.close();
               } catch (Throwable var12) {
                  var13.addSuppressed(var12);
               }
            }

            throw var13;
         }

         if (var15 != null) {
            var15.close();
         }
      } catch (Throwable var14) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var11) {
               var14.addSuppressed(var11);
            }
         }

         throw var14;
      }

      if (var3 != null) {
         var3.close();
      }

      return var16;
   }

   private GpuBuffer buildSunQuad() {
      ByteBufferBuilder var1 = ByteBufferBuilder.exactlySized(4 * DefaultVertexFormat.POSITION_TEX.getVertexSize());

      GpuBuffer var5;
      try {
         BufferBuilder var2 = new BufferBuilder(var1, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         Matrix4f var3 = new Matrix4f();
         var2.addVertex(var3, -1.0F, 0.0F, -1.0F).setUv(0.0F, 0.0F);
         var2.addVertex(var3, 1.0F, 0.0F, -1.0F).setUv(1.0F, 0.0F);
         var2.addVertex(var3, 1.0F, 0.0F, 1.0F).setUv(1.0F, 1.0F);
         var2.addVertex(var3, -1.0F, 0.0F, 1.0F).setUv(0.0F, 1.0F);
         MeshData var4 = var2.buildOrThrow();

         try {
            var5 = RenderSystem.getDevice().createBuffer(() -> {
               return "Sun quad";
            }, 40, var4.vertexBuffer());
         } catch (Throwable var9) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (Throwable var10) {
         if (var1 != null) {
            try {
               var1.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (var1 != null) {
         var1.close();
      }

      return var5;
   }

   private GpuBuffer buildMoonPhases() {
      boolean var1 = true;
      int var2 = DefaultVertexFormat.POSITION_TEX.getVertexSize();
      ByteBufferBuilder var3 = ByteBufferBuilder.exactlySized(32 * var2);

      GpuBuffer var18;
      try {
         BufferBuilder var4 = new BufferBuilder(var3, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         Matrix4f var5 = new Matrix4f();

         for(int var6 = 0; var6 < 8; ++var6) {
            int var7 = var6 % 4;
            int var8 = var6 / 4 % 2;
            float var9 = (float)var7 / 4.0F;
            float var10 = (float)var8 / 2.0F;
            float var11 = (float)(var7 + 1) / 4.0F;
            float var12 = (float)(var8 + 1) / 2.0F;
            var4.addVertex(var5, -1.0F, 0.0F, 1.0F).setUv(var11, var12);
            var4.addVertex(var5, 1.0F, 0.0F, 1.0F).setUv(var9, var12);
            var4.addVertex(var5, 1.0F, 0.0F, -1.0F).setUv(var9, var10);
            var4.addVertex(var5, -1.0F, 0.0F, -1.0F).setUv(var11, var10);
         }

         MeshData var17 = var4.buildOrThrow();

         try {
            var18 = RenderSystem.getDevice().createBuffer(() -> {
               return "Moon phases";
            }, 32, var17.vertexBuffer());
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
      } catch (Throwable var16) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var13) {
               var16.addSuppressed(var13);
            }
         }

         throw var16;
      }

      if (var3 != null) {
         var3.close();
      }

      return var18;
   }

   private GpuBuffer buildStars() {
      RandomSource var1 = RandomSource.create(10842L);
      float var2 = 100.0F;
      ByteBufferBuilder var3 = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION.getVertexSize() * 1500 * 4);

      GpuBuffer var19;
      try {
         BufferBuilder var4 = new BufferBuilder(var3, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

         for(int var5 = 0; var5 < 1500; ++var5) {
            float var6 = var1.nextFloat() * 2.0F - 1.0F;
            float var7 = var1.nextFloat() * 2.0F - 1.0F;
            float var8 = var1.nextFloat() * 2.0F - 1.0F;
            float var9 = 0.15F + var1.nextFloat() * 0.1F;
            float var10 = Mth.lengthSquared(var6, var7, var8);
            if (!(var10 <= 0.010000001F) && !(var10 >= 1.0F)) {
               Vector3f var11 = (new Vector3f(var6, var7, var8)).normalize(100.0F);
               float var12 = (float)(var1.nextDouble() * 3.1415927410125732D * 2.0D);
               Matrix3f var13 = (new Matrix3f()).rotateTowards((new Vector3f(var11)).negate(), new Vector3f(0.0F, 1.0F, 0.0F)).rotateZ(-var12);
               var4.addVertex((new Vector3f(var9, -var9, 0.0F)).mul(var13).add(var11));
               var4.addVertex((new Vector3f(var9, var9, 0.0F)).mul(var13).add(var11));
               var4.addVertex((new Vector3f(-var9, var9, 0.0F)).mul(var13).add(var11));
               var4.addVertex((new Vector3f(-var9, -var9, 0.0F)).mul(var13).add(var11));
            }
         }

         MeshData var18 = var4.buildOrThrow();

         try {
            this.starIndexCount = var18.drawState().indexCount();
            var19 = RenderSystem.getDevice().createBuffer(() -> {
               return "Stars vertex buffer";
            }, 40, var18.vertexBuffer());
         } catch (Throwable var16) {
            if (var18 != null) {
               try {
                  var18.close();
               } catch (Throwable var15) {
                  var16.addSuppressed(var15);
               }
            }

            throw var16;
         }

         if (var18 != null) {
            var18.close();
         }
      } catch (Throwable var17) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var14) {
               var17.addSuppressed(var14);
            }
         }

         throw var17;
      }

      if (var3 != null) {
         var3.close();
      }

      return var19;
   }

   private void buildSkyDisc(VertexConsumer var1, float var2) {
      float var3 = Math.signum(var2) * 512.0F;
      var1.addVertex(0.0F, var2, 0.0F);

      for(int var4 = -180; var4 <= 180; var4 += 45) {
         var1.addVertex(var3 * Mth.cos((float)var4 * 0.017453292F), var2, 512.0F * Mth.sin((float)var4 * 0.017453292F));
      }

   }

   private static GpuBuffer buildEndSky() {
      ByteBufferBuilder var0 = ByteBufferBuilder.exactlySized(24 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize());

      GpuBuffer var10;
      try {
         BufferBuilder var1 = new BufferBuilder(var0, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

         for(int var2 = 0; var2 < 6; ++var2) {
            Matrix4f var3 = new Matrix4f();
            switch(var2) {
            case 1:
               var3.rotationX(1.5707964F);
               break;
            case 2:
               var3.rotationX(-1.5707964F);
               break;
            case 3:
               var3.rotationX(3.1415927F);
               break;
            case 4:
               var3.rotationZ(1.5707964F);
               break;
            case 5:
               var3.rotationZ(-1.5707964F);
            }

            var1.addVertex(var3, -100.0F, -100.0F, -100.0F).setUv(0.0F, 0.0F).setColor(-14145496);
            var1.addVertex(var3, -100.0F, -100.0F, 100.0F).setUv(0.0F, 16.0F).setColor(-14145496);
            var1.addVertex(var3, 100.0F, -100.0F, 100.0F).setUv(16.0F, 16.0F).setColor(-14145496);
            var1.addVertex(var3, 100.0F, -100.0F, -100.0F).setUv(16.0F, 0.0F).setColor(-14145496);
         }

         MeshData var9 = var1.buildOrThrow();

         try {
            var10 = RenderSystem.getDevice().createBuffer(() -> {
               return "End sky vertex buffer";
            }, 40, var9.vertexBuffer());
         } catch (Throwable var7) {
            if (var9 != null) {
               try {
                  var9.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (var9 != null) {
            var9.close();
         }
      } catch (Throwable var8) {
         if (var0 != null) {
            try {
               var0.close();
            } catch (Throwable var5) {
               var8.addSuppressed(var5);
            }
         }

         throw var8;
      }

      if (var0 != null) {
         var0.close();
      }

      return var10;
   }

   private GpuBuffer buildEndFlashQuad() {
      ByteBufferBuilder var1 = ByteBufferBuilder.exactlySized(4 * DefaultVertexFormat.POSITION_TEX.getVertexSize());

      GpuBuffer var5;
      try {
         BufferBuilder var2 = new BufferBuilder(var1, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         Matrix4f var3 = new Matrix4f();
         var2.addVertex(var3, -1.0F, 0.0F, -1.0F).setUv(0.0F, 0.0F);
         var2.addVertex(var3, 1.0F, 0.0F, -1.0F).setUv(1.0F, 0.0F);
         var2.addVertex(var3, 1.0F, 0.0F, 1.0F).setUv(1.0F, 1.0F);
         var2.addVertex(var3, -1.0F, 0.0F, 1.0F).setUv(0.0F, 1.0F);
         MeshData var4 = var2.buildOrThrow();

         try {
            var5 = RenderSystem.getDevice().createBuffer(() -> {
               return "End flash quad";
            }, 32, var4.vertexBuffer());
         } catch (Throwable var9) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (Throwable var10) {
         if (var1 != null) {
            try {
               var1.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (var1 != null) {
         var1.close();
      }

      return var5;
   }

   public void renderSkyDisc(float var1, float var2, float var3) {
      GpuBufferSlice var4 = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(var1, var2, var3, 1.0F), new Vector3f(), new Matrix4f(), 0.0F);
      GpuTextureView var5 = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
      GpuTextureView var6 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
      RenderPass var7 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
         return "Sky disc";
      }, var5, OptionalInt.empty(), var6, OptionalDouble.empty());

      try {
         var7.setPipeline(RenderPipelines.SKY);
         RenderSystem.bindDefaultUniforms(var7);
         var7.setUniform("DynamicTransforms", var4);
         var7.setVertexBuffer(0, this.topSkyBuffer);
         var7.draw(0, 10);
      } catch (Throwable var11) {
         if (var7 != null) {
            try {
               var7.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }
         }

         throw var11;
      }

      if (var7 != null) {
         var7.close();
      }

   }

   public void extractRenderState(ClientLevel var1, float var2, Vec3 var3, SkyRenderState var4) {
      DimensionSpecialEffects var5 = var1.effects();
      var4.skyType = var5.skyType();
      if (var4.skyType != DimensionSpecialEffects.SkyType.NONE) {
         if (var4.skyType == DimensionSpecialEffects.SkyType.END) {
            EndFlashState var6 = var1.endFlashState();
            if (var6 != null) {
               var4.endFlashIntensity = var6.getIntensity(var2);
               var4.endFlashXAngle = var6.getXAngle();
               var4.endFlashYAngle = var6.getYAngle();
            }
         } else {
            var4.sunAngle = var1.getSunAngle(var2);
            var4.timeOfDay = var1.getTimeOfDay(var2);
            var4.rainBrightness = 1.0F - var1.getRainLevel(var2);
            var4.starBrightness = var1.getStarBrightness(var2) * var4.rainBrightness;
            var4.sunriseAndSunsetColor = var5.getSunriseOrSunsetColor(var4.timeOfDay);
            var4.moonPhase = var1.getMoonPhase();
            var4.skyColor = var1.getSkyColor(var3, var2);
            var4.shouldRenderDarkDisc = this.shouldRenderDarkDisc(var2, var1);
            var4.isSunriseOrSunset = var5.isSunriseOrSunset(var4.timeOfDay);
         }
      }
   }

   private boolean shouldRenderDarkDisc(float var1, ClientLevel var2) {
      return Minecraft.getInstance().player.getEyePosition(var1).y - var2.getLevelData().getHorizonHeight(var2) < 0.0D;
   }

   public void renderDarkDisc() {
      Matrix4fStack var1 = RenderSystem.getModelViewStack();
      var1.pushMatrix();
      var1.translate(0.0F, 12.0F, 0.0F);
      GpuBufferSlice var2 = RenderSystem.getDynamicUniforms().writeTransform(var1, new Vector4f(0.0F, 0.0F, 0.0F, 1.0F), new Vector3f(), new Matrix4f(), 0.0F);
      GpuTextureView var3 = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
      GpuTextureView var4 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
      RenderPass var5 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
         return "Sky dark";
      }, var3, OptionalInt.empty(), var4, OptionalDouble.empty());

      try {
         var5.setPipeline(RenderPipelines.SKY);
         RenderSystem.bindDefaultUniforms(var5);
         var5.setUniform("DynamicTransforms", var2);
         var5.setVertexBuffer(0, this.bottomSkyBuffer);
         var5.draw(0, 10);
      } catch (Throwable var9) {
         if (var5 != null) {
            try {
               var5.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (var5 != null) {
         var5.close();
      }

      var1.popMatrix();
   }

   public void renderSunMoonAndStars(PoseStack var1, float var2, int var3, float var4, float var5) {
      var1.pushPose();
      var1.mulPose((Quaternionfc)Axis.YP.rotationDegrees(-90.0F));
      var1.mulPose((Quaternionfc)Axis.XP.rotationDegrees(var2 * 360.0F));
      this.renderSun(var4, var1);
      this.renderMoon(var3, var4, var1);
      if (var5 > 0.0F) {
         this.renderStars(var5, var1);
      }

      var1.popPose();
   }

   private void renderSun(float var1, PoseStack var2) {
      if (this.sunTexture != null) {
         Matrix4fStack var3 = RenderSystem.getModelViewStack();
         var3.pushMatrix();
         var3.mul(var2.last().pose());
         var3.translate(0.0F, 100.0F, 0.0F);
         var3.scale(30.0F, 1.0F, 30.0F);
         GpuBufferSlice var4 = RenderSystem.getDynamicUniforms().writeTransform(var3, new Vector4f(1.0F, 1.0F, 1.0F, var1), new Vector3f(), new Matrix4f(), 0.0F);
         GpuTextureView var5 = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
         GpuTextureView var6 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
         GpuBuffer var7 = this.quadIndices.getBuffer(6);
         RenderPass var8 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
            return "Sky sun";
         }, var5, OptionalInt.empty(), var6, OptionalDouble.empty());

         try {
            var8.setPipeline(RenderPipelines.CELESTIAL);
            RenderSystem.bindDefaultUniforms(var8);
            var8.setUniform("DynamicTransforms", var4);
            var8.bindSampler("Sampler0", this.sunTexture.getTextureView());
            var8.setVertexBuffer(0, this.sunBuffer);
            var8.setIndexBuffer(var7, this.quadIndices.type());
            var8.drawIndexed(0, 0, 6, 1);
         } catch (Throwable var12) {
            if (var8 != null) {
               try {
                  var8.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (var8 != null) {
            var8.close();
         }

         var3.popMatrix();
      }
   }

   private void renderMoon(int var1, float var2, PoseStack var3) {
      if (this.moonTexture != null) {
         int var4 = var1 & 7;
         int var5 = var4 * 4;
         Matrix4fStack var6 = RenderSystem.getModelViewStack();
         var6.pushMatrix();
         var6.mul(var3.last().pose());
         var6.translate(0.0F, -100.0F, 0.0F);
         var6.scale(20.0F, 1.0F, 20.0F);
         GpuBufferSlice var7 = RenderSystem.getDynamicUniforms().writeTransform(var6, new Vector4f(1.0F, 1.0F, 1.0F, var2), new Vector3f(), new Matrix4f(), 0.0F);
         GpuTextureView var8 = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
         GpuTextureView var9 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
         GpuBuffer var10 = this.quadIndices.getBuffer(6);
         RenderPass var11 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
            return "Sky moon";
         }, var8, OptionalInt.empty(), var9, OptionalDouble.empty());

         try {
            var11.setPipeline(RenderPipelines.CELESTIAL);
            RenderSystem.bindDefaultUniforms(var11);
            var11.setUniform("DynamicTransforms", var7);
            var11.bindSampler("Sampler0", this.moonTexture.getTextureView());
            var11.setVertexBuffer(0, this.moonBuffer);
            var11.setIndexBuffer(var10, this.quadIndices.type());
            var11.drawIndexed(var5, 0, 6, 1);
         } catch (Throwable var15) {
            if (var11 != null) {
               try {
                  var11.close();
               } catch (Throwable var14) {
                  var15.addSuppressed(var14);
               }
            }

            throw var15;
         }

         if (var11 != null) {
            var11.close();
         }

         var6.popMatrix();
      }
   }

   private void renderStars(float var1, PoseStack var2) {
      Matrix4fStack var3 = RenderSystem.getModelViewStack();
      var3.pushMatrix();
      var3.mul(var2.last().pose());
      RenderPipeline var4 = RenderPipelines.STARS;
      GpuTextureView var5 = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
      GpuTextureView var6 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
      GpuBuffer var7 = this.starIndices.getBuffer(this.starIndexCount);
      GpuBufferSlice var8 = RenderSystem.getDynamicUniforms().writeTransform(var3, new Vector4f(var1, var1, var1, var1), new Vector3f(), new Matrix4f(), 0.0F);
      RenderPass var9 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
         return "Stars";
      }, var5, OptionalInt.empty(), var6, OptionalDouble.empty());

      try {
         var9.setPipeline(var4);
         RenderSystem.bindDefaultUniforms(var9);
         var9.setUniform("DynamicTransforms", var8);
         var9.setVertexBuffer(0, this.starBuffer);
         var9.setIndexBuffer(var7, this.starIndices.type());
         var9.drawIndexed(0, 0, this.starIndexCount, 1);
      } catch (Throwable var13) {
         if (var9 != null) {
            try {
               var9.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }
         }

         throw var13;
      }

      if (var9 != null) {
         var9.close();
      }

      var3.popMatrix();
   }

   public void renderSunriseAndSunset(PoseStack var1, float var2, int var3) {
      float var4 = ARGB.alphaFloat(var3);
      if (!(var4 <= 0.001F)) {
         float var5 = ARGB.redFloat(var3);
         float var6 = ARGB.greenFloat(var3);
         float var7 = ARGB.blueFloat(var3);
         var1.pushPose();
         var1.mulPose((Quaternionfc)Axis.XP.rotationDegrees(90.0F));
         float var8 = Mth.sin(var2) < 0.0F ? 180.0F : 0.0F;
         var1.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(var8 + 90.0F));
         Matrix4fStack var9 = RenderSystem.getModelViewStack();
         var9.pushMatrix();
         var9.mul(var1.last().pose());
         var9.scale(1.0F, 1.0F, var4);
         GpuBufferSlice var10 = RenderSystem.getDynamicUniforms().writeTransform(var9, new Vector4f(var5, var6, var7, var4), new Vector3f(), new Matrix4f(), 0.0F);
         GpuTextureView var11 = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
         GpuTextureView var12 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
         RenderPass var13 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
            return "Sunrise sunset";
         }, var11, OptionalInt.empty(), var12, OptionalDouble.empty());

         try {
            var13.setPipeline(RenderPipelines.SUNRISE_SUNSET);
            RenderSystem.bindDefaultUniforms(var13);
            var13.setUniform("DynamicTransforms", var10);
            var13.setVertexBuffer(0, this.sunriseBuffer);
            var13.draw(0, 18);
         } catch (Throwable var17) {
            if (var13 != null) {
               try {
                  var13.close();
               } catch (Throwable var16) {
                  var17.addSuppressed(var16);
               }
            }

            throw var17;
         }

         if (var13 != null) {
            var13.close();
         }

         var9.popMatrix();
         var1.popPose();
      }
   }

   public void renderEndSky() {
      if (this.endSkyTexture != null) {
         RenderSystem.AutoStorageIndexBuffer var1 = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
         GpuBuffer var2 = var1.getBuffer(36);
         GpuTextureView var3 = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
         GpuTextureView var4 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
         GpuBufferSlice var5 = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f(), 0.0F);
         RenderPass var6 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
            return "End sky";
         }, var3, OptionalInt.empty(), var4, OptionalDouble.empty());

         try {
            var6.setPipeline(RenderPipelines.END_SKY);
            RenderSystem.bindDefaultUniforms(var6);
            var6.setUniform("DynamicTransforms", var5);
            var6.bindSampler("Sampler0", this.endSkyTexture.getTextureView());
            var6.setVertexBuffer(0, this.endSkyBuffer);
            var6.setIndexBuffer(var2, var1.type());
            var6.drawIndexed(0, 0, 36, 1);
         } catch (Throwable var10) {
            if (var6 != null) {
               try {
                  var6.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (var6 != null) {
            var6.close();
         }

      }
   }

   public void renderEndFlash(PoseStack var1, float var2, float var3, float var4) {
      if (this.endFlashTexture != null) {
         var1.mulPose((Quaternionfc)Axis.YP.rotationDegrees(180.0F - var4));
         var1.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-90.0F - var3));
         Matrix4fStack var5 = RenderSystem.getModelViewStack();
         var5.pushMatrix();
         var5.mul(var1.last().pose());
         var5.translate(0.0F, 100.0F, 0.0F);
         var5.scale(60.0F, 1.0F, 60.0F);
         GpuBufferSlice var6 = RenderSystem.getDynamicUniforms().writeTransform(var5, new Vector4f(var2, var2, var2, var2), new Vector3f(), new Matrix4f(), 0.0F);
         GpuTextureView var7 = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
         GpuTextureView var8 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
         GpuBuffer var9 = this.quadIndices.getBuffer(6);
         RenderPass var10 = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> {
            return "End flash";
         }, var7, OptionalInt.empty(), var8, OptionalDouble.empty());

         try {
            var10.setPipeline(RenderPipelines.CELESTIAL);
            RenderSystem.bindDefaultUniforms(var10);
            var10.setUniform("DynamicTransforms", var6);
            var10.bindSampler("Sampler0", this.endFlashTexture.getTextureView());
            var10.setVertexBuffer(0, this.endFlashBuffer);
            var10.setIndexBuffer(var9, this.quadIndices.type());
            var10.drawIndexed(0, 0, 6, 1);
         } catch (Throwable var14) {
            if (var10 != null) {
               try {
                  var10.close();
               } catch (Throwable var13) {
                  var14.addSuppressed(var13);
               }
            }

            throw var14;
         }

         if (var10 != null) {
            var10.close();
         }

         var5.popMatrix();
      }
   }

   public void close() {
      this.sunBuffer.close();
      this.moonBuffer.close();
      this.starBuffer.close();
      this.topSkyBuffer.close();
      this.bottomSkyBuffer.close();
      this.endSkyBuffer.close();
      this.sunriseBuffer.close();
      this.endFlashBuffer.close();
   }
}
