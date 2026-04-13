package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public interface MultiLineLabel {
   MultiLineLabel EMPTY = new MultiLineLabel() {
      public int render(GuiGraphics var1, MultiLineLabel.Align var2, int var3, int var4, int var5, boolean var6, int var7) {
         return var4;
      }

      public Style getStyle(MultiLineLabel.Align var1, int var2, int var3, int var4, double var5, double var7) {
         return null;
      }

      public int getLineCount() {
         return 0;
      }

      public int getWidth() {
         return 0;
      }
   };

   static MultiLineLabel create(Font var0, Component... var1) {
      return create(var0, 2147483647, 2147483647, var1);
   }

   static MultiLineLabel create(Font var0, int var1, Component... var2) {
      return create(var0, var1, 2147483647, var2);
   }

   static MultiLineLabel create(Font var0, Component var1, int var2) {
      return create(var0, var2, 2147483647, var1);
   }

   static MultiLineLabel create(final Font var0, final int var1, final int var2, final Component... var3) {
      return var3.length == 0 ? EMPTY : new MultiLineLabel() {
         @Nullable
         private List<MultiLineLabel.TextAndWidth> cachedTextAndWidth;
         @Nullable
         private Language splitWithLanguage;

         public int render(GuiGraphics var1x, MultiLineLabel.Align var2x, int var3x, int var4, int var5, boolean var6, int var7) {
            int var8 = var4;

            for(Iterator var9 = this.getSplitMessage().iterator(); var9.hasNext(); var8 += var5) {
               MultiLineLabel.TextAndWidth var10 = (MultiLineLabel.TextAndWidth)var9.next();
               int var11 = var2x.calculateLeft(var3x, var10.width);
               var1x.drawString(var0, var10.text, var11, var8, var7);
            }

            return var8;
         }

         @Nullable
         public Style getStyle(MultiLineLabel.Align var1x, int var2x, int var3x, int var4, double var5, double var7) {
            List var9 = this.getSplitMessage();
            int var10 = Mth.floor((var7 - (double)var3x) / (double)var4);
            if (var10 >= 0 && var10 < var9.size()) {
               MultiLineLabel.TextAndWidth var11 = (MultiLineLabel.TextAndWidth)var9.get(var10);
               int var12 = var1x.calculateLeft(var2x, var11.width);
               if (var5 < (double)var12) {
                  return null;
               } else {
                  int var13 = Mth.floor(var5 - (double)var12);
                  return var0.getSplitter().componentStyleAtWidth(var11.text, var13);
               }
            } else {
               return null;
            }
         }

         private List<MultiLineLabel.TextAndWidth> getSplitMessage() {
            Language var1x = Language.getInstance();
            if (this.cachedTextAndWidth != null && var1x == this.splitWithLanguage) {
               return this.cachedTextAndWidth;
            } else {
               this.splitWithLanguage = var1x;
               ArrayList var2x = new ArrayList();
               Component[] var3x = var3;
               int var4 = var3x.length;

               int var5;
               for(var5 = 0; var5 < var4; ++var5) {
                  Component var6 = var3x[var5];
                  var2x.addAll(var0.splitIgnoringLanguage(var6, var1));
               }

               this.cachedTextAndWidth = new ArrayList();
               int var10 = Math.min(var2x.size(), var2);
               List var11 = var2x.subList(0, var10);

               for(var5 = 0; var5 < var11.size(); ++var5) {
                  FormattedText var12 = (FormattedText)var11.get(var5);
                  FormattedCharSequence var7 = Language.getInstance().getVisualOrder(var12);
                  if (var5 == var11.size() - 1 && var10 == var2 && var10 != var2x.size()) {
                     FormattedText var8 = var0.substrByWidth(var12, var0.width(var12) - var0.width((FormattedText)CommonComponents.ELLIPSIS));
                     FormattedText var9 = FormattedText.composite(var8, CommonComponents.ELLIPSIS);
                     this.cachedTextAndWidth.add(new MultiLineLabel.TextAndWidth(Language.getInstance().getVisualOrder(var9), var0.width(var9)));
                  } else {
                     this.cachedTextAndWidth.add(new MultiLineLabel.TextAndWidth(var7, var0.width(var7)));
                  }
               }

               return this.cachedTextAndWidth;
            }
         }

         public int getLineCount() {
            return this.getSplitMessage().size();
         }

         public int getWidth() {
            return Math.min(var1, this.getSplitMessage().stream().mapToInt(MultiLineLabel.TextAndWidth::width).max().orElse(0));
         }
      };
   }

   int render(GuiGraphics var1, MultiLineLabel.Align var2, int var3, int var4, int var5, boolean var6, int var7);

   @Nullable
   Style getStyle(MultiLineLabel.Align var1, int var2, int var3, int var4, double var5, double var7);

   int getLineCount();

   int getWidth();

   public static enum Align {
      LEFT {
         int calculateLeft(int var1, int var2) {
            return var1;
         }
      },
      CENTER {
         int calculateLeft(int var1, int var2) {
            return var1 - var2 / 2;
         }
      },
      RIGHT {
         int calculateLeft(int var1, int var2) {
            return var1 - var2;
         }
      };

      Align() {
      }

      abstract int calculateLeft(int var1, int var2);

      // $FF: synthetic method
      private static MultiLineLabel.Align[] $values() {
         return new MultiLineLabel.Align[]{LEFT, CENTER, RIGHT};
      }
   }

   public static record TextAndWidth(FormattedCharSequence text, int width) {
      final FormattedCharSequence text;
      final int width;

      public TextAndWidth(FormattedCharSequence param1, int param2) {
         super();
         this.text = var1;
         this.width = var2;
      }

      public FormattedCharSequence text() {
         return this.text;
      }

      public int width() {
         return this.width;
      }
   }
}
