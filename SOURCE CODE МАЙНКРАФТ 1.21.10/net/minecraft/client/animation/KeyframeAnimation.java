package net.minecraft.client.animation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import org.joml.Vector3f;

public class KeyframeAnimation {
   private final AnimationDefinition definition;
   private final List<KeyframeAnimation.Entry> entries;

   private KeyframeAnimation(AnimationDefinition var1, List<KeyframeAnimation.Entry> var2) {
      super();
      this.definition = var1;
      this.entries = var2;
   }

   static KeyframeAnimation bake(ModelPart var0, AnimationDefinition var1) {
      ArrayList var2 = new ArrayList();
      Function var3 = var0.createPartLookup();
      Iterator var4 = var1.boneAnimations().entrySet().iterator();

      while(var4.hasNext()) {
         java.util.Map.Entry var5 = (java.util.Map.Entry)var4.next();
         String var6 = (String)var5.getKey();
         List var7 = (List)var5.getValue();
         ModelPart var8 = (ModelPart)var3.apply(var6);
         if (var8 == null) {
            throw new IllegalArgumentException("Cannot animate " + var6 + ", which does not exist in model");
         }

         Iterator var9 = var7.iterator();

         while(var9.hasNext()) {
            AnimationChannel var10 = (AnimationChannel)var9.next();
            var2.add(new KeyframeAnimation.Entry(var8, var10.target(), var10.keyframes()));
         }
      }

      return new KeyframeAnimation(var1, List.copyOf(var2));
   }

   public void applyStatic() {
      this.apply(0L, 1.0F);
   }

   public void applyWalk(float var1, float var2, float var3, float var4) {
      long var5 = (long)(var1 * 50.0F * var3);
      float var7 = Math.min(var2 * var4, 1.0F);
      this.apply(var5, var7);
   }

   public void apply(AnimationState var1, float var2) {
      this.apply(var1, var2, 1.0F);
   }

   public void apply(AnimationState var1, float var2, float var3) {
      var1.ifStarted((var3x) -> {
         this.apply((long)((float)var3x.getTimeInMillis(var2) * var3), 1.0F);
      });
   }

   public void apply(long var1, float var3) {
      float var4 = this.getElapsedSeconds(var1);
      Vector3f var5 = new Vector3f();
      Iterator var6 = this.entries.iterator();

      while(var6.hasNext()) {
         KeyframeAnimation.Entry var7 = (KeyframeAnimation.Entry)var6.next();
         var7.apply(var4, var3, var5);
      }

   }

   private float getElapsedSeconds(long var1) {
      float var3 = (float)var1 / 1000.0F;
      return this.definition.looping() ? var3 % this.definition.lengthInSeconds() : var3;
   }

   private static record Entry(ModelPart part, AnimationChannel.Target target, Keyframe[] keyframes) {
      Entry(ModelPart param1, AnimationChannel.Target param2, Keyframe[] param3) {
         super();
         this.part = var1;
         this.target = var2;
         this.keyframes = var3;
      }

      public void apply(float var1, float var2, Vector3f var3) {
         int var4 = Math.max(0, Mth.binarySearch(0, this.keyframes.length, (var2x) -> {
            return var1 <= this.keyframes[var2x].timestamp();
         }) - 1);
         int var5 = Math.min(this.keyframes.length - 1, var4 + 1);
         Keyframe var6 = this.keyframes[var4];
         Keyframe var7 = this.keyframes[var5];
         float var8 = var1 - var6.timestamp();
         float var9;
         if (var5 != var4) {
            var9 = Mth.clamp(var8 / (var7.timestamp() - var6.timestamp()), 0.0F, 1.0F);
         } else {
            var9 = 0.0F;
         }

         var7.interpolation().apply(var3, var9, this.keyframes, var4, var5, var2);
         this.target.apply(this.part, var3);
      }

      public ModelPart part() {
         return this.part;
      }

      public AnimationChannel.Target target() {
         return this.target;
      }

      public Keyframe[] keyframes() {
         return this.keyframes;
      }
   }
}
