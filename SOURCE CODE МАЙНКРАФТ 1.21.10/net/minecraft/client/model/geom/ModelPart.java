package net.minecraft.client.model.geom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class ModelPart {
   public static final float DEFAULT_SCALE = 1.0F;
   public float x;
   public float y;
   public float z;
   public float xRot;
   public float yRot;
   public float zRot;
   public float xScale = 1.0F;
   public float yScale = 1.0F;
   public float zScale = 1.0F;
   public boolean visible = true;
   public boolean skipDraw;
   private final List<ModelPart.Cube> cubes;
   private final Map<String, ModelPart> children;
   private PartPose initialPose;

   public ModelPart(List<ModelPart.Cube> var1, Map<String, ModelPart> var2) {
      super();
      this.initialPose = PartPose.ZERO;
      this.cubes = var1;
      this.children = var2;
   }

   public PartPose storePose() {
      return PartPose.offsetAndRotation(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot);
   }

   public PartPose getInitialPose() {
      return this.initialPose;
   }

   public void setInitialPose(PartPose var1) {
      this.initialPose = var1;
   }

   public void resetPose() {
      this.loadPose(this.initialPose);
   }

   public void loadPose(PartPose var1) {
      this.x = var1.x();
      this.y = var1.y();
      this.z = var1.z();
      this.xRot = var1.xRot();
      this.yRot = var1.yRot();
      this.zRot = var1.zRot();
      this.xScale = var1.xScale();
      this.yScale = var1.yScale();
      this.zScale = var1.zScale();
   }

   public boolean hasChild(String var1) {
      return this.children.containsKey(var1);
   }

   public ModelPart getChild(String var1) {
      ModelPart var2 = (ModelPart)this.children.get(var1);
      if (var2 == null) {
         throw new NoSuchElementException("Can't find part " + var1);
      } else {
         return var2;
      }
   }

   public void setPos(float var1, float var2, float var3) {
      this.x = var1;
      this.y = var2;
      this.z = var3;
   }

   public void setRotation(float var1, float var2, float var3) {
      this.xRot = var1;
      this.yRot = var2;
      this.zRot = var3;
   }

   public void render(PoseStack var1, VertexConsumer var2, int var3, int var4) {
      this.render(var1, var2, var3, var4, -1);
   }

   public void render(PoseStack var1, VertexConsumer var2, int var3, int var4, int var5) {
      if (this.visible) {
         if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
            var1.pushPose();
            this.translateAndRotate(var1);
            if (!this.skipDraw) {
               this.compile(var1.last(), var2, var3, var4, var5);
            }

            Iterator var6 = this.children.values().iterator();

            while(var6.hasNext()) {
               ModelPart var7 = (ModelPart)var6.next();
               var7.render(var1, var2, var3, var4, var5);
            }

            var1.popPose();
         }
      }
   }

   public void rotateBy(Quaternionf var1) {
      Matrix3f var2 = (new Matrix3f()).rotationZYX(this.zRot, this.yRot, this.xRot);
      Matrix3f var3 = var2.rotate(var1);
      Vector3f var4 = var3.getEulerAnglesZYX(new Vector3f());
      this.setRotation(var4.x, var4.y, var4.z);
   }

   public void getExtentsForGui(PoseStack var1, Set<Vector3f> var2) {
      this.visit(var1, (var1x, var2x, var3, var4) -> {
         ModelPart.Polygon[] var5 = var4.polygons;
         int var6 = var5.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            ModelPart.Polygon var8 = var5[var7];
            ModelPart.Vertex[] var9 = var8.vertices();
            int var10 = var9.length;

            for(int var11 = 0; var11 < var10; ++var11) {
               ModelPart.Vertex var12 = var9[var11];
               float var13 = var12.worldX();
               float var14 = var12.worldY();
               float var15 = var12.worldZ();
               Vector3f var16 = var1x.pose().transformPosition(var13, var14, var15, new Vector3f());
               var2.add(var16);
            }
         }

      });
   }

   public void visit(PoseStack var1, ModelPart.Visitor var2) {
      this.visit(var1, var2, "");
   }

   private void visit(PoseStack var1, ModelPart.Visitor var2, String var3) {
      if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
         var1.pushPose();
         this.translateAndRotate(var1);
         PoseStack.Pose var4 = var1.last();

         for(int var5 = 0; var5 < this.cubes.size(); ++var5) {
            var2.visit(var4, var3, var5, (ModelPart.Cube)this.cubes.get(var5));
         }

         String var6 = var3 + "/";
         this.children.forEach((var3x, var4x) -> {
            var4x.visit(var1, var2, var6 + var3x);
         });
         var1.popPose();
      }
   }

   public void translateAndRotate(PoseStack var1) {
      var1.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
      if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
         var1.mulPose((Quaternionfc)(new Quaternionf()).rotationZYX(this.zRot, this.yRot, this.xRot));
      }

      if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
         var1.scale(this.xScale, this.yScale, this.zScale);
      }

   }

   private void compile(PoseStack.Pose var1, VertexConsumer var2, int var3, int var4, int var5) {
      Iterator var6 = this.cubes.iterator();

      while(var6.hasNext()) {
         ModelPart.Cube var7 = (ModelPart.Cube)var6.next();
         var7.compile(var1, var2, var3, var4, var5);
      }

   }

   public ModelPart.Cube getRandomCube(RandomSource var1) {
      return (ModelPart.Cube)this.cubes.get(var1.nextInt(this.cubes.size()));
   }

   public boolean isEmpty() {
      return this.cubes.isEmpty();
   }

   public void offsetPos(Vector3f var1) {
      this.x += var1.x();
      this.y += var1.y();
      this.z += var1.z();
   }

   public void offsetRotation(Vector3f var1) {
      this.xRot += var1.x();
      this.yRot += var1.y();
      this.zRot += var1.z();
   }

   public void offsetScale(Vector3f var1) {
      this.xScale += var1.x();
      this.yScale += var1.y();
      this.zScale += var1.z();
   }

   public List<ModelPart> getAllParts() {
      ArrayList var1 = new ArrayList();
      var1.add(this);
      this.addAllChildren((var1x, var2) -> {
         var1.add(var2);
      });
      return List.copyOf(var1);
   }

   public Function<String, ModelPart> createPartLookup() {
      HashMap var1 = new HashMap();
      var1.put("root", this);
      Objects.requireNonNull(var1);
      this.addAllChildren(var1::putIfAbsent);
      Objects.requireNonNull(var1);
      return var1::get;
   }

   private void addAllChildren(BiConsumer<String, ModelPart> var1) {
      Iterator var2 = this.children.entrySet().iterator();

      while(var2.hasNext()) {
         Entry var3 = (Entry)var2.next();
         var1.accept((String)var3.getKey(), (ModelPart)var3.getValue());
      }

      var2 = this.children.values().iterator();

      while(var2.hasNext()) {
         ModelPart var4 = (ModelPart)var2.next();
         var4.addAllChildren(var1);
      }

   }

   @FunctionalInterface
   public interface Visitor {
      void visit(PoseStack.Pose var1, String var2, int var3, ModelPart.Cube var4);
   }

   public static class Cube {
      public final ModelPart.Polygon[] polygons;
      public final float minX;
      public final float minY;
      public final float minZ;
      public final float maxX;
      public final float maxY;
      public final float maxZ;

      public Cube(int var1, int var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, boolean var12, float var13, float var14, Set<Direction> var15) {
         super();
         this.minX = var3;
         this.minY = var4;
         this.minZ = var5;
         this.maxX = var3 + var6;
         this.maxY = var4 + var7;
         this.maxZ = var5 + var8;
         this.polygons = new ModelPart.Polygon[var15.size()];
         float var16 = var3 + var6;
         float var17 = var4 + var7;
         float var18 = var5 + var8;
         var3 -= var9;
         var4 -= var10;
         var5 -= var11;
         var16 += var9;
         var17 += var10;
         var18 += var11;
         if (var12) {
            float var19 = var16;
            var16 = var3;
            var3 = var19;
         }

         ModelPart.Vertex var37 = new ModelPart.Vertex(var3, var4, var5, 0.0F, 0.0F);
         ModelPart.Vertex var20 = new ModelPart.Vertex(var16, var4, var5, 0.0F, 8.0F);
         ModelPart.Vertex var21 = new ModelPart.Vertex(var16, var17, var5, 8.0F, 8.0F);
         ModelPart.Vertex var22 = new ModelPart.Vertex(var3, var17, var5, 8.0F, 0.0F);
         ModelPart.Vertex var23 = new ModelPart.Vertex(var3, var4, var18, 0.0F, 0.0F);
         ModelPart.Vertex var24 = new ModelPart.Vertex(var16, var4, var18, 0.0F, 8.0F);
         ModelPart.Vertex var25 = new ModelPart.Vertex(var16, var17, var18, 8.0F, 8.0F);
         ModelPart.Vertex var26 = new ModelPart.Vertex(var3, var17, var18, 8.0F, 0.0F);
         float var27 = (float)var1;
         float var28 = (float)var1 + var8;
         float var29 = (float)var1 + var8 + var6;
         float var30 = (float)var1 + var8 + var6 + var6;
         float var31 = (float)var1 + var8 + var6 + var8;
         float var32 = (float)var1 + var8 + var6 + var8 + var6;
         float var33 = (float)var2;
         float var34 = (float)var2 + var8;
         float var35 = (float)var2 + var8 + var7;
         int var36 = 0;
         if (var15.contains(Direction.DOWN)) {
            this.polygons[var36++] = new ModelPart.Polygon(new ModelPart.Vertex[]{var24, var23, var37, var20}, var28, var33, var29, var34, var13, var14, var12, Direction.DOWN);
         }

         if (var15.contains(Direction.UP)) {
            this.polygons[var36++] = new ModelPart.Polygon(new ModelPart.Vertex[]{var21, var22, var26, var25}, var29, var34, var30, var33, var13, var14, var12, Direction.UP);
         }

         if (var15.contains(Direction.WEST)) {
            this.polygons[var36++] = new ModelPart.Polygon(new ModelPart.Vertex[]{var37, var23, var26, var22}, var27, var34, var28, var35, var13, var14, var12, Direction.WEST);
         }

         if (var15.contains(Direction.NORTH)) {
            this.polygons[var36++] = new ModelPart.Polygon(new ModelPart.Vertex[]{var20, var37, var22, var21}, var28, var34, var29, var35, var13, var14, var12, Direction.NORTH);
         }

         if (var15.contains(Direction.EAST)) {
            this.polygons[var36++] = new ModelPart.Polygon(new ModelPart.Vertex[]{var24, var20, var21, var25}, var29, var34, var31, var35, var13, var14, var12, Direction.EAST);
         }

         if (var15.contains(Direction.SOUTH)) {
            this.polygons[var36] = new ModelPart.Polygon(new ModelPart.Vertex[]{var23, var24, var25, var26}, var31, var34, var32, var35, var13, var14, var12, Direction.SOUTH);
         }

      }

      public void compile(PoseStack.Pose var1, VertexConsumer var2, int var3, int var4, int var5) {
         Matrix4f var6 = var1.pose();
         Vector3f var7 = new Vector3f();
         ModelPart.Polygon[] var8 = this.polygons;
         int var9 = var8.length;

         for(int var10 = 0; var10 < var9; ++var10) {
            ModelPart.Polygon var11 = var8[var10];
            Vector3f var12 = var1.transformNormal(var11.normal, var7);
            float var13 = var12.x();
            float var14 = var12.y();
            float var15 = var12.z();
            ModelPart.Vertex[] var16 = var11.vertices;
            int var17 = var16.length;

            for(int var18 = 0; var18 < var17; ++var18) {
               ModelPart.Vertex var19 = var16[var18];
               float var20 = var19.worldX();
               float var21 = var19.worldY();
               float var22 = var19.worldZ();
               Vector3f var23 = var6.transformPosition(var20, var21, var22, var7);
               var2.addVertex(var23.x(), var23.y(), var23.z(), var5, var19.u, var19.v, var4, var3, var13, var14, var15);
            }
         }

      }
   }

   public static record Polygon(ModelPart.Vertex[] vertices, Vector3fc normal) {
      final ModelPart.Vertex[] vertices;
      final Vector3fc normal;

      public Polygon(ModelPart.Vertex[] var1, float var2, float var3, float var4, float var5, float var6, float var7, boolean var8, Direction var9) {
         this(var1, (var8 ? mirrorFacing(var9) : var9).getUnitVec3f());
         float var10 = 0.0F / var6;
         float var11 = 0.0F / var7;
         var1[0] = var1[0].remap(var4 / var6 - var10, var3 / var7 + var11);
         var1[1] = var1[1].remap(var2 / var6 + var10, var3 / var7 + var11);
         var1[2] = var1[2].remap(var2 / var6 + var10, var5 / var7 - var11);
         var1[3] = var1[3].remap(var4 / var6 - var10, var5 / var7 - var11);
         if (var8) {
            int var12 = var1.length;

            for(int var13 = 0; var13 < var12 / 2; ++var13) {
               ModelPart.Vertex var14 = var1[var13];
               var1[var13] = var1[var12 - 1 - var13];
               var1[var12 - 1 - var13] = var14;
            }
         }

      }

      public Polygon(ModelPart.Vertex[] param1, Vector3fc param2) {
         super();
         this.vertices = var1;
         this.normal = var2;
      }

      private static Direction mirrorFacing(Direction var0) {
         return var0.getAxis() == Direction.Axis.X ? var0.getOpposite() : var0;
      }

      public ModelPart.Vertex[] vertices() {
         return this.vertices;
      }

      public Vector3fc normal() {
         return this.normal;
      }
   }

   public static record Vertex(float x, float y, float z, float u, float v) {
      final float u;
      final float v;
      public static final float SCALE_FACTOR = 16.0F;

      public Vertex(float param1, float param2, float param3, float param4, float param5) {
         super();
         this.x = var1;
         this.y = var2;
         this.z = var3;
         this.u = var4;
         this.v = var5;
      }

      public ModelPart.Vertex remap(float var1, float var2) {
         return new ModelPart.Vertex(this.x, this.y, this.z, var1, var2);
      }

      public float worldX() {
         return this.x / 16.0F;
      }

      public float worldY() {
         return this.y / 16.0F;
      }

      public float worldZ() {
         return this.z / 16.0F;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }

      public float z() {
         return this.z;
      }

      public float u() {
         return this.u;
      }

      public float v() {
         return this.v;
      }
   }
}
