package com.mojang.realmsclient.dto;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.world.item.component.ResolvableProfile;
import org.slf4j.Logger;

public class RealmsServerPlayerLists extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public Map<Long, List<ResolvableProfile>> servers = Map.of();

   public RealmsServerPlayerLists() {
      super();
   }

   public static RealmsServerPlayerLists parse(String var0) {
      RealmsServerPlayerLists var1 = new RealmsServerPlayerLists();
      Builder var2 = ImmutableMap.builder();

      try {
         JsonObject var3 = GsonHelper.parse(var0);
         if (GsonHelper.isArrayNode(var3, "lists")) {
            JsonArray var4 = var3.getAsJsonArray("lists");

            Object var7;
            JsonObject var8;
            for(Iterator var5 = var4.iterator(); var5.hasNext(); var2.put(JsonUtils.getLongOr("serverId", var8, -1L), var7)) {
               JsonElement var6 = (JsonElement)var5.next();
               var8 = var6.getAsJsonObject();
               String var9 = JsonUtils.getStringOr("playerList", var8, (String)null);
               if (var9 != null) {
                  JsonElement var10 = LenientJsonParser.parse(var9);
                  if (var10.isJsonArray()) {
                     var7 = parsePlayers(var10.getAsJsonArray());
                  } else {
                     var7 = Lists.newArrayList();
                  }
               } else {
                  var7 = Lists.newArrayList();
               }
            }
         }
      } catch (Exception var11) {
         LOGGER.error("Could not parse RealmsServerPlayerLists: {}", var11.getMessage());
      }

      var1.servers = var2.build();
      return var1;
   }

   private static List<ResolvableProfile> parsePlayers(JsonArray var0) {
      ArrayList var1 = new ArrayList(var0.size());
      Iterator var2 = var0.iterator();

      while(var2.hasNext()) {
         JsonElement var3 = (JsonElement)var2.next();
         if (var3.isJsonObject()) {
            UUID var4 = JsonUtils.getUuidOr("playerId", var3.getAsJsonObject(), (UUID)null);
            if (var4 != null && !Minecraft.getInstance().isLocalPlayer(var4)) {
               var1.add(ResolvableProfile.createUnresolved(var4));
            }
         }
      }

      return var1;
   }

   public List<ResolvableProfile> getProfileResultsFor(long var1) {
      List var3 = (List)this.servers.get(var1);
      return var3 != null ? var3 : List.of();
   }
}
