package net.minecraft.util.debug;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record DebugStructureInfo(BoundingBox boundingBox, List<DebugStructureInfo.Piece> pieces) {
   public static final StreamCodec<ByteBuf, DebugStructureInfo> STREAM_CODEC;

   public DebugStructureInfo(BoundingBox param1, List<DebugStructureInfo.Piece> param2) {
      super();
      this.boundingBox = var1;
      this.pieces = var2;
   }

   public BoundingBox boundingBox() {
      return this.boundingBox;
   }

   public List<DebugStructureInfo.Piece> pieces() {
      return this.pieces;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(BoundingBox.STREAM_CODEC, DebugStructureInfo::boundingBox, DebugStructureInfo.Piece.STREAM_CODEC.apply(ByteBufCodecs.list()), DebugStructureInfo::pieces, DebugStructureInfo::new);
   }

   public static record Piece(BoundingBox boundingBox, boolean isStart) {
      public static final StreamCodec<ByteBuf, DebugStructureInfo.Piece> STREAM_CODEC;

      public Piece(BoundingBox param1, boolean param2) {
         super();
         this.boundingBox = var1;
         this.isStart = var2;
      }

      public BoundingBox boundingBox() {
         return this.boundingBox;
      }

      public boolean isStart() {
         return this.isStart;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(BoundingBox.STREAM_CODEC, DebugStructureInfo.Piece::boundingBox, ByteBufCodecs.BOOL, DebugStructureInfo.Piece::isStart, DebugStructureInfo.Piece::new);
      }
   }
}
