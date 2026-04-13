package net.minecraft.world.level.saveddata.maps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record MapFrame(BlockPos pos, int rotation, int entityId) {
   public static final Codec<MapFrame> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(BlockPos.CODEC.fieldOf("pos").forGetter(MapFrame::pos), Codec.INT.fieldOf("rotation").forGetter(MapFrame::rotation), Codec.INT.fieldOf("entity_id").forGetter(MapFrame::entityId)).apply(var0, MapFrame::new);
   });

   public MapFrame(BlockPos param1, int param2, int param3) {
      super();
      this.pos = var1;
      this.rotation = var2;
      this.entityId = var3;
   }

   public String getId() {
      return frameId(this.pos);
   }

   public static String frameId(BlockPos var0) {
      int var10000 = var0.getX();
      return "frame-" + var10000 + "," + var0.getY() + "," + var0.getZ();
   }

   public BlockPos pos() {
      return this.pos;
   }

   public int rotation() {
      return this.rotation;
   }

   public int entityId() {
      return this.entityId;
   }
}
