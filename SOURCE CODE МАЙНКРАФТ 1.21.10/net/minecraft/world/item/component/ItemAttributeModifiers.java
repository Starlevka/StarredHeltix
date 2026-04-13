package net.minecraft.world.item.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.function.TriConsumer;

public record ItemAttributeModifiers(List<ItemAttributeModifiers.Entry> modifiers) {
   public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(List.of());
   public static final Codec<ItemAttributeModifiers> CODEC;
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers> STREAM_CODEC;
   public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT;

   public ItemAttributeModifiers(List<ItemAttributeModifiers.Entry> param1) {
      super();
      this.modifiers = var1;
   }

   public static ItemAttributeModifiers.Builder builder() {
      return new ItemAttributeModifiers.Builder();
   }

   public ItemAttributeModifiers withModifierAdded(Holder<Attribute> var1, AttributeModifier var2, EquipmentSlotGroup var3) {
      com.google.common.collect.ImmutableList.Builder var4 = ImmutableList.builderWithExpectedSize(this.modifiers.size() + 1);
      Iterator var5 = this.modifiers.iterator();

      while(var5.hasNext()) {
         ItemAttributeModifiers.Entry var6 = (ItemAttributeModifiers.Entry)var5.next();
         if (!var6.matches(var1, var2.id())) {
            var4.add(var6);
         }
      }

      var4.add(new ItemAttributeModifiers.Entry(var1, var2, var3));
      return new ItemAttributeModifiers(var4.build());
   }

   public void forEach(EquipmentSlotGroup var1, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> var2) {
      Iterator var3 = this.modifiers.iterator();

      while(var3.hasNext()) {
         ItemAttributeModifiers.Entry var4 = (ItemAttributeModifiers.Entry)var3.next();
         if (var4.slot.equals(var1)) {
            var2.accept(var4.attribute, var4.modifier, var4.display);
         }
      }

   }

   public void forEach(EquipmentSlotGroup var1, BiConsumer<Holder<Attribute>, AttributeModifier> var2) {
      Iterator var3 = this.modifiers.iterator();

      while(var3.hasNext()) {
         ItemAttributeModifiers.Entry var4 = (ItemAttributeModifiers.Entry)var3.next();
         if (var4.slot.equals(var1)) {
            var2.accept(var4.attribute, var4.modifier);
         }
      }

   }

   public void forEach(EquipmentSlot var1, BiConsumer<Holder<Attribute>, AttributeModifier> var2) {
      Iterator var3 = this.modifiers.iterator();

      while(var3.hasNext()) {
         ItemAttributeModifiers.Entry var4 = (ItemAttributeModifiers.Entry)var3.next();
         if (var4.slot.test(var1)) {
            var2.accept(var4.attribute, var4.modifier);
         }
      }

   }

   public double compute(double var1, EquipmentSlot var3) {
      double var4 = var1;
      Iterator var6 = this.modifiers.iterator();

      while(var6.hasNext()) {
         ItemAttributeModifiers.Entry var7 = (ItemAttributeModifiers.Entry)var6.next();
         if (var7.slot.test(var3)) {
            double var8 = var7.modifier.amount();
            double var10001;
            switch(var7.modifier.operation()) {
            case ADD_VALUE:
               var10001 = var8;
               break;
            case ADD_MULTIPLIED_BASE:
               var10001 = var8 * var1;
               break;
            case ADD_MULTIPLIED_TOTAL:
               var10001 = var8 * var4;
               break;
            default:
               throw new MatchException((String)null, (Throwable)null);
            }

            var4 += var10001;
         }
      }

      return var4;
   }

   public List<ItemAttributeModifiers.Entry> modifiers() {
      return this.modifiers;
   }

   static {
      CODEC = ItemAttributeModifiers.Entry.CODEC.listOf().xmap(ItemAttributeModifiers::new, ItemAttributeModifiers::modifiers);
      STREAM_CODEC = StreamCodec.composite(ItemAttributeModifiers.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), ItemAttributeModifiers::modifiers, ItemAttributeModifiers::new);
      ATTRIBUTE_MODIFIER_FORMAT = (DecimalFormat)Util.make(new DecimalFormat("#.##"), (var0) -> {
         var0.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
      });
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<ItemAttributeModifiers.Entry> entries = ImmutableList.builder();

      Builder() {
         super();
      }

      public ItemAttributeModifiers.Builder add(Holder<Attribute> var1, AttributeModifier var2, EquipmentSlotGroup var3) {
         this.entries.add(new ItemAttributeModifiers.Entry(var1, var2, var3));
         return this;
      }

      public ItemAttributeModifiers.Builder add(Holder<Attribute> var1, AttributeModifier var2, EquipmentSlotGroup var3, ItemAttributeModifiers.Display var4) {
         this.entries.add(new ItemAttributeModifiers.Entry(var1, var2, var3, var4));
         return this;
      }

