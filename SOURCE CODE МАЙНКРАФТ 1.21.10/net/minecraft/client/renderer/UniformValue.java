package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

public interface UniformValue {
   Codec<UniformValue> CODEC = UniformValue.Type.CODEC.dispatch(UniformValue::type, (var0) -> {
      return var0.valueCodec;
   });

   void writeTo(Std140Builder var1);

   void addSize(Std140SizeCalculator var1);

   UniformValue.Type type();

   public static enum Type implements StringRepresentable {
      INT("int", UniformValue.IntUniform.CODEC),
      IVEC3("ivec3", UniformValue.IVec3Uniform.CODEC),
      FLOAT("float", UniformValue.FloatUniform.CODEC),
      VEC2("vec2", UniformValue.Vec2Uniform.CODEC),
      VEC3("vec3", UniformValue.Vec3Uniform.CODEC),
      VEC4("vec4", UniformValue.Vec4Uniform.CODEC),
      MATRIX4X4("matrix4x4", UniformValue.Matrix4x4Uniform.CODEC);

      public static final StringRepresentable.EnumCodec<UniformValue.Type> CODEC = StringRepresentable.fromEnum(UniformValue.Type::values);
      private final String name;
      final MapCodec<? extends UniformValue> valueCodec;

      private Type(final String param3, final Codec<? extends UniformValue> param4) {
         this.name = var3;
         this.valueCodec = var4.fieldOf("value");
      }

      public String getSerializedName() {
         return this.name;
      }

      // $FF: synthetic method
      private static UniformValue.Type[] $values() {
         return new UniformValue.Type[]{INT, IVEC3, FLOAT, VEC2, VEC3, VEC4, MATRIX4X4};
      }
   }

   public static record Matrix4x4Uniform(Matrix4fc value) implements UniformValue {
      public static final Codec<UniformValue.Matrix4x4Uniform> CODEC;

      public Matrix4x4Uniform(Matrix4fc param1) {
         super();
         this.value = var1;
      }

      public void writeTo(Std140Builder var1) {
         var1.putMat4f(this.value);
      }

      public void addSize(Std140SizeCalculator var1) {
         var1.putMat4f();
      }

      public UniformValue.Type type() {
         return UniformValue.Type.MATRIX4X4;
      }

      public Matrix4fc value() {
         return this.value;
      }

      static {
         CODEC = ExtraCodecs.MATRIX4F.xmap(UniformValue.Matrix4x4Uniform::new, UniformValue.Matrix4x4Uniform::value);
      }
   }

   public static record Vec4Uniform(Vector4f value) implements UniformValue {
      public static final Codec<UniformValue.Vec4Uniform> CODEC;

      public Vec4Uniform(Vector4f param1) {
         super();
         this.value = var1;
      }

      public void writeTo(Std140Builder var1) {
         var1.putVec4(this.value);
      }

      public void addSize(Std140SizeCalculator var1) {
         var1.putVec4();
      }

      public UniformValue.Type type() {
         return UniformValue.Type.VEC4;
      }

      public Vector4f value() {
         return this.value;
      }

      static {
         CODEC = ExtraCodecs.VECTOR4F.xmap(UniformValue.Vec4Uniform::new, UniformValue.Vec4Uniform::value);
      }
   }

   public static record Vec3Uniform(Vector3f value) implements UniformValue {
      public static final Codec<UniformValue.Vec3Uniform> CODEC;

      public Vec3Uniform(Vector3f param1) {
         super();
         this.value = var1;
      }

      public void writeTo(Std140Builder var1) {
         var1.putVec3(this.value);
      }

      public void addSize(Std140SizeCalculator var1) {
         var1.putVec3();
      }

      public UniformValue.Type type() {
         return UniformValue.Type.VEC3;
      }

      public Vector3f value() {
         return this.value;
      }

      static {
         CODEC = ExtraCodecs.VECTOR3F.xmap(UniformValue.Vec3Uniform::new, UniformValue.Vec3Uniform::value);
      }
   }

   public static record Vec2Uniform(Vector2f value) implements UniformValue {
      public static final Codec<UniformValue.Vec2Uniform> CODEC;

      public Vec2Uniform(Vector2f param1) {
         super();
         this.value = var1;
      }

      public void writeTo(Std140Builder var1) {
         var1.putVec2(this.value);
      }

      public void addSize(Std140SizeCalculator var1) {
         var1.putVec2();
      }

      public UniformValue.Type type() {
         return UniformValue.Type.VEC2;
      }

      public Vector2f value() {
         return this.value;
      }

      static {
         CODEC = ExtraCodecs.VECTOR2F.xmap(UniformValue.Vec2Uniform::new, UniformValue.Vec2Uniform::value);
      }
   }

   public static record FloatUniform(float value) implements UniformValue {
      public static final Codec<UniformValue.FloatUniform> CODEC;

      public FloatUniform(float param1) {
         super();
         this.value = var1;
      }

      public void writeTo(Std140Builder var1) {
         var1.putFloat(this.value);
      }

      public void addSize(Std140SizeCalculator var1) {
         var1.putFloat();
      }

      public UniformValue.Type type() {
         return UniformValue.Type.FLOAT;
      }

      public float value() {
         return this.value;
      }

      static {
         CODEC = Codec.FLOAT.xmap(UniformValue.FloatUniform::new, UniformValue.FloatUniform::value);
      }
   }

   public static record IVec3Uniform(Vector3i value) implements UniformValue {
      public static final Codec<UniformValue.IVec3Uniform> CODEC;

      public IVec3Uniform(Vector3i param1) {
         super();
         this.value = var1;
      }

      public void writeTo(Std140Builder var1) {
         var1.putIVec3(this.value);
      }

      public void addSize(Std140SizeCalculator var1) {
         var1.putIVec3();
      }

      public UniformValue.Type type() {
         return UniformValue.Type.IVEC3;
      }

      public Vector3i value() {
         return this.value;
      }

      static {
         CODEC = ExtraCodecs.VECTOR3I.xmap(UniformValue.IVec3Uniform::new, UniformValue.IVec3Uniform::value);
      }
   }

   public static record IntUniform(int value) implements UniformValue {
      public static final Codec<UniformValue.IntUniform> CODEC;

      public IntUniform(int param1) {
         super();
         this.value = var1;
      }

      public void writeTo(Std140Builder var1) {
         var1.putInt(this.value);
      }

      public void addSize(Std140SizeCalculator var1) {
         var1.putInt();
      }

      public UniformValue.Type type() {
         return UniformValue.Type.INT;
      }

      public int value() {
         return this.value;
      }

      static {
         CODEC = Codec.INT.xmap(UniformValue.IntUniform::new, UniformValue.IntUniform::value);
      }
   }
}
