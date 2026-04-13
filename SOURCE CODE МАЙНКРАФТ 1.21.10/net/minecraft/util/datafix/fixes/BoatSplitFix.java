package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class BoatSplitFix extends DataFix {
   public BoatSplitFix(Schema var1) {
      super(var1, true);
   }

   private static boolean isNormalBoat(String var0) {
      return var0.equals("minecraft:boat");
   }

   private static boolean isChestBoat(String var0) {
      return var0.equals("minecraft:chest_boat");
   }

   private static boolean isAnyBoat(String var0) {
      return isNormalBoat(var0) || isChestBoat(var0);
   }

   private static String mapVariantToNormalBoat(String var0) {
      byte var2 = -1;
      switch(var0.hashCode()) {
      case -1423522852:
         if (var0.equals("acacia")) {
            var2 = 4;
         }
         break;
      case -1396384012:
         if (var0.equals("bamboo")) {
            var2 = 8;
         }
         break;
      case -1361513063:
         if (var0.equals("cherry")) {
            var2 = 5;
         }
         break;
      case -1148845891:
         if (var0.equals("jungle")) {
            var2 = 3;
         }
         break;
      case -895668798:
         if (var0.equals("spruce")) {
            var2 = 1;
         }
         break;
      case 93745840:
         if (var0.equals("birch")) {
            var2 = 2;
         }
         break;
      case 129145209:
         if (var0.equals("mangrove")) {
            var2 = 7;
         }
         break;
      case 1741365392:
         if (var0.equals("dark_oak")) {
            var2 = 6;
         }
      }

      String var10000;
      switch(var2) {
      case 1:
         var10000 = "minecraft:spruce_boat";
         break;
      case 2:
         var10000 = "minecraft:birch_boat";
         break;
      case 3:
         var10000 = "minecraft:jungle_boat";
         break;
      case 4:
         var10000 = "minecraft:acacia_boat";
         break;
      case 5:
         var10000 = "minecraft:cherry_boat";
         break;
      case 6:
         var10000 = "minecraft:dark_oak_boat";
         break;
      case 7:
         var10000 = "minecraft:mangrove_boat";
         break;
      case 8:
         var10000 = "minecraft:bamboo_raft";
         break;
      default:
         var10000 = "minecraft:oak_boat";
      }

      return var10000;
   }

   private static String mapVariantToChestBoat(String var0) {
      byte var2 = -1;
      switch(var0.hashCode()) {
      case -1423522852:
         if (var0.equals("acacia")) {
            var2 = 4;
         }
         break;
      case -1396384012:
         if (var0.equals("bamboo")) {
            var2 = 8;
         }
         break;
      case -1361513063:
         if (var0.equals("cherry")) {
            var2 = 5;
         }
         break;
      case -1148845891:
         if (var0.equals("jungle")) {
            var2 = 3;
         }
         break;
      case -895668798:
         if (var0.equals("spruce")) {
            var2 = 1;
         }
         break;
      case 93745840:
         if (var0.equals("birch")) {
            var2 = 2;
         }
         break;
      case 129145209:
         if (var0.equals("mangrove")) {
            var2 = 7;
         }
         break;
      case 1741365392:
         if (var0.equals("dark_oak")) {
            var2 = 6;
         }
      }

      String var10000;
      switch(var2) {
      case 1:
         var10000 = "minecraft:spruce_chest_boat";
         break;
      case 2:
         var10000 = "minecraft:birch_chest_boat";
         break;
      case 3:
         var10000 = "minecraft:jungle_chest_boat";
         break;
      case 4:
         var10000 = "minecraft:acacia_chest_boat";
         break;
      case 5:
         var10000 = "minecraft:cherry_chest_boat";
         break;
      case 6:
         var10000 = "minecraft:dark_oak_chest_boat";
         break;
      case 7:
         var10000 = "minecraft:mangrove_chest_boat";
         break;
      case 8:
         var10000 = "minecraft:bamboo_chest_raft";
         break;
      default:
         var10000 = "minecraft:oak_chest_boat";
      }

      return var10000;
   }

   public TypeRewriteRule makeRule() {
      OpticFinder var1 = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
      Type var2 = this.getInputSchema().getType(References.ENTITY);
      Type var3 = this.getOutputSchema().getType(References.ENTITY);
      return this.fixTypeEverywhereTyped("BoatSplitFix", var2, var3, (var2x) -> {
         Optional var3x = var2x.getOptional(var1);
         if (var3x.isPresent() && isAnyBoat((String)var3x.get())) {
            Dynamic var4 = (Dynamic)var2x.getOrCreate(DSL.remainderFinder());
            Optional var5 = var4.get("Type").asString().result();
            String var6;
            if (isChestBoat((String)var3x.get())) {
               var6 = (String)var5.map(BoatSplitFix::mapVariantToChestBoat).orElse("minecraft:oak_chest_boat");
            } else {
               var6 = (String)var5.map(BoatSplitFix::mapVariantToNormalBoat).orElse("minecraft:oak_boat");
            }

            return ExtraDataFixUtils.cast(var3, var2x).update(DSL.remainderFinder(), (var0) -> {
               return var0.remove("Type");
            }).set(var1, var6);
         } else {
            return ExtraDataFixUtils.cast(var3, var2x);
         }
      });
   }
}
