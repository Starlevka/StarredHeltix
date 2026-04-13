package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimPattern;

public class EquipmentLayerRenderer {
   private static final int NO_LAYER_COLOR = 0;
   private final EquipmentAssetManager equipmentAssets;
   private final Function<EquipmentLayerRenderer.LayerTextureKey, ResourceLocation> layerTextureLookup;
   private final Function<EquipmentLayerRenderer.TrimSpriteKey, TextureAtlasSprite> trimSpriteLookup;

   public EquipmentLayerRenderer(EquipmentAssetManager var1, TextureAtlas var2) {
      super();
      this.equipmentAssets = var1;
      this.layerTextureLookup = Util.memoize((var0) -> {
         return var0.layer.getTextureLocation(var0.layerType);
      });
      this.trimSpriteLookup = Util.memoize((var1x) -> {
         return var2.getSprite(var1x.spriteId());
      });
   }

   public <S> void renderLayers(EquipmentClientInfo.LayerType var1, ResourceKey<EquipmentAsset> var2, Model<? super S> var3, S var4, ItemStack var5, PoseStack var6, SubmitNodeCollector var7, int var8, int var9) {
      this.renderLayers(var1, var2, var3, var4, var5, var6, var7, var8, (ResourceLocation)null, var9, 1);
   }

   public <S> void renderLayers(EquipmentClientInfo.LayerType var1, ResourceKey<EquipmentAsset> var2, Model<? super S> var3, S var4, ItemStack var5, PoseStack var6, SubmitNodeCollector var7, int var8, @Nullable ResourceLocation var9, int var10, int var11) {
      List var12 = this.equipmentAssets.get(var2).getLayers(var1);
      if (!var12.isEmpty()) {
         int var13 = DyedItemColor.getOrDefault(var5, 0);
         boolean var14 = var5.hasFoil();
         int var15 = var11;
         Iterator var16 = var12.iterator();

         while(true) {
            EquipmentClientInfo.Layer var17;
            int var18;
            do {
               if (!var16.hasNext()) {
                  ArmorTrim var20 = (ArmorTrim)var5.get(DataComponents.TRIM);
                  if (var20 != null) {
                     TextureAtlasSprite var21 = (TextureAtlasSprite)this.trimSpriteLookup.apply(new EquipmentLayerRenderer.TrimSpriteKey(var20, var1, var2));
                     RenderType var22 = Sheets.armorTrimsSheet(((TrimPattern)var20.pattern().value()).decal());
                     var7.order(var15++).submitModel(var3, var4, var6, var22, var8, OverlayTexture.NO_OVERLAY, -1, var21, var10, (ModelFeatureRenderer.CrumblingOverlay)null);
                  }

                  return;
               }

               var17 = (EquipmentClientInfo.Layer)var16.next();
               var18 = getColorForLayer(var17, var13);
            } while(var18 == 0);

            ResourceLocation var19 = var17.usePlayerTexture() && var9 != null ? var9 : (ResourceLocation)this.layerTextureLookup.apply(new EquipmentLayerRenderer.LayerTextureKey(var1, var17));
            var7.order(var15++).submitModel(var3, var4, var6, RenderType.armorCutoutNoCull(var19), var8, OverlayTexture.NO_OVERLAY, var18, (TextureAtlasSprite)null, var10, (ModelFeatureRenderer.CrumblingOverlay)null);
            if (var14) {
               var7.order(var15++).submitModel(var3, var4, var6, RenderType.armorEntityGlint(), var8, OverlayTexture.NO_OVERLAY, var18, (TextureAtlasSprite)null, var10, (ModelFeatureRenderer.CrumblingOverlay)null);
            }

            var14 = false;
         }
      }
   }

   private static int getColorForLayer(EquipmentClientInfo.Layer var0, int var1) {
      Optional var2 = var0.dyeable();
      if (var2.isPresent()) {
         int var3 = (Integer)((EquipmentClientInfo.Dyeable)var2.get()).colorWhenUndyed().map(ARGB::opaque).orElse(0);
         return var1 != 0 ? var1 : var3;
      } else {
         return -1;
      }
   }

   static record LayerTextureKey(EquipmentClientInfo.LayerType layerType, EquipmentClientInfo.Layer layer) {
      final EquipmentClientInfo.LayerType layerType;
      final EquipmentClientInfo.Layer layer;

      LayerTextureKey(EquipmentClientInfo.LayerType param1, EquipmentClientInfo.Layer param2) {
         super();
         this.layerType = var1;
         this.layer = var2;
      }

      public EquipmentClientInfo.LayerType layerType() {
         return this.layerType;
      }

      public EquipmentClientInfo.Layer layer() {
         return this.layer;
      }
   }

   static record TrimSpriteKey(ArmorTrim trim, EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> equipmentAssetId) {
      TrimSpriteKey(ArmorTrim param1, EquipmentClientInfo.LayerType param2, ResourceKey<EquipmentAsset> param3) {
         super();
         this.trim = var1;
         this.layerType = var2;
         this.equipmentAssetId = var3;
      }

      public ResourceLocation spriteId() {
         return this.trim.layerAssetId(this.layerType.trimAssetPrefix(), this.equipmentAssetId);
      }

      public ArmorTrim trim() {
         return this.trim;
      }

      public EquipmentClientInfo.LayerType layerType() {
         return this.layerType;
      }

      public ResourceKey<EquipmentAsset> equipmentAssetId() {
         return this.equipmentAssetId;
      }
   }
}
