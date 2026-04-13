package net.minecraft.world.level.timers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.List;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;

public record FunctionTagCallback(ResourceLocation tagId) implements TimerCallback<MinecraftServer> {
   public static final MapCodec<FunctionTagCallback> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(ResourceLocation.CODEC.fieldOf("Name").forGetter(FunctionTagCallback::tagId)).apply(var0, FunctionTagCallback::new);
   });

   public FunctionTagCallback(ResourceLocation param1) {
      super();
      this.tagId = var1;
   }

   public void handle(MinecraftServer var1, TimerQueue<MinecraftServer> var2, long var3) {
      ServerFunctionManager var5 = var1.getFunctions();
      List var6 = var5.getTag(this.tagId);
      Iterator var7 = var6.iterator();

      while(var7.hasNext()) {
         CommandFunction var8 = (CommandFunction)var7.next();
         var5.execute(var8, var5.getGameLoopSender());
      }

   }

   public MapCodec<FunctionTagCallback> codec() {
      return CODEC;
   }

   public ResourceLocation tagId() {
      return this.tagId;
   }
}
