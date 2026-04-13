package net.minecraft.world.level.block.state.properties;

import java.util.List;
import java.util.Optional;

public final class BooleanProperty extends Property<Boolean> {
   private static final List<Boolean> VALUES = List.of(true, false);
   private static final int TRUE_INDEX = 0;
   private static final int FALSE_INDEX = 1;

   private BooleanProperty(String var1) {
      super(var1, Boolean.class);
   }

   public List<Boolean> getPossibleValues() {
      return VALUES;
   }

   public static BooleanProperty create(String var0) {
      return new BooleanProperty(var0);
   }

   public Optional<Boolean> getValue(String var1) {
      byte var3 = -1;
      switch(var1.hashCode()) {
      case 3569038:
         if (var1.equals("true")) {
            var3 = 0;
         }
         break;
      case 97196323:
         if (var1.equals("false")) {
            var3 = 1;
         }
      }

      Optional var10000;
      switch(var3) {
      case 0:
         var10000 = Optional.of(true);
         break;
      case 1:
         var10000 = Optional.of(false);
         break;
      default:
         var10000 = Optional.empty();
      }

      return var10000;
   }

   public String getName(Boolean var1) {
      return var1.toString();
   }

   public int getInternalIndex(Boolean var1) {
      return var1 ? 0 : 1;
   }

   // $FF: synthetic method
   public int getInternalIndex(final Comparable param1) {
      return this.getInternalIndex((Boolean)var1);
   }

   // $FF: synthetic method
   public String getName(final Comparable param1) {
      return this.getName((Boolean)var1);
   }
}
