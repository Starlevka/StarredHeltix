package net.minecraft.world.scores;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.level.saveddata.SavedData;

public class ScoreboardSaveData extends SavedData {
   public static final String FILE_ID = "scoreboard";
   private final Scoreboard scoreboard;

   public ScoreboardSaveData(Scoreboard var1) {
      super();
      this.scoreboard = var1;
   }

   public void loadFrom(ScoreboardSaveData.Packed var1) {
      List var10000 = var1.objectives();
      Scoreboard var10001 = this.scoreboard;
      Objects.requireNonNull(var10001);
      var10000.forEach(var10001::loadObjective);
      var10000 = var1.scores();
      var10001 = this.scoreboard;
      Objects.requireNonNull(var10001);
      var10000.forEach(var10001::loadPlayerScore);
      var1.displaySlots().forEach((var1x, var2) -> {
         Objective var3 = this.scoreboard.getObjective(var2);
         this.scoreboard.setDisplayObjective(var1x, var3);
      });
      var10000 = var1.teams();
      var10001 = this.scoreboard;
      Objects.requireNonNull(var10001);
      var10000.forEach(var10001::loadPlayerTeam);
   }

   public ScoreboardSaveData.Packed pack() {
      EnumMap var1 = new EnumMap(DisplaySlot.class);
      DisplaySlot[] var2 = DisplaySlot.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         DisplaySlot var5 = var2[var4];
         Objective var6 = this.scoreboard.getDisplayObjective(var5);
         if (var6 != null) {
            var1.put(var5, var6.getName());
         }
      }

      return new ScoreboardSaveData.Packed(this.scoreboard.getObjectives().stream().map(Objective::pack).toList(), this.scoreboard.packPlayerScores(), var1, this.scoreboard.getPlayerTeams().stream().map(PlayerTeam::pack).toList());
   }

   public static record Packed(List<Objective.Packed> objectives, List<Scoreboard.PackedScore> scores, Map<DisplaySlot, String> displaySlots, List<PlayerTeam.Packed> teams) {
      public static final Codec<ScoreboardSaveData.Packed> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(Objective.Packed.CODEC.listOf().optionalFieldOf("Objectives", List.of()).forGetter(ScoreboardSaveData.Packed::objectives), Scoreboard.PackedScore.CODEC.listOf().optionalFieldOf("PlayerScores", List.of()).forGetter(ScoreboardSaveData.Packed::scores), Codec.unboundedMap(DisplaySlot.CODEC, Codec.STRING).optionalFieldOf("DisplaySlots", Map.of()).forGetter(ScoreboardSaveData.Packed::displaySlots), PlayerTeam.Packed.CODEC.listOf().optionalFieldOf("Teams", List.of()).forGetter(ScoreboardSaveData.Packed::teams)).apply(var0, ScoreboardSaveData.Packed::new);
      });

      public Packed(List<Objective.Packed> param1, List<Scoreboard.PackedScore> param2, Map<DisplaySlot, String> param3, List<PlayerTeam.Packed> param4) {
         super();
         this.objectives = var1;
         this.scores = var2;
         this.displaySlots = var3;
         this.teams = var4;
      }

      public List<Objective.Packed> objectives() {
         return this.objectives;
      }

      public List<Scoreboard.PackedScore> scores() {
         return this.scores;
      }

      public Map<DisplaySlot, String> displaySlots() {
         return this.displaySlots;
      }

      public List<PlayerTeam.Packed> teams() {
         return this.teams;
      }
   }
}
