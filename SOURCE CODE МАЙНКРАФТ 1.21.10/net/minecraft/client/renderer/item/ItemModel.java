package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public interface ItemModel {
   void update(ItemStackRenderState var1, ItemStack var2, ItemModelResolver var3, ItemDisplayContext var4, @Nullable ClientLevel var5, @Nullable ItemOwner var6, int var7);

   public static record BakingContext(ModelBaker blockModelBaker, EntityModelSet entityModelSet, MaterialSet materials, PlayerSkinRenderCache playerSkinRenderCache, ItemModel missingItemModel, @Nullable RegistryContextSwapper contextSwapper) implements SpecialModelRenderer.BakingContext {
      public BakingContext(ModelBaker param1, EntityModelSet param2, MaterialSet param3, PlayerSkinRenderCache param4, ItemModel param5, @Nullable RegistryContextSwapper param6) {
         super();
         this.blockModelBaker = var1;
         this.entityModelSet = var2;
         this.materials = var3;
         this.playerSkinRenderCache = var4;
         this.missingItemModel = var5;
         this.contextSwapper = var6;
      }

      public ModelBaker blockModelBaker() {
         return this.blockModelBaker;
      }

      public EntityModelSet entityModelSet() {
         return this.entityModelSet;
      }

      public MaterialSet materials() {
         return this.materials;
      }

      public PlayerSkinRenderCache playerSkinRenderCache() {
         return this.playerSkinRenderCache;
      }

      public ItemModel missingItemModel() {
         return this.missingItemModel;
      }

      @Nullable
      public RegistryContextSwapper contextSwapper() {
         return this.contextSwapper;
      }
   }

   public interface Unbaked extends ResolvableModel {
      MapCodec<? extends ItemModel.Unbaked> type();

      ItemModel bake(ItemModel.BakingContext var1);
   }
}
