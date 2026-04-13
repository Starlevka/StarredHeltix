package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.critereon.CollectionPredicate;
import net.minecraft.advancements.critereon.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WritableBookContent;

public record WritableBookPredicate(Optional<CollectionPredicate<Filterable<String>, WritableBookPredicate.PagePredicate>> pages) implements SingleComponentItemPredicate<WritableBookContent> {
   public static final Codec<WritableBookPredicate> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(CollectionPredicate.codec(WritableBookPredicate.PagePredicate.CODEC).optionalFieldOf("pages").forGetter(WritableBookPredicate::pages)).apply(var0, WritableBookPredicate::new);
   });

   public WritableBookPredicate(Optional<CollectionPredicate<Filterable<String>, WritableBookPredicate.PagePredicate>> param1) {
      super();
      this.pages = var1;
   }

   public DataComponentType<WritableBookContent> componentType() {
      return DataComponents.WRITABLE_BOOK_CONTENT;
   }

   public boolean matches(WritableBookContent var1) {
      return !this.pages.isPresent() || ((CollectionPredicate)this.pages.get()).test((Iterable)var1.pages());
   }

   public Optional<CollectionPredicate<Filterable<String>, WritableBookPredicate.PagePredicate>> pages() {
      return this.pages;
   }

   public static record PagePredicate(String contents) implements Predicate<Filterable<String>> {
      public static final Codec<WritableBookPredicate.PagePredicate> CODEC;

      public PagePredicate(String param1) {
         super();
         this.contents = var1;
      }

      public boolean test(Filterable<String> var1) {
         return ((String)var1.raw()).equals(this.contents);
      }

      public String contents() {
         return this.contents;
      }

      // $FF: synthetic method
      public boolean test(final Object param1) {
         return this.test((Filterable)var1);
      }

      static {
         CODEC = Codec.STRING.xmap(WritableBookPredicate.PagePredicate::new, WritableBookPredicate.PagePredicate::contents);
      }
   }
}
