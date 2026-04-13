package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record ServerboundMoveVehiclePacket(Vec3 position, float yRot, float xRot, boolean onGround) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundMoveVehiclePacket> STREAM_CODEC;

   public ServerboundMoveVehiclePacket(Vec3 param1, float param2, float param3, boolean param4) {
      super();
      this.position = var1;
      this.yRot = var2;
      this.xRot = var3;
      this.onGround = var4;
   }

   public static ServerboundMoveVehiclePacket fromEntity(Entity var0) {
      return var0.isInterpolating() ? new ServerboundMoveVehiclePacket(var0.getInterpolation().position(), var0.getInterpolation().yRot(), var0.getInterpolation().xRot(), var0.onGround()) : new ServerboundMoveVehiclePacket(var0.position(), var0.getYRot(), var0.getXRot(), var0.onGround());
   }

   public PacketType<ServerboundMoveVehiclePacket> type() {
      return GamePacketTypes.SERVERBOUND_MOVE_VEHICLE;
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleMoveVehicle(this);
   }

   public Vec3 position() {
      return this.position;
   }

   public float yRot() {
      return this.yRot;
   }

   public float xRot() {
      return this.xRot;
   }

   public boolean onGround() {
      return this.onGround;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(Vec3.STREAM_CODEC, ServerboundMoveVehiclePacket::position, ByteBufCodecs.FLOAT, ServerboundMoveVehiclePacket::yRot, ByteBufCodecs.FLOAT, ServerboundMoveVehiclePacket::xRot, ByteBufCodecs.BOOL, ServerboundMoveVehiclePacket::onGround, ServerboundMoveVehiclePacket::new);
   }
}
