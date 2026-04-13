package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.minecraft.world.item.ItemDisplayContext;

public record ItemTransforms(ItemTransform thirdPersonLeftHand, ItemTransform thirdPersonRightHand, ItemTransform firstPersonLeftHand, ItemTransform firstPersonRightHand, ItemTransform head, ItemTransform gui, ItemTransform ground, ItemTransform fixed, ItemTransform fixedFromBottom) {
   public static final ItemTransforms NO_TRANSFORMS;

   public ItemTransforms(ItemTransform param1, ItemTransform param2, ItemTransform param3, ItemTransform param4, ItemTransform param5, ItemTransform param6, ItemTransform param7, ItemTransform param8, ItemTransform param9) {
      super();
      this.thirdPersonLeftHand = var1;
      this.thirdPersonRightHand = var2;
      this.firstPersonLeftHand = var3;
      this.firstPersonRightHand = var4;
      this.head = var5;
      this.gui = var6;
      this.ground = var7;
      this.fixed = var8;
      this.fixedFromBottom = var9;
   }

   public ItemTransform getTransform(ItemDisplayContext var1) {
      ItemTransform var10000;
      switch(var1) {
      case THIRD_PERSON_LEFT_HAND:
         var10000 = this.thirdPersonLeftHand;
         break;
      case THIRD_PERSON_RIGHT_HAND:
         var10000 = this.thirdPersonRightHand;
         break;
      case FIRST_PERSON_LEFT_HAND:
         var10000 = this.firstPersonLeftHand;
         break;
      case FIRST_PERSON_RIGHT_HAND:
         var10000 = this.firstPersonRightHand;
         break;
      case HEAD:
         var10000 = this.head;
         break;
      case GUI:
         var10000 = this.gui;
         break;
      case GROUND:
         var10000 = this.ground;
         break;
      case FIXED:
         var10000 = this.fixed;
         break;
      case ON_SHELF:
         var10000 = this.fixedFromBottom;
         break;
      default:
         var10000 = ItemTransform.NO_TRANSFORM;
      }

      return var10000;
   }

   public ItemTransform thirdPersonLeftHand() {
      return this.thirdPersonLeftHand;
   }

   public ItemTransform thirdPersonRightHand() {
      return this.thirdPersonRightHand;
   }

   public ItemTransform firstPersonLeftHand() {
      return this.firstPersonLeftHand;
   }

   public ItemTransform firstPersonRightHand() {
      return this.firstPersonRightHand;
   }

   public ItemTransform head() {
      return this.head;
   }

   public ItemTransform gui() {
      return this.gui;
   }

   public ItemTransform ground() {
      return this.ground;
   }

   public ItemTransform fixed() {
      return this.fixed;
   }

   public ItemTransform fixedFromBottom() {
      return this.fixedFromBottom;
   }

   static {
      NO_TRANSFORMS = new ItemTransforms(ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM);
   }

   protected static class Deserializer implements JsonDeserializer<ItemTransforms> {
      protected Deserializer() {
         super();
      }

      public ItemTransforms deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3) throws JsonParseException {
         JsonObject var4 = var1.getAsJsonObject();
         ItemTransform var5 = this.getTransform(var3, var4, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
         ItemTransform var6 = this.getTransform(var3, var4, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
         if (var6 == ItemTransform.NO_TRANSFORM) {
            var6 = var5;
         }

         ItemTransform var7 = this.getTransform(var3, var4, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
         ItemTransform var8 = this.getTransform(var3, var4, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
         if (var8 == ItemTransform.NO_TRANSFORM) {
            var8 = var7;
         }

         ItemTransform var9 = this.getTransform(var3, var4, ItemDisplayContext.HEAD);
         ItemTransform var10 = this.getTransform(var3, var4, ItemDisplayContext.GUI);
         ItemTransform var11 = this.getTransform(var3, var4, ItemDisplayContext.GROUND);
         ItemTransform var12 = this.getTransform(var3, var4, ItemDisplayContext.FIXED);
         ItemTransform var13 = this.getTransform(var3, var4, ItemDisplayContext.ON_SHELF);
         return new ItemTransforms(var6, var5, var8, var7, var9, var10, var11, var12, var13);
      }

      private ItemTransform getTransform(JsonDeserializationContext var1, JsonObject var2, ItemDisplayContext var3) {
         String var4 = var3.getSerializedName();
         return var2.has(var4) ? (ItemTransform)var1.deserialize(var2.get(var4), ItemTransform.class) : ItemTransform.NO_TRANSFORM;
      }

      // $FF: synthetic method
      public Object deserialize(final JsonElement param1, final Type param2, final JsonDeserializationContext param3) throws JsonParseException {
         return this.deserialize(var1, var2, var3);
      }
   }
}
