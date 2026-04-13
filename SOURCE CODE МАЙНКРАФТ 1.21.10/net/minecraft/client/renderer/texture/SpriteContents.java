package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.ARGB;
import org.slf4j.Logger;

public class SpriteContents implements Stitcher.Entry, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   final ResourceLocation name;
   final int width;
   final int height;
   private final NativeImage originalImage;
   NativeImage[] byMipLevel;
   @Nullable
   private final SpriteContents.AnimatedTexture animatedTexture;
   private final List<MetadataSectionType.WithValue<?>> additionalMetadata;

   public SpriteContents(ResourceLocation var1, FrameSize var2, NativeImage var3) {
      this(var1, var2, var3, Optional.empty(), List.of());
   }

   public SpriteContents(ResourceLocation var1, FrameSize var2, NativeImage var3, Optional<AnimationMetadataSection> var4, List<MetadataSectionType.WithValue<?>> var5) {
      super();
      this.name = var1;
      this.width = var2.width();
      this.height = var2.height();
      this.additionalMetadata = var5;
      this.animatedTexture = (SpriteContents.AnimatedTexture)var4.map((var3x) -> {
         return this.createAnimatedTexture(var2, var3.getWidth(), var3.getHeight(), var3x);
      }).orElse((Object)null);
      this.originalImage = var3;
      this.byMipLevel = new NativeImage[]{this.originalImage};
   }

   public void increaseMipLevel(int var1) {
      try {
         this.byMipLevel = MipmapGenerator.generateMipLevels(this.byMipLevel, var1);
      } catch (Throwable var5) {
         CrashReport var3 = CrashReport.forThrowable(var5, "Generating mipmaps for frame");
         CrashReportCategory var4 = var3.addCategory("Frame being iterated");
         var4.setDetail("Sprite name", (Object)this.name);
         var4.setDetail("Sprite size", () -> {
            return this.width + " x " + this.height;
         });
         var4.setDetail("Sprite frames", () -> {
            return this.getFrameCount() + " frames";
         });
         var4.setDetail("Mipmap levels", (Object)var1);
         var4.setDetail("Original image size", () -> {
            int var10000 = this.originalImage.getWidth();
            return var10000 + "x" + this.originalImage.getHeight();
         });
         throw new ReportedException(var3);
      }
   }

   private int getFrameCount() {
      return this.animatedTexture != null ? this.animatedTexture.frames.size() : 1;
   }

   public boolean isAnimated() {
      return this.getFrameCount() > 1;
   }

   @Nullable
   private SpriteContents.AnimatedTexture createAnimatedTexture(FrameSize var1, int var2, int var3, AnimationMetadataSection var4) {
      int var5 = var2 / var1.width();
      int var6 = var3 / var1.height();
      int var7 = var5 * var6;
      int var8 = var4.defaultFrameTime();
      ArrayList var9;
      if (var4.frames().isEmpty()) {
         var9 = new ArrayList(var7);

         for(int var10 = 0; var10 < var7; ++var10) {
            var9.add(new SpriteContents.FrameInfo(var10, var8));
         }
      } else {
         List var16 = (List)var4.frames().get();
         var9 = new ArrayList(var16.size());
         Iterator var11 = var16.iterator();

         while(var11.hasNext()) {
            AnimationFrame var12 = (AnimationFrame)var11.next();
            var9.add(new SpriteContents.FrameInfo(var12.index(), var12.timeOr(var8)));
         }

         int var17 = 0;
         IntOpenHashSet var18 = new IntOpenHashSet();

         for(Iterator var13 = var9.iterator(); var13.hasNext(); ++var17) {
            SpriteContents.FrameInfo var14 = (SpriteContents.FrameInfo)var13.next();
            boolean var15 = true;
            if (var14.time <= 0) {
               LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", new Object[]{this.name, var17, var14.time});
               var15 = false;
            }

            if (var14.index < 0 || var14.index >= var7) {
               LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", new Object[]{this.name, var17, var14.index});
               var15 = false;
            }

            if (var15) {
               var18.add(var14.index);
            } else {
               var13.remove();
            }
         }

         int[] var19 = IntStream.range(0, var7).filter((var1x) -> {
            return !var18.contains(var1x);
         }).toArray();
         if (var19.length > 0) {
            LOGGER.warn("Unused frames in sprite {}: {}", this.name, Arrays.toString(var19));
         }
      }

      return var9.size() <= 1 ? null : new SpriteContents.AnimatedTexture(List.copyOf(var9), var5, var4.interpolatedFrames());
   }

   void upload(int var1, int var2, int var3, int var4, NativeImage[] var5, GpuTexture var6) {
      for(int var7 = 0; var7 < this.byMipLevel.length; ++var7) {
         RenderSystem.getDevice().createCommandEncoder().writeToTexture(var6, var5[var7], var7, 0, var1 >> var7, var2 >> var7, this.width >> var7, this.height >> var7, var3 >> var7, var4 >> var7);
      }

   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   public ResourceLocation name() {
      return this.name;
   }

   public IntStream getUniqueFrames() {
      return this.animatedTexture != null ? this.animatedTexture.getUniqueFrames() : IntStream.of(1);
   }

   @Nullable
   public SpriteTicker createTicker() {
      return this.animatedTexture != null ? this.animatedTexture.createTicker() : null;
   }

   public <T> Optional<T> getAdditionalMetadata(MetadataSectionType<T> var1) {
      Iterator var2 = this.additionalMetadata.iterator();

      Optional var4;
      do {
         if (!var2.hasNext()) {
            return Optional.empty();
         }

         MetadataSectionType.WithValue var3 = (MetadataSectionType.WithValue)var2.next();
         var4 = var3.unwrapToType(var1);
      } while(!var4.isPresent());

      return var4;
   }

   public void close() {
      NativeImage[] var1 = this.byMipLevel;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         NativeImage var4 = var1[var3];
         var4.close();
      }

   }

   public String toString() {
      String var10000 = String.valueOf(this.name);
      return "SpriteContents{name=" + var10000 + ", frameCount=" + this.getFrameCount() + ", height=" + this.height + ", width=" + this.width + "}";
   }

   public boolean isTransparent(int var1, int var2, int var3) {
      int var4 = var2;
      int var5 = var3;
      if (this.animatedTexture != null) {
         var4 = var2 + this.animatedTexture.getFrameX(var1) * this.width;
         var5 = var3 + this.animatedTexture.getFrameY(var1) * this.height;
      }

      return ARGB.alpha(this.originalImage.getPixel(var4, var5)) == 0;
   }

   public void uploadFirstFrame(int var1, int var2, GpuTexture var3) {
      if (this.animatedTexture != null) {
         this.animatedTexture.uploadFirstFrame(var1, var2, var3);
      } else {
         this.upload(var1, var2, 0, 0, this.byMipLevel, var3);
      }

   }

   private class AnimatedTexture {
      final List<SpriteContents.FrameInfo> frames;
      private final int frameRowSize;
      private final boolean interpolateFrames;

      AnimatedTexture(final List<SpriteContents.FrameInfo> param2, final int param3, final boolean param4) {
         super();
         this.frames = var2;
         this.frameRowSize = var3;
         this.interpolateFrames = var4;
      }

      int getFrameX(int var1) {
         return var1 % this.frameRowSize;
      }

      int getFrameY(int var1) {
         return var1 / this.frameRowSize;
      }

      void uploadFrame(int var1, int var2, int var3, GpuTexture var4) {
         int var5 = this.getFrameX(var3) * SpriteContents.this.width;
         int var6 = this.getFrameY(var3) * SpriteContents.this.height;
         SpriteContents.this.upload(var1, var2, var5, var6, SpriteContents.this.byMipLevel, var4);
      }

      public SpriteTicker createTicker() {
         return SpriteContents.this.new Ticker(SpriteContents.this, this, this.interpolateFrames ? SpriteContents.this.new InterpolationData() : null);
      }

      public void uploadFirstFrame(int var1, int var2, GpuTexture var3) {
         this.uploadFrame(var1, var2, ((SpriteContents.FrameInfo)this.frames.get(0)).index, var3);
      }

      public IntStream getUniqueFrames() {
         return this.frames.stream().mapToInt((var0) -> {
            return var0.index;
         }).distinct();
      }
   }

   private static record FrameInfo(int index, int time) {
      final int index;
      final int time;

      FrameInfo(int param1, int param2) {
         super();
         this.index = var1;
         this.time = var2;
      }

      public int index() {
         return this.index;
      }

      public int time() {
         return this.time;
      }
   }

   private class Ticker implements SpriteTicker {
      int frame;
      int subFrame;
      final SpriteContents.AnimatedTexture animationInfo;
      @Nullable
      private final SpriteContents.InterpolationData interpolationData;

      Ticker(final SpriteContents param1, @Nullable final SpriteContents.AnimatedTexture param2, final SpriteContents.InterpolationData param3) {
         super();
         this.animationInfo = var2;
         this.interpolationData = var3;
      }

      public void tickAndUpload(int var1, int var2, GpuTexture var3) {
         ++this.subFrame;
         SpriteContents.FrameInfo var4 = (SpriteContents.FrameInfo)this.animationInfo.frames.get(this.frame);
         if (this.subFrame >= var4.time) {
            int var5 = var4.index;
            this.frame = (this.frame + 1) % this.animationInfo.frames.size();
            this.subFrame = 0;
            int var6 = ((SpriteContents.FrameInfo)this.animationInfo.frames.get(this.frame)).index;
            if (var5 != var6) {
               this.animationInfo.uploadFrame(var1, var2, var6, var3);
            }
         } else if (this.interpolationData != null) {
            this.interpolationData.uploadInterpolatedFrame(var1, var2, this, var3);
         }

      }

      public void close() {
         if (this.interpolationData != null) {
            this.interpolationData.close();
         }

      }
   }

   private final class InterpolationData implements AutoCloseable {
      private final NativeImage[] activeFrame;

      InterpolationData() {
         super();
         this.activeFrame = new NativeImage[SpriteContents.this.byMipLevel.length];

         for(int var2 = 0; var2 < this.activeFrame.length; ++var2) {
            int var3 = SpriteContents.this.width >> var2;
            int var4 = SpriteContents.this.height >> var2;
            this.activeFrame[var2] = new NativeImage(var3, var4, false);
         }

      }

      void uploadInterpolatedFrame(int var1, int var2, SpriteContents.Ticker var3, GpuTexture var4) {
         SpriteContents.AnimatedTexture var5 = var3.animationInfo;
         List var6 = var5.frames;
         SpriteContents.FrameInfo var7 = (SpriteContents.FrameInfo)var6.get(var3.frame);
         float var8 = (float)var3.subFrame / (float)var7.time;
         int var9 = var7.index;
         int var10 = ((SpriteContents.FrameInfo)var6.get((var3.frame + 1) % var6.size())).index;
         if (var9 != var10) {
            int var13;
            for(int var11 = 0; var11 < this.activeFrame.length; ++var11) {
               int var12 = SpriteContents.this.width >> var11;
               var13 = SpriteContents.this.height >> var11;

               for(int var14 = 0; var14 < var13; ++var14) {
                  for(int var15 = 0; var15 < var12; ++var15) {
                     int var16 = this.getPixel(var5, var9, var11, var15, var14);
                     int var17 = this.getPixel(var5, var10, var11, var15, var14);
                     this.activeFrame[var11].setPixel(var15, var14, ARGB.lerp(var8, var16, var17));
                  }
               }
            }

            SpriteContents.this.upload(var1, var2, 0, 0, this.activeFrame, var4);
            if (SharedConstants.DEBUG_DUMP_INTERPOLATED_TEXTURE_FRAMES) {
               try {
                  Path var19 = TextureUtil.getDebugTexturePath();
                  Path var20 = var19.resolve(SpriteContents.this.name.toDebugFileName());
                  Files.createDirectories(var20);

                  for(var13 = 0; var13 < this.activeFrame.length; ++var13) {
                     this.activeFrame[var13].writeToFile(var20.resolve(SpriteContents.this.name.toDebugFileName() + "_" + var13 + "_" + var9 + "_" + var10 + ".png"));
                  }
               } catch (IOException var18) {
               }
            }
         }

      }

      private int getPixel(SpriteContents.AnimatedTexture var1, int var2, int var3, int var4, int var5) {
         return SpriteContents.this.byMipLevel[var3].getPixel(var4 + (var1.getFrameX(var2) * SpriteContents.this.width >> var3), var5 + (var1.getFrameY(var2) * SpriteContents.this.height >> var3));
      }

      public void close() {
         NativeImage[] var1 = this.activeFrame;
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            NativeImage var4 = var1[var3];
            var4.close();
         }

      }
   }
}
