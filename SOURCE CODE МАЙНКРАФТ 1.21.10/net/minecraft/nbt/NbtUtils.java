package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

public final class NbtUtils {
   private static final Comparator<ListTag> YXZ_LISTTAG_INT_COMPARATOR = Comparator.comparingInt((var0) -> {
      return var0.getIntOr(1, 0);
   }).thenComparingInt((var0) -> {
      return var0.getIntOr(0, 0);
   }).thenComparingInt((var0) -> {
      return var0.getIntOr(2, 0);
   });
   private static final Comparator<ListTag> YXZ_LISTTAG_DOUBLE_COMPARATOR = Comparator.comparingDouble((var0) -> {
      return var0.getDoubleOr(1, 0.0D);
   }).thenComparingDouble((var0) -> {
      return var0.getDoubleOr(0, 0.0D);
   }).thenComparingDouble((var0) -> {
      return var0.getDoubleOr(2, 0.0D);
   });
   private static final Codec<ResourceKey<Block>> BLOCK_NAME_CODEC;
   public static final String SNBT_DATA_TAG = "data";
   private static final char PROPERTIES_START = '{';
   private static final char PROPERTIES_END = '}';
   private static final String ELEMENT_SEPARATOR = ",";
   private static final char KEY_VALUE_SEPARATOR = ':';
   private static final Splitter COMMA_SPLITTER;
   private static final Splitter COLON_SPLITTER;
   private static final Logger LOGGER;
   private static final int INDENT = 2;
   private static final int NOT_FOUND = -1;

   private NbtUtils() {
      super();
   }

   @VisibleForTesting
   public static boolean compareNbt(@Nullable Tag var0, @Nullable Tag var1, boolean var2) {
      if (var0 == var1) {
         return true;
      } else if (var0 == null) {
         return true;
      } else if (var1 == null) {
         return false;
      } else if (!var0.getClass().equals(var1.getClass())) {
         return false;
      } else {
         Iterator var6;
         if (var0 instanceof CompoundTag) {
            CompoundTag var3 = (CompoundTag)var0;
            CompoundTag var11 = (CompoundTag)var1;
            if (var11.size() < var3.size()) {
               return false;
            } else {
               var6 = var3.entrySet().iterator();

               Entry var12;
               Tag var13;
               do {
                  if (!var6.hasNext()) {
                     return true;
                  }

                  var12 = (Entry)var6.next();
                  var13 = (Tag)var12.getValue();
               } while(compareNbt(var13, var11.get((String)var12.getKey()), var2));

               return false;
            }
         } else {
            if (var0 instanceof ListTag) {
               ListTag var4 = (ListTag)var0;
               if (var2) {
                  ListTag var5 = (ListTag)var1;
                  if (var4.isEmpty()) {
                     return var5.isEmpty();
                  }

                  if (var5.size() < var4.size()) {
                     return false;
                  }

                  var6 = var4.iterator();

                  boolean var8;
                  do {
                     if (!var6.hasNext()) {
                        return true;
                     }

                     Tag var7 = (Tag)var6.next();
                     var8 = false;
                     Iterator var9 = var5.iterator();

                     while(var9.hasNext()) {
                        Tag var10 = (Tag)var9.next();
                        if (compareNbt(var7, var10, var2)) {
                           var8 = true;
                           break;
                        }
                     }
                  } while(var8);

                  return false;
               }
            }

            return var0.equals(var1);
         }
      }
   }

   public static BlockState readBlockState(HolderGetter<Block> var0, CompoundTag var1) {
      Optional var10000 = var1.read("Name", BLOCK_NAME_CODEC);
      Objects.requireNonNull(var0);
      Optional var2 = var10000.flatMap(var0::get);
      if (var2.isEmpty()) {
         return Blocks.AIR.defaultBlockState();
      } else {
         Block var3 = (Block)((Holder)var2.get()).value();
         BlockState var4 = var3.defaultBlockState();
         Optional var5 = var1.getCompound("Properties");
         if (var5.isPresent()) {
            StateDefinition var6 = var3.getStateDefinition();
            Iterator var7 = ((CompoundTag)var5.get()).keySet().iterator();

            while(var7.hasNext()) {
               String var8 = (String)var7.next();
               Property var9 = var6.getProperty(var8);
               if (var9 != null) {
                  var4 = (BlockState)setValueHelper(var4, var9, var8, (CompoundTag)var5.get(), var1);
               }
            }
         }

         return var4;
      }
   }

