package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;

public record Message(Optional<String> literal, Optional<String> translatable, Optional<List<String>> translatableParams) {
   public static final Codec<Message> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(Codec.STRING.optionalFieldOf("literal").forGetter(Message::literal), Codec.STRING.optionalFieldOf("translatable").forGetter(Message::translatable), Codec.STRING.listOf().lenientOptionalFieldOf("translatableParams").forGetter(Message::translatableParams)).apply(var0, Message::new);
   });

   public Message(Optional<String> param1, Optional<String> param2, Optional<List<String>> param3) {
      super();
      this.literal = var1;
      this.translatable = var2;
      this.translatableParams = var3;
   }

   public Optional<Component> asComponent() {
      if (this.translatable.isPresent()) {
         String var1 = (String)this.translatable.get();
         if (this.translatableParams.isPresent()) {
            List var2 = (List)this.translatableParams.get();
            return Optional.of(Component.translatable(var1, var2.toArray()));
         } else {
            return Optional.of(Component.translatable(var1));
         }
      } else {
         return this.literal.map(Component::literal);
      }
   }

   public Optional<String> literal() {
      return this.literal;
   }

   public Optional<String> translatable() {
      return this.translatable;
   }

   public Optional<List<String>> translatableParams() {
      return this.translatableParams;
   }
}
