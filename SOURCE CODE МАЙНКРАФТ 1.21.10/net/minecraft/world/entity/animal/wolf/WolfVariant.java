package net.minecraft.world.entity.animal.wolf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnCondition;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;

public record WolfVariant(WolfVariant.AssetInfo assetInfo, SpawnPrioritySelectors spawnConditions) implements PriorityProvider<SpawnContext, SpawnCondition> {
   public static final Codec<WolfVariant> DIRECT_CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(WolfVariant.AssetInfo.CODEC.fieldOf("assets").forGetter(WolfVariant::assetInfo), SpawnPrioritySelectors.CODEC.fieldOf("spawn_conditions").forGetter(WolfVariant::spawnConditions)).apply(var0, WolfVariant::new);
   });
   public static final Codec<WolfVariant> NETWORK_CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(WolfVariant.AssetInfo.CODEC.fieldOf("assets").forGetter(WolfVariant::assetInfo)).apply(var0, WolfVariant::new);
   });
   public static final Codec<Holder<WolfVariant>> CODEC;
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<WolfVariant>> STREAM_CODEC;

   private WolfVariant(WolfVariant.AssetInfo var1) {
      this(var1, SpawnPrioritySelectors.EMPTY);
   }

   public WolfVariant(WolfVariant.AssetInfo param1, SpawnPrioritySelectors param2) {
      super();
      this.assetInfo = var1;
      this.spawnConditions = var2;
   }

   public List<PriorityProvider.Selector<SpawnContext, SpawnCondition>> selectors() {
      return this.spawnConditions.selectors();
   }

   public WolfVariant.AssetInfo assetInfo() {
      return this.assetInfo;
   }

   public SpawnPrioritySelectors spawnConditions() {
      return this.spawnConditions;
   }

   static {
      CODEC = RegistryFixedCodec.create(Registries.WOLF_VARIANT);
      STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.WOLF_VARIANT);
   }

   public static record AssetInfo(ClientAsset.ResourceTexture wild, ClientAsset.ResourceTexture tame, ClientAsset.ResourceTexture angry) {
      public static final Codec<WolfVariant.AssetInfo> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(ClientAsset.ResourceTexture.CODEC.fieldOf("wild").forGetter(WolfVariant.AssetInfo::wild), ClientAsset.ResourceTexture.CODEC.fieldOf("tame").forGetter(WolfVariant.AssetInfo::tame), ClientAsset.ResourceTexture.CODEC.fieldOf("angry").forGetter(WolfVariant.AssetInfo::angry)).apply(var0, WolfVariant.AssetInfo::new);
      });

      public AssetInfo(ClientAsset.ResourceTexture param1, ClientAsset.ResourceTexture param2, ClientAsset.ResourceTexture param3) {
         super();
         this.wild = var1;
         this.tame = var2;
         this.angry = var3;
      }

      public ClientAsset.ResourceTexture wild() {
         return this.wild;
      }

      public ClientAsset.ResourceTexture tame() {
         return this.tame;
      }

      public ClientAsset.ResourceTexture angry() {
         return this.angry;
      }
   }
}