   private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(S var0, Property<T> var1, String var2, CompoundTag var3, CompoundTag var4) {
      Optional var10000 = var3.getString(var2);
      Objects.requireNonNull(var1);
      Optional var5 = var10000.flatMap(var1::getValue);
      if (var5.isPresent()) {
         return (StateHolder)var0.setValue(var1, (Comparable)var5.get());
      } else {
         LOGGER.warn("Unable to read property: {} with value: {} for blockstate: {}", new Object[]{var2, var3.get(var2), var4});
         return var0;
      }
   }

   public static CompoundTag writeBlockState(BlockState var0) {
      CompoundTag var1 = new CompoundTag();
      var1.putString("Name", BuiltInRegistries.BLOCK.getKey(var0.getBlock()).toString());
      Map var2 = var0.getValues();
      if (!var2.isEmpty()) {
         CompoundTag var3 = new CompoundTag();
         Iterator var4 = var2.entrySet().iterator();

         while(var4.hasNext()) {
            Entry var5 = (Entry)var4.next();
            Property var6 = (Property)var5.getKey();
            var3.putString(var6.getName(), getName(var6, (Comparable)var5.getValue()));
         }

         var1.put("Properties", var3);
      }

      return var1;
   }

   public static CompoundTag writeFluidState(FluidState var0) {
      CompoundTag var1 = new CompoundTag();
      var1.putString("Name", BuiltInRegistries.FLUID.getKey(var0.getType()).toString());
      Map var2 = var0.getValues();
      if (!var2.isEmpty()) {
         CompoundTag var3 = new CompoundTag();
         Iterator var4 = var2.entrySet().iterator();

         while(var4.hasNext()) {
            Entry var5 = (Entry)var4.next();
            Property var6 = (Property)var5.getKey();
            var3.putString(var6.getName(), getName(var6, (Comparable)var5.getValue()));
         }

         var1.put("Properties", var3);
      }

      return var1;
   }

   private static <T extends Comparable<T>> String getName(Property<T> var0, Comparable<?> var1) {
      return var0.getName(var1);
   }

   public static String prettyPrint(Tag var0) {
      return prettyPrint(var0, false);
   }

   public static String prettyPrint(Tag var0, boolean var1) {
      return prettyPrint(new StringBuilder(), var0, 0, var1).toString();
   }

