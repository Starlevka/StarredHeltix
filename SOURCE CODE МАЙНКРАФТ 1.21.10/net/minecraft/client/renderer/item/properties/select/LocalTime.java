package net.minecraft.client.renderer.item.properties.select;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class LocalTime implements SelectItemModelProperty<String> {
   public static final String ROOT_LOCALE = "";
   private static final long UPDATE_INTERVAL_MS;
   public static final Codec<String> VALUE_CODEC;
   private static final Codec<TimeZone> TIME_ZONE_CODEC;
   private static final MapCodec<LocalTime.Data> DATA_MAP_CODEC;
   public static final SelectItemModelProperty.Type<LocalTime, String> TYPE;
   private final LocalTime.Data data;
   private final DateFormat parsedFormat;
   private long nextUpdateTimeMs;
   private String lastResult = "";

   private LocalTime(LocalTime.Data var1, DateFormat var2) {
      super();
      this.data = var1;
      this.parsedFormat = var2;
   }

   public static LocalTime create(String var0, String var1, Optional<TimeZone> var2) {
      return (LocalTime)create(new LocalTime.Data(var0, var1, var2)).getOrThrow((var0x) -> {
         return new IllegalStateException("Failed to validate format: " + var0x);
      });
   }

   private static DataResult<LocalTime> create(LocalTime.Data var0) {
      ULocale var1 = new ULocale(var0.localeId);
      Calendar var2 = (Calendar)var0.timeZone.map((var1x) -> {
         return Calendar.getInstance(var1x, var1);
      }).orElseGet(() -> {
         return Calendar.getInstance(var1);
      });
      SimpleDateFormat var3 = new SimpleDateFormat(var0.format, var1);
      var3.setCalendar(var2);

      try {
         var3.format(new Date());
      } catch (Exception var5) {
         return DataResult.error(() -> {
            String var10000 = String.valueOf(var3);
            return "Invalid time format '" + var10000 + "': " + var5.getMessage();
         });
      }

      return DataResult.success(new LocalTime(var0, var3));
   }

   @Nullable
   public String get(ItemStack var1, @Nullable ClientLevel var2, @Nullable LivingEntity var3, int var4, ItemDisplayContext var5) {
      long var6 = Util.getMillis();
      if (var6 > this.nextUpdateTimeMs) {
         this.lastResult = this.update();
         this.nextUpdateTimeMs = var6 + UPDATE_INTERVAL_MS;
      }

      return this.lastResult;
   }

   private String update() {
      return this.parsedFormat.format(new Date());
   }

   public SelectItemModelProperty.Type<LocalTime, String> type() {
      return TYPE;
   }

   public Codec<String> valueCodec() {
      return VALUE_CODEC;
   }

   // $FF: synthetic method
   @Nullable
   public Object get(final ItemStack param1, @Nullable final ClientLevel param2, @Nullable final LivingEntity param3, final int param4, final ItemDisplayContext param5) {
      return this.get(var1, var2, var3, var4, var5);
   }

   static {
      UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1L);
      VALUE_CODEC = Codec.STRING;
      TIME_ZONE_CODEC = VALUE_CODEC.comapFlatMap((var0) -> {
         TimeZone var1 = TimeZone.getTimeZone(var0);
         return var1.equals(TimeZone.UNKNOWN_ZONE) ? DataResult.error(() -> {
            return "Unknown timezone: " + var0;
         }) : DataResult.success(var1);
      }, TimeZone::getID);
      DATA_MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.STRING.fieldOf("pattern").forGetter((var0x) -> {
            return var0x.format;
         }), Codec.STRING.optionalFieldOf("locale", "").forGetter((var0x) -> {
            return var0x.localeId;
         }), TIME_ZONE_CODEC.optionalFieldOf("time_zone").forGetter((var0x) -> {
            return var0x.timeZone;
         })).apply(var0, LocalTime.Data::new);
      });
      TYPE = SelectItemModelProperty.Type.create(DATA_MAP_CODEC.flatXmap(LocalTime::create, (var0) -> {
         return DataResult.success(var0.data);
      }), VALUE_CODEC);
   }

   static record Data(String format, String localeId, Optional<TimeZone> timeZone) {
      final String format;
      final String localeId;
      final Optional<TimeZone> timeZone;

      Data(String param1, String param2, Optional<TimeZone> param3) {
         super();
         this.format = var1;
         this.localeId = var2;
         this.timeZone = var3;
      }

      public String format() {
         return this.format;
      }

      public String localeId() {
         return this.localeId;
      }

      public Optional<TimeZone> timeZone() {
         return this.timeZone;
      }
   }
}
