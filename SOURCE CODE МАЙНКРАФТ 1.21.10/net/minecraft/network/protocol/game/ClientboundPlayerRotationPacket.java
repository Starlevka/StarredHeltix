package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundPlayerRotationPacket(float yRot, boolean relativeY, float xRot, boolean relativeX) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundPlayerRotationPacket> STREAM_CODEC;

   public ClientboundPlayerRotationPacket(float param1, boolean param2, float param3, boolean param4) {
      super();
      this.yRot = var1;
      this.relativeY = var2;
      this.xRot = var3;
      this.relativeX = var4;
   }

   public PacketType<ClientboundPlayerRotationPacket> type() {
      return GamePacketTypes.CLIENTBOUND_PLAYER_ROTATION;
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleRotatePlayer(this);
   }

   public float yRot() {
      return this.yRot;
   }

   public boolean relativeY() {
      return this.relativeY;
   }

   public float xRot() {
      return this.xRot;
   }

   public boolean relativeX() {
      return this.relativeX;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, ClientboundPlayerRotationPacket::yRot, ByteBufCodecs.BOOL, ClientboundPlayerRotationPacket::relativeY, ByteBufCodecs.FLOAT, ClientboundPlayerRotationPacket::xRot, ByteBufCodecs.BOOL, ClientboundPlayerRotationPacket::relativeX, ClientboundPlayerRotationPacket::new);
   }
}
