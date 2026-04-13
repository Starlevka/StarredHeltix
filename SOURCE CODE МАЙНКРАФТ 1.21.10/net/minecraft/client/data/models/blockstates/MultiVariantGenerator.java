package net.minecraft.client.data.models.blockstates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

public class MultiVariantGenerator implements BlockModelDefinitionGenerator {
   private final Block block;
   private final List<MultiVariantGenerator.Entry> entries;
   private final Set<Property<?>> seenProperties;

   MultiVariantGenerator(Block var1, List<MultiVariantGenerator.Entry> var2, Set<Property<?>> var3) {
      super();
      this.block = var1;
      this.entries = var2;
      this.seenProperties = var3;
   }

   static Set<Property<?>> validateAndExpandProperties(Set<Property<?>> var0, Block var1, PropertyDispatch<?> var2) {
      List var3 = var2.getDefinedProperties();
      var3.forEach((var2x) -> {
         String var10002;
         if (var1.getStateDefinition().getProperty(var2x.getName()) != var2x) {
            var10002 = String.valueOf(var2x);
            throw new IllegalStateException("Property " + var10002 + " is not defined for block " + String.valueOf(var1));
         } else if (var0.contains(var2x)) {
            var10002 = String.valueOf(var2x);
            throw new IllegalStateException("Values of property " + var10002 + " already defined for block " + String.valueOf(var1));
         }
      });
      HashSet var4 = new HashSet(var0);
      var4.addAll(var3);
      return var4;
   }

   public MultiVariantGenerator with(PropertyDispatch<VariantMutator> var1) {
      Set var2 = validateAndExpandProperties(this.seenProperties, this.block, var1);
      List var3 = this.entries.stream().flatMap((var1x) -> {
         return var1x.apply(var1);
      }).toList();
      return new MultiVariantGenerator(this.block, var3, var2);
   }

   public MultiVariantGenerator with(VariantMutator var1) {
      List var2 = this.entries.stream().flatMap((var1x) -> {
         return var1x.apply(var1);
      }).toList();
      return new MultiVariantGenerator(this.block, var2, this.seenProperties);
   }

   public BlockModelDefinition create() {
      HashMap var1 = new HashMap();
      Iterator var2 = this.entries.iterator();

      while(var2.hasNext()) {
         MultiVariantGenerator.Entry var3 = (MultiVariantGenerator.Entry)var2.next();
         var1.put(var3.properties.getKey(), var3.variant.toUnbaked());
      }

      return new BlockModelDefinition(Optional.of(new BlockModelDefinition.SimpleModelSelectors(var1)), Optional.empty());
   }

   public Block block() {
      return this.block;
   }

   public static MultiVariantGenerator.Empty dispatch(Block var0) {
      return new MultiVariantGenerator.Empty(var0);
   }

   public static MultiVariantGenerator dispatch(Block var0, MultiVariant var1) {
      return new MultiVariantGenerator(var0, List.of(new MultiVariantGenerator.Entry(PropertyValueList.EMPTY, var1)), Set.of());
   }

   private static record Entry(PropertyValueList properties, MultiVariant variant) {
      final PropertyValueList properties;
      final MultiVariant variant;

      Entry(PropertyValueList param1, MultiVariant param2) {
         super();
         this.properties = var1;
         this.variant = var2;
      }

      public Stream<MultiVariantGenerator.Entry> apply(PropertyDispatch<VariantMutator> var1) {
         return var1.getEntries().entrySet().stream().map((var1x) -> {
            PropertyValueList var2 = this.properties.extend((PropertyValueList)var1x.getKey());
            MultiVariant var3 = this.variant.with((VariantMutator)var1x.getValue());
            return new MultiVariantGenerator.Entry(var2, var3);
         });
      }

      public Stream<MultiVariantGenerator.Entry> apply(VariantMutator var1) {
         return Stream.of(new MultiVariantGenerator.Entry(this.properties, this.variant.with(var1)));
      }

      public PropertyValueList properties() {
         return this.properties;
      }

      public MultiVariant variant() {
         return this.variant;
      }
   }

   public static class Empty {
      private final Block block;

      public Empty(Block var1) {
         super();
         this.block = var1;
      }

      public MultiVariantGenerator with(PropertyDispatch<MultiVariant> var1) {
         Set var2 = MultiVariantGenerator.validateAndExpandProperties(Set.of(), this.block, var1);
         List var3 = var1.getEntries().entrySet().stream().map((var0) -> {
            return new MultiVariantGenerator.Entry((PropertyValueList)var0.getKey(), (MultiVariant)var0.getValue());
         }).toList();
         return new MultiVariantGenerator(this.block, var3, var2);
      }
   }
}
