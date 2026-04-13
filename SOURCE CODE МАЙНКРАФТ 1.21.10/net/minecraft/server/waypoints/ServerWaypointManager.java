package net.minecraft.server.waypoints;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.Sets.SetView;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointManager;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class ServerWaypointManager implements WaypointManager<WaypointTransmitter> {
   private final Set<WaypointTransmitter> waypoints = new HashSet();
   private final Set<ServerPlayer> players = new HashSet();
   private final Table<ServerPlayer, WaypointTransmitter, WaypointTransmitter.Connection> connections = HashBasedTable.create();

   public ServerWaypointManager() {
      super();
   }

   public void trackWaypoint(WaypointTransmitter var1) {
      this.waypoints.add(var1);
      Iterator var2 = this.players.iterator();

      while(var2.hasNext()) {
         ServerPlayer var3 = (ServerPlayer)var2.next();
         this.createConnection(var3, var1);
      }

   }

   public void updateWaypoint(WaypointTransmitter var1) {
      if (this.waypoints.contains(var1)) {
         Map var2 = Tables.transpose(this.connections).row(var1);
         SetView var3 = Sets.difference(this.players, var2.keySet());
         UnmodifiableIterator var4 = ImmutableSet.copyOf(var2.entrySet()).iterator();

         while(var4.hasNext()) {
            Entry var5 = (Entry)var4.next();
            this.updateConnection((ServerPlayer)var5.getKey(), var1, (WaypointTransmitter.Connection)var5.getValue());
         }

         var4 = var3.iterator();

         while(var4.hasNext()) {
            ServerPlayer var6 = (ServerPlayer)var4.next();
            this.createConnection(var6, var1);
         }

      }
   }

   public void untrackWaypoint(WaypointTransmitter var1) {
      this.connections.column(var1).forEach((var0, var1x) -> {
         var1x.disconnect();
      });
      Tables.transpose(this.connections).row(var1).clear();
      this.waypoints.remove(var1);
   }

   public void addPlayer(ServerPlayer var1) {
      this.players.add(var1);
      Iterator var2 = this.waypoints.iterator();

      while(var2.hasNext()) {
         WaypointTransmitter var3 = (WaypointTransmitter)var2.next();
         this.createConnection(var1, var3);
      }

      if (var1.isTransmittingWaypoint()) {
         this.trackWaypoint((WaypointTransmitter)var1);
      }

   }

   public void updatePlayer(ServerPlayer var1) {
      Map var2 = this.connections.row(var1);
      SetView var3 = Sets.difference(this.waypoints, var2.keySet());
      UnmodifiableIterator var4 = ImmutableSet.copyOf(var2.entrySet()).iterator();

      while(var4.hasNext()) {
         Entry var5 = (Entry)var4.next();
         this.updateConnection(var1, (WaypointTransmitter)var5.getKey(), (WaypointTransmitter.Connection)var5.getValue());
      }

      var4 = var3.iterator();

      while(var4.hasNext()) {
         WaypointTransmitter var6 = (WaypointTransmitter)var4.next();
         this.createConnection(var1, var6);
      }

   }

   public void removePlayer(ServerPlayer var1) {
      this.connections.row(var1).values().removeIf((var0) -> {
         var0.disconnect();
         return true;
      });
      this.untrackWaypoint((WaypointTransmitter)var1);
      this.players.remove(var1);
   }

   public void breakAllConnections() {
      this.connections.values().forEach(WaypointTransmitter.Connection::disconnect);
      this.connections.clear();
   }

   public void remakeConnections(WaypointTransmitter var1) {
      Iterator var2 = this.players.iterator();

      while(var2.hasNext()) {
         ServerPlayer var3 = (ServerPlayer)var2.next();
         this.createConnection(var3, var1);
      }

   }

   public Set<WaypointTransmitter> transmitters() {
      return this.waypoints;
   }

   private static boolean isLocatorBarEnabledFor(ServerPlayer var0) {
      return var0.level().getServer().getGameRules().getBoolean(GameRules.RULE_LOCATOR_BAR);
   }

   private void createConnection(ServerPlayer var1, WaypointTransmitter var2) {
      if (var1 != var2) {
         if (isLocatorBarEnabledFor(var1)) {
            var2.makeWaypointConnectionWith(var1).ifPresentOrElse((var3) -> {
               this.connections.put(var1, var2, var3);
               var3.connect();
            }, () -> {
               WaypointTransmitter.Connection var3 = (WaypointTransmitter.Connection)this.connections.remove(var1, var2);
               if (var3 != null) {
                  var3.disconnect();
               }

            });
         }
      }
   }

   private void updateConnection(ServerPlayer var1, WaypointTransmitter var2, WaypointTransmitter.Connection var3) {
      if (var1 != var2) {
         if (isLocatorBarEnabledFor(var1)) {
            if (!var3.isBroken()) {
               var3.update();
            } else {
               var2.makeWaypointConnectionWith(var1).ifPresentOrElse((var3x) -> {
                  var3x.connect();
                  this.connections.put(var1, var2, var3x);
               }, () -> {
                  var3.disconnect();
                  this.connections.remove(var1, var2);
               });
            }
         }
      }
   }

   // $FF: synthetic method
   public void untrackWaypoint(final Waypoint param1) {
      this.untrackWaypoint((WaypointTransmitter)var1);
   }

   // $FF: synthetic method
   public void trackWaypoint(final Waypoint param1) {
      this.trackWaypoint((WaypointTransmitter)var1);
   }
}
