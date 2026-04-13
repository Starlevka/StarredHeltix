package net.minecraft.client.sounds;

import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class SoundPreviewHandler {
   @Nullable
   private static SoundInstance activePreview;
   @Nullable
   private static SoundSource previousCategory;

   public SoundPreviewHandler() {
      super();
   }

   public static void preview(SoundManager var0, SoundSource var1, float var2) {
      stopOtherCategoryPreview(var0, var1);
      if (canPlaySound(var0)) {
         SoundEvent var10000;
         switch(var1) {
         case RECORDS:
            var10000 = (SoundEvent)SoundEvents.NOTE_BLOCK_GUITAR.value();
            break;
         case WEATHER:
            var10000 = SoundEvents.LIGHTNING_BOLT_THUNDER;
            break;
         case BLOCKS:
            var10000 = SoundEvents.GRASS_PLACE;
            break;
         case HOSTILE:
            var10000 = SoundEvents.ZOMBIE_AMBIENT;
            break;
         case NEUTRAL:
            var10000 = SoundEvents.COW_AMBIENT;
            break;
         case PLAYERS:
            var10000 = (SoundEvent)SoundEvents.GENERIC_EAT.value();
            break;
         case AMBIENT:
            var10000 = (SoundEvent)SoundEvents.AMBIENT_CAVE.value();
            break;
         case UI:
            var10000 = (SoundEvent)SoundEvents.UI_BUTTON_CLICK.value();
            break;
         default:
            var10000 = SoundEvents.EMPTY;
         }

         SoundEvent var3 = var10000;
         if (var3 != SoundEvents.EMPTY) {
            activePreview = SimpleSoundInstance.forUI(var3, 1.0F, var2);
            var0.play(activePreview);
         }
      }

   }

   private static void stopOtherCategoryPreview(SoundManager var0, SoundSource var1) {
      if (previousCategory != var1) {
         previousCategory = var1;
         if (activePreview != null) {
            var0.stop(activePreview);
         }
      }

   }

   private static boolean canPlaySound(SoundManager var0) {
      return activePreview == null || !var0.isActive(activePreview);
   }
}
