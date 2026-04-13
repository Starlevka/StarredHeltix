package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;

public class NameTagFeatureRenderer {
   public NameTagFeatureRenderer() {
      super();
   }

   public void render(SubmitNodeCollection var1, MultiBufferSource.BufferSource var2, Font var3) {
      NameTagFeatureRenderer.Storage var4 = var1.getNameTagSubmits();
      var4.nameTagSubmitsSeethrough.sort(Comparator.comparing(SubmitNodeStorage.NameTagSubmit::distanceToCameraSq).reversed());
      Iterator var5 = var4.nameTagSubmitsSeethrough.iterator();

      SubmitNodeStorage.NameTagSubmit var6;
      while(var5.hasNext()) {
         var6 = (SubmitNodeStorage.NameTagSubmit)var5.next();
         var3.drawInBatch((Component)var6.text(), var6.x(), var6.y(), var6.color(), false, var6.pose(), var2, Font.DisplayMode.SEE_THROUGH, var6.backgroundColor(), var6.lightCoords());
      }

      var5 = var4.nameTagSubmitsNormal.iterator();

      while(var5.hasNext()) {
         var6 = (SubmitNodeStorage.NameTagSubmit)var5.next();
         var3.drawInBatch((Component)var6.text(), var6.x(), var6.y(), var6.color(), false, var6.pose(), var2, Font.DisplayMode.NORMAL, var6.backgroundColor(), var6.lightCoords());
      }

   }

   public static class Storage {
      final List<SubmitNodeStorage.NameTagSubmit> nameTagSubmitsSeethrough = new ArrayList();
      final List<SubmitNodeStorage.NameTagSubmit> nameTagSubmitsNormal = new ArrayList();

      public Storage() {
         super();
      }

      public void add(PoseStack var1, @Nullable Vec3 var2, int var3, Component var4, boolean var5, int var6, double var7, CameraRenderState var9) {
         if (var2 != null) {
            Minecraft var10 = Minecraft.getInstance();
            var1.pushPose();
            var1.translate(var2.x, var2.y + 0.5D, var2.z);
            var1.mulPose((Quaternionfc)var9.orientation);
            var1.scale(0.025F, -0.025F, 0.025F);
            Matrix4f var11 = new Matrix4f(var1.last().pose());
            float var12 = (float)(-var10.font.width((FormattedText)var4)) / 2.0F;
            int var13 = (int)(var10.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
            if (var5) {
               this.nameTagSubmitsNormal.add(new SubmitNodeStorage.NameTagSubmit(var11, var12, (float)var3, var4, LightTexture.lightCoordsWithEmission(var6, 2), -1, 0, var7));
               this.nameTagSubmitsSeethrough.add(new SubmitNodeStorage.NameTagSubmit(var11, var12, (float)var3, var4, var6, -2130706433, var13, var7));
            } else {
               this.nameTagSubmitsNormal.add(new SubmitNodeStorage.NameTagSubmit(var11, var12, (float)var3, var4, var6, -2130706433, var13, var7));
            }

            var1.popPose();
         }
      }

      public void clear() {
         this.nameTagSubmitsNormal.clear();
         this.nameTagSubmitsSeethrough.clear();
      }
   }
}
