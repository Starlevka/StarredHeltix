package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.resources.model.ModelBakery;
import org.joml.Vector3f;

public class ModelFeatureRenderer {
   private final PoseStack poseStack = new PoseStack();

   public ModelFeatureRenderer() {
      super();
   }

   public void render(SubmitNodeCollection var1, MultiBufferSource.BufferSource var2, OutlineBufferSource var3, MultiBufferSource.BufferSource var4) {
      ModelFeatureRenderer.Storage var5 = var1.getModelSubmits();
      this.renderBatch(var2, var3, var5.opaqueModelSubmits, var4);
      var5.translucentModelSubmits.sort(Comparator.comparingDouble((var0) -> {
         return (double)(-var0.position().lengthSquared());
      }));
      this.renderTranslucents(var2, var3, var5.translucentModelSubmits, var4);
   }

   private void renderTranslucents(MultiBufferSource.BufferSource var1, OutlineBufferSource var2, List<SubmitNodeStorage.TranslucentModelSubmit<?>> var3, MultiBufferSource.BufferSource var4) {
      Iterator var5 = var3.iterator();

      while(var5.hasNext()) {
         SubmitNodeStorage.TranslucentModelSubmit var6 = (SubmitNodeStorage.TranslucentModelSubmit)var5.next();
         this.renderModel(var6.modelSubmit(), var6.renderType(), var1.getBuffer(var6.renderType()), var2, var4);
      }

   }

   private void renderBatch(MultiBufferSource.BufferSource var1, OutlineBufferSource var2, Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> var3, MultiBufferSource.BufferSource var4) {
      Object var5;
      if (SharedConstants.DEBUG_SHUFFLE_MODELS) {
         ArrayList var6 = new ArrayList(var3.entrySet());
         Collections.shuffle(var6);
         var5 = var6;
      } else {
         var5 = var3.entrySet();
      }

      Iterator var11 = ((Iterable)var5).iterator();

      while(var11.hasNext()) {
         Entry var7 = (Entry)var11.next();
         VertexConsumer var8 = var1.getBuffer((RenderType)var7.getKey());
         Iterator var9 = ((List)var7.getValue()).iterator();

         while(var9.hasNext()) {
            SubmitNodeStorage.ModelSubmit var10 = (SubmitNodeStorage.ModelSubmit)var9.next();
            this.renderModel(var10, (RenderType)var7.getKey(), var8, var2, var4);
         }
      }

   }

   private <S> void renderModel(SubmitNodeStorage.ModelSubmit<S> var1, RenderType var2, VertexConsumer var3, OutlineBufferSource var4, MultiBufferSource.BufferSource var5) {
      this.poseStack.pushPose();
      this.poseStack.last().set(var1.pose());
      Model var6 = var1.model();
      VertexConsumer var7 = var1.sprite() == null ? var3 : var1.sprite().wrap(var3);
      var6.setupAnim(var1.state());
      var6.renderToBuffer(this.poseStack, var7, var1.lightCoords(), var1.overlayCoords(), var1.tintedColor());
      if (var1.outlineColor() != 0 && (var2.outline().isPresent() || var2.isOutline())) {
         var4.setColor(var1.outlineColor());
         VertexConsumer var8 = var4.getBuffer(var2);
         var6.renderToBuffer(this.poseStack, var1.sprite() == null ? var8 : var1.sprite().wrap(var8), var1.lightCoords(), var1.overlayCoords(), var1.tintedColor());
      }

      if (var1.crumblingOverlay() != null && var2.affectsCrumbling()) {
         SheetedDecalTextureGenerator var9 = new SheetedDecalTextureGenerator(var5.getBuffer((RenderType)ModelBakery.DESTROY_TYPES.get(var1.crumblingOverlay().progress())), var1.crumblingOverlay().cameraPose(), 1.0F);
         var6.renderToBuffer(this.poseStack, (VertexConsumer)(var1.sprite() == null ? var9 : var1.sprite().wrap(var9)), var1.lightCoords(), var1.overlayCoords(), var1.tintedColor());
      }

      this.poseStack.popPose();
   }

   public static class Storage {
      final Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> opaqueModelSubmits = new HashMap();
      final List<SubmitNodeStorage.TranslucentModelSubmit<?>> translucentModelSubmits = new ArrayList();
      private final Set<RenderType> usedModelSubmitBuckets = new ObjectOpenHashSet();

      public Storage() {
         super();
      }

      public void add(RenderType var1, SubmitNodeStorage.ModelSubmit<?> var2) {
         if (var1.pipeline().getBlendFunction().isEmpty()) {
            ((List)this.opaqueModelSubmits.computeIfAbsent(var1, (var0) -> {
               return new ArrayList();
            })).add(var2);
         } else {
            Vector3f var3 = var2.pose().pose().transformPosition(new Vector3f());
            this.translucentModelSubmits.add(new SubmitNodeStorage.TranslucentModelSubmit(var2, var1, var3));
         }

      }

      public void clear() {
         this.translucentModelSubmits.clear();
         Iterator var1 = this.opaqueModelSubmits.entrySet().iterator();

         while(var1.hasNext()) {
            Entry var2 = (Entry)var1.next();
            List var3 = (List)var2.getValue();
            if (!var3.isEmpty()) {
               this.usedModelSubmitBuckets.add((RenderType)var2.getKey());
               var3.clear();
            }
         }

      }

      public void endFrame() {
         this.opaqueModelSubmits.keySet().removeIf((var1) -> {
            return !this.usedModelSubmitBuckets.contains(var1);
         });
         this.usedModelSubmitBuckets.clear();
      }
   }

   public static record CrumblingOverlay(int progress, PoseStack.Pose cameraPose) {
      public CrumblingOverlay(int param1, PoseStack.Pose param2) {
         super();
         this.progress = var1;
         this.cameraPose = var2;
      }

      public int progress() {
         return this.progress;
      }

      public PoseStack.Pose cameraPose() {
         return this.cameraPose;
      }
   }
}
