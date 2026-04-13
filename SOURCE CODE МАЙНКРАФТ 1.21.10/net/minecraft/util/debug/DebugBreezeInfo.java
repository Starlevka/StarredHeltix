package net.minecraft.util.debug;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DebugBreezeInfo(Optional<Integer> attackTarget, Optional<BlockPos> jumpTarget) {
   public static final StreamCodec<ByteBuf, DebugBreezeInfo> STREAM_CODEC;

   public DebugBreezeInfo(Optional<Integer> param1, Optional<BlockPos> param2) {
      super();
      this.attackTarget = var1;
      this.jumpTarget = var2;
   }

   public Optional<Integer> attackTarget() {
      return this.attackTarget;
   }

   public Optional<BlockPos> jumpTarget() {
      return this.jumpTarget;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs::optional), DebugBreezeInfo::attackTarget, BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional), DebugBreezeInfo::jumpTarget, DebugBreezeInfo::new);
   }
}
