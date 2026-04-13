package net.minecraft.world.item.crafting.display;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.block.entity.FuelValues;

public interface SlotDisplay {
   Codec<SlotDisplay> CODEC = BuiltInRegistries.SLOT_DISPLAY.byNameCodec().dispatch(SlotDisplay::type, SlotDisplay.Type::codec);
   StreamCodec<RegistryFriendlyByteBuf, SlotDisplay> STREAM_CODEC = ByteBufCodecs.registry(Registries.SLOT_DISPLAY).dispatch(SlotDisplay::type, SlotDisplay.Type::streamCodec);

   <T> Stream<T> resolve(ContextMap var1, DisplayContentsFactory<T> var2);

   SlotDisplay.Type<? extends SlotDisplay> type();

   default boolean isEnabled(FeatureFlagSet var1) {
      return true;
   }

   default List<ItemStack> resolveForStacks(ContextMap var1) {
      return this.resolve(var1, SlotDisplay.ItemStackContentsFactory.INSTANCE).toList();
   }

   default ItemStack resolveForFirstStack(ContextMap var1) {
      return (ItemStack)this.resolve(var1, SlotDisplay.ItemStackContentsFactory.INSTANCE).findFirst().orElse(ItemStack.EMPTY);
   }

   public static class ItemStackContentsFactory implements DisplayContentsFactory.ForStacks<ItemStack> {
      public static final SlotDisplay.ItemStackContentsFactory INSTANCE = new SlotDisplay.ItemStackContentsFactory();

      public ItemStackContentsFactory() {
         super();
      }

      public ItemStack forStack(ItemStack var1) {
         return var1;
      }

      // $FF: synthetic method
      public Object forStack(final ItemStack param1) {
         return this.forStack(var1);
      }
   }

