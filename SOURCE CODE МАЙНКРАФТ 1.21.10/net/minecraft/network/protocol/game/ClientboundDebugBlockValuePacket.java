package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.debug.DebugSubscription;

public record ClientboundDebugBlockValuePacket(BlockPos blockPos, DebugSubscription.Update<?> update) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundDebugBlockValuePacket> STREAM_CODEC;

   public ClientboundDebugBlockValuePacket(BlockPos param1, DebugSubscription.Update<?> param2) {
      super();
      this.blockPos = var1;
      this.update = var2;
   }

   public PacketType<ClientboundDebugBlockValuePacket> type() {
      return GamePacketTypes.CLIENTBOUND_DEBUG_BLOCK_VALUE;
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleDebugBlockValue(this);
   }

   public BlockPos blockPos() {
      return this.blockPos;
   }

   public DebugSubscription.Update<?> update() {
      return this.update;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, ClientboundDebugBlockValuePacket::blockPos, DebugSubscription.Update.STREAM_CODEC, ClientboundDebugBlockValuePacket::update, ClientboundDebugBlockValuePacket::new);
   }
}
