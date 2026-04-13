package net.minecraft.client.renderer.block.model;

import com.mojang.math.Quadrant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.ResourceLocation;

public record Variant(ResourceLocation modelLocation, Variant.SimpleModelState modelState) implements BlockModelPart.Unbaked {
   public static final MapCodec<Variant> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(ResourceLocation.CODEC.fieldOf("model").forGetter(Variant::modelLocation), Variant.SimpleModelState.MAP_CODEC.forGetter(Variant::modelState)).apply(var0, Variant::new);
   });
   public static final Codec<Variant> CODEC;

   public Variant(ResourceLocation var1) {
      this(var1, Variant.SimpleModelState.DEFAULT);
   }

   public Variant(ResourceLocation param1, Variant.SimpleModelState param2) {
      super();
      this.modelLocation = var1;
      this.modelState = var2;
   }

   public Variant withXRot(Quadrant var1) {
      return this.withState(this.modelState.withX(var1));
   }

   public Variant withYRot(Quadrant var1) {
      return this.withState(this.modelState.withY(var1));
   }

   public Variant withUvLock(boolean var1) {
      return this.withState(this.modelState.withUvLock(var1));
   }

   public Variant withModel(ResourceLocation var1) {
      return new Variant(var1, this.modelState);
   }

   public Variant withState(Variant.SimpleModelState var1) {
      return new Variant(this.modelLocation, var1);
   }

   public Variant with(VariantMutator var1) {
      return (Variant)var1.apply(this);
   }

   public BlockModelPart bake(ModelBaker var1) {
      return SimpleModelWrapper.bake(var1, this.modelLocation, this.modelState.asModelState());
   }

   public void resolveDependencies(ResolvableModel.Resolver var1) {
      var1.markDependency(this.modelLocation);
   }

   public ResourceLocation modelLocation() {
      return this.modelLocation;
   }

   public Variant.SimpleModelState modelState() {
      return this.modelState;
   }

   static {
      CODEC = MAP_CODEC.codec();
   }

   public static record SimpleModelState(Quadrant x, Quadrant y, boolean uvLock) {
      public static final MapCodec<Variant.SimpleModelState> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Quadrant.CODEC.optionalFieldOf("x", Quadrant.R0).forGetter(Variant.SimpleModelState::x), Quadrant.CODEC.optionalFieldOf("y", Quadrant.R0).forGetter(Variant.SimpleModelState::y), Codec.BOOL.optionalFieldOf("uvlock", false).forGetter(Variant.SimpleModelState::uvLock)).apply(var0, Variant.SimpleModelState::new);
      });
      public static final Variant.SimpleModelState DEFAULT;

      public SimpleModelState(Quadrant param1, Quadrant param2, boolean param3) {
         super();
         this.x = var1;
         this.y = var2;
         this.uvLock = var3;
      }

      public ModelState asModelState() {
         BlockModelRotation var1 = BlockModelRotation.by(this.x, this.y);
         return (ModelState)(this.uvLock ? var1.withUvLock() : var1);
      }

      public Variant.SimpleModelState withX(Quadrant var1) {
         return new Variant.SimpleModelState(var1, this.y, this.uvLock);
      }

      public Variant.SimpleModelState withY(Quadrant var1) {
         return new Variant.SimpleModelState(this.x, var1, this.uvLock);
      }

      public Variant.SimpleModelState withUvLock(boolean var1) {
         return new Variant.SimpleModelState(this.x, this.y, var1);
      }

      public Quadrant x() {
         return this.x;
      }

      public Quadrant y() {
         return this.y;
      }

      public boolean uvLock() {
         return this.uvLock;
      }

      static {
         DEFAULT = new Variant.SimpleModelState(Quadrant.R0, Quadrant.R0, false);
      }
   }
}