   public static record WithRemainder(SlotDisplay input, SlotDisplay remainder) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.WithRemainder> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(SlotDisplay.CODEC.fieldOf("input").forGetter(SlotDisplay.WithRemainder::input), SlotDisplay.CODEC.fieldOf("remainder").forGetter(SlotDisplay.WithRemainder::remainder)).apply(var0, SlotDisplay.WithRemainder::new);
      });
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.WithRemainder> STREAM_CODEC;
      public static final SlotDisplay.Type<SlotDisplay.WithRemainder> TYPE;

      public WithRemainder(SlotDisplay param1, SlotDisplay param2) {
         super();
         this.input = var1;
         this.remainder = var2;
      }

      public SlotDisplay.Type<SlotDisplay.WithRemainder> type() {
         return TYPE;
      }

      public <T> Stream<T> resolve(ContextMap var1, DisplayContentsFactory<T> var2) {
         if (var2 instanceof DisplayContentsFactory.ForRemainders) {
            DisplayContentsFactory.ForRemainders var3 = (DisplayContentsFactory.ForRemainders)var2;
            List var4 = this.remainder.resolve(var1, var2).toList();
            return this.input.resolve(var1, var2).map((var2x) -> {
               return var3.addRemainder(var2x, var4);
            });
         } else {
            return this.input.resolve(var1, var2);
         }
      }

      public boolean isEnabled(FeatureFlagSet var1) {
         return this.input.isEnabled(var1) && this.remainder.isEnabled(var1);
      }

      public SlotDisplay input() {
         return this.input;
      }

      public SlotDisplay remainder() {
         return this.remainder;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(SlotDisplay.STREAM_CODEC, SlotDisplay.WithRemainder::input, SlotDisplay.STREAM_CODEC, SlotDisplay.WithRemainder::remainder, SlotDisplay.WithRemainder::new);
         TYPE = new SlotDisplay.Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static record Composite(List<SlotDisplay> contents) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.Composite> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(SlotDisplay.CODEC.listOf().fieldOf("contents").forGetter(SlotDisplay.Composite::contents)).apply(var0, SlotDisplay.Composite::new);
      });
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.Composite> STREAM_CODEC;
      public static final SlotDisplay.Type<SlotDisplay.Composite> TYPE;

      public Composite(List<SlotDisplay> param1) {
         super();
         this.contents = var1;
      }

      public SlotDisplay.Type<SlotDisplay.Composite> type() {
         return TYPE;
      }

      public <T> Stream<T> resolve(ContextMap var1, DisplayContentsFactory<T> var2) {
         return this.contents.stream().flatMap((var2x) -> {
            return var2x.resolve(var1, var2);
         });
      }

      public boolean isEnabled(FeatureFlagSet var1) {
         return this.contents.stream().allMatch((var1x) -> {
            return var1x.isEnabled(var1);
         });
      }

      public List<SlotDisplay> contents() {
         return this.contents;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(SlotDisplay.STREAM_CODEC.apply(ByteBufCodecs.list()), SlotDisplay.Composite::contents, SlotDisplay.Composite::new);
         TYPE = new SlotDisplay.Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static record TagSlotDisplay(TagKey<Item> tag) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.TagSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(SlotDisplay.TagSlotDisplay::tag)).apply(var0, SlotDisplay.TagSlotDisplay::new);
      });
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.TagSlotDisplay> STREAM_CODEC;
      public static final SlotDisplay.Type<SlotDisplay.TagSlotDisplay> TYPE;

      public TagSlotDisplay(TagKey<Item> param1) {
         super();
         this.tag = var1;
      }

      public SlotDisplay.Type<SlotDisplay.TagSlotDisplay> type() {
         return TYPE;
      }

      public <T> Stream<T> resolve(ContextMap var1, DisplayContentsFactory<T> var2) {
         if (var2 instanceof DisplayContentsFactory.ForStacks) {
            DisplayContentsFactory.ForStacks var3 = (DisplayContentsFactory.ForStacks)var2;
            HolderLookup.Provider var4 = (HolderLookup.Provider)var1.getOptional(SlotDisplayContext.REGISTRIES);
            if (var4 != null) {
               return var4.lookupOrThrow(Registries.ITEM).get(this.tag).map((var1x) -> {
                  Stream var10000 = var1x.stream();
                  Objects.requireNonNull(var3);
                  return var10000.map(var3::forStack);
               }).stream().flatMap((var0) -> {
                  return var0;
               });
            }
         }

         return Stream.empty();
      }

      public TagKey<Item> tag() {
         return this.tag;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(TagKey.streamCodec(Registries.ITEM), SlotDisplay.TagSlotDisplay::tag, SlotDisplay.TagSlotDisplay::new);
         TYPE = new SlotDisplay.Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static record ItemStackSlotDisplay(ItemStack stack) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.ItemStackSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ItemStack.STRICT_CODEC.fieldOf("item").forGetter(SlotDisplay.ItemStackSlotDisplay::stack)).apply(var0, SlotDisplay.ItemStackSlotDisplay::new);
      });
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.ItemStackSlotDisplay> STREAM_CODEC;
      public static final SlotDisplay.Type<SlotDisplay.ItemStackSlotDisplay> TYPE;

      public ItemStackSlotDisplay(ItemStack param1) {
         super();
         this.stack = var1;
      }

      public SlotDisplay.Type<SlotDisplay.ItemStackSlotDisplay> type() {
         return TYPE;
      }

      public <T> Stream<T> resolve(ContextMap var1, DisplayContentsFactory<T> var2) {
         if (var2 instanceof DisplayContentsFactory.ForStacks) {
            DisplayContentsFactory.ForStacks var3 = (DisplayContentsFactory.ForStacks)var2;
            return Stream.of(var3.forStack(this.stack));
         } else {
            return Stream.empty();
         }
      }

      public boolean equals(Object var1) {
         boolean var10000;
         if (this != var1) {
            label26: {
               if (var1 instanceof SlotDisplay.ItemStackSlotDisplay) {
                  SlotDisplay.ItemStackSlotDisplay var2 = (SlotDisplay.ItemStackSlotDisplay)var1;
                  if (ItemStack.matches(this.stack, var2.stack)) {
                     break label26;
                  }
               }

               var10000 = false;
               return var10000;
            }
         }

         var10000 = true;
         return var10000;
      }

      public boolean isEnabled(FeatureFlagSet var1) {
         return this.stack.getItem().isEnabled(var1);
      }

      public ItemStack stack() {
         return this.stack;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(ItemStack.STREAM_CODEC, SlotDisplay.ItemStackSlotDisplay::stack, SlotDisplay.ItemStackSlotDisplay::new);
         TYPE = new SlotDisplay.Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static record ItemSlotDisplay(Holder<Item> item) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.ItemSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Item.CODEC.fieldOf("item").forGetter(SlotDisplay.ItemSlotDisplay::item)).apply(var0, SlotDisplay.ItemSlotDisplay::new);
      });
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.ItemSlotDisplay> STREAM_CODEC;
      public static final SlotDisplay.Type<SlotDisplay.ItemSlotDisplay> TYPE;

      public ItemSlotDisplay(Item var1) {
         this((Holder)var1.builtInRegistryHolder());
      }

      public ItemSlotDisplay(Holder<Item> param1) {
         super();
         this.item = var1;
      }

      public SlotDisplay.Type<SlotDisplay.ItemSlotDisplay> type() {
         return TYPE;
      }

      public <T> Stream<T> resolve(ContextMap var1, DisplayContentsFactory<T> var2) {
         if (var2 instanceof DisplayContentsFactory.ForStacks) {
            DisplayContentsFactory.ForStacks var3 = (DisplayContentsFactory.ForStacks)var2;
            return Stream.of(var3.forStack(this.item));
         } else {
            return Stream.empty();
         }
      }

      public boolean isEnabled(FeatureFlagSet var1) {
         return ((Item)this.item.value()).isEnabled(var1);
      }

      public Holder<Item> item() {
         return this.item;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(Item.STREAM_CODEC, SlotDisplay.ItemSlotDisplay::item, SlotDisplay.ItemSlotDisplay::new);
         TYPE = new SlotDisplay.Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static record SmithingTrimDemoSlotDisplay(SlotDisplay base, SlotDisplay material, Holder<TrimPattern> pattern) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.SmithingTrimDemoSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(SlotDisplay.CODEC.fieldOf("base").forGetter(SlotDisplay.SmithingTrimDemoSlotDisplay::base), SlotDisplay.CODEC.fieldOf("material").forGetter(SlotDisplay.SmithingTrimDemoSlotDisplay::material), TrimPattern.CODEC.fieldOf("pattern").forGetter(SlotDisplay.SmithingTrimDemoSlotDisplay::pattern)).apply(var0, SlotDisplay.SmithingTrimDemoSlotDisplay::new);
      });
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.SmithingTrimDemoSlotDisplay> STREAM_CODEC;
      public static final SlotDisplay.Type<SlotDisplay.SmithingTrimDemoSlotDisplay> TYPE;

      public SmithingTrimDemoSlotDisplay(SlotDisplay param1, SlotDisplay param2, Holder<TrimPattern> param3) {
         super();
         this.base = var1;
         this.material = var2;
         this.pattern = var3;
      }

      public SlotDisplay.Type<SlotDisplay.SmithingTrimDemoSlotDisplay> type() {
         return TYPE;
      }

      public <T> Stream<T> resolve(ContextMap var1, DisplayContentsFactory<T> var2) {
         if (var2 instanceof DisplayContentsFactory.ForStacks) {
            DisplayContentsFactory.ForStacks var3 = (DisplayContentsFactory.ForStacks)var2;
            HolderLookup.Provider var4 = (HolderLookup.Provider)var1.getOptional(SlotDisplayContext.REGISTRIES);
            if (var4 != null) {
               RandomSource var5 = RandomSource.create((long)System.identityHashCode(this));
               List var6 = this.base.resolveForStacks(var1);
               if (var6.isEmpty()) {
                  return Stream.empty();
               }

               List var7 = this.material.resolveForStacks(var1);
               if (var7.isEmpty()) {
                  return Stream.empty();
               }

               Stream var10000 = Stream.generate(() -> {
                  ItemStack var5x = (ItemStack)Util.getRandom(var6, var5);
                  ItemStack var6x = (ItemStack)Util.getRandom(var7, var5);
                  return SmithingTrimRecipe.applyTrim(var4, var5x, var6x, this.pattern);
               }).limit(256L).filter((var0) -> {
                  return !var0.isEmpty();
               }).limit(16L);
               Objects.requireNonNull(var3);
               return var10000.map(var3::forStack);
            }
         }

         return Stream.empty();
      }

      public SlotDisplay base() {
         return this.base;
      }

      public SlotDisplay material() {
         return this.material;
      }

      public Holder<TrimPattern> pattern() {
         return this.pattern;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(SlotDisplay.STREAM_CODEC, SlotDisplay.SmithingTrimDemoSlotDisplay::base, SlotDisplay.STREAM_CODEC, SlotDisplay.SmithingTrimDemoSlotDisplay::material, TrimPattern.STREAM_CODEC, SlotDisplay.SmithingTrimDemoSlotDisplay::pattern, SlotDisplay.SmithingTrimDemoSlotDisplay::new);
         TYPE = new SlotDisplay.Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static class AnyFuel implements SlotDisplay {
      public static final SlotDisplay.AnyFuel INSTANCE = new SlotDisplay.AnyFuel();
      public static final MapCodec<SlotDisplay.AnyFuel> MAP_CODEC;
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.AnyFuel> STREAM_CODEC;
      public static final SlotDisplay.Type<SlotDisplay.AnyFuel> TYPE;

      private AnyFuel() {
         super();
      }

      public SlotDisplay.Type<SlotDisplay.AnyFuel> type() {
         return TYPE;
      }

      public String toString() {
         return "<any fuel>";
      }

      public <T> Stream<T> resolve(ContextMap var1, DisplayContentsFactory<T> var2) {
         if (var2 instanceof DisplayContentsFactory.ForStacks) {
            DisplayContentsFactory.ForStacks var3 = (DisplayContentsFactory.ForStacks)var2;
            FuelValues var4 = (FuelValues)var1.getOptional(SlotDisplayContext.FUEL_VALUES);
            if (var4 != null) {
               Stream var10000 = var4.fuelItems().stream();
               Objects.requireNonNull(var3);
               return var10000.map(var3::forStack);
            }
         }

         return Stream.empty();
      }

      static {
         MAP_CODEC = MapCodec.unit(INSTANCE);
         STREAM_CODEC = StreamCodec.unit(INSTANCE);
         TYPE = new SlotDisplay.Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static class Empty implements SlotDisplay {
      public static final SlotDisplay.Empty INSTANCE = new SlotDisplay.Empty();
      public static final MapCodec<SlotDisplay.Empty> MAP_CODEC;
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.Empty> STREAM_CODEC;
      public static final SlotDisplay.Type<SlotDisplay.Empty> TYPE;

      private Empty() {
         super();
      }

      public SlotDisplay.Type<SlotDisplay.Empty> type() {
         return TYPE;
      }

      public String toString() {
         return "<empty>";
      }

      public <T> Stream<T> resolve(ContextMap var1, DisplayContentsFactory<T> var2) {
         return Stream.empty();
      }

      static {
         MAP_CODEC = MapCodec.unit(INSTANCE);
         STREAM_CODEC = StreamCodec.unit(INSTANCE);
         TYPE = new SlotDisplay.Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static record Type<T extends SlotDisplay>(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
      public Type(MapCodec<T> param1, StreamCodec<RegistryFriendlyByteBuf, T> param2) {
         super();
         this.codec = var1;
         this.streamCodec = var2;
      }

      public MapCodec<T> codec() {
         return this.codec;
      }

      public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
         return this.streamCodec;
      }
   }
}
