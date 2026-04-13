package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;

public class CustomFeatureRenderer {
   public CustomFeatureRenderer() {
      super();
   }

   public void render(SubmitNodeCollection var1, MultiBufferSource.BufferSource var2) {
      CustomFeatureRenderer.Storage var3 = var1.getCustomGeometrySubmits();
      Iterator var4 = var3.customGeometrySubmits.entrySet().iterator();

      while(var4.hasNext()) {
         Entry var5 = (Entry)var4.next();
         VertexConsumer var6 = var2.getBuffer((RenderType)var5.getKey());
         Iterator var7 = ((List)var5.getValue()).iterator();

         while(var7.hasNext()) {
            SubmitNodeStorage.CustomGeometrySubmit var8 = (SubmitNodeStorage.CustomGeometrySubmit)var7.next();
            var8.customGeometryRenderer().render(var8.pose(), var6);
         }
      }

   }

   public static class Storage {
      final Map<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>> customGeometrySubmits = new HashMap();
      private final Set<RenderType> customGeometrySubmitsUsage = new ObjectOpenHashSet();

      public Storage() {
         super();
      }

      public void add(PoseStack var1, RenderType var2, SubmitNodeCollector.CustomGeometryRenderer var3) {
         List var4 = (List)this.customGeometrySubmits.computeIfAbsent(var2, (var0) -> {
            return new ArrayList();
         });
         var4.add(new SubmitNodeStorage.CustomGeometrySubmit(var1.last().copy(), var3));
      }

      public void clear() {
         Iterator var1 = this.customGeometrySubmits.entrySet().iterator();

         while(var1.hasNext()) {
            Entry var2 = (Entry)var1.next();
            if (!((List)var2.getValue()).isEmpty()) {
               this.customGeometrySubmitsUsage.add((RenderType)var2.getKey());
               ((List)var2.getValue()).clear();
            }
         }

      }

      public void endFrame() {
         this.customGeometrySubmits.keySet().removeIf((var1) -> {
            return !this.customGeometrySubmitsUsage.contains(var1);
         });
         this.customGeometrySubmitsUsage.clear();
      }
   }
}
