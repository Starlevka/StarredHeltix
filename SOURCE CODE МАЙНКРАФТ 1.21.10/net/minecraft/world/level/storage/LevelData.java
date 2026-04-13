package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;

public interface LevelData {
   LevelData.RespawnData getRespawnData();

   long getGameTime();

   long getDayTime();

   boolean isThundering();

   boolean isRaining();

   void setRaining(boolean var1);

   boolean isHardcore();

   Difficulty getDifficulty();

   boolean isDifficultyLocked();

   default void fillCrashReportCategory(CrashReportCategory var1, LevelHeightAccessor var2) {
      var1.setDetail("Level spawn location", () -> {
         return CrashReportCategory.formatLocation(var2, this.getRespawnData().pos());
      });
      var1.setDetail("Level time", () -> {
         return String.format(Locale.ROOT, "%d game time, %d day time", this.getGameTime(), this.getDayTime());
      });
   }

   public static record RespawnData(GlobalPos globalPos, float yaw, float pitch) {
      public static final LevelData.RespawnData DEFAULT;
      public static final MapCodec<LevelData.RespawnData> MAP_CODEC;
      public static final Codec<LevelData.RespawnData> CODEC;
      public static final StreamCodec<ByteBuf, LevelData.RespawnData> STREAM_CODEC;

      public RespawnData(GlobalPos param1, float param2, float param3) {
         super();
         this.globalPos = var1;
         this.yaw = var2;
         this.pitch = var3;
      }

      public static LevelData.RespawnData of(ResourceKey<Level> var0, BlockPos var1, float var2, float var3) {
         return new LevelData.RespawnData(GlobalPos.of(var0, var1.immutable()), var2, var3);
      }

      public ResourceKey<Level> dimension() {
         return this.globalPos.dimension();
      }

      public BlockPos pos() {
         return this.globalPos.pos();
      }

      public GlobalPos globalPos() {
         return this.globalPos;
      }

      public float yaw() {
         return this.yaw;
      }

      public float pitch() {
         return this.pitch;
      }

      static {
         DEFAULT = new LevelData.RespawnData(GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO), 0.0F, 0.0F);
         MAP_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
            return var0.group(GlobalPos.MAP_CODEC.forGetter(LevelData.RespawnData::globalPos), Codec.floatRange(-180.0F, 180.0F).fieldOf("yaw").forGetter(LevelData.RespawnData::yaw), Codec.floatRange(-90.0F, 90.0F).fieldOf("pitch").forGetter(LevelData.RespawnData::pitch)).apply(var0, LevelData.RespawnData::new);
         });
         CODEC = MAP_CODEC.codec();
         STREAM_CODEC = StreamCodec.composite(GlobalPos.STREAM_CODEC, LevelData.RespawnData::globalPos, ByteBufCodecs.FLOAT, LevelData.RespawnData::yaw, ByteBufCodecs.FLOAT, LevelData.RespawnData::pitch, LevelData.RespawnData::new);
      }
   }
}
