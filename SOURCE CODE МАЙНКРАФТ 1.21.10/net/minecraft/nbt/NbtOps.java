package net.minecraft.nbt;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractStringBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;

public class NbtOps implements DynamicOps<Tag> {
   public static final NbtOps INSTANCE = new NbtOps();

   private NbtOps() {
      super();
   }

   public Tag empty() {
      return EndTag.INSTANCE;
   }

   public <U> U convertTo(DynamicOps<U> var1, Tag var2) {
      Objects.requireNonNull(var2);
      byte var4 = 0;
      Object var10000;
      boolean var10001;
      Throwable var40;
      switch(var2.typeSwitch<invokedynamic>(var2, var4)) {
      case 0:
         EndTag var5 = (EndTag)var2;
         var10000 = (Object)var1.empty();
         return var10000;
      case 1:
         ByteTag var6 = (ByteTag)var2;
         ByteTag var54 = var6;

         byte var55;
         try {
            var55 = var54.value();
         } catch (Throwable var33) {
            var40 = var33;
            var10001 = false;
            break;
         }

         byte var34 = var55;
         var10000 = (Object)var1.createByte(var34);
         return var10000;
      case 2:
         ShortTag var8 = (ShortTag)var2;
         ShortTag var52 = var8;

         short var53;
         try {
            var53 = var52.value();
         } catch (Throwable var32) {
            var40 = var32;
            var10001 = false;
            break;
         }

         short var35 = var53;
         var10000 = (Object)var1.createShort(var35);
         return var10000;
      case 3:
         IntTag var10 = (IntTag)var2;
         IntTag var49 = var10;

         int var51;
         try {
            var51 = var49.value();
         } catch (Throwable var31) {
            var40 = var31;
            var10001 = false;
            break;
         }

         int var36 = var51;
         var10000 = (Object)var1.createInt(var36);
         return var10000;
      case 4:
         LongTag var12 = (LongTag)var2;
         LongTag var47 = var12;

         long var48;
         try {
            var48 = var47.value();
         } catch (Throwable var30) {
            var40 = var30;
            var10001 = false;
            break;
         }

         long var37 = var48;
         var10000 = (Object)var1.createLong(var37);
         return var10000;
      case 5:
         FloatTag var15 = (FloatTag)var2;
         FloatTag var44 = var15;

         float var46;
         try {
            var46 = var44.value();
         } catch (Throwable var29) {
            var40 = var29;
            var10001 = false;
            break;
         }

         float var39 = var46;
         var10000 = (Object)var1.createFloat(var39);
         return var10000;
      case 6:
         DoubleTag var17 = (DoubleTag)var2;
         DoubleTag var42 = var17;

         double var43;
         try {
            var43 = var42.value();
         } catch (Throwable var28) {
            var40 = var28;
            var10001 = false;
            break;
         }

         double var45 = var43;
         var10000 = (Object)var1.createDouble(var45);
         return var10000;
      case 7:
         ByteArrayTag var20 = (ByteArrayTag)var2;
         var10000 = (Object)var1.createByteList(ByteBuffer.wrap(var20.getAsByteArray()));
         return var10000;
      case 8:
         StringTag var21 = (StringTag)var2;
         StringTag var38 = var21;

         String var41;
         try {
            var41 = var38.value();
         } catch (Throwable var27) {
            var40 = var27;
            var10001 = false;
            break;
         }

         String var50 = var41;
         var10000 = (Object)var1.createString(var50);
         return var10000;
      case 9:
         ListTag var23 = (ListTag)var2;
         var10000 = (Object)this.convertList(var1, var23);
         return var10000;
      case 10:
         CompoundTag var24 = (CompoundTag)var2;
         var10000 = (Object)this.convertMap(var1, var24);
         return var10000;
      case 11:
         IntArrayTag var25 = (IntArrayTag)var2;
         var10000 = (Object)var1.createIntList(Arrays.stream(var25.getAsIntArray()));
         return var10000;
      case 12:
         LongArrayTag var26 = (LongArrayTag)var2;
         var10000 = (Object)var1.createLongList(Arrays.stream(var26.getAsLongArray()));
         return var10000;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      Throwable var3 = var40;
      throw new MatchException(var3.toString(), var3);
   }

   public DataResult<Number> getNumberValue(Tag var1) {
      return (DataResult)var1.asNumber().map(DataResult::success).orElseGet(() -> {
         return DataResult.error(() -> {
            return "Not a number";
         });
      });
   }

   public Tag createNumeric(Number var1) {
      return DoubleTag.valueOf(var1.doubleValue());
   }

   public Tag createByte(byte var1) {
      return ByteTag.valueOf(var1);
   }

   public Tag createShort(short var1) {
      return ShortTag.valueOf(var1);
   }

   public Tag createInt(int var1) {
      return IntTag.valueOf(var1);
   }

   public Tag createLong(long var1) {
      return LongTag.valueOf(var1);
   }

   public Tag createFloat(float var1) {
      return FloatTag.valueOf(var1);
   }

   public Tag createDouble(double var1) {
      return DoubleTag.valueOf(var1);
   }

   public Tag createBoolean(boolean var1) {
      return ByteTag.valueOf(var1);
   }

   public DataResult<String> getStringValue(Tag var1) {
      if (var1 instanceof StringTag) {
         StringTag var2 = (StringTag)var1;
         StringTag var10000 = var2;

         String var6;
         try {
            var6 = var10000.value();
         } catch (Throwable var5) {
            throw new MatchException(var5.toString(), var5);
         }

         String var4 = var6;
         return DataResult.success(var4);
      } else {
         return DataResult.error(() -> {
            return "Not a string";
         });
      }
   }

   public Tag createString(String var1) {
      return StringTag.valueOf(var1);
   }

   public DataResult<Tag> mergeToList(Tag var1, Tag var2) {
      return (DataResult)createCollector(var1).map((var1x) -> {
         return DataResult.success(var1x.accept(var2).result());
      }).orElseGet(() -> {
         return DataResult.error(() -> {
            return "mergeToList called with not a list: " + String.valueOf(var1);
         }, var1);
      });
   }

   public DataResult<Tag> mergeToList(Tag var1, List<Tag> var2) {
      return (DataResult)createCollector(var1).map((var1x) -> {
         return DataResult.success(var1x.acceptAll((Iterable)var2).result());
      }).orElseGet(() -> {
         return DataResult.error(() -> {
            return "mergeToList called with not a list: " + String.valueOf(var1);
         }, var1);
      });
   }

   public DataResult<Tag> mergeToMap(Tag var1, Tag var2, Tag var3) {
      if (!(var1 instanceof CompoundTag) && !(var1 instanceof EndTag)) {
         return DataResult.error(() -> {
            return "mergeToMap called with not a map: " + String.valueOf(var1);
         }, var1);
      } else if (var2 instanceof StringTag) {
         StringTag var5 = (StringTag)var2;
         StringTag var10000 = var5;

         String var9;
         try {
            var9 = var10000.value();
         } catch (Throwable var7) {
            throw new MatchException(var7.toString(), var7);
         }

         Object var6 = var9;
         CompoundTag var10;
         if (var1 instanceof CompoundTag) {
            var6 = (CompoundTag)var1;
            var10 = ((CompoundTag)var6).shallowCopy();
         } else {
            var10 = new CompoundTag();
         }

         CompoundTag var8 = var10;
         var8.put((String)var6, var3);
         return DataResult.success(var8);
      } else {
         return DataResult.error(() -> {
            return "key is not a string: " + String.valueOf(var2);
         }, var1);
      }
   }

   public DataResult<Tag> mergeToMap(Tag var1, MapLike<Tag> var2) {
      if (!(var1 instanceof CompoundTag) && !(var1 instanceof EndTag)) {
         return DataResult.error(() -> {
            return "mergeToMap called with not a map: " + String.valueOf(var1);
         }, var1);
      } else {
         CompoundTag var10000;
         if (var1 instanceof CompoundTag) {
            CompoundTag var4 = (CompoundTag)var1;
            var10000 = var4.shallowCopy();
         } else {
            var10000 = new CompoundTag();
         }

         CompoundTag var3 = var10000;
         ArrayList var5 = new ArrayList();
         var2.entries().forEach((var2x) -> {
            Tag var3x = (Tag)var2x.getFirst();
            if (var3x instanceof StringTag) {
               StringTag var5x = (StringTag)var3x;
               StringTag var10000 = var5x;

               String var8;
               try {
                  var8 = var10000.value();
               } catch (Throwable var7) {
                  throw new MatchException(var7.toString(), var7);
               }

               String var6 = var8;
               var3.put(var6, (Tag)var2x.getSecond());
            } else {
               var5.add(var3x);
            }
         });
         return !var5.isEmpty() ? DataResult.error(() -> {
            return "some keys are not strings: " + String.valueOf(var5);
         }, var3) : DataResult.success(var3);
      }
   }

   public DataResult<Tag> mergeToMap(Tag var1, Map<Tag, Tag> var2) {
      if (!(var1 instanceof CompoundTag) && !(var1 instanceof EndTag)) {
         return DataResult.error(() -> {
            return "mergeToMap called with not a map: " + String.valueOf(var1);
         }, var1);
      } else {
         CompoundTag var10000;
         if (var1 instanceof CompoundTag) {
            CompoundTag var4 = (CompoundTag)var1;
            var10000 = var4.shallowCopy();
         } else {
            var10000 = new CompoundTag();
         }

         CompoundTag var3 = var10000;
         ArrayList var12 = new ArrayList();
         Iterator var5 = var2.entrySet().iterator();

         while(var5.hasNext()) {
            Entry var6 = (Entry)var5.next();
            Tag var7 = (Tag)var6.getKey();
            if (var7 instanceof StringTag) {
               StringTag var8 = (StringTag)var7;
               StringTag var13 = var8;

               String var14;
               try {
                  var14 = var13.value();
               } catch (Throwable var11) {
                  throw new MatchException(var11.toString(), var11);
               }

               String var10 = var14;
               var3.put(var10, (Tag)var6.getValue());
            } else {
               var12.add(var7);
            }
         }

         if (!var12.isEmpty()) {
            return DataResult.error(() -> {
               return "some keys are not strings: " + String.valueOf(var12);
            }, var3);
         } else {
            return DataResult.success(var3);
         }
      }
   }

   public DataResult<Stream<Pair<Tag, Tag>>> getMapValues(Tag var1) {
      if (var1 instanceof CompoundTag) {
         CompoundTag var2 = (CompoundTag)var1;
         return DataResult.success(var2.entrySet().stream().map((var1x) -> {
            return Pair.of(this.createString((String)var1x.getKey()), (Tag)var1x.getValue());
         }));
      } else {
         return DataResult.error(() -> {
            return "Not a map: " + String.valueOf(var1);
         });
      }
   }

   public DataResult<Consumer<BiConsumer<Tag, Tag>>> getMapEntries(Tag var1) {
      if (var1 instanceof CompoundTag) {
         CompoundTag var2 = (CompoundTag)var1;
         return DataResult.success((var2x) -> {
            Iterator var3 = var2.entrySet().iterator();

            while(var3.hasNext()) {
               Entry var4 = (Entry)var3.next();
               var2x.accept(this.createString((String)var4.getKey()), (Tag)var4.getValue());
            }

         });
      } else {
         return DataResult.error(() -> {
            return "Not a map: " + String.valueOf(var1);
         });
      }
   }

   public DataResult<MapLike<Tag>> getMap(Tag var1) {
      if (var1 instanceof CompoundTag) {
         final CompoundTag var2 = (CompoundTag)var1;
         return DataResult.success(new MapLike<Tag>() {
            @Nullable
            public Tag get(Tag var1) {
               if (var1 instanceof StringTag) {
                  StringTag var2x = (StringTag)var1;
                  StringTag var10000 = var2x;

                  String var6;
                  try {
                     var6 = var10000.value();
                  } catch (Throwable var5) {
                     throw new MatchException(var5.toString(), var5);
                  }

                  String var4 = var6;
                  return var2.get(var4);
               } else {
                  throw new UnsupportedOperationException("Cannot get map entry with non-string key: " + String.valueOf(var1));
               }
            }

            @Nullable
            public Tag get(String var1) {
               return var2.get(var1);
            }

            public Stream<Pair<Tag, Tag>> entries() {
               return var2.entrySet().stream().map((var1) -> {
                  return Pair.of(NbtOps.this.createString((String)var1.getKey()), (Tag)var1.getValue());
               });
            }

            public String toString() {
               return "MapLike[" + String.valueOf(var2) + "]";
            }

            // $FF: synthetic method
            @Nullable
            public Object get(final String param1) {
               return this.get(var1);
            }

            // $FF: synthetic method
            @Nullable
            public Object get(final Object param1) {
               return this.get((Tag)var1);
            }
         });
      } else {
         return DataResult.error(() -> {
            return "Not a map: " + String.valueOf(var1);
         });
      }
   }

   public Tag createMap(Stream<Pair<Tag, Tag>> var1) {
      CompoundTag var2 = new CompoundTag();
      var1.forEach((var1x) -> {
         Tag var2x = (Tag)var1x.getFirst();
         Tag var3 = (Tag)var1x.getSecond();
         if (var2x instanceof StringTag) {
            StringTag var5 = (StringTag)var2x;
            StringTag var10000 = var5;

            String var8;
            try {
               var8 = var10000.value();
            } catch (Throwable var7) {
               throw new MatchException(var7.toString(), var7);
            }

            String var6 = var8;
            var2.put(var6, var3);
         } else {
            throw new UnsupportedOperationException("Cannot create map with non-string key: " + String.valueOf(var2x));
         }
      });
      return var2;
   }

   public DataResult<Stream<Tag>> getStream(Tag var1) {
      if (var1 instanceof CollectionTag) {
         CollectionTag var2 = (CollectionTag)var1;
         return DataResult.success(var2.stream());
      } else {
         return DataResult.error(() -> {
            return "Not a list";
         });
      }
   }

   public DataResult<Consumer<Consumer<Tag>>> getList(Tag var1) {
      if (var1 instanceof CollectionTag) {
         CollectionTag var2 = (CollectionTag)var1;
         Objects.requireNonNull(var2);
         return DataResult.success(var2::forEach);
      } else {
         return DataResult.error(() -> {
            return "Not a list: " + String.valueOf(var1);
         });
      }
   }

   public DataResult<ByteBuffer> getByteBuffer(Tag var1) {
      if (var1 instanceof ByteArrayTag) {
         ByteArrayTag var2 = (ByteArrayTag)var1;
         return DataResult.success(ByteBuffer.wrap(var2.getAsByteArray()));
      } else {
         return super.getByteBuffer(var1);
      }
   }

   public Tag createByteList(ByteBuffer var1) {
      ByteBuffer var2 = var1.duplicate().clear();
      byte[] var3 = new byte[var1.capacity()];
      var2.get(0, var3, 0, var3.length);
      return new ByteArrayTag(var3);
   }

   public DataResult<IntStream> getIntStream(Tag var1) {
      if (var1 instanceof IntArrayTag) {
         IntArrayTag var2 = (IntArrayTag)var1;
         return DataResult.success(Arrays.stream(var2.getAsIntArray()));
      } else {
         return super.getIntStream(var1);
      }
   }

   public Tag createIntList(IntStream var1) {
      return new IntArrayTag(var1.toArray());
   }

   public DataResult<LongStream> getLongStream(Tag var1) {
      if (var1 instanceof LongArrayTag) {
         LongArrayTag var2 = (LongArrayTag)var1;
         return DataResult.success(Arrays.stream(var2.getAsLongArray()));
      } else {
         return super.getLongStream(var1);
      }
   }

   public Tag createLongList(LongStream var1) {
      return new LongArrayTag(var1.toArray());
   }

   public Tag createList(Stream<Tag> var1) {
      return new ListTag((List)var1.collect(Util.toMutableList()));
   }

   public Tag remove(Tag var1, String var2) {
      if (var1 instanceof CompoundTag) {
         CompoundTag var3 = (CompoundTag)var1;
         CompoundTag var4 = var3.shallowCopy();
         var4.remove(var2);
         return var4;
      } else {
         return var1;
      }
   }

   public String toString() {
      return "NBT";
   }

   public RecordBuilder<Tag> mapBuilder() {
      return new NbtOps.NbtRecordBuilder(this);
   }

   private static Optional<NbtOps.ListCollector> createCollector(Tag var0) {
      if (var0 instanceof EndTag) {
         return Optional.of(new NbtOps.GenericListCollector());
      } else if (var0 instanceof CollectionTag) {
         CollectionTag var1 = (CollectionTag)var0;
         if (var1.isEmpty()) {
            return Optional.of(new NbtOps.GenericListCollector());
         } else {
            Objects.requireNonNull(var1);
            byte var3 = 0;
            Optional var10000;
            switch(var1.typeSwitch<invokedynamic>(var1, var3)) {
            case 0:
               ListTag var4 = (ListTag)var1;
               var10000 = Optional.of(new NbtOps.GenericListCollector(var4));
               break;
            case 1:
               ByteArrayTag var5 = (ByteArrayTag)var1;
               var10000 = Optional.of(new NbtOps.ByteListCollector(var5.getAsByteArray()));
               break;
            case 2:
               IntArrayTag var6 = (IntArrayTag)var1;
               var10000 = Optional.of(new NbtOps.IntListCollector(var6.getAsIntArray()));
               break;
            case 3:
               LongArrayTag var7 = (LongArrayTag)var1;
               var10000 = Optional.of(new NbtOps.LongListCollector(var7.getAsLongArray()));
               break;
            default:
               throw new MatchException((String)null, (Throwable)null);
            }

            return var10000;
         }
      } else {
         return Optional.empty();
      }
   }

   // $FF: synthetic method
   public Object remove(final Object param1, final String param2) {
      return this.remove((Tag)var1, var2);
   }

   // $FF: synthetic method
   public Object createLongList(final LongStream param1) {
      return this.createLongList(var1);
   }

   // $FF: synthetic method
   public DataResult getLongStream(final Object param1) {
      return this.getLongStream((Tag)var1);
   }

   // $FF: synthetic method
   public Object createIntList(final IntStream param1) {
      return this.createIntList(var1);
   }

   // $FF: synthetic method
   public DataResult getIntStream(final Object param1) {
      return this.getIntStream((Tag)var1);
   }

   // $FF: synthetic method
   public Object createByteList(final ByteBuffer param1) {
      return this.createByteList(var1);
   }

   // $FF: synthetic method
   public DataResult getByteBuffer(final Object param1) {
      return this.getByteBuffer((Tag)var1);
   }

   // $FF: synthetic method
   public Object createList(final Stream param1) {
      return this.createList(var1);
   }

   // $FF: synthetic method
   public DataResult getList(final Object param1) {
      return this.getList((Tag)var1);
   }

   // $FF: synthetic method
   public DataResult getStream(final Object param1) {
      return this.getStream((Tag)var1);
   }

   // $FF: synthetic method
   public DataResult getMap(final Object param1) {
      return this.getMap((Tag)var1);
   }

   // $FF: synthetic method
   public Object createMap(final Stream param1) {
      return this.createMap(var1);
   }

   // $FF: synthetic method
   public DataResult getMapEntries(final Object param1) {
      return this.getMapEntries((Tag)var1);
   }

   // $FF: synthetic method
   public DataResult getMapValues(final Object param1) {
      return this.getMapValues((Tag)var1);
   }

   // $FF: synthetic method
   public DataResult mergeToMap(final Object param1, final MapLike param2) {
      return this.mergeToMap((Tag)var1, var2);
   }

   // $FF: synthetic method
   public DataResult mergeToMap(final Object param1, final Map param2) {
      return this.mergeToMap((Tag)var1, var2);
   }

   // $FF: synthetic method
   public DataResult mergeToMap(final Object param1, final Object param2, final Object param3) {
      return this.mergeToMap((Tag)var1, (Tag)var2, (Tag)var3);
   }

   // $FF: synthetic method
   public DataResult mergeToList(final Object param1, final List param2) {
      return this.mergeToList((Tag)var1, var2);
   }

   // $FF: synthetic method
   public DataResult mergeToList(final Object param1, final Object param2) {
      return this.mergeToList((Tag)var1, (Tag)var2);
   }

   // $FF: synthetic method
   public Object createString(final String param1) {
      return this.createString(var1);
   }

   // $FF: synthetic method
   public DataResult getStringValue(final Object param1) {
      return this.getStringValue((Tag)var1);
   }

   // $FF: synthetic method
   public Object createBoolean(final boolean param1) {
      return this.createBoolean(var1);
   }

   // $FF: synthetic method
   public Object createDouble(final double param1) {
      return this.createDouble(var1);
   }

   // $FF: synthetic method
   public Object createFloat(final float param1) {
      return this.createFloat(var1);
   }

   // $FF: synthetic method
   public Object createLong(final long param1) {
      return this.createLong(var1);
   }

   // $FF: synthetic method
   public Object createInt(final int param1) {
      return this.createInt(var1);
   }

   // $FF: synthetic method
   public Object createShort(final short param1) {
      return this.createShort(var1);
   }

   // $FF: synthetic method
   public Object createByte(final byte param1) {
      return this.createByte(var1);
   }

   // $FF: synthetic method
   public Object createNumeric(final Number param1) {
      return this.createNumeric(var1);
   }

   // $FF: synthetic method
   public DataResult getNumberValue(final Object param1) {
      return this.getNumberValue((Tag)var1);
   }

   // $FF: synthetic method
   public Object convertTo(final DynamicOps param1, final Object param2) {
      return this.convertTo(var1, (Tag)var2);
   }

   // $FF: synthetic method
   public Object empty() {
      return this.empty();
   }

   class NbtRecordBuilder extends AbstractStringBuilder<Tag, CompoundTag> {
      protected NbtRecordBuilder(final NbtOps param1) {
         super(var1);
      }

      protected CompoundTag initBuilder() {
         return new CompoundTag();
      }

      protected CompoundTag append(String var1, Tag var2, CompoundTag var3) {
         var3.put(var1, var2);
         return var3;
      }

      protected DataResult<Tag> build(CompoundTag var1, Tag var2) {
         if (var2 != null && var2 != EndTag.INSTANCE) {
            if (!(var2 instanceof CompoundTag)) {
               return DataResult.error(() -> {
                  return "mergeToMap called with not a map: " + String.valueOf(var2);
               }, var2);
            } else {
               CompoundTag var3 = (CompoundTag)var2;
               CompoundTag var4 = var3.shallowCopy();
               Iterator var5 = var1.entrySet().iterator();

               while(var5.hasNext()) {
                  Entry var6 = (Entry)var5.next();
                  var4.put((String)var6.getKey(), (Tag)var6.getValue());
               }

               return DataResult.success(var4);
            }
         } else {
            return DataResult.success(var1);
         }
      }

      // $FF: synthetic method
      protected Object append(final String param1, final Object param2, final Object param3) {
         return this.append(var1, (Tag)var2, (CompoundTag)var3);
      }

      // $FF: synthetic method
      protected DataResult build(final Object param1, final Object param2) {
         return this.build((CompoundTag)var1, (Tag)var2);
      }

      // $FF: synthetic method
      protected Object initBuilder() {
         return this.initBuilder();
      }
   }

   private static class GenericListCollector implements NbtOps.ListCollector {
      private final ListTag result = new ListTag();

      GenericListCollector() {
         super();
      }

      GenericListCollector(ListTag var1) {
         super();
         this.result.addAll(var1);
      }

      public GenericListCollector(IntArrayList var1) {
         super();
         var1.forEach((var1x) -> {
            this.result.add(IntTag.valueOf(var1x));
         });
      }

      public GenericListCollector(ByteArrayList var1) {
         super();
         var1.forEach((var1x) -> {
            this.result.add(ByteTag.valueOf(var1x));
         });
      }

      public GenericListCollector(LongArrayList var1) {
         super();
         var1.forEach((var1x) -> {
            this.result.add(LongTag.valueOf(var1x));
         });
      }

      public NbtOps.ListCollector accept(Tag var1) {
         this.result.add(var1);
         return this;
      }

      public Tag result() {
         return this.result;
      }
   }

   private static class ByteListCollector implements NbtOps.ListCollector {
      private final ByteArrayList values = new ByteArrayList();

      public ByteListCollector(byte[] var1) {
         super();
         this.values.addElements(0, var1);
      }

      public NbtOps.ListCollector accept(Tag var1) {
         if (var1 instanceof ByteTag) {
            ByteTag var2 = (ByteTag)var1;
            this.values.add(var2.byteValue());
            return this;
         } else {
            return (new NbtOps.GenericListCollector(this.values)).accept(var1);
         }
      }

      public Tag result() {
         return new ByteArrayTag(this.values.toByteArray());
      }
   }

   static class IntListCollector implements NbtOps.ListCollector {
      private final IntArrayList values = new IntArrayList();

      public IntListCollector(int[] var1) {
         super();
         this.values.addElements(0, var1);
      }

      public NbtOps.ListCollector accept(Tag var1) {
         if (var1 instanceof IntTag) {
            IntTag var2 = (IntTag)var1;
            this.values.add(var2.intValue());
            return this;
         } else {
            return (new NbtOps.GenericListCollector(this.values)).accept(var1);
         }
      }

      public Tag result() {
         return new IntArrayTag(this.values.toIntArray());
      }
   }

   static class LongListCollector implements NbtOps.ListCollector {
      private final LongArrayList values = new LongArrayList();

      public LongListCollector(long[] var1) {
         super();
         this.values.addElements(0, var1);
      }

      public NbtOps.ListCollector accept(Tag var1) {
         if (var1 instanceof LongTag) {
            LongTag var2 = (LongTag)var1;
            this.values.add(var2.longValue());
            return this;
         } else {
            return (new NbtOps.GenericListCollector(this.values)).accept(var1);
         }
      }

      public Tag result() {
         return new LongArrayTag(this.values.toLongArray());
      }
   }

   private interface ListCollector {
      NbtOps.ListCollector accept(Tag var1);

      default NbtOps.ListCollector acceptAll(Iterable<Tag> var1) {
         NbtOps.ListCollector var2 = this;

         Tag var4;
         for(Iterator var3 = var1.iterator(); var3.hasNext(); var2 = var2.accept(var4)) {
            var4 = (Tag)var3.next();
         }

         return var2;
      }

      default NbtOps.ListCollector acceptAll(Stream<Tag> var1) {
         Objects.requireNonNull(var1);
         return this.acceptAll(var1::iterator);
      }

      Tag result();
   }
}
