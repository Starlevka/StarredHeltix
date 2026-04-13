package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class TextComponentHoverAndClickEventFix extends DataFix {
   public TextComponentHoverAndClickEventFix(Schema var1) {
      super(var1, true);
   }

   protected TypeRewriteRule makeRule() {
      Type var1 = this.getInputSchema().getType(References.TEXT_COMPONENT).findFieldType("hoverEvent");
      return this.createFixer(this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT), this.getOutputSchema().getType(References.TEXT_COMPONENT), var1);
   }

   private <C1, C2, H extends Pair<String, ?>> TypeRewriteRule createFixer(Type<C1> var1, Type<C2> var2, Type<H> var3) {
      Type var4 = DSL.named(References.TEXT_COMPONENT.typeName(), DSL.or(DSL.or(DSL.string(), DSL.list(var1)), DSL.and(DSL.optional(DSL.field("extra", DSL.list(var1))), DSL.optional(DSL.field("separator", var1)), DSL.optional(DSL.field("hoverEvent", var3)), DSL.remainderType())));
      if (!var4.equals(this.getInputSchema().getType(References.TEXT_COMPONENT))) {
         String var10002 = String.valueOf(var4);
         throw new IllegalStateException("Text component type did not match, expected " + var10002 + " but got " + String.valueOf(this.getInputSchema().getType(References.TEXT_COMPONENT)));
      } else {
         Type var5 = ExtraDataFixUtils.patchSubType(var4, var4, var2);
         return this.fixTypeEverywhere("TextComponentHoverAndClickEventFix", var4, var2, (var2x) -> {
            return (var3) -> {
               boolean var4 = (Boolean)((Either)var3.getSecond()).map((var0) -> {
                  return false;
               }, (var0) -> {
                  Pair var1 = (Pair)((Pair)var0.getSecond()).getSecond();
                  boolean var2 = ((Either)var1.getFirst()).left().isPresent();
                  boolean var3 = ((Dynamic)var1.getSecond()).get("clickEvent").result().isPresent();
                  return var2 || var3;
               });
               return !var4 ? var3 : Util.writeAndReadTypedOrThrow(ExtraDataFixUtils.cast(var5, var3, var2x), var2, TextComponentHoverAndClickEventFix::fixTextComponent).getValue();
            };
         });
      }
   }

   private static Dynamic<?> fixTextComponent(Dynamic<?> var0) {
      return var0.renameAndFixField("hoverEvent", "hover_event", TextComponentHoverAndClickEventFix::fixHoverEvent).renameAndFixField("clickEvent", "click_event", TextComponentHoverAndClickEventFix::fixClickEvent);
   }

   private static Dynamic<?> copyFields(Dynamic<?> var0, Dynamic<?> var1, String... var2) {
      String[] var3 = var2;
      int var4 = var2.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         String var6 = var3[var5];
         var0 = Dynamic.copyField(var1, var6, var0, var6);
      }

      return var0;
   }

   private static Dynamic<?> fixHoverEvent(Dynamic<?> var0) {
      String var1 = var0.get("action").asString("");
      byte var3 = -1;
      switch(var1.hashCode()) {
      case -1903644907:
         if (var1.equals("show_item")) {
            var3 = 1;
         }
         break;
      case -1903331025:
         if (var1.equals("show_text")) {
            var3 = 0;
         }
         break;
      case 133701477:
         if (var1.equals("show_entity")) {
            var3 = 2;
         }
      }

      Dynamic var10000;
      Dynamic var4;
      switch(var3) {
      case 0:
         var10000 = var0.renameField("contents", "value");
         break;
      case 1:
         var4 = var0.get("contents").orElseEmptyMap();
         Optional var5 = var4.asString().result();
         var10000 = var5.isPresent() ? var0.renameField("contents", "id") : copyFields(var0.remove("contents"), var4, "id", "count", "components");
         break;
      case 2:
         var4 = var0.get("contents").orElseEmptyMap();
         var10000 = copyFields(var0.remove("contents"), var4, "id", "type", "name").renameField("id", "uuid").renameField("type", "id");
         break;
      default:
         var10000 = var0;
      }

      return var10000;
   }

   @Nullable
   private static <T> Dynamic<T> fixClickEvent(Dynamic<T> var0) {
      String var1 = var0.get("action").asString("");
      String var2 = var0.get("value").asString("");
      byte var4 = -1;
      switch(var1.hashCode()) {
      case -1654598210:
         if (var1.equals("change_page")) {
            var4 = 4;
         }
         break;
      case -504306182:
         if (var1.equals("open_url")) {
            var4 = 0;
         }
         break;
      case 378483088:
         if (var1.equals("suggest_command")) {
            var4 = 3;
         }
         break;
      case 1545922129:
         if (var1.equals("open_file")) {
            var4 = 1;
         }
         break;
      case 1845855639:
         if (var1.equals("run_command")) {
            var4 = 2;
         }
      }

      Dynamic var10000;
      switch(var4) {
      case 0:
         var10000 = !validateUri(var2) ? null : var0.renameField("value", "url");
         break;
      case 1:
         var10000 = var0.renameField("value", "path");
         break;
      case 2:
      case 3:
         var10000 = !validateChat(var2) ? null : var0.renameField("value", "command");
         break;
      case 4:
         Integer var5 = (Integer)var0.get("value").result().map(TextComponentHoverAndClickEventFix::parseOldPage).orElse((Object)null);
         if (var5 == null) {
            var10000 = null;
         } else {
            int var6 = Math.max(var5, 1);
            var10000 = var0.remove("value").set("page", var0.createInt(var6));
         }
         break;
      default:
         var10000 = var0;
      }

      return var10000;
   }

   @Nullable
   private static Integer parseOldPage(Dynamic<?> var0) {
      Optional var1 = var0.asNumber().result();
      if (var1.isPresent()) {
         return ((Number)var1.get()).intValue();
      } else {
         try {
            return Integer.parseInt(var0.asString(""));
         } catch (Exception var3) {
            return null;
         }
      }
   }

   private static boolean validateUri(String var0) {
      try {
         URI var1 = new URI(var0);
         String var2 = var1.getScheme();
         if (var2 == null) {
            return false;
         } else {
            String var3 = var2.toLowerCase(Locale.ROOT);
            return "http".equals(var3) || "https".equals(var3);
         }
      } catch (URISyntaxException var4) {
         return false;
      }
   }

   private static boolean validateChat(String var0) {
      for(int var1 = 0; var1 < var0.length(); ++var1) {
         char var2 = var0.charAt(var1);
         if (var2 == 167 || var2 < ' ' || var2 == 127) {
            return false;
         }
      }

      return true;
   }
}
