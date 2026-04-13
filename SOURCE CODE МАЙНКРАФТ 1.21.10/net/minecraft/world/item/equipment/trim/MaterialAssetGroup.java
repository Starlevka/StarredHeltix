package net.minecraft.world.item.equipment.trim;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public record MaterialAssetGroup(MaterialAssetGroup.AssetInfo base, Map<ResourceKey<EquipmentAsset>, MaterialAssetGroup.AssetInfo> overrides) {
   public static final String SEPARATOR = "_";
   public static final MapCodec<MaterialAssetGroup> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(MaterialAssetGroup.AssetInfo.CODEC.fieldOf("asset_name").forGetter(MaterialAssetGroup::base), Codec.unboundedMap(ResourceKey.codec(EquipmentAssets.ROOT_ID), MaterialAssetGroup.AssetInfo.CODEC).optionalFieldOf("override_armor_assets", Map.of()).forGetter(MaterialAssetGroup::overrides)).apply(var0, MaterialAssetGroup::new);
   });
   public static final StreamCodec<ByteBuf, MaterialAssetGroup> STREAM_CODEC;
   public static final MaterialAssetGroup QUARTZ;
   public static final MaterialAssetGroup IRON;
   public static final MaterialAssetGroup NETHERITE;
   public static final MaterialAssetGroup REDSTONE;
   public static final MaterialAssetGroup COPPER;
   public static final MaterialAssetGroup GOLD;
   public static final MaterialAssetGroup EMERALD;
   public static final MaterialAssetGroup DIAMOND;
   public static final MaterialAssetGroup LAPIS;
   public static final MaterialAssetGroup AMETHYST;
   public static final MaterialAssetGroup RESIN;

   public MaterialAssetGroup(MaterialAssetGroup.AssetInfo param1, Map<ResourceKey<EquipmentAsset>, MaterialAssetGroup.AssetInfo> param2) {
      super();
      this.base = var1;
      this.overrides = var2;
   }

   public static MaterialAssetGroup create(String var0) {
      return new MaterialAssetGroup(new MaterialAssetGroup.AssetInfo(var0), Map.of());
   }

   public static MaterialAssetGroup create(String var0, Map<ResourceKey<EquipmentAsset>, String> var1) {
      return new MaterialAssetGroup(new MaterialAssetGroup.AssetInfo(var0), Map.copyOf(Maps.transformValues(var1, MaterialAssetGroup.AssetInfo::new)));
   }

   public MaterialAssetGroup.AssetInfo assetId(ResourceKey<EquipmentAsset> var1) {
      return (MaterialAssetGroup.AssetInfo)this.overrides.getOrDefault(var1, this.base);
   }

   public MaterialAssetGroup.AssetInfo base() {
      return this.base;
   }

   public Map<ResourceKey<EquipmentAsset>, MaterialAssetGroup.AssetInfo> overrides() {
      return this.overrides;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(MaterialAssetGroup.AssetInfo.STREAM_CODEC, MaterialAssetGroup::base, ByteBufCodecs.map(Object2ObjectOpenHashMap::new, ResourceKey.streamCodec(EquipmentAssets.ROOT_ID), MaterialAssetGroup.AssetInfo.STREAM_CODEC), MaterialAssetGroup::overrides, MaterialAssetGroup::new);
      QUARTZ = create("quartz");
      IRON = create("iron", Map.of(EquipmentAssets.IRON, "iron_darker"));
      NETHERITE = create("netherite", Map.of(EquipmentAssets.NETHERITE, "netherite_darker"));
      REDSTONE = create("redstone");
      COPPER = create("copper", Map.of(EquipmentAssets.COPPER, "copper_darker"));
      GOLD = create("gold", Map.of(EquipmentAssets.GOLD, "gold_darker"));
      EMERALD = create("emerald");
      DIAMOND = create("diamond", Map.of(EquipmentAssets.DIAMOND, "diamond_darker"));
      LAPIS = create("lapis");
      AMETHYST = create("amethyst");
      RESIN = create("resin");
   }

   public static record AssetInfo(String suffix) {
      public static final Codec<MaterialAssetGroup.AssetInfo> CODEC;
      public static final StreamCodec<ByteBuf, MaterialAssetGroup.AssetInfo> STREAM_CODEC;

      public AssetInfo(String param1) {
         super();
         if (!ResourceLocation.isValidPath(var1)) {
            throw new IllegalArgumentException("Invalid string to use as a resource path element: " + var1);
         } else {
            this.suffix = var1;
         }
      }

      public String suffix() {
         return this.suffix;
      }

      static {
         CODEC = ExtraCodecs.RESOURCE_PATH_CODEC.xmap(MaterialAssetGroup.AssetInfo::new, MaterialAssetGroup.AssetInfo::suffix);
         STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(MaterialAssetGroup.AssetInfo::new, MaterialAssetGroup.AssetInfo::suffix);
      }
   }
}
