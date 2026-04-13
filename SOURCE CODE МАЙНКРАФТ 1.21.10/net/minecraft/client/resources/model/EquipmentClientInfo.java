package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

public record EquipmentClientInfo(Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> layers) {
   private static final Codec<List<EquipmentClientInfo.Layer>> LAYER_LIST_CODEC;
   public static final Codec<EquipmentClientInfo> CODEC;

   public EquipmentClientInfo(Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> param1) {
      super();
      this.layers = var1;
   }

   public static EquipmentClientInfo.Builder builder() {
      return new EquipmentClientInfo.Builder();
   }

   public List<EquipmentClientInfo.Layer> getLayers(EquipmentClientInfo.LayerType var1) {
      return (List)this.layers.getOrDefault(var1, List.of());
   }

   public Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> layers() {
      return this.layers;
   }

   static {
      LAYER_LIST_CODEC = ExtraCodecs.nonEmptyList(EquipmentClientInfo.Layer.CODEC.listOf());
      CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(ExtraCodecs.nonEmptyMap(Codec.unboundedMap(EquipmentClientInfo.LayerType.CODEC, LAYER_LIST_CODEC)).fieldOf("layers").forGetter(EquipmentClientInfo::layers)).apply(var0, EquipmentClientInfo::new);
      });
   }

   public static class Builder {
      private final Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> layersByType = new EnumMap(EquipmentClientInfo.LayerType.class);

      Builder() {
         super();
      }

      public EquipmentClientInfo.Builder addHumanoidLayers(ResourceLocation var1) {
         return this.addHumanoidLayers(var1, false);
      }

      public EquipmentClientInfo.Builder addHumanoidLayers(ResourceLocation var1, boolean var2) {
         this.addLayers(EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, EquipmentClientInfo.Layer.leatherDyeable(var1, var2));
         this.addMainHumanoidLayer(var1, var2);
         return this;
      }

      public EquipmentClientInfo.Builder addMainHumanoidLayer(ResourceLocation var1, boolean var2) {
         return this.addLayers(EquipmentClientInfo.LayerType.HUMANOID, EquipmentClientInfo.Layer.leatherDyeable(var1, var2));
      }

      public EquipmentClientInfo.Builder addLayers(EquipmentClientInfo.LayerType var1, EquipmentClientInfo.Layer... var2) {
         Collections.addAll((Collection)this.layersByType.computeIfAbsent(var1, (var0) -> {
            return new ArrayList();
         }), var2);
         return this;
      }

      public EquipmentClientInfo build() {
         return new EquipmentClientInfo((Map)this.layersByType.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (var0) -> {
            return List.copyOf((Collection)var0.getValue());
         })));
      }
   }

   public static enum LayerType implements StringRepresentable {
      HUMANOID("humanoid"),
      HUMANOID_LEGGINGS("humanoid_leggings"),
      WINGS("wings"),
      WOLF_BODY("wolf_body"),
      HORSE_BODY("horse_body"),
      LLAMA_BODY("llama_body"),
      PIG_SADDLE("pig_saddle"),
      STRIDER_SADDLE("strider_saddle"),
      CAMEL_SADDLE("camel_saddle"),
      HORSE_SADDLE("horse_saddle"),
      DONKEY_SADDLE("donkey_saddle"),
      MULE_SADDLE("mule_saddle"),
      ZOMBIE_HORSE_SADDLE("zombie_horse_saddle"),
      SKELETON_HORSE_SADDLE("skeleton_horse_saddle"),
      HAPPY_GHAST_BODY("happy_ghast_body");

      public static final Codec<EquipmentClientInfo.LayerType> CODEC = StringRepresentable.fromEnum(EquipmentClientInfo.LayerType::values);
      private final String id;

      private LayerType(final String param3) {
         this.id = var3;
      }

      public String getSerializedName() {
         return this.id;
      }

      public String trimAssetPrefix() {
         return "trims/entity/" + this.id;
      }

      // $FF: synthetic method
      private static EquipmentClientInfo.LayerType[] $values() {
         return new EquipmentClientInfo.LayerType[]{HUMANOID, HUMANOID_LEGGINGS, WINGS, WOLF_BODY, HORSE_BODY, LLAMA_BODY, PIG_SADDLE, STRIDER_SADDLE, CAMEL_SADDLE, HORSE_SADDLE, DONKEY_SADDLE, MULE_SADDLE, ZOMBIE_HORSE_SADDLE, SKELETON_HORSE_SADDLE, HAPPY_GHAST_BODY};
      }
   }

   public static record Layer(ResourceLocation textureId, Optional<EquipmentClientInfo.Dyeable> dyeable, boolean usePlayerTexture) {
      public static final Codec<EquipmentClientInfo.Layer> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(ResourceLocation.CODEC.fieldOf("texture").forGetter(EquipmentClientInfo.Layer::textureId), EquipmentClientInfo.Dyeable.CODEC.optionalFieldOf("dyeable").forGetter(EquipmentClientInfo.Layer::dyeable), Codec.BOOL.optionalFieldOf("use_player_texture", false).forGetter(EquipmentClientInfo.Layer::usePlayerTexture)).apply(var0, EquipmentClientInfo.Layer::new);
      });

      public Layer(ResourceLocation var1) {
         this(var1, Optional.empty(), false);
      }

      public Layer(ResourceLocation param1, Optional<EquipmentClientInfo.Dyeable> param2, boolean param3) {
         super();
         this.textureId = var1;
         this.dyeable = var2;
         this.usePlayerTexture = var3;
      }

      public static EquipmentClientInfo.Layer leatherDyeable(ResourceLocation var0, boolean var1) {
         return new EquipmentClientInfo.Layer(var0, var1 ? Optional.of(new EquipmentClientInfo.Dyeable(Optional.of(-6265536))) : Optional.empty(), false);
      }

      public static EquipmentClientInfo.Layer onlyIfDyed(ResourceLocation var0, boolean var1) {
         return new EquipmentClientInfo.Layer(var0, var1 ? Optional.of(new EquipmentClientInfo.Dyeable(Optional.empty())) : Optional.empty(), false);
      }

      public ResourceLocation getTextureLocation(EquipmentClientInfo.LayerType var1) {
         return this.textureId.withPath((var1x) -> {
            String var10000 = var1.getSerializedName();
            return "textures/entity/equipment/" + var10000 + "/" + var1x + ".png";
         });
      }

      public ResourceLocation textureId() {
         return this.textureId;
      }

      public Optional<EquipmentClientInfo.Dyeable> dyeable() {
         return this.dyeable;
      }

      public boolean usePlayerTexture() {
         return this.usePlayerTexture;
      }
   }

   public static record Dyeable(Optional<Integer> colorWhenUndyed) {
      public static final Codec<EquipmentClientInfo.Dyeable> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("color_when_undyed").forGetter(EquipmentClientInfo.Dyeable::colorWhenUndyed)).apply(var0, EquipmentClientInfo.Dyeable::new);
      });

      public Dyeable(Optional<Integer> param1) {
         super();
         this.colorWhenUndyed = var1;
      }

      public Optional<Integer> colorWhenUndyed() {
         return this.colorWhenUndyed;
      }
   }
}
