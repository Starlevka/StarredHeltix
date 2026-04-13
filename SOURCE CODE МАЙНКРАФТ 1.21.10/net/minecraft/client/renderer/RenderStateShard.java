package net.minecraft.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public abstract class RenderStateShard {
   public static final double MAX_ENCHANTMENT_GLINT_SPEED_MILLIS = 8.0D;
   protected final String name;
   private final Runnable setupState;
   private final Runnable clearState;
   protected static final RenderStateShard.TextureStateShard BLOCK_SHEET_MIPPED;
   protected static final RenderStateShard.TextureStateShard BLOCK_SHEET;
   protected static final RenderStateShard.EmptyTextureStateShard NO_TEXTURE;
   protected static final RenderStateShard.TexturingStateShard DEFAULT_TEXTURING;
   protected static final RenderStateShard.TexturingStateShard GLINT_TEXTURING;
   protected static final RenderStateShard.TexturingStateShard ENTITY_GLINT_TEXTURING;
   protected static final RenderStateShard.TexturingStateShard ARMOR_ENTITY_GLINT_TEXTURING;
   protected static final RenderStateShard.LightmapStateShard LIGHTMAP;
   protected static final RenderStateShard.LightmapStateShard NO_LIGHTMAP;
   protected static final RenderStateShard.OverlayStateShard OVERLAY;
   protected static final RenderStateShard.OverlayStateShard NO_OVERLAY;
   protected static final RenderStateShard.LayeringStateShard NO_LAYERING;
   protected static final RenderStateShard.LayeringStateShard VIEW_OFFSET_Z_LAYERING;
   protected static final RenderStateShard.LayeringStateShard VIEW_OFFSET_Z_LAYERING_FORWARD;
   protected static final RenderStateShard.OutputStateShard MAIN_TARGET;
   protected static final RenderStateShard.OutputStateShard OUTLINE_TARGET;
   protected static final RenderStateShard.OutputStateShard WEATHER_TARGET;
   protected static final RenderStateShard.OutputStateShard ITEM_ENTITY_TARGET;
   protected static final RenderStateShard.LineStateShard DEFAULT_LINE;

   public RenderStateShard(String var1, Runnable var2, Runnable var3) {
      super();
      this.name = var1;
      this.setupState = var2;
      this.clearState = var3;
   }

   public void setupRenderState() {
      this.setupState.run();
   }

   public void clearRenderState() {
      this.clearState.run();
   }

   public String toString() {
      return this.name;
   }

   public String getName() {
      return this.name;
   }

   private static void setupGlintTexturing(float var0) {
      long var1 = (long)((double)Util.getMillis() * (Double)Minecraft.getInstance().options.glintSpeed().get() * 8.0D);
      float var3 = (float)(var1 % 110000L) / 110000.0F;
      float var4 = (float)(var1 % 30000L) / 30000.0F;
      Matrix4f var5 = (new Matrix4f()).translation(-var3, var4, 0.0F);
      var5.rotateZ(0.17453292F).scale(var0);
      RenderSystem.setTextureMatrix(var5);
   }

   static {
      BLOCK_SHEET_MIPPED = new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, true);
      BLOCK_SHEET = new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false);
      NO_TEXTURE = new RenderStateShard.EmptyTextureStateShard();
      DEFAULT_TEXTURING = new RenderStateShard.TexturingStateShard("default_texturing", () -> {
      }, () -> {
      });
      GLINT_TEXTURING = new RenderStateShard.TexturingStateShard("glint_texturing", () -> {
         setupGlintTexturing(8.0F);
      }, RenderSystem::resetTextureMatrix);
      ENTITY_GLINT_TEXTURING = new RenderStateShard.TexturingStateShard("entity_glint_texturing", () -> {
         setupGlintTexturing(0.5F);
      }, RenderSystem::resetTextureMatrix);
      ARMOR_ENTITY_GLINT_TEXTURING = new RenderStateShard.TexturingStateShard("armor_entity_glint_texturing", () -> {
         setupGlintTexturing(0.16F);
      }, RenderSystem::resetTextureMatrix);
      LIGHTMAP = new RenderStateShard.LightmapStateShard(true);
      NO_LIGHTMAP = new RenderStateShard.LightmapStateShard(false);
      OVERLAY = new RenderStateShard.OverlayStateShard(true);
      NO_OVERLAY = new RenderStateShard.OverlayStateShard(false);
      NO_LAYERING = new RenderStateShard.LayeringStateShard("no_layering", () -> {
      }, () -> {
      });
      VIEW_OFFSET_Z_LAYERING = new RenderStateShard.LayeringStateShard("view_offset_z_layering", () -> {
         Matrix4fStack var0 = RenderSystem.getModelViewStack();
         var0.pushMatrix();
         RenderSystem.getProjectionType().applyLayeringTransform(var0, 1.0F);
      }, () -> {
         Matrix4fStack var0 = RenderSystem.getModelViewStack();
         var0.popMatrix();
      });
      VIEW_OFFSET_Z_LAYERING_FORWARD = new RenderStateShard.LayeringStateShard("view_offset_z_layering_forward", () -> {
         Matrix4fStack var0 = RenderSystem.getModelViewStack();
         var0.pushMatrix();
         RenderSystem.getProjectionType().applyLayeringTransform(var0, -1.0F);
      }, () -> {
         Matrix4fStack var0 = RenderSystem.getModelViewStack();
         var0.popMatrix();
      });
      MAIN_TARGET = new RenderStateShard.OutputStateShard("main_target", () -> {
         return Minecraft.getInstance().getMainRenderTarget();
      });
      OUTLINE_TARGET = new RenderStateShard.OutputStateShard("outline_target", () -> {
         RenderTarget var0 = Minecraft.getInstance().levelRenderer.entityOutlineTarget();
         return var0 != null ? var0 : Minecraft.getInstance().getMainRenderTarget();
      });
      WEATHER_TARGET = new RenderStateShard.OutputStateShard("weather_target", () -> {
         RenderTarget var0 = Minecraft.getInstance().levelRenderer.getWeatherTarget();
         return var0 != null ? var0 : Minecraft.getInstance().getMainRenderTarget();
      });
      ITEM_ENTITY_TARGET = new RenderStateShard.OutputStateShard("item_entity_target", () -> {
         RenderTarget var0 = Minecraft.getInstance().levelRenderer.getItemEntityTarget();
         return var0 != null ? var0 : Minecraft.getInstance().getMainRenderTarget();
      });
      DEFAULT_LINE = new RenderStateShard.LineStateShard(OptionalDouble.of(1.0D));
   }

   protected static class TextureStateShard extends RenderStateShard.EmptyTextureStateShard {
      private final Optional<ResourceLocation> texture;
      private final boolean mipmap;

      public TextureStateShard(ResourceLocation var1, boolean var2) {
         super(() -> {
            TextureManager var2x = Minecraft.getInstance().getTextureManager();
            AbstractTexture var3 = var2x.getTexture(var1);
            var3.setUseMipmaps(var2);
            RenderSystem.setShaderTexture(0, var3.getTextureView());
         }, () -> {
         });
         this.texture = Optional.of(var1);
         this.mipmap = var2;
      }

      public String toString() {
         String var10000 = this.name;
         return var10000 + "[" + String.valueOf(this.texture) + "(mipmap=" + this.mipmap + ")]";
      }

      protected Optional<ResourceLocation> cutoutTexture() {
         return this.texture;
      }
   }

   protected static class EmptyTextureStateShard extends RenderStateShard {
      public EmptyTextureStateShard(Runnable var1, Runnable var2) {
         super("texture", var1, var2);
      }

      EmptyTextureStateShard() {
         super("texture", () -> {
         }, () -> {
         });
      }

      protected Optional<ResourceLocation> cutoutTexture() {
         return Optional.empty();
      }
   }

   protected static class TexturingStateShard extends RenderStateShard {
      public TexturingStateShard(String var1, Runnable var2, Runnable var3) {
         super(var1, var2, var3);
      }
   }

   protected static class LightmapStateShard extends RenderStateShard.BooleanStateShard {
      public LightmapStateShard(boolean var1) {
         super("lightmap", () -> {
            if (var1) {
               Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            }

         }, () -> {
            if (var1) {
               Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
            }

         }, var1);
      }
   }

   protected static class OverlayStateShard extends RenderStateShard.BooleanStateShard {
      public OverlayStateShard(boolean var1) {
         super("overlay", () -> {
            if (var1) {
               Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();
            }

         }, () -> {
            if (var1) {
               Minecraft.getInstance().gameRenderer.overlayTexture().teardownOverlayColor();
            }

         }, var1);
      }
   }

   protected static class LayeringStateShard extends RenderStateShard {
      public LayeringStateShard(String var1, Runnable var2, Runnable var3) {
         super(var1, var2, var3);
      }
   }

   protected static class OutputStateShard extends RenderStateShard {
      private final Supplier<RenderTarget> renderTargetSupplier;

      public OutputStateShard(String var1, Supplier<RenderTarget> var2) {
         super(var1, () -> {
         }, () -> {
         });
         this.renderTargetSupplier = var2;
      }

      public RenderTarget getRenderTarget() {
         return (RenderTarget)this.renderTargetSupplier.get();
      }
   }

   protected static class LineStateShard extends RenderStateShard {
      private final OptionalDouble width;

      public LineStateShard(OptionalDouble var1) {
         super("line_width", () -> {
            if (!Objects.equals(var1, OptionalDouble.of(1.0D))) {
               if (var1.isPresent()) {
                  RenderSystem.lineWidth((float)var1.getAsDouble());
               } else {
                  RenderSystem.lineWidth(Math.max(2.5F, (float)Minecraft.getInstance().getWindow().getWidth() / 1920.0F * 2.5F));
               }
            }

         }, () -> {
            if (!Objects.equals(var1, OptionalDouble.of(1.0D))) {
               RenderSystem.lineWidth(1.0F);
            }

         });
         this.width = var1;
      }

      public String toString() {
         String var10000 = this.name;
         return var10000 + "[" + String.valueOf(this.width.isPresent() ? this.width.getAsDouble() : "window_scale") + "]";
      }
   }

   private static class BooleanStateShard extends RenderStateShard {
      private final boolean enabled;

      public BooleanStateShard(String var1, Runnable var2, Runnable var3, boolean var4) {
         super(var1, var2, var3);
         this.enabled = var4;
      }

      public String toString() {
         return this.name + "[" + this.enabled + "]";
      }
   }

   protected static final class OffsetTexturingStateShard extends RenderStateShard.TexturingStateShard {
      public OffsetTexturingStateShard(float var1, float var2) {
         super("offset_texturing", () -> {
            RenderSystem.setTextureMatrix((new Matrix4f()).translation(var1, var2, 0.0F));
         }, () -> {
            RenderSystem.resetTextureMatrix();
         });
      }
   }

   protected static class MultiTextureStateShard extends RenderStateShard.EmptyTextureStateShard {
      private final Optional<ResourceLocation> cutoutTexture;

      MultiTextureStateShard(List<RenderStateShard.MultiTextureStateShard.Entry> var1) {
         super(() -> {
            for(int var1x = 0; var1x < var1.size(); ++var1x) {
               RenderStateShard.MultiTextureStateShard.Entry var2 = (RenderStateShard.MultiTextureStateShard.Entry)var1.get(var1x);
               TextureManager var3 = Minecraft.getInstance().getTextureManager();
               AbstractTexture var4 = var3.getTexture(var2.id);
               var4.setUseMipmaps(var2.mipmap);
               RenderSystem.setShaderTexture(var1x, var4.getTextureView());
            }

         }, () -> {
         });
         this.cutoutTexture = var1.isEmpty() ? Optional.empty() : Optional.of(((RenderStateShard.MultiTextureStateShard.Entry)var1.getFirst()).id);
      }

      protected Optional<ResourceLocation> cutoutTexture() {
         return this.cutoutTexture;
      }

      public static RenderStateShard.MultiTextureStateShard.Builder builder() {
         return new RenderStateShard.MultiTextureStateShard.Builder();
      }

      private static record Entry(ResourceLocation id, boolean mipmap) {
         final ResourceLocation id;
         final boolean mipmap;

         Entry(ResourceLocation param1, boolean param2) {
            super();
            this.id = var1;
            this.mipmap = var2;
         }

         public ResourceLocation id() {
            return this.id;
         }

         public boolean mipmap() {
            return this.mipmap;
         }
      }

      public static final class Builder {
         private final com.google.common.collect.ImmutableList.Builder<RenderStateShard.MultiTextureStateShard.Entry> builder = new com.google.common.collect.ImmutableList.Builder();

         public Builder() {
            super();
         }

         public RenderStateShard.MultiTextureStateShard.Builder add(ResourceLocation var1, boolean var2) {
            this.builder.add(new RenderStateShard.MultiTextureStateShard.Entry(var1, var2));
            return this;
         }

         public RenderStateShard.MultiTextureStateShard build() {
            return new RenderStateShard.MultiTextureStateShard(this.builder.build());
         }
      }
   }
}
