package net.minecraft.client.gui.render.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;

public class GuiRenderState {
   private static final int DEBUG_RECTANGLE_COLOR = 2000962815;
   private final List<GuiRenderState.Node> strata = new ArrayList();
   private int firstStratumAfterBlur = 2147483647;
   private GuiRenderState.Node current;
   private final Set<Object> itemModelIdentities = new HashSet();
   @Nullable
   private ScreenRectangle lastElementBounds;

   public GuiRenderState() {
      super();
      this.nextStratum();
   }

   public void nextStratum() {
      this.current = new GuiRenderState.Node((GuiRenderState.Node)null);
      this.strata.add(this.current);
   }

   public void blurBeforeThisStratum() {
      if (this.firstStratumAfterBlur != 2147483647) {
         throw new IllegalStateException("Can only blur once per frame");
      } else {
         this.firstStratumAfterBlur = this.strata.size() - 1;
      }
   }

   public void up() {
      if (this.current.up == null) {
         this.current.up = new GuiRenderState.Node(this.current);
      }

      this.current = this.current.up;
   }

   public void submitItem(GuiItemRenderState var1) {
      if (this.findAppropriateNode(var1)) {
         this.itemModelIdentities.add(var1.itemStackRenderState().getModelIdentity());
         this.current.submitItem(var1);
         this.sumbitDebugRectangleIfEnabled(var1.bounds());
      }
   }

   public void submitText(GuiTextRenderState var1) {
      if (this.findAppropriateNode(var1)) {
         this.current.submitText(var1);
         this.sumbitDebugRectangleIfEnabled(var1.bounds());
      }
   }

   public void submitPicturesInPictureState(PictureInPictureRenderState var1) {
      if (this.findAppropriateNode(var1)) {
         this.current.submitPicturesInPictureState(var1);
         this.sumbitDebugRectangleIfEnabled(var1.bounds());
      }
   }

   public void submitGuiElement(GuiElementRenderState var1) {
      if (this.findAppropriateNode(var1)) {
         this.current.submitGuiElement(var1);
         this.sumbitDebugRectangleIfEnabled(var1.bounds());
      }
   }

   private void sumbitDebugRectangleIfEnabled(@Nullable ScreenRectangle var1) {
      if (SharedConstants.DEBUG_RENDER_UI_LAYERING_RECTANGLES && var1 != null) {
         this.up();
         this.current.submitGuiElement(new ColoredRectangleRenderState(RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(), 0, 0, 10000, 10000, 2000962815, 2000962815, var1));
      }
   }

   private boolean findAppropriateNode(ScreenArea var1) {
      ScreenRectangle var2 = var1.bounds();
      if (var2 == null) {
         return false;
      } else {
         if (this.lastElementBounds != null && this.lastElementBounds.encompasses(var2)) {
            this.up();
         } else {
            this.navigateToAboveHighestElementWithIntersectingBounds(var2);
         }

         this.lastElementBounds = var2;
         return true;
      }
   }

   private void navigateToAboveHighestElementWithIntersectingBounds(ScreenRectangle var1) {
      GuiRenderState.Node var2;
      for(var2 = (GuiRenderState.Node)this.strata.getLast(); var2.up != null; var2 = var2.up) {
      }

      boolean var3 = false;

      while(!var3) {
         var3 = this.hasIntersection(var1, var2.elementStates) || this.hasIntersection(var1, var2.itemStates) || this.hasIntersection(var1, var2.textStates) || this.hasIntersection(var1, var2.picturesInPictureStates);
         if (var2.parent == null) {
            break;
         }

         if (!var3) {
            var2 = var2.parent;
         }
      }

      this.current = var2;
      if (var3) {
         this.up();
      }

   }

   private boolean hasIntersection(ScreenRectangle var1, @Nullable List<? extends ScreenArea> var2) {
      if (var2 != null) {
         Iterator var3 = var2.iterator();

         while(var3.hasNext()) {
            ScreenArea var4 = (ScreenArea)var3.next();
            ScreenRectangle var5 = var4.bounds();
            if (var5 != null && var5.intersects(var1)) {
               return true;
            }
         }
      }

      return false;
   }

   public void submitBlitToCurrentLayer(BlitRenderState var1) {
      this.current.submitGuiElement(var1);
   }

   public void submitGlyphToCurrentLayer(GuiElementRenderState var1) {
      this.current.submitGlyph(var1);
   }

   public Set<Object> getItemModelIdentities() {
      return this.itemModelIdentities;
   }

   public void forEachElement(Consumer<GuiElementRenderState> var1, GuiRenderState.TraverseRange var2) {
      this.traverse((var1x) -> {
         if (var1x.elementStates != null || var1x.glyphStates != null) {
            Iterator var2;
            GuiElementRenderState var3;
            if (var1x.elementStates != null) {
               var2 = var1x.elementStates.iterator();

               while(var2.hasNext()) {
                  var3 = (GuiElementRenderState)var2.next();
                  var1.accept(var3);
               }
            }

            if (var1x.glyphStates != null) {
               var2 = var1x.glyphStates.iterator();

               while(var2.hasNext()) {
                  var3 = (GuiElementRenderState)var2.next();
                  var1.accept(var3);
               }
            }

         }
      }, var2);
   }

