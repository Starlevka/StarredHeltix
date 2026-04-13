package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

public record GlyphRenderState(Matrix3x2f pose, TextRenderable renderable, @Nullable ScreenRectangle scissorArea) implements GuiElementRenderState {
   public GlyphRenderState(Matrix3x2f param1, TextRenderable param2, @Nullable ScreenRectangle param3) {
      super();
      this.pose = var1;
      this.renderable = var2;
      this.scissorArea = var3;
   }

   public void buildVertices(VertexConsumer var1) {
      this.renderable.render((new Matrix4f()).mul(this.pose), var1, 15728880, true);
   }

   public RenderPipeline pipeline() {
      return this.renderable.guiPipeline();
   }

   public TextureSetup textureSetup() {
      return TextureSetup.singleTextureWithLightmap(this.renderable.textureView());
   }

   @Nullable
   public ScreenRectangle bounds() {
      return null;
   }

   public Matrix3x2f pose() {
      return this.pose;
   }

   public TextRenderable renderable() {
      return this.renderable;
   }

   @Nullable
   public ScreenRectangle scissorArea() {
      return this.scissorArea;
   }
}
