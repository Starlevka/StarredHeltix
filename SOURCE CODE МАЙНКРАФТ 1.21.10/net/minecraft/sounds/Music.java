package net.minecraft.sounds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;

public record Music(Holder<SoundEvent> event, int minDelay, int maxDelay, boolean replaceCurrentMusic) {
   public static final Codec<Music> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(SoundEvent.CODEC.fieldOf("sound").forGetter((var0x) -> {
         return var0x.event;
      }), Codec.INT.fieldOf("min_delay").forGetter((var0x) -> {
         return var0x.minDelay;
      }), Codec.INT.fieldOf("max_delay").forGetter((var0x) -> {
         return var0x.maxDelay;
      }), Codec.BOOL.fieldOf("replace_current_music").forGetter((var0x) -> {
         return var0x.replaceCurrentMusic;
      })).apply(var0, Music::new);
   });

   public Music(Holder<SoundEvent> param1, int param2, int param3, boolean param4) {
      super();
      this.event = var1;
      this.minDelay = var2;
      this.maxDelay = var3;
      this.replaceCurrentMusic = var4;
   }

   public Holder<SoundEvent> event() {
      return this.event;
   }

   public int minDelay() {
      return this.minDelay;
   }

   public int maxDelay() {
      return this.maxDelay;
   }

   public boolean replaceCurrentMusic() {
      return this.replaceCurrentMusic;
   }
}