   public void forEachItem(Consumer<GuiItemRenderState> var1) {
      GuiRenderState.Node var2 = this.current;
      this.traverse((var2x) -> {
         if (var2x.itemStates != null) {
            this.current = var2x;
            Iterator var3 = var2x.itemStates.iterator();

            while(var3.hasNext()) {
               GuiItemRenderState var4 = (GuiItemRenderState)var3.next();
               var1.accept(var4);
            }
         }

      }, GuiRenderState.TraverseRange.ALL);
      this.current = var2;
   }

   public void forEachText(Consumer<GuiTextRenderState> var1) {
      GuiRenderState.Node var2 = this.current;
      this.traverse((var2x) -> {
         if (var2x.textStates != null) {
            Iterator var3 = var2x.textStates.iterator();

            while(var3.hasNext()) {
               GuiTextRenderState var4 = (GuiTextRenderState)var3.next();
               this.current = var2x;
               var1.accept(var4);
            }
         }

      }, GuiRenderState.TraverseRange.ALL);
      this.current = var2;
   }

   public void forEachPictureInPicture(Consumer<PictureInPictureRenderState> var1) {
      GuiRenderState.Node var2 = this.current;
      this.traverse((var2x) -> {
         if (var2x.picturesInPictureStates != null) {
            this.current = var2x;
            Iterator var3 = var2x.picturesInPictureStates.iterator();

            while(var3.hasNext()) {
               PictureInPictureRenderState var4 = (PictureInPictureRenderState)var3.next();
               var1.accept(var4);
            }
         }

      }, GuiRenderState.TraverseRange.ALL);
      this.current = var2;
   }

   public void sortElements(Comparator<GuiElementRenderState> var1) {
      this.traverse((var1x) -> {
         if (var1x.elementStates != null) {
            if (SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER) {
               Collections.shuffle(var1x.elementStates);
            }

            var1x.elementStates.sort(var1);
         }

      }, GuiRenderState.TraverseRange.ALL);
   }

   private void traverse(Consumer<GuiRenderState.Node> var1, GuiRenderState.TraverseRange var2) {
      int var3 = 0;
      int var4 = this.strata.size();
      if (var2 == GuiRenderState.TraverseRange.BEFORE_BLUR) {
         var4 = Math.min(this.firstStratumAfterBlur, this.strata.size());
      } else if (var2 == GuiRenderState.TraverseRange.AFTER_BLUR) {
         var3 = this.firstStratumAfterBlur;
      }

      for(int var5 = var3; var5 < var4; ++var5) {
         GuiRenderState.Node var6 = (GuiRenderState.Node)this.strata.get(var5);
         this.traverse(var6, var1);
      }

   }

   private void traverse(GuiRenderState.Node var1, Consumer<GuiRenderState.Node> var2) {
      var2.accept(var1);
      if (var1.up != null) {
         this.traverse(var1.up, var2);
      }

   }

   public void reset() {
      this.itemModelIdentities.clear();
      this.strata.clear();
      this.firstStratumAfterBlur = 2147483647;
      this.nextStratum();
   }

   private static class Node {
      @Nullable
      public final GuiRenderState.Node parent;
      @Nullable
      public GuiRenderState.Node up;
      @Nullable
      public List<GuiElementRenderState> elementStates;
      @Nullable
      public List<GuiElementRenderState> glyphStates;
      @Nullable
      public List<GuiItemRenderState> itemStates;
      @Nullable
      public List<GuiTextRenderState> textStates;
      @Nullable
      public List<PictureInPictureRenderState> picturesInPictureStates;

      Node(@Nullable GuiRenderState.Node var1) {
         super();
         this.parent = var1;
      }

      public void submitItem(GuiItemRenderState var1) {
         if (this.itemStates == null) {
            this.itemStates = new ArrayList();
         }

         this.itemStates.add(var1);
      }

      public void submitText(GuiTextRenderState var1) {
         if (this.textStates == null) {
            this.textStates = new ArrayList();
         }

         this.textStates.add(var1);
      }

      public void submitPicturesInPictureState(PictureInPictureRenderState var1) {
         if (this.picturesInPictureStates == null) {
            this.picturesInPictureStates = new ArrayList();
         }

         this.picturesInPictureStates.add(var1);
      }

      public void submitGuiElement(GuiElementRenderState var1) {
         if (this.elementStates == null) {
            this.elementStates = new ArrayList();
         }

         this.elementStates.add(var1);
      }

      public void submitGlyph(GuiElementRenderState var1) {
         if (this.glyphStates == null) {
            this.glyphStates = new ArrayList();
         }

         this.glyphStates.add(var1);
      }
   }

   public static enum TraverseRange {
      ALL,
      BEFORE_BLUR,
      AFTER_BLUR;

      private TraverseRange() {
      }

      // $FF: synthetic method
      private static GuiRenderState.TraverseRange[] $values() {
         return new GuiRenderState.TraverseRange[]{ALL, BEFORE_BLUR, AFTER_BLUR};
      }
   }
}
