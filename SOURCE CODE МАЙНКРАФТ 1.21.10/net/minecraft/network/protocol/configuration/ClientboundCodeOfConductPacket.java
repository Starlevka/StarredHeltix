package net.minecraft.network.protocol.configuration;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundCodeOfConductPacket(String codeOfConduct) implements Packet<ClientConfigurationPacketListener> {
   public static final StreamCodec<ByteBuf, ClientboundCodeOfConductPacket> STREAM_CODEC;

   public ClientboundCodeOfConductPacket(String param1) {
      super();
      this.codeOfConduct = var1;
   }

   public PacketType<ClientboundCodeOfConductPacket> type() {
      return ConfigurationPacketTypes.CLIENTBOUND_CODE_OF_CONDUCT;
   }

   public void handle(ClientConfigurationPacketListener var1) {
      var1.handleCodeOfConduct(this);
   }

   public String codeOfConduct() {
      return this.codeOfConduct;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ClientboundCodeOfConductPacket::codeOfConduct, ClientboundCodeOfConductPacket::new);
   }
}
