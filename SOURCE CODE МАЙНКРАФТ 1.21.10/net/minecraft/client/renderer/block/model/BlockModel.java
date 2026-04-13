package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public record BlockModel(@Nullable UnbakedGeometry geometry, @Nullable UnbakedModel.GuiLight guiLight, @Nullable Boolean ambientOcclusion, @Nullable ItemTransforms transforms, TextureSlots.Data textureSlots, @Nullable ResourceLocation parent) implements UnbakedModel {
   @VisibleForTesting
   static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(BlockModel.class, new BlockModel.Deserializer()).registerTypeAdapter(BlockElement.class, new BlockElement.Deserializer()).registerTypeAdapter(BlockElementFace.class, new BlockElementFace.Deserializer()).registerTypeAdapter(ItemTransform.class, new ItemTransform.Deserializer()).registerTypeAdapter(ItemTransforms.class, new ItemTransforms.Deserializer()).create();

   public BlockModel(@Nullable UnbakedGeometry param1, @Nullable UnbakedModel.GuiLight param2, @Nullable Boolean param3, @Nullable ItemTransforms param4, TextureSlots.Data param5, @Nullable ResourceLocation param6) {
      super();
      this.geometry = var1;
      this.guiLight = var2;
      this.ambientOcclusion = var3;
      this.transforms = var4;
      this.textureSlots = var5;
      this.parent = var6;
   }

   public static BlockModel fromStream(Reader var0) {
      return (BlockModel)GsonHelper.fromJson(GSON, var0, BlockModel.class);
   }

   @Nullable
   public UnbakedGeometry geometry() {
      return this.geometry;
   }

   @Nullable
   public UnbakedModel.GuiLight guiLight() {
      return this.guiLight;
   }

   @Nullable
   public Boolean ambientOcclusion() {
      return this.ambientOcclusion;
   }

   @Nullable
   public ItemTransforms transforms() {
      return this.transforms;
   }

   public TextureSlots.Data textureSlots() {
      return this.textureSlots;
   }

   @Nullable
   public ResourceLocation parent() {
      return this.parent;
   }

   public static class Deserializer implements JsonDeserializer<BlockModel> {
      public Deserializer() {
         super();
      }

      public BlockModel deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3) throws JsonParseException {
         JsonObject var4 = var1.getAsJsonObject();
         UnbakedGeometry var5 = this.getElements(var3, var4);
         String var6 = this.getParentName(var4);
         TextureSlots.Data var7 = this.getTextureMap(var4);
         Boolean var8 = this.getAmbientOcclusion(var4);
         ItemTransforms var9 = null;
         if (var4.has("display")) {
            JsonObject var10 = GsonHelper.getAsJsonObject(var4, "display");
            var9 = (ItemTransforms)var3.deserialize(var10, ItemTransforms.class);
         }

         UnbakedModel.GuiLight var12 = null;
         if (var4.has("gui_light")) {
            var12 = UnbakedModel.GuiLight.getByName(GsonHelper.getAsString(var4, "gui_light"));
         }

         ResourceLocation var11 = var6.isEmpty() ? null : ResourceLocation.parse(var6);
         return new BlockModel(var5, var12, var8, var9, var7, var11);
      }

      private TextureSlots.Data getTextureMap(JsonObject var1) {
         if (var1.has("textures")) {
            JsonObject var2 = GsonHelper.getAsJsonObject(var1, "textures");
            return TextureSlots.parseTextureMap(var2, TextureAtlas.LOCATION_BLOCKS);
         } else {
            return TextureSlots.Data.EMPTY;
         }
      }

      private String getParentName(JsonObject var1) {
         return GsonHelper.getAsString(var1, "parent", "");
      }

      @Nullable
      protected Boolean getAmbientOcclusion(JsonObject var1) {
         return var1.has("ambientocclusion") ? GsonHelper.getAsBoolean(var1, "ambientocclusion") : null;
      }

      @Nullable
      protected UnbakedGeometry getElements(JsonDeserializationContext var1, JsonObject var2) {
         if (!var2.has("elements")) {
            return null;
         } else {
            ArrayList var3 = new ArrayList();
            Iterator var4 = GsonHelper.getAsJsonArray(var2, "elements").iterator();

            while(var4.hasNext()) {
               JsonElement var5 = (JsonElement)var4.next();
               var3.add((BlockElement)var1.deserialize(var5, BlockElement.class));
            }

            return new SimpleUnbakedGeometry(var3);
         }
      }

      // $FF: synthetic method
      public Object deserialize(final JsonElement param1, final Type param2, final JsonDeserializationContext param3) throws JsonParseException {
         return this.deserialize(var1, var2, var3);
      }
   }
}
