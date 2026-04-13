package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1458 extends NamespacedSchema {
   public V1458(int var1, Schema var2) {
      super(var1, var2);
   }

   public void registerTypes(Schema var1, Map<String, Supplier<TypeTemplate>> var2, Map<String, Supplier<TypeTemplate>> var3) {
      super.registerTypes(var1, var2, var3);
      var1.registerType(true, References.ENTITY, () -> {
         return DSL.and(References.ENTITY_EQUIPMENT.in(var1), DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(var1), DSL.taggedChoiceLazy("id", namespacedString(), var2)));
      });
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema var1) {
      Map var2 = super.registerBlockEntities(var1);
      var1.register(var2, "minecraft:beacon", () -> {
         return nameable(var1);
      });
      var1.register(var2, "minecraft:banner", () -> {
         return nameable(var1);
      });
      var1.register(var2, "minecraft:brewing_stand", () -> {
         return nameableInventory(var1);
      });
      var1.register(var2, "minecraft:chest", () -> {
         return nameableInventory(var1);
      });
      var1.register(var2, "minecraft:trapped_chest", () -> {
         return nameableInventory(var1);
      });
      var1.register(var2, "minecraft:dispenser", () -> {
         return nameableInventory(var1);
      });
      var1.register(var2, "minecraft:dropper", () -> {
         return nameableInventory(var1);
      });
      var1.register(var2, "minecraft:enchanting_table", () -> {
         return nameable(var1);
      });
      var1.register(var2, "minecraft:furnace", () -> {
         return nameableInventory(var1);
      });
      var1.register(var2, "minecraft:hopper", () -> {
         return nameableInventory(var1);
      });
      var1.register(var2, "minecraft:shulker_box", () -> {
         return nameableInventory(var1);
      });
      return var2;
   }

   public static TypeTemplate nameableInventory(Schema var0) {
      return DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(var0)), "CustomName", References.TEXT_COMPONENT.in(var0));
   }

   public static TypeTemplate nameable(Schema var0) {
      return DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(var0));
   }
}
