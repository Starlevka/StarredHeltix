package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.jsonrpc.IncomingRpcMethod;
import net.minecraft.server.jsonrpc.OutgoingRpcMethod;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.Schema;
import net.minecraft.server.jsonrpc.api.SchemaComponent;

public class DiscoveryService {
   public DiscoveryService() {
      super();
   }

   public static DiscoveryService.DiscoverResponse discover(List<SchemaComponent> var0) {
      ArrayList var1 = new ArrayList(BuiltInRegistries.INCOMING_RPC_METHOD.size() + BuiltInRegistries.OUTGOING_RPC_METHOD.size());
      BuiltInRegistries.INCOMING_RPC_METHOD.listElements().forEach((var1x) -> {
         if (((IncomingRpcMethod)var1x.value()).attributes().discoverable()) {
            var1.add(((IncomingRpcMethod)var1x.value()).info().named(var1x.key().location()));
         }

      });
      BuiltInRegistries.OUTGOING_RPC_METHOD.listElements().forEach((var1x) -> {
         if (((OutgoingRpcMethod)var1x.value()).attributes().discoverable()) {
            var1.add(((OutgoingRpcMethod)var1x.value()).info().named(var1x.key().location()));
         }

      });
      HashMap var2 = new HashMap();
      Iterator var3 = var0.iterator();

      while(var3.hasNext()) {
         SchemaComponent var4 = (SchemaComponent)var3.next();
         var2.put(var4.name(), var4.schema());
      }

      DiscoveryService.DiscoverInfo var5 = new DiscoveryService.DiscoverInfo("Minecraft Server JSON-RPC", "1.0.0");
      return new DiscoveryService.DiscoverResponse("1.3.2", var5, var1, new DiscoveryService.DiscoverComponents(var2));
   }

   public static record DiscoverInfo(String title, String version) {
      public static final MapCodec<DiscoveryService.DiscoverInfo> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.STRING.fieldOf("title").forGetter(DiscoveryService.DiscoverInfo::title), Codec.STRING.fieldOf("version").forGetter(DiscoveryService.DiscoverInfo::version)).apply(var0, DiscoveryService.DiscoverInfo::new);
      });

      public DiscoverInfo(String param1, String param2) {
         super();
         this.title = var1;
         this.version = var2;
      }

      public String title() {
         return this.title;
      }

      public String version() {
         return this.version;
      }
   }

   public static record DiscoverResponse(String jsonRpcProtocolVersion, DiscoveryService.DiscoverInfo discoverInfo, List<MethodInfo.Named> methods, DiscoveryService.DiscoverComponents components) {
      public static final MapCodec<DiscoveryService.DiscoverResponse> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.STRING.fieldOf("openrpc").forGetter(DiscoveryService.DiscoverResponse::jsonRpcProtocolVersion), DiscoveryService.DiscoverInfo.CODEC.codec().fieldOf("info").forGetter(DiscoveryService.DiscoverResponse::discoverInfo), Codec.list(MethodInfo.Named.CODEC).fieldOf("methods").forGetter(DiscoveryService.DiscoverResponse::methods), DiscoveryService.DiscoverComponents.CODEC.codec().fieldOf("components").forGetter(DiscoveryService.DiscoverResponse::components)).apply(var0, DiscoveryService.DiscoverResponse::new);
      });

      public DiscoverResponse(String param1, DiscoveryService.DiscoverInfo param2, List<MethodInfo.Named> param3, DiscoveryService.DiscoverComponents param4) {
         super();
         this.jsonRpcProtocolVersion = var1;
         this.discoverInfo = var2;
         this.methods = var3;
         this.components = var4;
      }

      public String jsonRpcProtocolVersion() {
         return this.jsonRpcProtocolVersion;
      }

      public DiscoveryService.DiscoverInfo discoverInfo() {
         return this.discoverInfo;
      }

      public List<MethodInfo.Named> methods() {
         return this.methods;
      }

      public DiscoveryService.DiscoverComponents components() {
         return this.components;
      }
   }

   public static record DiscoverComponents(Map<String, Schema> schemas) {
      public static final MapCodec<DiscoveryService.DiscoverComponents> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.unboundedMap(Codec.STRING, Schema.CODEC).fieldOf("schemas").forGetter(DiscoveryService.DiscoverComponents::schemas)).apply(var0, DiscoveryService.DiscoverComponents::new);
      });

      public DiscoverComponents(Map<String, Schema> param1) {
         super();
         this.schemas = var1;
      }

      public Map<String, Schema> schemas() {
         return this.schemas;
      }
   }
}