      public ItemAttributeModifiers build() {
         return new ItemAttributeModifiers(this.entries.build());
      }
   }

   public static record Entry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot, ItemAttributeModifiers.Display display) {
      final Holder<Attribute> attribute;
      final AttributeModifier modifier;
      final EquipmentSlotGroup slot;
      final ItemAttributeModifiers.Display display;
      public static final Codec<ItemAttributeModifiers.Entry> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(Attribute.CODEC.fieldOf("type").forGetter(ItemAttributeModifiers.Entry::attribute), AttributeModifier.MAP_CODEC.forGetter(ItemAttributeModifiers.Entry::modifier), EquipmentSlotGroup.CODEC.optionalFieldOf("slot", EquipmentSlotGroup.ANY).forGetter(ItemAttributeModifiers.Entry::slot), ItemAttributeModifiers.Display.CODEC.optionalFieldOf("display", ItemAttributeModifiers.Display.Default.INSTANCE).forGetter(ItemAttributeModifiers.Entry::display)).apply(var0, ItemAttributeModifiers.Entry::new);
      });
      public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Entry> STREAM_CODEC;

      public Entry(Holder<Attribute> var1, AttributeModifier var2, EquipmentSlotGroup var3) {
         this(var1, var2, var3, ItemAttributeModifiers.Display.attributeModifiers());
      }

      public Entry(Holder<Attribute> param1, AttributeModifier param2, EquipmentSlotGroup param3, ItemAttributeModifiers.Display param4) {
         super();
         this.attribute = var1;
         this.modifier = var2;
         this.slot = var3;
         this.display = var4;
      }

      public boolean matches(Holder<Attribute> var1, ResourceLocation var2) {
         return var1.equals(this.attribute) && this.modifier.is(var2);
      }

      public Holder<Attribute> attribute() {
         return this.attribute;
      }

      public AttributeModifier modifier() {
         return this.modifier;
      }

      public EquipmentSlotGroup slot() {
         return this.slot;
      }

      public ItemAttributeModifiers.Display display() {
         return this.display;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(Attribute.STREAM_CODEC, ItemAttributeModifiers.Entry::attribute, AttributeModifier.STREAM_CODEC, ItemAttributeModifiers.Entry::modifier, EquipmentSlotGroup.STREAM_CODEC, ItemAttributeModifiers.Entry::slot, ItemAttributeModifiers.Display.STREAM_CODEC, ItemAttributeModifiers.Entry::display, ItemAttributeModifiers.Entry::new);
      }
   }

   public interface Display {
      Codec<ItemAttributeModifiers.Display> CODEC = ItemAttributeModifiers.Display.Type.CODEC.dispatch("type", ItemAttributeModifiers.Display::type, (var0) -> {
         return var0.codec;
      });
      StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display> STREAM_CODEC = ItemAttributeModifiers.Display.Type.STREAM_CODEC.cast().dispatch(ItemAttributeModifiers.Display::type, ItemAttributeModifiers.Display.Type::streamCodec);

      static ItemAttributeModifiers.Display attributeModifiers() {
         return ItemAttributeModifiers.Display.Default.INSTANCE;
      }

      static ItemAttributeModifiers.Display hidden() {
         return ItemAttributeModifiers.Display.Hidden.INSTANCE;
      }

      static ItemAttributeModifiers.Display override(Component var0) {
         return new ItemAttributeModifiers.Display.OverrideText(var0);
      }

      ItemAttributeModifiers.Display.Type type();

      void apply(Consumer<Component> var1, @Nullable Player var2, Holder<Attribute> var3, AttributeModifier var4);

      public static record Default() implements ItemAttributeModifiers.Display {
         static final ItemAttributeModifiers.Display.Default INSTANCE = new ItemAttributeModifiers.Display.Default();
         static final MapCodec<ItemAttributeModifiers.Display.Default> CODEC;
         static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.Default> STREAM_CODEC;

         public Default() {
            super();
         }

         public ItemAttributeModifiers.Display.Type type() {
            return ItemAttributeModifiers.Display.Type.DEFAULT;
         }

         public void apply(Consumer<Component> var1, @Nullable Player var2, Holder<Attribute> var3, AttributeModifier var4) {
            double var5 = var4.amount();
            boolean var7 = false;
            if (var2 != null) {
               if (var4.is(Item.BASE_ATTACK_DAMAGE_ID)) {
                  var5 += var2.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                  var7 = true;
               } else if (var4.is(Item.BASE_ATTACK_SPEED_ID)) {
                  var5 += var2.getAttributeBaseValue(Attributes.ATTACK_SPEED);
                  var7 = true;
               }
            }

            double var8;
            if (var4.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE && var4.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
               if (var3.is(Attributes.KNOCKBACK_RESISTANCE)) {
                  var8 = var5 * 10.0D;
               } else {
                  var8 = var5;
               }
            } else {
               var8 = var5 * 100.0D;
            }

            if (var7) {
               var1.accept(CommonComponents.space().append((Component)Component.translatable("attribute.modifier.equals." + var4.operation().id(), ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(var8), Component.translatable(((Attribute)var3.value()).getDescriptionId()))).withStyle(ChatFormatting.DARK_GREEN));
            } else if (var5 > 0.0D) {
               var1.accept(Component.translatable("attribute.modifier.plus." + var4.operation().id(), ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(var8), Component.translatable(((Attribute)var3.value()).getDescriptionId())).withStyle(((Attribute)var3.value()).getStyle(true)));
            } else if (var5 < 0.0D) {
               var1.accept(Component.translatable("attribute.modifier.take." + var4.operation().id(), ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(-var8), Component.translatable(((Attribute)var3.value()).getDescriptionId())).withStyle(((Attribute)var3.value()).getStyle(false)));
            }

         }

         static {
            CODEC = MapCodec.unit(INSTANCE);
            STREAM_CODEC = StreamCodec.unit(INSTANCE);
         }
      }

      public static record Hidden() implements ItemAttributeModifiers.Display {
         static final ItemAttributeModifiers.Display.Hidden INSTANCE = new ItemAttributeModifiers.Display.Hidden();
         static final MapCodec<ItemAttributeModifiers.Display.Hidden> CODEC;
         static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.Hidden> STREAM_CODEC;

         public Hidden() {
            super();
         }

         public ItemAttributeModifiers.Display.Type type() {
            return ItemAttributeModifiers.Display.Type.HIDDEN;
         }

         public void apply(Consumer<Component> var1, @Nullable Player var2, Holder<Attribute> var3, AttributeModifier var4) {
         }

         static {
            CODEC = MapCodec.unit(INSTANCE);
            STREAM_CODEC = StreamCodec.unit(INSTANCE);
         }
      }

      public static record OverrideText(Component component) implements ItemAttributeModifiers.Display {
         static final MapCodec<ItemAttributeModifiers.Display.OverrideText> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
            return var0.group(ComponentSerialization.CODEC.fieldOf("value").forGetter(ItemAttributeModifiers.Display.OverrideText::component)).apply(var0, ItemAttributeModifiers.Display.OverrideText::new);
         });
         static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.OverrideText> STREAM_CODEC;

         public OverrideText(Component param1) {
            super();
            this.component = var1;
         }

         public ItemAttributeModifiers.Display.Type type() {
            return ItemAttributeModifiers.Display.Type.OVERRIDE;
         }

         public void apply(Consumer<Component> var1, @Nullable Player var2, Holder<Attribute> var3, AttributeModifier var4) {
            var1.accept(this.component);
         }

         public Component component() {
            return this.component;
         }

         static {
            STREAM_CODEC = StreamCodec.composite(ComponentSerialization.STREAM_CODEC, ItemAttributeModifiers.Display.OverrideText::component, ItemAttributeModifiers.Display.OverrideText::new);
         }
      }

      public static enum Type implements StringRepresentable {
         DEFAULT("default", 0, ItemAttributeModifiers.Display.Default.CODEC, ItemAttributeModifiers.Display.Default.STREAM_CODEC),
         HIDDEN("hidden", 1, ItemAttributeModifiers.Display.Hidden.CODEC, ItemAttributeModifiers.Display.Hidden.STREAM_CODEC),
         OVERRIDE("override", 2, ItemAttributeModifiers.Display.OverrideText.CODEC, ItemAttributeModifiers.Display.OverrideText.STREAM_CODEC);

         static final Codec<ItemAttributeModifiers.Display.Type> CODEC = StringRepresentable.fromEnum(ItemAttributeModifiers.Display.Type::values);
         private static final IntFunction<ItemAttributeModifiers.Display.Type> BY_ID = ByIdMap.continuous(ItemAttributeModifiers.Display.Type::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
         static final StreamCodec<ByteBuf, ItemAttributeModifiers.Display.Type> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ItemAttributeModifiers.Display.Type::id);
         private final String name;
         private final int id;
         final MapCodec<? extends ItemAttributeModifiers.Display> codec;
         private final StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> streamCodec;

         private Type(final String param3, final int param4, final MapCodec<? extends ItemAttributeModifiers.Display> param5, final StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> param6) {
            this.name = var3;
            this.id = var4;
            this.codec = var5;
            this.streamCodec = var6;
         }

         public String getSerializedName() {
            return this.name;
         }

         private int id() {
            return this.id;
         }

         private StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> streamCodec() {
            return this.streamCodec;
         }

         // $FF: synthetic method
         private static ItemAttributeModifiers.Display.Type[] $values() {
            return new ItemAttributeModifiers.Display.Type[]{DEFAULT, HIDDEN, OVERRIDE};
         }
      }
   }
}
