package net.minecraft.world.entity.animal.coppergolem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public record CopperGolemOxidationLevel(SoundEvent spinHeadSound, SoundEvent hurtSound, SoundEvent deathSound, SoundEvent stepSound, ResourceLocation texture, ResourceLocation eyeTexture) {
   public CopperGolemOxidationLevel(SoundEvent param1, SoundEvent param2, SoundEvent param3, SoundEvent param4, ResourceLocation param5, ResourceLocation param6) {
      super();
      this.spinHeadSound = var1;
      this.hurtSound = var2;
      this.deathSound = var3;
      this.stepSound = var4;
      this.texture = var5;
      this.eyeTexture = var6;
   }

   public SoundEvent spinHeadSound() {
      return this.spinHeadSound;
   }

   public SoundEvent hurtSound() {
      return this.hurtSound;
   }

   public SoundEvent deathSound() {
      return this.deathSound;
   }

   public SoundEvent stepSound() {
      return this.stepSound;
   }

   public ResourceLocation texture() {
      return this.texture;
   }

   public ResourceLocation eyeTexture() {
      return this.eyeTexture;
   }
}
