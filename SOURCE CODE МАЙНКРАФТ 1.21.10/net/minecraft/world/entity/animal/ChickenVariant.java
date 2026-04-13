package net.minecraft.world.entity.animal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnCondition;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;

public record ChickenVariant(ModelAndTexture<ChickenVariant.ModelType> modelAndTexture, SpawnPrioritySelectors spawnConditions) implements PriorityProvider<SpawnContext, SpawnCondition> {
   public static final Codec<ChickenVariant> DIRECT_CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(ModelAndTexture.codec(ChickenVariant.ModelType.CODEC, ChickenVariant.ModelType.NORMAL).forGetter(ChickenVariant::modelAndTexture), SpawnPrioritySelectors.CODEC.fieldOf("spawn_conditions").forGetter(ChickenVariant::spawnConditions)).apply(var0, ChickenVariant::new);
   });
   public static final Codec<ChickenVariant> NETWORK_CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(ModelAndTexture.codec(ChickenVariant.ModelType.CODEC, ChickenVariant.ModelType.NORMAL).forGetter(ChickenVariant::modelAndTexture)).apply(var0, ChickenVariant::new);
   });
   public static final Codec<Holder<ChickenVariant>> CODEC;
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<ChickenVariant>> STREAM_CODEC;

   private ChickenVariant(ModelAndTexture<ChickenVariant.ModelType> var1) {
      this(var1, SpawnPrioritySelectors.EMPTY);
   }

   public ChickenVariant(ModelAndTexture<ChickenVariant.ModelType> param1, SpawnPrioritySelectors param2) {
      super();
      this.modelAndTexture = var1;
      this.spawnConditions = var2;
   }

   public List<PriorityProvider.Selector<SpawnContext, SpawnCondition>> selectors() {
      return this.spawnConditions.selectors();
   }

   public ModelAndTexture<ChickenVariant.ModelType> modelAndTexture() {
      return this.modelAndTexture;
   }

   public SpawnPrioritySelectors spawnConditions() {
      return this.spawnConditions;
   }

   static {
      CODEC = RegistryFixedCodec.create(Registries.CHICKEN_VARIANT);
      STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.CHICKEN_VARIANT);
   }

   public static enum ModelType implements StringRepresentable {
      NORMAL("normal"),
      COLD("cold");

      public static final Codec<ChickenVariant.ModelType> CODEC = StringRepresentable.fromEnum(ChickenVariant.ModelType::values);
      private final String name;

      private ModelType(final String param3) {
         this.name = var3;
      }

      public String getSerializedName() {
         return this.name;
      }

      // $FF: synthetic method
      private static ChickenVariant.ModelType[] $values() {
         return new ChickenVariant.ModelType[]{NORMAL, COLD};
      }
   }
}
