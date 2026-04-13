package net.minecraft.client.gui.components.debug;

import net.minecraft.network.chat.Component;

public record DebugEntryCategory(Component label, float sortKey) {
   public static final DebugEntryCategory SCREEN_TEXT = new DebugEntryCategory(Component.translatable("debug.options.category.text"), 1.0F);
   public static final DebugEntryCategory RENDERER = new DebugEntryCategory(Component.translatable("debug.options.category.renderer"), 2.0F);

   public DebugEntryCategory(Component param1, float param2) {
      super();
      this.label = var1;
      this.sortKey = var2;
   }

   public Component label() {
      return this.label;
   }

   public float sortKey() {
      return this.sortKey;
   }
}
