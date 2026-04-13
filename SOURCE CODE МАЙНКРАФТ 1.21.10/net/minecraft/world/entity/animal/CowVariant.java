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

public record CowVariant(ModelAndTexture<CowVariant.ModelType> modelAndTexture, SpawnPrioritySelectors spawnConditions) implements PriorityProvider<SpawnContext, SpawnCondition> {
   public static final Codec<CowVariant> DIRECT_CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(ModelAndTexture.codec(CowVariant.ModelType.CODEC, CowVariant.ModelType.NORMAL).forGetter(CowVariant::modelAndTexture), SpawnPrioritySelectors.CODEC.fieldOf("spawn_conditions").forGetter(CowVariant::spawnConditions)).apply(var0, CowVariant::new);
   });
   public static final Codec<CowVariant> NETWORK_CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(ModelAndTexture.codec(CowVariant.ModelType.CODEC, CowVariant.ModelType.NORMAL).forGetter(CowVariant::modelAndTexture)).apply(var0, CowVariant::new);
   });
   public static final Codec<Holder<CowVariant>> CODEC;
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<CowVariant>> STREAM_CODEC;

   private CowVariant(ModelAndTexture<CowVariant.ModelType> var1) {
      this(var1, SpawnPrioritySelectors.EMPTY);
   }

   public CowVariant(ModelAndTexture<CowVariant.ModelType> param1, SpawnPrioritySelectors param2) {
      super();
      this.modelAndTexture = var1;
      this.spawnConditions = var2;
   }

   public List<PriorityProvider.Selector<SpawnContext, SpawnCondition>> selectors() {
      return this.spawnConditions.selectors();
   }

   public ModelAndTexture<CowVariant.ModelType> modelAndTexture() {
      return this.modelAndTexture;
   }

   public SpawnPrioritySelectors spawnConditions() {
      return this.spawnConditions;
   }

   static {
      CODEC = RegistryFixedCodec.create(Registries.COW_VARIANT);
      STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.COW_VARIANT);
   }

   public static enum ModelType implements StringRepresentable {
      NORMAL("normal"),
      COLD("cold"),
      WARM("warm");

      public static final Codec<CowVariant.ModelType> CODEC = StringRepresentable.fromEnum(CowVariant.ModelType::values);
      private final String name;

      private ModelType(final String param3) {
         this.name = var3;
      }

      public String getSerializedName() {
         return this.name;
      }

      // $FF: synthetic method
      private static CowVariant.ModelType[] $values() {
         return new CowVariant.ModelType[]{NORMAL, COLD, WARM};
      }
   }
}
