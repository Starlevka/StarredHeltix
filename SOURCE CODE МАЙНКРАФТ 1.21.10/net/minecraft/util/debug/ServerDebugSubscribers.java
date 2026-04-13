package net.minecraft.util.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public class ServerDebugSubscribers {
   private final MinecraftServer server;
   private final Map<DebugSubscription<?>, List<ServerPlayer>> enabledSubscriptions = new HashMap();

   public ServerDebugSubscribers(MinecraftServer var1) {
      super();
      this.server = var1;
   }

   private List<ServerPlayer> getSubscribersFor(DebugSubscription<?> var1) {
      return (List)this.enabledSubscriptions.getOrDefault(var1, List.of());
   }

   public void tick() {
      this.enabledSubscriptions.values().forEach(List::clear);
      Iterator var1 = this.server.getPlayerList().getPlayers().iterator();

      while(var1.hasNext()) {
         ServerPlayer var2 = (ServerPlayer)var1.next();
         Iterator var3 = var2.debugSubscriptions().iterator();

         while(var3.hasNext()) {
            DebugSubscription var4 = (DebugSubscription)var3.next();
            ((List)this.enabledSubscriptions.computeIfAbsent(var4, (var0) -> {
               return new ArrayList();
            })).add(var2);
         }
      }

      this.enabledSubscriptions.values().removeIf(List::isEmpty);
   }

   public void broadcastToAll(DebugSubscription<?> var1, Packet<?> var2) {
      Iterator var3 = this.getSubscribersFor(var1).iterator();

      while(var3.hasNext()) {
         ServerPlayer var4 = (ServerPlayer)var3.next();
         var4.connection.send(var2);
      }

   }

   public Set<DebugSubscription<?>> enabledSubscriptions() {
      return Set.copyOf(this.enabledSubscriptions.keySet());
   }

   public boolean hasAnySubscriberFor(DebugSubscription<?> var1) {
      return !this.getSubscribersFor(var1).isEmpty();
   }

   public boolean hasRequiredPermissions(ServerPlayer var1) {
      NameAndId var2 = var1.nameAndId();
      return SharedConstants.IS_RUNNING_IN_IDE && this.server.isSingleplayerOwner(var2) ? true : this.server.getPlayerList().isOp(var2);
   }
}