   public static StringBuilder prettyPrint(StringBuilder var0, Tag var1, int var2, boolean var3) {
      Objects.requireNonNull(var1);
      byte var5 = 0;
      int var15;
      StringBuilder var10000;
      int var22;
      int var24;
      int var27;
      switch(var1.typeSwitch<invokedynamic>(var1, var5)) {
      case 0:
         PrimitiveTag var6 = (PrimitiveTag)var1;
         var10000 = var0.append(var6);
         break;
      case 1:
         EndTag var7 = (EndTag)var1;
         var10000 = var0;
         break;
      case 2:
         ByteArrayTag var8 = (ByteArrayTag)var1;
         byte[] var21 = var8.getAsByteArray();
         var22 = var21.length;
         indent(var2, var0).append("byte[").append(var22).append("] {\n");
         if (!var3) {
            indent(var2 + 1, var0).append(" // Skipped, supply withBinaryBlobs true");
         } else {
            indent(var2 + 1, var0);

            for(var24 = 0; var24 < var21.length; ++var24) {
               if (var24 != 0) {
                  var0.append(',');
               }

               if (var24 % 16 == 0 && var24 / 16 > 0) {
                  var0.append('\n');
                  if (var24 < var21.length) {
                     indent(var2 + 1, var0);
                  }
               } else if (var24 != 0) {
                  var0.append(' ');
               }

               var0.append(String.format(Locale.ROOT, "0x%02X", var21[var24] & 255));
            }
         }

         var0.append('\n');
         indent(var2, var0).append('}');
         var10000 = var0;
         break;
      case 3:
         ListTag var9 = (ListTag)var1;
         var22 = var9.size();
         indent(var2, var0).append("list").append("[").append(var22).append("] [");
         if (var22 != 0) {
            var0.append('\n');
         }

         for(var24 = 0; var24 < var22; ++var24) {
            if (var24 != 0) {
               var0.append(",\n");
            }

            indent(var2 + 1, var0);
            prettyPrint(var0, var9.get(var24), var2 + 1, var3);
         }

         if (var22 != 0) {
            var0.append('\n');
         }

         indent(var2, var0).append(']');
         var10000 = var0;
         break;
      case 4:
         IntArrayTag var10 = (IntArrayTag)var1;
         int[] var23 = var10.getAsIntArray();
         int var26 = 0;
         int[] var28 = var23;
         int var30 = var23.length;

         for(var15 = 0; var15 < var30; ++var15) {
            int var33 = var28[var15];
            var26 = Math.max(var26, String.format(Locale.ROOT, "%X", var33).length());
         }

         var27 = var23.length;
         indent(var2, var0).append("int[").append(var27).append("] {\n");
         if (!var3) {
            indent(var2 + 1, var0).append(" // Skipped, supply withBinaryBlobs true");
         } else {
            indent(var2 + 1, var0);

            for(var30 = 0; var30 < var23.length; ++var30) {
               if (var30 != 0) {
                  var0.append(',');
               }

               if (var30 % 16 == 0 && var30 / 16 > 0) {
                  var0.append('\n');
                  if (var30 < var23.length) {
                     indent(var2 + 1, var0);
                  }
               } else if (var30 != 0) {
                  var0.append(' ');
               }

               var0.append(String.format(Locale.ROOT, "0x%0" + var26 + "X", var23[var30]));
            }
         }

         var0.append('\n');
         indent(var2, var0).append('}');
         var10000 = var0;
         break;
      case 5:
         CompoundTag var11 = (CompoundTag)var1;
         ArrayList var25 = Lists.newArrayList(var11.keySet());
         Collections.sort(var25);
         indent(var2, var0).append('{');
         if (var0.length() - var0.lastIndexOf("\n") > 2 * (var2 + 1)) {
            var0.append('\n');
            indent(var2 + 1, var0);
         }

         var27 = var25.stream().mapToInt(String::length).max().orElse(0);
         String var29 = Strings.repeat(" ", var27);

         for(var15 = 0; var15 < var25.size(); ++var15) {
            if (var15 != 0) {
               var0.append(",\n");
            }

            String var32 = (String)var25.get(var15);
            indent(var2 + 1, var0).append('"').append(var32).append('"').append(var29, 0, var29.length() - var32.length()).append(": ");
            prettyPrint(var0, var11.get(var32), var2 + 1, var3);
         }

         if (!var25.isEmpty()) {
            var0.append('\n');
         }

         indent(var2, var0).append('}');
         var10000 = var0;
         break;
      case 6:
         LongArrayTag var12 = (LongArrayTag)var1;
         long[] var13 = var12.getAsLongArray();
         long var14 = 0L;
         long[] var16 = var13;
         int var17 = var13.length;

         int var18;
         for(var18 = 0; var18 < var17; ++var18) {
            long var19 = var16[var18];
            var14 = Math.max(var14, (long)String.format(Locale.ROOT, "%X", var19).length());
         }

         long var31 = (long)var13.length;
         indent(var2, var0).append("long[").append(var31).append("] {\n");
         if (!var3) {
            indent(var2 + 1, var0).append(" // Skipped, supply withBinaryBlobs true");
         } else {
            indent(var2 + 1, var0);

            for(var18 = 0; var18 < var13.length; ++var18) {
               if (var18 != 0) {
                  var0.append(',');
               }

               if (var18 % 16 == 0 && var18 / 16 > 0) {
                  var0.append('\n');
                  if (var18 < var13.length) {
                     indent(var2 + 1, var0);
                  }
               } else if (var18 != 0) {
                  var0.append(' ');
               }

               var0.append(String.format(Locale.ROOT, "0x%0" + var14 + "X", var13[var18]));
            }
         }

         var0.append('\n');
         indent(var2, var0).append('}');
         var10000 = var0;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private static StringBuilder indent(int var0, StringBuilder var1) {
      int var2 = var1.lastIndexOf("\n") + 1;
      int var3 = var1.length() - var2;

      for(int var4 = 0; var4 < 2 * var0 - var3; ++var4) {
         var1.append(' ');
      }

      return var1;
   }

   public static Component toPrettyComponent(Tag var0) {
      return (new TextComponentTagVisitor("")).visit(var0);
   }

   public static String structureToSnbt(CompoundTag var0) {
      return (new SnbtPrinterTagVisitor()).visit(packStructureTemplate(var0));
   }

   public static CompoundTag snbtToStructure(String var0) throws CommandSyntaxException {
      return unpackStructureTemplate(TagParser.parseCompoundFully(var0));
   }

   @VisibleForTesting
   static CompoundTag packStructureTemplate(CompoundTag var0) {
      Optional var2 = var0.getList("palettes");
      ListTag var1;
      if (var2.isPresent()) {
         var1 = ((ListTag)var2.get()).getListOrEmpty(0);
      } else {
         var1 = var0.getListOrEmpty("palette");
      }

      ListTag var3 = (ListTag)var1.compoundStream().map(NbtUtils::packBlockState).map(StringTag::valueOf).collect(Collectors.toCollection(ListTag::new));
      var0.put("palette", var3);
      if (var2.isPresent()) {
         ListTag var4 = new ListTag();
         ((ListTag)var2.get()).stream().flatMap((var0x) -> {
            return var0x.asList().stream();
         }).forEach((var2x) -> {
            CompoundTag var3x = new CompoundTag();

            for(int var4x = 0; var4x < var2x.size(); ++var4x) {
               var3x.putString((String)var3.getString(var4x).orElseThrow(), packBlockState((CompoundTag)var2x.getCompound(var4x).orElseThrow()));
            }

            var4.add(var3x);
         });
         var0.put("palettes", var4);
      }

      Optional var6 = var0.getList("entities");
      ListTag var5;
      if (var6.isPresent()) {
         var5 = (ListTag)((ListTag)var6.get()).compoundStream().sorted(Comparator.comparing((var0x) -> {
            return var0x.getList("pos");
         }, Comparators.emptiesLast(YXZ_LISTTAG_DOUBLE_COMPARATOR))).collect(Collectors.toCollection(ListTag::new));
         var0.put("entities", var5);
      }

      var5 = (ListTag)var0.getList("blocks").stream().flatMap(ListTag::compoundStream).sorted(Comparator.comparing((var0x) -> {
         return var0x.getList("pos");
      }, Comparators.emptiesLast(YXZ_LISTTAG_INT_COMPARATOR))).peek((var1x) -> {
         var1x.putString("state", (String)var3.getString(var1x.getIntOr("state", 0)).orElseThrow());
      }).collect(Collectors.toCollection(ListTag::new));
      var0.put("data", var5);
      var0.remove("blocks");
      return var0;
   }

   @VisibleForTesting
   static CompoundTag unpackStructureTemplate(CompoundTag var0) {
      ListTag var1 = var0.getListOrEmpty("palette");
      Map var2 = (Map)var1.stream().flatMap((var0x) -> {
         return var0x.asString().stream();
      }).collect(ImmutableMap.toImmutableMap(Function.identity(), NbtUtils::unpackBlockState));
      Optional var3 = var0.getList("palettes");
      if (var3.isPresent()) {
         var0.put("palettes", (Tag)((ListTag)var3.get()).compoundStream().map((var1x) -> {
            return (ListTag)var2.keySet().stream().map((var1) -> {
               return (String)var1x.getString(var1).orElseThrow();
            }).map(NbtUtils::unpackBlockState).collect(Collectors.toCollection(ListTag::new));
         }).collect(Collectors.toCollection(ListTag::new)));
         var0.remove("palette");
      } else {
         var0.put("palette", (Tag)var2.values().stream().collect(Collectors.toCollection(ListTag::new)));
      }

      Optional var4 = var0.getList("data");
      if (var4.isPresent()) {
         Object2IntOpenHashMap var5 = new Object2IntOpenHashMap();
         var5.defaultReturnValue(-1);

         for(int var6 = 0; var6 < var1.size(); ++var6) {
            var5.put((String)var1.getString(var6).orElseThrow(), var6);
         }

         ListTag var11 = (ListTag)var4.get();

         for(int var7 = 0; var7 < var11.size(); ++var7) {
            CompoundTag var8 = (CompoundTag)var11.getCompound(var7).orElseThrow();
            String var9 = (String)var8.getString("state").orElseThrow();
            int var10 = var5.getInt(var9);
            if (var10 == -1) {
               throw new IllegalStateException("Entry " + var9 + " missing from palette");
            }

            var8.putInt("state", var10);
         }

         var0.put("blocks", var11);
         var0.remove("data");
      }

      return var0;
   }

   @VisibleForTesting
   static String packBlockState(CompoundTag var0) {
      StringBuilder var1 = new StringBuilder((String)var0.getString("Name").orElseThrow());
      var0.getCompound("Properties").ifPresent((var1x) -> {
         String var2 = (String)var1x.entrySet().stream().sorted(Entry.comparingByKey()).map((var0) -> {
            String var10000 = (String)var0.getKey();
            return var10000 + ":" + (String)((Tag)var0.getValue()).asString().orElseThrow();
         }).collect(Collectors.joining(","));
         var1.append('{').append(var2).append('}');
      });
      return var1.toString();
   }

   @VisibleForTesting
   static CompoundTag unpackBlockState(String var0) {
      CompoundTag var1 = new CompoundTag();
      int var2 = var0.indexOf(123);
      String var3;
      if (var2 >= 0) {
         var3 = var0.substring(0, var2);
         CompoundTag var4 = new CompoundTag();
         if (var2 + 2 <= var0.length()) {
            String var5 = var0.substring(var2 + 1, var0.indexOf(125, var2));
            COMMA_SPLITTER.split(var5).forEach((var2x) -> {
               List var3 = COLON_SPLITTER.splitToList(var2x);
               if (var3.size() == 2) {
                  var4.putString((String)var3.get(0), (String)var3.get(1));
               } else {
                  LOGGER.error("Something went wrong parsing: '{}' -- incorrect gamedata!", var0);
               }

            });
            var1.put("Properties", var4);
         }
      } else {
         var3 = var0;
      }

      var1.putString("Name", var3);
      return var1;
   }

   public static CompoundTag addCurrentDataVersion(CompoundTag var0) {
      int var1 = SharedConstants.getCurrentVersion().dataVersion().version();
      return addDataVersion(var0, var1);
   }

   public static CompoundTag addDataVersion(CompoundTag var0, int var1) {
      var0.putInt("DataVersion", var1);
      return var0;
   }

   public static Dynamic<Tag> addCurrentDataVersion(Dynamic<Tag> var0) {
      int var1 = SharedConstants.getCurrentVersion().dataVersion().version();
      return addDataVersion(var0, var1);
   }

   public static Dynamic<Tag> addDataVersion(Dynamic<Tag> var0, int var1) {
      return var0.set("DataVersion", var0.createInt(var1));
   }

   public static void addCurrentDataVersion(ValueOutput var0) {
      int var1 = SharedConstants.getCurrentVersion().dataVersion().version();
      addDataVersion(var0, var1);
   }

   public static void addDataVersion(ValueOutput var0, int var1) {
      var0.putInt("DataVersion", var1);
   }

   public static int getDataVersion(CompoundTag var0, int var1) {
      return var0.getIntOr("DataVersion", var1);
   }

   public static int getDataVersion(Dynamic<?> var0, int var1) {
      return var0.get("DataVersion").asInt(var1);
   }

   static {
      BLOCK_NAME_CODEC = ResourceKey.codec(Registries.BLOCK);
      COMMA_SPLITTER = Splitter.on(",");
      COLON_SPLITTER = Splitter.on(':').limit(2);
      LOGGER = LogUtils.getLogger();
   }
}
