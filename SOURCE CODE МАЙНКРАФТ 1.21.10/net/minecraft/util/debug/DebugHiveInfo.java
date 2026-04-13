package net.minecraft.util.debug;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;

public record DebugHiveInfo(Block type, int occupantCount, int honeyLevel, boolean sedated) {
   public static final StreamCodec<RegistryFriendlyByteBuf, DebugHiveInfo> STREAM_CODEC;

   public DebugHiveInfo(Block param1, int param2, int param3, boolean param4) {
      super();
      this.type = var1;
      this.occupantCount = var2;
      this.honeyLevel = var3;
      this.sedated = var4;
   }

   public static DebugHiveInfo pack(BeehiveBlockEntity var0) {
      return new DebugHiveInfo(var0.getBlockState().getBlock(), var0.getOccupantCount(), BeehiveBlockEntity.getHoneyLevel(var0.getBlockState()), var0.isSedated());
   }

   public Block type() {
      return this.type;
   }

   public int occupantCount() {
      return this.occupantCount;
   }

   public int honeyLevel() {
      return this.honeyLevel;
   }

   public boolean sedated() {
      return this.sedated;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.registry(Registries.BLOCK), DebugHiveInfo::type, ByteBufCodecs.VAR_INT, DebugHiveInfo::occupantCount, ByteBufCodecs.VAR_INT, DebugHiveInfo::honeyLevel, ByteBufCodecs.BOOL, DebugHiveInfo::sedated, DebugHiveInfo::new);
   }
}
