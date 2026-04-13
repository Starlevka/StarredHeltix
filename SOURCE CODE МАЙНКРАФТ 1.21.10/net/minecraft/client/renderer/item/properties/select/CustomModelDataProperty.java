package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

public record CustomModelDataProperty(int index) implements SelectItemModelProperty<String> {
   public static final PrimitiveCodec<String> VALUE_CODEC;
   public static final SelectItemModelProperty.Type<CustomModelDataProperty, String> TYPE;

   public CustomModelDataProperty(int param1) {
      super();
      this.index = var1;
   }

   @Nullable
   public String get(ItemStack var1, @Nullable ClientLevel var2, @Nullable LivingEntity var3, int var4, ItemDisplayContext var5) {
      CustomModelData var6 = (CustomModelData)var1.get(DataComponents.CUSTOM_MODEL_DATA);
      return var6 != null ? var6.getString(this.index) : null;
   }

   public SelectItemModelProperty.Type<CustomModelDataProperty, String> type() {
      return TYPE;
   }

   public Codec<String> valueCodec() {
      return VALUE_CODEC;
   }

   public int index() {
      return this.index;
   }

   // $FF: synthetic method
   @Nullable
   public Object get(final ItemStack param1, @Nullable final ClientLevel param2, @Nullable final LivingEntity param3, final int param4, final ItemDisplayContext param5) {
      return this.get(var1, var2, var3, var4, var5);
   }

   static {
      VALUE_CODEC = Codec.STRING;
      TYPE = SelectItemModelProperty.Type.create(RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataProperty::index)).apply(var0, CustomModelDataProperty::new);
      }), VALUE_CODEC);
   }
}
