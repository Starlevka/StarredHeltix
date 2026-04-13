package net.minecraft.server.jsonrpc.api;

import java.net.URI;

public record SchemaComponent(String name, URI ref, Schema schema) {
   public SchemaComponent(String param1, URI param2, Schema param3) {
      super();
      this.name = var1;
      this.ref = var2;
      this.schema = var3;
   }

   public Schema asRef() {
      return Schema.ofRef(this.ref);
   }

   public Schema asArray() {
      return Schema.arrayOf(Schema.ofRef(this.ref));
   }

   public String name() {
      return this.name;
   }

   public URI ref() {
      return this.ref;
   }

   public Schema schema() {
      return this.schema;
   }
}
