package net.minecraft.server.jsonrpc;

import com.google.gson.JsonElement;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;

public record PendingRpcRequest<Result>(Holder.Reference<? extends OutgoingRpcMethod<?, ? extends Result>> method, CompletableFuture<Result> resultFuture, long timeoutTime) {
   public PendingRpcRequest(Holder.Reference<? extends OutgoingRpcMethod<?, ? extends Result>> param1, CompletableFuture<Result> param2, long param3) {
      super();
      this.method = var1;
      this.resultFuture = var2;
      this.timeoutTime = var3;
   }

   public void accept(JsonElement var1) {
      try {
         Object var2 = ((OutgoingRpcMethod)this.method.value()).decodeResult(var1);
         this.resultFuture.complete(Objects.requireNonNull(var2));
      } catch (Exception var3) {
         this.resultFuture.completeExceptionally(var3);
      }

   }

   public boolean timedOut(long var1) {
      return var1 > this.timeoutTime;
   }

   public Holder.Reference<? extends OutgoingRpcMethod<?, ? extends Result>> method() {
      return this.method;
   }

   public CompletableFuture<Result> resultFuture() {
      return this.resultFuture;
   }

   public long timeoutTime() {
      return this.timeoutTime;
   }
}
