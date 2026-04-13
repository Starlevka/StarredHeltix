package net.minecraft.client.gui.font;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class FontSet implements AutoCloseable {
   private static final float LARGE_FORWARD_ADVANCE = 32.0F;
   private static final BakedGlyph INVISIBLE_MISSING_GLYPH = new BakedGlyph() {
      public GlyphInfo info() {
         return SpecialGlyphs.MISSING;
      }

      @Nullable
      public TextRenderable createGlyph(float var1, float var2, int var3, int var4, Style var5, float var6, float var7) {
         return null;
      }
   };
   final GlyphStitcher stitcher;
   final UnbakedGlyph.Stitcher wrappedStitcher = new UnbakedGlyph.Stitcher() {
      public BakedGlyph stitch(GlyphInfo var1, GlyphBitmap var2) {
         return (BakedGlyph)Objects.requireNonNullElse(FontSet.this.stitcher.stitch(var1, var2), FontSet.this.missingGlyph);
      }

      public BakedGlyph getMissing() {
         return FontSet.this.missingGlyph;
      }
   };
   private List<GlyphProvider.Conditional> allProviders = List.of();
   private List<GlyphProvider> activeProviders = List.of();
   private final Int2ObjectMap<IntList> glyphsByWidth = new Int2ObjectOpenHashMap();
   private final CodepointMap<FontSet.SelectedGlyphs> glyphCache = new CodepointMap((var0) -> {
      return new FontSet.SelectedGlyphs[var0];
   }, (var0) -> {
      return new FontSet.SelectedGlyphs[var0][];
   });
   private final IntFunction<FontSet.SelectedGlyphs> glyphGetter = this::computeGlyphInfo;
   BakedGlyph missingGlyph;
   private final Supplier<BakedGlyph> missingGlyphGetter;
   private final FontSet.SelectedGlyphs missingSelectedGlyphs;
   @Nullable
   private EffectGlyph whiteGlyph;
   private final GlyphSource anyGlyphs;
   private final GlyphSource nonFishyGlyphs;

   public FontSet(GlyphStitcher var1) {
      super();
      this.missingGlyph = INVISIBLE_MISSING_GLYPH;
      this.missingGlyphGetter = () -> {
         return this.missingGlyph;
      };
      this.missingSelectedGlyphs = new FontSet.SelectedGlyphs(this.missingGlyphGetter, this.missingGlyphGetter);
      this.anyGlyphs = new FontSet.Source(false);
      this.nonFishyGlyphs = new FontSet.Source(true);
      this.stitcher = var1;
   }

   public void reload(List<GlyphProvider.Conditional> var1, Set<FontOption> var2) {
      this.allProviders = var1;
      this.reload(var2);
   }

   public void reload(Set<FontOption> var1) {
      this.activeProviders = List.of();
      this.resetTextures();
      this.activeProviders = this.selectProviders(this.allProviders, var1);
   }

   private void resetTextures() {
      this.stitcher.reset();
      this.glyphCache.clear();
      this.glyphsByWidth.clear();
      this.missingGlyph = (BakedGlyph)Objects.requireNonNull(SpecialGlyphs.MISSING.bake(this.stitcher));
      this.whiteGlyph = SpecialGlyphs.WHITE.bake(this.stitcher);
   }

   private List<GlyphProvider> selectProviders(List<GlyphProvider.Conditional> var1, Set<FontOption> var2) {
      IntOpenHashSet var3 = new IntOpenHashSet();
      ArrayList var4 = new ArrayList();
      Iterator var5 = var1.iterator();

      while(var5.hasNext()) {
         GlyphProvider.Conditional var6 = (GlyphProvider.Conditional)var5.next();
         if (var6.filter().apply(var2)) {
            var4.add(var6.provider());
            var3.addAll(var6.provider().getSupportedGlyphs());
         }
      }

      HashSet var7 = Sets.newHashSet();
      var3.forEach((var3x) -> {
         Iterator var4x = var4.iterator();

         while(var4x.hasNext()) {
            GlyphProvider var5 = (GlyphProvider)var4x.next();
            UnbakedGlyph var6 = var5.getGlyph(var3x);
            if (var6 != null) {
               var7.add(var5);
               if (var6.info() != SpecialGlyphs.MISSING) {
                  ((IntList)this.glyphsByWidth.computeIfAbsent(Mth.ceil(var6.info().getAdvance(false)), (var0) -> {
                     return new IntArrayList();
                  })).add(var3x);
               }
               break;
            }
         }

      });
      Stream var10000 = var4.stream();
      Objects.requireNonNull(var7);
      return var10000.filter(var7::contains).toList();
   }

   public void close() {
      this.stitcher.close();
   }

   private static boolean hasFishyAdvance(GlyphInfo var0) {
      float var1 = var0.getAdvance(false);
      if (!(var1 < 0.0F) && !(var1 > 32.0F)) {
         float var2 = var0.getAdvance(true);
         return var2 < 0.0F || var2 > 32.0F;
      } else {
         return true;
      }
   }

   private FontSet.SelectedGlyphs computeGlyphInfo(int var1) {
      FontSet.DelayedBake var2 = null;
      Iterator var3 = this.activeProviders.iterator();

      while(var3.hasNext()) {
         GlyphProvider var4 = (GlyphProvider)var3.next();
         UnbakedGlyph var5 = var4.getGlyph(var1);
         if (var5 != null) {
            if (var2 == null) {
               var2 = new FontSet.DelayedBake(var5);
            }

            if (!hasFishyAdvance(var5.info())) {
               if (var2.unbaked == var5) {
                  return new FontSet.SelectedGlyphs(var2, var2);
               }

               return new FontSet.SelectedGlyphs(var2, new FontSet.DelayedBake(var5));
            }
         }
      }

      if (var2 != null) {
         return new FontSet.SelectedGlyphs(var2, this.missingGlyphGetter);
      } else {
         return this.missingSelectedGlyphs;
      }
   }

   FontSet.SelectedGlyphs getGlyph(int var1) {
      return (FontSet.SelectedGlyphs)this.glyphCache.computeIfAbsent(var1, this.glyphGetter);
   }

   public BakedGlyph getRandomGlyph(RandomSource var1, int var2) {
      IntList var3 = (IntList)this.glyphsByWidth.get(var2);
      return var3 != null && !var3.isEmpty() ? (BakedGlyph)this.getGlyph(var3.getInt(var1.nextInt(var3.size()))).nonFishy().get() : this.missingGlyph;
   }

   public EffectGlyph whiteGlyph() {
      return (EffectGlyph)Objects.requireNonNull(this.whiteGlyph);
   }

   public GlyphSource source(boolean var1) {
      return var1 ? this.nonFishyGlyphs : this.anyGlyphs;
   }

   private static record SelectedGlyphs(Supplier<BakedGlyph> any, Supplier<BakedGlyph> nonFishy) {
      SelectedGlyphs(Supplier<BakedGlyph> param1, Supplier<BakedGlyph> param2) {
         super();
         this.any = var1;
         this.nonFishy = var2;
      }

      Supplier<BakedGlyph> select(boolean var1) {
         return var1 ? this.nonFishy : this.any;
      }

      public Supplier<BakedGlyph> any() {
         return this.any;
      }

      public Supplier<BakedGlyph> nonFishy() {
         return this.nonFishy;
      }
   }

   public class Source implements GlyphSource {
      private final boolean filterFishyGlyphs;

      public Source(final boolean param2) {
         super();
         this.filterFishyGlyphs = var2;
      }

      public BakedGlyph getGlyph(int var1) {
         return (BakedGlyph)FontSet.this.getGlyph(var1).select(this.filterFishyGlyphs).get();
      }

      public BakedGlyph getRandomGlyph(RandomSource var1, int var2) {
         return FontSet.this.getRandomGlyph(var1, var2);
      }
   }

   private class DelayedBake implements Supplier<BakedGlyph> {
      final UnbakedGlyph unbaked;
      @Nullable
      private BakedGlyph baked;

      DelayedBake(final UnbakedGlyph param2) {
         super();
         this.unbaked = var2;
      }

      public BakedGlyph get() {
         if (this.baked == null) {
            this.baked = this.unbaked.bake(FontSet.this.wrappedStitcher);
         }

         return this.baked;
      }

      // $FF: synthetic method
      public Object get() {
         return this.get();
      }
   }
}
