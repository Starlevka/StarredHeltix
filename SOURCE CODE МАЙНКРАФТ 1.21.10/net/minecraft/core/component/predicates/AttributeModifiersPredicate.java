package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.critereon.CollectionPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SingleComponentItemPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public record AttributeModifiersPredicate(Optional<CollectionPredicate<ItemAttributeModifiers.Entry, AttributeModifiersPredicate.EntryPredicate>> modifiers) implements SingleComponentItemPredicate<ItemAttributeModifiers> {
   public static final Codec<AttributeModifiersPredicate> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(CollectionPredicate.codec(AttributeModifiersPredicate.EntryPredicate.CODEC).optionalFieldOf("modifiers").forGetter(AttributeModifiersPredicate::modifiers)).apply(var0, AttributeModifiersPredicate::new);
   });

   public AttributeModifiersPredicate(Optional<CollectionPredicate<ItemAttributeModifiers.Entry, AttributeModifiersPredicate.EntryPredicate>> param1) {
      super();
      this.modifiers = var1;
   }

   public DataComponentType<ItemAttributeModifiers> componentType() {
      return DataComponents.ATTRIBUTE_MODIFIERS;
   }

   public boolean matches(ItemAttributeModifiers var1) {
      return !this.modifiers.isPresent() || ((CollectionPredicate)this.modifiers.get()).test((Iterable)var1.modifiers());
   }

   public Optional<CollectionPredicate<ItemAttributeModifiers.Entry, AttributeModifiersPredicate.EntryPredicate>> modifiers() {
      return this.modifiers;
   }

   public static record EntryPredicate(Optional<HolderSet<Attribute>> attribute, Optional<ResourceLocation> id, MinMaxBounds.Doubles amount, Optional<AttributeModifier.Operation> operation, Optional<EquipmentSlotGroup> slot) implements Predicate<ItemAttributeModifiers.Entry> {
      public static final Codec<AttributeModifiersPredicate.EntryPredicate> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(RegistryCodecs.homogeneousList(Registries.ATTRIBUTE).optionalFieldOf("attribute").forGetter(AttributeModifiersPredicate.EntryPredicate::attribute), ResourceLocation.CODEC.optionalFieldOf("id").forGetter(AttributeModifiersPredicate.EntryPredicate::id), MinMaxBounds.Doubles.CODEC.optionalFieldOf("amount", MinMaxBounds.Doubles.ANY).forGetter(AttributeModifiersPredicate.EntryPredicate::amount), AttributeModifier.Operation.CODEC.optionalFieldOf("operation").forGetter(AttributeModifiersPredicate.EntryPredicate::operation), EquipmentSlotGroup.CODEC.optionalFieldOf("slot").forGetter(AttributeModifiersPredicate.EntryPredicate::slot)).apply(var0, AttributeModifiersPredicate.EntryPredicate::new);
      });

      public EntryPredicate(Optional<HolderSet<Attribute>> param1, Optional<ResourceLocation> param2, MinMaxBounds.Doubles param3, Optional<AttributeModifier.Operation> param4, Optional<EquipmentSlotGroup> param5) {
         super();
         this.attribute = var1;
         this.id = var2;
         this.amount = var3;
         this.operation = var4;
         this.slot = var5;
      }

      public boolean test(ItemAttributeModifiers.Entry var1) {
         if (this.attribute.isPresent() && !((HolderSet)this.attribute.get()).contains(var1.attribute())) {
            return false;
         } else if (this.id.isPresent() && !((ResourceLocation)this.id.get()).equals(var1.modifier().id())) {
            return false;
         } else if (!this.amount.matches(var1.modifier().amount())) {
            return false;
         } else if (this.operation.isPresent() && this.operation.get() != var1.modifier().operation()) {
            return false;
         } else {
            return !this.slot.isPresent() || this.slot.get() == var1.slot();
         }
      }

      public Optional<HolderSet<Attribute>> attribute() {
         return this.attribute;
      }

      public Optional<ResourceLocation> id() {
         return this.id;
      }

      public MinMaxBounds.Doubles amount() {
         return this.amount;
      }

      public Optional<AttributeModifier.Operation> operation() {
         return this.operation;
      }

      public Optional<EquipmentSlotGroup> slot() {
         return this.slot;
      }

      // $FF: synthetic method
      public boolean test(final Object param1) {
         return this.test((ItemAttributeModifiers.Entry)var1);
      }
   }
}
