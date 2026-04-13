package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.inventory.ClickType;

public record ServerboundContainerClickPacket(int containerId, int stateId, short slotNum, byte buttonNum, ClickType clickType, Int2ObjectMap<HashedStack> changedSlots, HashedStack carriedItem) implements Packet<ServerGamePacketListener> {
   private static final int MAX_SLOT_COUNT = 128;
   private static final StreamCodec<RegistryFriendlyByteBuf, Int2ObjectMap<HashedStack>> SLOTS_STREAM_CODEC;
   public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundContainerClickPacket> STREAM_CODEC;

   public ServerboundContainerClickPacket(int param1, int param2, short param3, byte param4, ClickType param5, Int2ObjectMap<HashedStack> param6, HashedStack param7) {
      super();
      var6 = Int2ObjectMaps.unmodifiable(var6);
      this.containerId = var1;
      this.stateId = var2;
      this.slotNum = var3;
      this.buttonNum = var4;
      this.clickType = var5;
      this.changedSlots = var6;
      this.carriedItem = var7;
   }

   public PacketType<ServerboundContainerClickPacket> type() {
      return GamePacketTypes.SERVERBOUND_CONTAINER_CLICK;
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleContainerClick(this);
   }

   public int containerId() {
      return this.containerId;
   }

   public int stateId() {
      return this.stateId;
   }

   public short slotNum() {
      return this.slotNum;
   }

   public byte buttonNum() {
      return this.buttonNum;
   }

   public ClickType clickType() {
      return this.clickType;
   }

   public Int2ObjectMap<HashedStack> changedSlots() {
      return this.changedSlots;
   }

   public HashedStack carriedItem() {
      return this.carriedItem;
   }

   static {
      SLOTS_STREAM_CODEC = ByteBufCodecs.map(Int2ObjectOpenHashMap::new, ByteBufCodecs.SHORT.map(Short::intValue, Integer::shortValue), HashedStack.STREAM_CODEC, 128);
      STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.CONTAINER_ID, ServerboundContainerClickPacket::containerId, ByteBufCodecs.VAR_INT, ServerboundContainerClickPacket::stateId, ByteBufCodecs.SHORT, ServerboundContainerClickPacket::slotNum, ByteBufCodecs.BYTE, ServerboundContainerClickPacket::buttonNum, ClickType.STREAM_CODEC, ServerboundContainerClickPacket::clickType, SLOTS_STREAM_CODEC, ServerboundContainerClickPacket::changedSlots, HashedStack.STREAM_CODEC, ServerboundContainerClickPacket::carriedItem, ServerboundContainerClickPacket::new);
   }
}
