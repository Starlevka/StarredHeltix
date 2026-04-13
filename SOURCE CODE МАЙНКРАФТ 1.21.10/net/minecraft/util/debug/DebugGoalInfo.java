package net.minecraft.util.debug;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DebugGoalInfo(List<DebugGoalInfo.DebugGoal> goals) {
   public static final StreamCodec<ByteBuf, DebugGoalInfo> STREAM_CODEC;

   public DebugGoalInfo(List<DebugGoalInfo.DebugGoal> param1) {
      super();
      this.goals = var1;
   }

   public List<DebugGoalInfo.DebugGoal> goals() {
      return this.goals;
   }

   static {
      STREAM_CODEC = StreamCodec.composite(DebugGoalInfo.DebugGoal.STREAM_CODEC.apply(ByteBufCodecs.list()), DebugGoalInfo::goals, DebugGoalInfo::new);
   }

   public static record DebugGoal(int priority, boolean isRunning, String name) {
      public static final StreamCodec<ByteBuf, DebugGoalInfo.DebugGoal> STREAM_CODEC;

      public DebugGoal(int param1, boolean param2, String param3) {
         super();
         this.priority = var1;
         this.isRunning = var2;
         this.name = var3;
      }

      public int priority() {
         return this.priority;
      }

      public boolean isRunning() {
         return this.isRunning;
      }

      public String name() {
         return this.name;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, DebugGoalInfo.DebugGoal::priority, ByteBufCodecs.BOOL, DebugGoalInfo.DebugGoal::isRunning, ByteBufCodecs.stringUtf8(255), DebugGoalInfo.DebugGoal::name, DebugGoalInfo.DebugGoal::new);
      }
   }
}
