package net.minecraft.world.level.storage;

import net.minecraft.SharedConstants;

public record DataVersion(int version, String series) {
   public static final String MAIN_SERIES = "main";

   public DataVersion(int param1, String param2) {
      super();
      this.version = var1;
      this.series = var2;
   }

   public boolean isSideSeries() {
      return !this.series.equals("main");
   }

   public boolean isCompatible(DataVersion var1) {
      return SharedConstants.DEBUG_OPEN_INCOMPATIBLE_WORLDS ? true : this.series().equals(var1.series());
   }

   public int version() {
      return this.version;
   }

   public String series() {
      return this.series;
   }
}
