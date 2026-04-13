package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Iterator;
import java.util.List;

public record RealmsSetting(String name, String value) implements ReflectionBasedSerialization {
   public RealmsSetting(String param1, String param2) {
      super();
      this.name = var1;
      this.value = var2;
   }

   public static RealmsSetting hardcoreSetting(boolean var0) {
      return new RealmsSetting("hardcore", Boolean.toString(var0));
   }

   public static boolean isHardcore(List<RealmsSetting> var0) {
      Iterator var1 = var0.iterator();

      RealmsSetting var2;
      do {
         if (!var1.hasNext()) {
            return false;
         }

         var2 = (RealmsSetting)var1.next();
      } while(!var2.name().equals("hardcore"));

      return Boolean.parseBoolean(var2.value());
   }

   @SerializedName("name")
   public String name() {
      return this.name;
   }

   @SerializedName("value")
   public String value() {
      return this.value;
   }
}
