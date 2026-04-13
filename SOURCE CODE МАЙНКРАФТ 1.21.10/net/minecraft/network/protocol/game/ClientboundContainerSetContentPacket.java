package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.item.ItemStack;

public record ClientboundContainerSetContentPacket(int containerId, int stateId, List<ItemStack> items, ItemStack carriedItem) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundContainerSetContentPacket> STREAM_CODEC;

   public ClientboundContainerSetContentPacket(int param1, int param2, List<ItemStack> param3, ItemStack param4) {
      super();
      this.containerId = var1;
      this.stateId = var2;
      this.items = var3;
      this.carriedItem = var4;
   }

   public PacketType<ClientboundContainerSetContentPacket> type() {
      return GamePacketTypes.CLIENTBOUND_CONTAINER_SET_CONTENT;
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleContainerContent(this);
   }

   public int containerId() {
      return this.containerId;
   }

   public int stateId() {
      return this.stateId;
   }

   public List<ItemStack> items() {
      return this.items;
   }

   public ItemStack carriedItem() {
      return this.carriedItem;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.CONTAINER_ID, ClientboundContainerSetContentPacket::containerId, ByteBufCodecs.VAR_INT, ClientboundContainerSetContentPacket::stateId, ItemStack.OPTIONAL_LIST_STREAM_CODEC, ClientboundContainerSetContentPacket::items, ItemStack.OPTIONAL_STREAM_CODEC, ClientboundContainerSetContentPacket::carriedItem, ClientboundContainerSetContentPacket::new);
   }
}
