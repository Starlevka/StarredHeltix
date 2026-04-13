package net.minecraft;

import java.util.Date;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.world.level.storage.DataVersion;

public interface WorldVersion {
   DataVersion dataVersion();

   String id();

   String name();

   int protocolVersion();

   PackFormat packVersion(PackType var1);

   Date buildTime();

   boolean stable();

   public static record Simple(String id, String name, DataVersion dataVersion, int protocolVersion, PackFormat resourcePackVersion, PackFormat datapackVersion, Date buildTime, boolean stable) implements WorldVersion {
      public Simple(String param1, String param2, DataVersion param3, int param4, PackFormat param5, PackFormat param6, Date param7, boolean param8) {
         super();
         this.id = var1;
         this.name = var2;
         this.dataVersion = var3;
         this.protocolVersion = var4;
         this.resourcePackVersion = var5;
         this.datapackVersion = var6;
         this.buildTime = var7;
         this.stable = var8;
      }

      public PackFormat packVersion(PackType var1) {
         PackFormat var10000;
         switch(var1) {
         case CLIENT_RESOURCES:
            var10000 = this.resourcePackVersion;
            break;
         case SERVER_DATA:
            var10000 = this.datapackVersion;
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      public String id() {
         return this.id;
      }

      public String name() {
         return this.name;
      }

      public DataVersion dataVersion() {
         return this.dataVersion;
      }

      public int protocolVersion() {
         return this.protocolVersion;
      }

      public PackFormat resourcePackVersion() {
         return this.resourcePackVersion;
      }

      public PackFormat datapackVersion() {
         return this.datapackVersion;
      }

      public Date buildTime() {
         return this.buildTime;
      }

      public boolean stable() {
         return this.stable;
      }
   }
}
