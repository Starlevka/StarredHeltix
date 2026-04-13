package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

public class DynamicUniformStorage<T extends DynamicUniformStorage.DynamicUniform> implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final List<MappableRingBuffer> oldBuffers = new ArrayList();
   private final int blockSize;
   private MappableRingBuffer ringBuffer;
   private int nextBlock;
   private int capacity;
   @Nullable
   private T lastUniform;
   private final String label;

   public DynamicUniformStorage(String var1, int var2, int var3) {
      super();
      GpuDevice var4 = RenderSystem.getDevice();
      this.blockSize = Mth.roundToward(var2, var4.getUniformOffsetAlignment());
      this.capacity = Mth.smallestEncompassingPowerOfTwo(var3);
      this.nextBlock = 0;
      this.ringBuffer = new MappableRingBuffer(() -> {
         return var1 + " x" + this.blockSize;
      }, 130, this.blockSize * this.capacity);
      this.label = var1;
   }

   public void endFrame() {
      this.nextBlock = 0;
      this.lastUniform = null;
      this.ringBuffer.rotate();
      if (!this.oldBuffers.isEmpty()) {
         Iterator var1 = this.oldBuffers.iterator();

         while(var1.hasNext()) {
            MappableRingBuffer var2 = (MappableRingBuffer)var1.next();
            var2.close();
         }

         this.oldBuffers.clear();
      }

   }

   private void resizeBuffers(int var1) {
      this.capacity = var1;
      this.nextBlock = 0;
      this.lastUniform = null;
      this.oldBuffers.add(this.ringBuffer);
      this.ringBuffer = new MappableRingBuffer(() -> {
         return this.label + " x" + this.blockSize;
      }, 130, this.blockSize * this.capacity);
   }

   public GpuBufferSlice writeUniform(T var1) {
      if (this.lastUniform != null && this.lastUniform.equals(var1)) {
         return this.ringBuffer.currentBuffer().slice((this.nextBlock - 1) * this.blockSize, this.blockSize);
      } else {
         int var2;
         if (this.nextBlock >= this.capacity) {
            var2 = this.capacity * 2;
            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", new Object[]{this.label, this.capacity, var2});
            this.resizeBuffers(var2);
         }

         var2 = this.nextBlock * this.blockSize;
         GpuBuffer.MappedView var3 = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.ringBuffer.currentBuffer().slice(var2, this.blockSize), false, true);

         try {
            var1.write(var3.data());
         } catch (Throwable var7) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (var3 != null) {
            var3.close();
         }

         ++this.nextBlock;
         this.lastUniform = var1;
         return this.ringBuffer.currentBuffer().slice(var2, this.blockSize);
      }
   }

   public GpuBufferSlice[] writeUniforms(T[] var1) {
      if (var1.length == 0) {
         return new GpuBufferSlice[0];
      } else {
         int var2;
         if (this.nextBlock + var1.length > this.capacity) {
            var2 = Mth.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, var1.length));
            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", new Object[]{this.label, this.capacity, var2});
            this.resizeBuffers(var2);
         }

         var2 = this.nextBlock * this.blockSize;
         GpuBufferSlice[] var3 = new GpuBufferSlice[var1.length];
         GpuBuffer.MappedView var4 = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.ringBuffer.currentBuffer().slice(var2, var1.length * this.blockSize), false, true);

         try {
            ByteBuffer var5 = var4.data();

            for(int var6 = 0; var6 < var1.length; ++var6) {
               DynamicUniformStorage.DynamicUniform var7 = var1[var6];
               var3[var6] = this.ringBuffer.currentBuffer().slice(var2 + var6 * this.blockSize, this.blockSize);
               var5.position(var6 * this.blockSize);
               var7.write(var5);
            }
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

         this.nextBlock += var1.length;
         this.lastUniform = var1[var1.length - 1];
         return var3;
      }
   }

   public void close() {
      Iterator var1 = this.oldBuffers.iterator();

      while(var1.hasNext()) {
         MappableRingBuffer var2 = (MappableRingBuffer)var1.next();
         var2.close();
      }

      this.ringBuffer.close();
   }

   public interface DynamicUniform {
      void write(ByteBuffer var1);
   }
}
