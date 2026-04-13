package net.minecraft.world.scores;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class Objective {
   private final Scoreboard scoreboard;
   private final String name;
   private final ObjectiveCriteria criteria;
   private Component displayName;
   private Component formattedDisplayName;
   private ObjectiveCriteria.RenderType renderType;
   private boolean displayAutoUpdate;
   @Nullable
   private NumberFormat numberFormat;

   public Objective(Scoreboard var1, String var2, ObjectiveCriteria var3, Component var4, ObjectiveCriteria.RenderType var5, boolean var6, @Nullable NumberFormat var7) {
      super();
      this.scoreboard = var1;
      this.name = var2;
      this.criteria = var3;
      this.displayName = var4;
      this.formattedDisplayName = this.createFormattedDisplayName();
      this.renderType = var5;
      this.displayAutoUpdate = var6;
      this.numberFormat = var7;
   }

   public Objective.Packed pack() {
      return new Objective.Packed(this.name, this.criteria, this.displayName, this.renderType, this.displayAutoUpdate, Optional.ofNullable(this.numberFormat));
   }

   public Scoreboard getScoreboard() {
      return this.scoreboard;
   }

   public String getName() {
      return this.name;
   }

   public ObjectiveCriteria getCriteria() {
      return this.criteria;
   }

   public Component getDisplayName() {
      return this.displayName;
   }

   public boolean displayAutoUpdate() {
      return this.displayAutoUpdate;
   }

   @Nullable
   public NumberFormat numberFormat() {
      return this.numberFormat;
   }

   public NumberFormat numberFormatOrDefault(NumberFormat var1) {
      return (NumberFormat)Objects.requireNonNullElse(this.numberFormat, var1);
   }

   private Component createFormattedDisplayName() {
      return ComponentUtils.wrapInSquareBrackets(this.displayName.copy().withStyle((var1) -> {
         return var1.withHoverEvent(new HoverEvent.ShowText(Component.literal(this.name)));
      }));
   }

   public Component getFormattedDisplayName() {
      return this.formattedDisplayName;
   }

   public void setDisplayName(Component var1) {
      this.displayName = var1;
      this.formattedDisplayName = this.createFormattedDisplayName();
      this.scoreboard.onObjectiveChanged(this);
   }

   public ObjectiveCriteria.RenderType getRenderType() {
      return this.renderType;
   }

   public void setRenderType(ObjectiveCriteria.RenderType var1) {
      this.renderType = var1;
      this.scoreboard.onObjectiveChanged(this);
   }

   public void setDisplayAutoUpdate(boolean var1) {
      this.displayAutoUpdate = var1;
      this.scoreboard.onObjectiveChanged(this);
   }

   public void setNumberFormat(@Nullable NumberFormat var1) {
      this.numberFormat = var1;
      this.scoreboard.onObjectiveChanged(this);
   }

   public static record Packed(String name, ObjectiveCriteria criteria, Component displayName, ObjectiveCriteria.RenderType renderType, boolean displayAutoUpdate, Optional<NumberFormat> numberFormat) {
      public static final Codec<Objective.Packed> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(Codec.STRING.fieldOf("Name").forGetter(Objective.Packed::name), ObjectiveCriteria.CODEC.optionalFieldOf("CriteriaName", ObjectiveCriteria.DUMMY).forGetter(Objective.Packed::criteria), ComponentSerialization.CODEC.fieldOf("DisplayName").forGetter(Objective.Packed::displayName), ObjectiveCriteria.RenderType.CODEC.optionalFieldOf("RenderType", ObjectiveCriteria.RenderType.INTEGER).forGetter(Objective.Packed::renderType), Codec.BOOL.optionalFieldOf("display_auto_update", false).forGetter(Objective.Packed::displayAutoUpdate), NumberFormatTypes.CODEC.optionalFieldOf("format").forGetter(Objective.Packed::numberFormat)).apply(var0, Objective.Packed::new);
      });

      public Packed(String param1, ObjectiveCriteria param2, Component param3, ObjectiveCriteria.RenderType param4, boolean param5, Optional<NumberFormat> param6) {
         super();
         this.name = var1;
         this.criteria = var2;
         this.displayName = var3;
         this.renderType = var4;
         this.displayAutoUpdate = var5;
         this.numberFormat = var6;
      }

      public String name() {
         return this.name;
      }

      public ObjectiveCriteria criteria() {
         return this.criteria;
      }

      public Component displayName() {
         return this.displayName;
      }

      public ObjectiveCriteria.RenderType renderType() {
         return this.renderType;
      }

      public boolean displayAutoUpdate() {
         return this.displayAutoUpdate;
      }

      public Optional<NumberFormat> numberFormat() {
         return this.numberFormat;
      }
   }
}
