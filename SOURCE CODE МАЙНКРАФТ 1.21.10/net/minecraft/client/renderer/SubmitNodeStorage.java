package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HitboxesRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SubmitNodeStorage implements SubmitNodeCollector {
   private final Int2ObjectAVLTreeMap<SubmitNodeCollection> submitsPerOrder = new Int2ObjectAVLTreeMap();

   public SubmitNodeStorage() {
      super();
   }

   public SubmitNodeCollection order(int var1) {
      return (SubmitNodeCollection)this.submitsPerOrder.computeIfAbsent(var1, (var1x) -> {
         return new SubmitNodeCollection(this);
      });
   }

   public void submitHitbox(PoseStack var1, EntityRenderState var2, HitboxesRenderState var3) {
      this.order(0).submitHitbox(var1, var2, var3);
   }

   public void submitShadow(PoseStack var1, float var2, List<EntityRenderState.ShadowPiece> var3) {
      this.order(0).submitShadow(var1, var2, var3);
   }

   public void submitNameTag(PoseStack var1, @Nullable Vec3 var2, int var3, Component var4, boolean var5, int var6, double var7, CameraRenderState var9) {
      this.order(0).submitNameTag(var1, var2, var3, var4, var5, var6, var7, var9);
   }

   public void submitText(PoseStack var1, float var2, float var3, FormattedCharSequence var4, boolean var5, Font.DisplayMode var6, int var7, int var8, int var9, int var10) {
      this.order(0).submitText(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10);
   }

   public void submitFlame(PoseStack var1, EntityRenderState var2, Quaternionf var3) {
      this.order(0).submitFlame(var1, var2, var3);
   }

   public void submitLeash(PoseStack var1, EntityRenderState.LeashState var2) {
      this.order(0).submitLeash(var1, var2);
   }

   public <S> void submitModel(Model<? super S> var1, S var2, PoseStack var3, RenderType var4, int var5, int var6, int var7, @Nullable TextureAtlasSprite var8, int var9, @Nullable ModelFeatureRenderer.CrumblingOverlay var10) {
      this.order(0).submitModel(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10);
   }

   public void submitModelPart(ModelPart var1, PoseStack var2, RenderType var3, int var4, int var5, @Nullable TextureAtlasSprite var6, boolean var7, boolean var8, int var9, ModelFeatureRenderer.CrumblingOverlay var10, int var11) {
      this.order(0).submitModelPart(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11);
   }

   public void submitBlock(PoseStack var1, BlockState var2, int var3, int var4, int var5) {
      this.order(0).submitBlock(var1, var2, var3, var4, var5);
   }

   public void submitMovingBlock(PoseStack var1, MovingBlockRenderState var2) {
      this.order(0).submitMovingBlock(var1, var2);
   }

   public void submitBlockModel(PoseStack var1, RenderType var2, BlockStateModel var3, float var4, float var5, float var6, int var7, int var8, int var9) {
      this.order(0).submitBlockModel(var1, var2, var3, var4, var5, var6, var7, var8, var9);
   }

   public void submitItem(PoseStack var1, ItemDisplayContext var2, int var3, int var4, int var5, int[] var6, List<BakedQuad> var7, RenderType var8, ItemStackRenderState.FoilType var9) {
      this.order(0).submitItem(var1, var2, var3, var4, var5, var6, var7, var8, var9);
   }

   public void submitCustomGeometry(PoseStack var1, RenderType var2, SubmitNodeCollector.CustomGeometryRenderer var3) {
      this.order(0).submitCustomGeometry(var1, var2, var3);
   }

   public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer var1) {
      this.order(0).submitParticleGroup(var1);
   }

   public void clear() {
      this.submitsPerOrder.values().forEach(SubmitNodeCollection::clear);
   }

   public void endFrame() {
      this.submitsPerOrder.values().removeIf((var0) -> {
         return !var0.wasUsed();
      });
      this.submitsPerOrder.values().forEach(SubmitNodeCollection::endFrame);
   }

   public Int2ObjectAVLTreeMap<SubmitNodeCollection> getSubmitsPerOrder() {
      return this.submitsPerOrder;
   }

   // $FF: synthetic method
   public OrderedSubmitNodeCollector order(final int param1) {
      return this.order(var1);
   }

   public static record CustomGeometrySubmit(PoseStack.Pose pose, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
      public CustomGeometrySubmit(PoseStack.Pose param1, SubmitNodeCollector.CustomGeometryRenderer param2) {
         super();
         this.pose = var1;
         this.customGeometryRenderer = var2;
      }

      public PoseStack.Pose pose() {
         return this.pose;
      }

      public SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer() {
         return this.customGeometryRenderer;
      }
   }

   public static record ItemSubmit(PoseStack.Pose pose, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, RenderType renderType, ItemStackRenderState.FoilType foilType) {
      public ItemSubmit(PoseStack.Pose param1, ItemDisplayContext param2, int param3, int param4, int param5, int[] param6, List<BakedQuad> param7, RenderType param8, ItemStackRenderState.FoilType param9) {
         super();
         this.pose = var1;
         this.displayContext = var2;
         this.lightCoords = var3;
         this.overlayCoords = var4;
         this.outlineColor = var5;
         this.tintLayers = var6;
         this.quads = var7;
         this.renderType = var8;
         this.foilType = var9;
      }

      public PoseStack.Pose pose() {
         return this.pose;
      }

      public ItemDisplayContext displayContext() {
         return this.displayContext;
      }

      public int lightCoords() {
         return this.lightCoords;
      }

      public int overlayCoords() {
         return this.overlayCoords;
      }

      public int outlineColor() {
         return this.outlineColor;
      }

      public int[] tintLayers() {
         return this.tintLayers;
      }

      public List<BakedQuad> quads() {
         return this.quads;
      }

      public RenderType renderType() {
         return this.renderType;
      }

      public ItemStackRenderState.FoilType foilType() {
         return this.foilType;
      }
   }

   public static record BlockModelSubmit(PoseStack.Pose pose, RenderType renderType, BlockStateModel model, float r, float g, float b, int lightCoords, int overlayCoords, int outlineColor) {
      public BlockModelSubmit(PoseStack.Pose param1, RenderType param2, BlockStateModel param3, float param4, float param5, float param6, int param7, int param8, int param9) {
         super();
         this.pose = var1;
         this.renderType = var2;
         this.model = var3;
         this.r = var4;
         this.g = var5;
         this.b = var6;
         this.lightCoords = var7;
         this.overlayCoords = var8;
         this.outlineColor = var9;
      }

      public PoseStack.Pose pose() {
         return this.pose;
      }

      public RenderType renderType() {
         return this.renderType;
      }

      public BlockStateModel model() {
         return this.model;
      }

      public float r() {
         return this.r;
      }

      public float g() {
         return this.g;
      }

      public float b() {
         return this.b;
      }

      public int lightCoords() {
         return this.lightCoords;
      }

      public int overlayCoords() {
         return this.overlayCoords;
      }

      public int outlineColor() {
         return this.outlineColor;
      }
   }

   public static record MovingBlockSubmit(Matrix4f pose, MovingBlockRenderState movingBlockRenderState) {
      public MovingBlockSubmit(Matrix4f param1, MovingBlockRenderState param2) {
         super();
         this.pose = var1;
         this.movingBlockRenderState = var2;
      }

      public Matrix4f pose() {
         return this.pose;
      }

      public MovingBlockRenderState movingBlockRenderState() {
         return this.movingBlockRenderState;
      }
   }

   public static record BlockSubmit(PoseStack.Pose pose, BlockState state, int lightCoords, int overlayCoords, int outlineColor) {
      public BlockSubmit(PoseStack.Pose param1, BlockState param2, int param3, int param4, int param5) {
         super();
         this.pose = var1;
         this.state = var2;
         this.lightCoords = var3;
         this.overlayCoords = var4;
         this.outlineColor = var5;
      }

      public PoseStack.Pose pose() {
         return this.pose;
      }

      public BlockState state() {
         return this.state;
      }

      public int lightCoords() {
         return this.lightCoords;
      }

      public int overlayCoords() {
         return this.overlayCoords;
      }

      public int outlineColor() {
         return this.outlineColor;
      }
   }

   public static record TranslucentModelSubmit<S>(SubmitNodeStorage.ModelSubmit<S> modelSubmit, RenderType renderType, Vector3f position) {
      public TranslucentModelSubmit(SubmitNodeStorage.ModelSubmit<S> param1, RenderType param2, Vector3f param3) {
         super();
         this.modelSubmit = var1;
         this.renderType = var2;
         this.position = var3;
      }

      public SubmitNodeStorage.ModelSubmit<S> modelSubmit() {
         return this.modelSubmit;
      }

      public RenderType renderType() {
         return this.renderType;
      }

      public Vector3f position() {
         return this.position;
      }
   }

   public static record ModelPartSubmit(PoseStack.Pose pose, ModelPart modelPart, int lightCoords, int overlayCoords, @Nullable TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int outlineColor) {
      public ModelPartSubmit(PoseStack.Pose param1, ModelPart param2, int param3, int param4, @Nullable TextureAtlasSprite param5, boolean param6, boolean param7, int param8, @Nullable ModelFeatureRenderer.CrumblingOverlay param9, int param10) {
         super();
         this.pose = var1;
         this.modelPart = var2;
         this.lightCoords = var3;
         this.overlayCoords = var4;
         this.sprite = var5;
         this.sheeted = var6;
         this.hasFoil = var7;
         this.tintedColor = var8;
         this.crumblingOverlay = var9;
         this.outlineColor = var10;
      }

      public PoseStack.Pose pose() {
         return this.pose;
      }

      public ModelPart modelPart() {
         return this.modelPart;
      }

      public int lightCoords() {
         return this.lightCoords;
      }

      public int overlayCoords() {
         return this.overlayCoords;
      }

      @Nullable
      public TextureAtlasSprite sprite() {
         return this.sprite;
      }

      public boolean sheeted() {
         return this.sheeted;
      }

      public boolean hasFoil() {
         return this.hasFoil;
      }

      public int tintedColor() {
         return this.tintedColor;
      }

      @Nullable
      public ModelFeatureRenderer.CrumblingOverlay crumblingOverlay() {
         return this.crumblingOverlay;
      }

      public int outlineColor() {
         return this.outlineColor;
      }
   }

   public static record ModelSubmit<S>(PoseStack.Pose pose, Model<? super S> model, S state, int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
      public ModelSubmit(PoseStack.Pose param1, Model<? super S> param2, S param3, int param4, int param5, int param6, @Nullable TextureAtlasSprite param7, int param8, @Nullable ModelFeatureRenderer.CrumblingOverlay param9) {
         super();
         this.pose = var1;
         this.model = var2;
         this.state = var3;
         this.lightCoords = var4;
         this.overlayCoords = var5;
         this.tintedColor = var6;
         this.sprite = var7;
         this.outlineColor = var8;
         this.crumblingOverlay = var9;
      }

      public PoseStack.Pose pose() {
         return this.pose;
      }

      public Model<? super S> model() {
         return this.model;
      }

      public S state() {
         return this.state;
      }

      public int lightCoords() {
         return this.lightCoords;
      }

      public int overlayCoords() {
         return this.overlayCoords;
      }

      public int tintedColor() {
         return this.tintedColor;
      }

      @Nullable
      public TextureAtlasSprite sprite() {
         return this.sprite;
      }

      public int outlineColor() {
         return this.outlineColor;
      }

      @Nullable
      public ModelFeatureRenderer.CrumblingOverlay crumblingOverlay() {
         return this.crumblingOverlay;
      }
   }

   public static record LeashSubmit(Matrix4f pose, EntityRenderState.LeashState leashState) {
      public LeashSubmit(Matrix4f param1, EntityRenderState.LeashState param2) {
         super();
         this.pose = var1;
         this.leashState = var2;
      }

      public Matrix4f pose() {
         return this.pose;
      }

      public EntityRenderState.LeashState leashState() {
         return this.leashState;
      }
   }

   public static record HitboxSubmit(Matrix4f pose, EntityRenderState entityRenderState, HitboxesRenderState hitboxesRenderState) {
      public HitboxSubmit(Matrix4f param1, EntityRenderState param2, HitboxesRenderState param3) {
         super();
         this.pose = var1;
         this.entityRenderState = var2;
         this.hitboxesRenderState = var3;
      }

      public Matrix4f pose() {
         return this.pose;
      }

      public EntityRenderState entityRenderState() {
         return this.entityRenderState;
      }

      public HitboxesRenderState hitboxesRenderState() {
         return this.hitboxesRenderState;
      }
   }

   public static record TextSubmit(Matrix4f pose, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
      public TextSubmit(Matrix4f param1, float param2, float param3, FormattedCharSequence param4, boolean param5, Font.DisplayMode param6, int param7, int param8, int param9, int param10) {
         super();
         this.pose = var1;
         this.x = var2;
         this.y = var3;
         this.string = var4;
         this.dropShadow = var5;
         this.displayMode = var6;
         this.lightCoords = var7;
         this.color = var8;
         this.backgroundColor = var9;
         this.outlineColor = var10;
      }

      public Matrix4f pose() {
         return this.pose;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }

      public FormattedCharSequence string() {
         return this.string;
      }

      public boolean dropShadow() {
         return this.dropShadow;
      }

      public Font.DisplayMode displayMode() {
         return this.displayMode;
      }

      public int lightCoords() {
         return this.lightCoords;
      }

      public int color() {
         return this.color;
      }

      public int backgroundColor() {
         return this.backgroundColor;
      }

      public int outlineColor() {
         return this.outlineColor;
      }
   }

   public static record NameTagSubmit(Matrix4f pose, float x, float y, Component text, int lightCoords, int color, int backgroundColor, double distanceToCameraSq) {
      public NameTagSubmit(Matrix4f param1, float param2, float param3, Component param4, int param5, int param6, int param7, double param8) {
         super();
         this.pose = var1;
         this.x = var2;
         this.y = var3;
         this.text = var4;
         this.lightCoords = var5;
         this.color = var6;
         this.backgroundColor = var7;
         this.distanceToCameraSq = var8;
      }

      public Matrix4f pose() {
         return this.pose;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }

      public Component text() {
         return this.text;
      }

      public int lightCoords() {
         return this.lightCoords;
      }

      public int color() {
         return this.color;
      }

      public int backgroundColor() {
         return this.backgroundColor;
      }

      public double distanceToCameraSq() {
         return this.distanceToCameraSq;
      }
   }

   public static record FlameSubmit(PoseStack.Pose pose, EntityRenderState entityRenderState, Quaternionf rotation) {
      public FlameSubmit(PoseStack.Pose param1, EntityRenderState param2, Quaternionf param3) {
         super();
         this.pose = var1;
         this.entityRenderState = var2;
         this.rotation = var3;
      }

      public PoseStack.Pose pose() {
         return this.pose;
      }

      public EntityRenderState entityRenderState() {
         return this.entityRenderState;
      }

      public Quaternionf rotation() {
         return this.rotation;
      }
   }

   public static record ShadowSubmit(Matrix4f pose, float radius, List<EntityRenderState.ShadowPiece> pieces) {
      public ShadowSubmit(Matrix4f param1, float param2, List<EntityRenderState.ShadowPiece> param3) {
         super();
         this.pose = var1;
         this.radius = var2;
         this.pieces = var3;
      }

      public Matrix4f pose() {
         return this.pose;
      }

      public float radius() {
         return this.radius;
      }

      public List<EntityRenderState.ShadowPiece> pieces() {
         return this.pieces;
      }
   }
}
