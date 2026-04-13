package com.mojang.realmsclient.dto;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.util.LenientJsonParser;

public class Ops extends ValueObject {
   public Set<String> ops = Sets.newHashSet();

   public Ops() {
      super();
   }

   public static Ops parse(String var0) {
      Ops var1 = new Ops();

      try {
         JsonObject var2 = LenientJsonParser.parse(var0).getAsJsonObject();
         JsonElement var3 = var2.get("ops");
         if (var3.isJsonArray()) {
            Iterator var4 = var3.getAsJsonArray().iterator();

            while(var4.hasNext()) {
               JsonElement var5 = (JsonElement)var4.next();
               var1.ops.add(var5.getAsString());
            }
         }
      } catch (Exception var6) {
      }

      return var1;
   }
}
