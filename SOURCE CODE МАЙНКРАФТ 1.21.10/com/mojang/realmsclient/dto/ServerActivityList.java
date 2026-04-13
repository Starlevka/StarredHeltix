package com.mojang.realmsclient.dto;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.Iterator;
import java.util.List;
import net.minecraft.util.LenientJsonParser;

public class ServerActivityList extends ValueObject {
   public long periodInMillis;
   public List<ServerActivity> serverActivities = Lists.newArrayList();

   public ServerActivityList() {
      super();
   }

   public static ServerActivityList parse(String var0) {
      ServerActivityList var1 = new ServerActivityList();

      try {
         JsonElement var2 = LenientJsonParser.parse(var0);
         JsonObject var3 = var2.getAsJsonObject();
         var1.periodInMillis = JsonUtils.getLongOr("periodInMillis", var3, -1L);
         JsonElement var4 = var3.get("playerActivityDto");
         if (var4 != null && var4.isJsonArray()) {
            JsonArray var5 = var4.getAsJsonArray();
            Iterator var6 = var5.iterator();

            while(var6.hasNext()) {
               JsonElement var7 = (JsonElement)var6.next();
               ServerActivity var8 = ServerActivity.parse(var7.getAsJsonObject());
               var1.serverActivities.add(var8);
            }
         }
      } catch (Exception var9) {
      }

      return var1;
   }
}
