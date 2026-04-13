package net.minecraft.client.animation;

import org.joml.Vector3fc;

public record Keyframe(float timestamp, Vector3fc preTarget, Vector3fc postTarget, AnimationChannel.Interpolation interpolation) {
   public Keyframe(float var1, Vector3fc var2, AnimationChannel.Interpolation var3) {
      this(var1, var2, var2, var3);
   }

   public Keyframe(float param1, Vector3fc param2, Vector3fc param3, AnimationChannel.Interpolation param4) {
      super();
      this.timestamp = var1;
      this.preTarget = var2;
      this.postTarget = var3;
      this.interpolation = var4;
   }

   public float timestamp() {
      return this.timestamp;
   }

   public Vector3fc preTarget() {
      return this.preTarget;
   }

   public Vector3fc postTarget() {
      return this.postTarget;
   }

   public AnimationChannel.Interpolation interpolation() {
      return this.interpolation;
   }
}
