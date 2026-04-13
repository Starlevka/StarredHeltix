package net.minecraft.world.entity.animal.wolf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.sounds.SoundEvent;

public record WolfSoundVariant(Holder<SoundEvent> ambientSound, Holder<SoundEvent> deathSound, Holder<SoundEvent> growlSound, Holder<SoundEvent> hurtSound, Holder<SoundEvent> pantSound, Holder<SoundEvent> whineSound) {
   public static final Codec<WolfSoundVariant> DIRECT_CODEC = getWolfSoundVariantCodec();
   public static final Codec<WolfSoundVariant> NETWORK_CODEC = getWolfSoundVariantCodec();
   public static final Codec<Holder<WolfSoundVariant>> CODEC;
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<WolfSoundVariant>> STREAM_CODEC;

   public WolfSoundVariant(Holder<SoundEvent> param1, Holder<SoundEvent> param2, Holder<SoundEvent> param3, Holder<SoundEvent> param4, Holder<SoundEvent> param5, Holder<SoundEvent> param6) {
      super();
      this.ambientSound = var1;
      this.deathSound = var2;
      this.growlSound = var3;
      this.hurtSound = var4;
      this.pantSound = var5;
      this.whineSound = var6;
   }

   private static Codec<WolfSoundVariant> getWolfSoundVariantCodec() {
      return RecordCodecBuilder.create((var0) -> {
         return var0.group(SoundEvent.CODEC.fieldOf("ambient_sound").forGetter(WolfSoundVariant::ambientSound), SoundEvent.CODEC.fieldOf("death_sound").forGetter(WolfSoundVariant::deathSound), SoundEvent.CODEC.fieldOf("growl_sound").forGetter(WolfSoundVariant::growlSound), SoundEvent.CODEC.fieldOf("hurt_sound").forGetter(WolfSoundVariant::hurtSound), SoundEvent.CODEC.fieldOf("pant_sound").forGetter(WolfSoundVariant::pantSound), SoundEvent.CODEC.fieldOf("whine_sound").forGetter(WolfSoundVariant::whineSound)).apply(var0, WolfSoundVariant::new);
      });
   }

   public Holder<SoundEvent> ambientSound() {
      return this.ambientSound;
   }

   public Holder<SoundEvent> deathSound() {
      return this.deathSound;
   }

   public Holder<SoundEvent> growlSound() {
      return this.growlSound;
   }

   public Holder<SoundEvent> hurtSound() {
      return this.hurtSound;
   }

   public Holder<SoundEvent> pantSound() {
      return this.pantSound;
   }

   public Holder<SoundEvent> whineSound() {
      return this.whineSound;
   }

   static {
      CODEC = RegistryFixedCodec.create(Registries.WOLF_SOUND_VARIANT);
      STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.WOLF_SOUND_VARIANT);
   }
}
