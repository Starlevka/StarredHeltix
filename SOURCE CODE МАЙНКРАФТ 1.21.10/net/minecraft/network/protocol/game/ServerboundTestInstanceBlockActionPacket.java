package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;

public record ServerboundTestInstanceBlockActionPacket(BlockPos pos, ServerboundTestInstanceBlockActionPacket.Action action, TestInstanceBlockEntity.Data data) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundTestInstanceBlockActionPacket> STREAM_CODEC;

   public ServerboundTestInstanceBlockActionPacket(BlockPos var1, ServerboundTestInstanceBlockActionPacket.Action var2, Optional<ResourceKey<GameTestInstance>> var3, Vec3i var4, Rotation var5, boolean var6) {
      this(var1, var2, new TestInstanceBlockEntity.Data(var3, var4, var5, var6, TestInstanceBlockEntity.Status.CLEARED, Optional.empty()));
   }

   public ServerboundTestInstanceBlockActionPacket(BlockPos param1, ServerboundTestInstanceBlockActionPacket.Action param2, TestInstanceBlockEntity.Data param3) {
      super();
      this.pos = var1;
      this.action = var2;
      this.data = var3;
   }

   public PacketType<ServerboundTestInstanceBlockActionPacket> type() {
      return GamePacketTypes.SERVERBOUND_TEST_INSTANCE_BLOCK_ACTION;
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleTestInstanceBlockAction(this);
   }

   public BlockPos pos() {
      return this.pos;
   }

   public ServerboundTestInstanceBlockActionPacket.Action action() {
      return this.action;
   }

   public TestInstanceBlockEntity.Data data() {
      return this.data;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, ServerboundTestInstanceBlockActionPacket::pos, ServerboundTestInstanceBlockActionPacket.Action.STREAM_CODEC, ServerboundTestInstanceBlockActionPacket::action, TestInstanceBlockEntity.Data.STREAM_CODEC, ServerboundTestInstanceBlockActionPacket::data, ServerboundTestInstanceBlockActionPacket::new);
   }

   public static enum Action {
      INIT(0),
      QUERY(1),
      SET(2),
      RESET(3),
      SAVE(4),
      EXPORT(5),
      RUN(6);

      private static final IntFunction<ServerboundTestInstanceBlockActionPacket.Action> BY_ID = ByIdMap.continuous((var0) -> {
         return var0.id;
      }, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
      public static final StreamCodec<ByteBuf, ServerboundTestInstanceBlockActionPacket.Action> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, (var0) -> {
         return var0.id;
      });
      private final int id;

      private Action(final int param3) {
         this.id = var3;
      }

      // $FF: synthetic method
      private static ServerboundTestInstanceBlockActionPacket.Action[] $values() {
         return new ServerboundTestInstanceBlockActionPacket.Action[]{INIT, QUERY, SET, RESET, SAVE, EXPORT, RUN};
      }
   }
}
