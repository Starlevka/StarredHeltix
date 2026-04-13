package net.minecraft.client.color;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public class ColorLerper {
   public static final DyeColor[] MUSIC_NOTE_COLORS;

   public ColorLerper() {
      super();
   }

   public static int getLerpedColor(ColorLerper.Type var0, float var1) {
      int var2 = Mth.floor(var1);
      int var3 = var2 / var0.colorDuration;
      int var4 = var0.colors.length;
      int var5 = var3 % var4;
      int var6 = (var3 + 1) % var4;
      float var7 = ((float)(var2 % var0.colorDuration) + Mth.frac(var1)) / (float)var0.colorDuration;
      int var8 = var0.getColor(var0.colors[var5]);
      int var9 = var0.getColor(var0.colors[var6]);
      return ARGB.lerp(var7, var8, var9);
   }

   static int getModifiedColor(DyeColor var0, float var1) {
      if (var0 == DyeColor.WHITE) {
         return -1644826;
      } else {
         int var2 = var0.getTextureDiffuseColor();
         return ARGB.color(255, Mth.floor((float)ARGB.red(var2) * var1), Mth.floor((float)ARGB.green(var2) * var1), Mth.floor((float)ARGB.blue(var2) * var1));
      }
   }

   static {
      MUSIC_NOTE_COLORS = new DyeColor[]{DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.LIGHT_BLUE, DyeColor.BLUE, DyeColor.CYAN, DyeColor.GREEN, DyeColor.LIME, DyeColor.YELLOW, DyeColor.ORANGE, DyeColor.PINK, DyeColor.RED, DyeColor.MAGENTA};
   }

   public static enum Type {
      SHEEP(25, DyeColor.values(), 0.75F),
      MUSIC_NOTE(30, ColorLerper.MUSIC_NOTE_COLORS, 1.25F);

      final int colorDuration;
      private final Map<DyeColor, Integer> colorByDye;
      final DyeColor[] colors;

      private Type(final int param3, final DyeColor[] param4, final float param5) {
         this.colorDuration = var3;
         this.colorByDye = Maps.newHashMap((Map)Arrays.stream(var4).collect(Collectors.toMap((var0) -> {
            return var0;
         }, (var1x) -> {
            return ColorLerper.getModifiedColor(var1x, var5);
         })));
         this.colors = var4;
      }

      public final int getColor(DyeColor var1) {
         return (Integer)this.colorByDye.get(var1);
      }

      // $FF: synthetic method
      private static ColorLerper.Type[] $values() {
         return new ColorLerper.Type[]{SHEEP, MUSIC_NOTE};
      }
   }
}
