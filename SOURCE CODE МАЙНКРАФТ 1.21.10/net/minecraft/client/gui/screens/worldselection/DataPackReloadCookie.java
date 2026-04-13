package net.minecraft.client.gui.screens.worldselection;

import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldGenSettings;

public record DataPackReloadCookie(WorldGenSettings worldGenSettings, WorldDataConfiguration dataConfiguration) {
   public DataPackReloadCookie(WorldGenSettings param1, WorldDataConfiguration param2) {
      super();
      this.worldGenSettings = var1;
      this.dataConfiguration = var2;
   }

   public WorldGenSettings worldGenSettings() {
      return this.worldGenSettings;
   }

   public WorldDataConfiguration dataConfiguration() {
      return this.dataConfiguration;
   }
}
