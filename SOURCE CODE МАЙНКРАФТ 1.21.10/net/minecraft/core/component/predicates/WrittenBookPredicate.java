package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.critereon.CollectionPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;

public record WrittenBookPredicate(Optional<CollectionPredicate<Filterable<Component>, WrittenBookPredicate.PagePredicate>> pages, Optional<String> author, Optional<String> title, MinMaxBounds.Ints generation, Optional<Boolean> resolved) implements SingleComponentItemPredicate<WrittenBookContent> {
   public static final Codec<WrittenBookPredicate> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(CollectionPredicate.codec(WrittenBookPredicate.PagePredicate.CODEC).optionalFieldOf("pages").forGetter(WrittenBookPredicate::pages), Codec.STRING.optionalFieldOf("author").forGetter(WrittenBookPredicate::author), Codec.STRING.optionalFieldOf("title").forGetter(WrittenBookPredicate::title), MinMaxBounds.Ints.CODEC.optionalFieldOf("generation", MinMaxBounds.Ints.ANY).forGetter(WrittenBookPredicate::generation), Codec.BOOL.optionalFieldOf("resolved").forGetter(WrittenBookPredicate::resolved)).apply(var0, WrittenBookPredicate::new);
   });

   public WrittenBookPredicate(Optional<CollectionPredicate<Filterable<Component>, WrittenBookPredicate.PagePredicate>> param1, Optional<String> param2, Optional<String> param3, MinMaxBounds.Ints param4, Optional<Boolean> param5) {
      super();
      this.pages = var1;
      this.author = var2;
      this.title = var3;
      this.generation = var4;
      this.resolved = var5;
   }

   public DataComponentType<WrittenBookContent> componentType() {
      return DataComponents.WRITTEN_BOOK_CONTENT;
   }

   public boolean matches(WrittenBookContent var1) {
      if (this.author.isPresent() && !((String)this.author.get()).equals(var1.author())) {
         return false;
      } else if (this.title.isPresent() && !((String)this.title.get()).equals(var1.title().raw())) {
         return false;
      } else if (!this.generation.matches(var1.generation())) {
         return false;
      } else if (this.resolved.isPresent() && (Boolean)this.resolved.get() != var1.resolved()) {
         return false;
      } else {
         return !this.pages.isPresent() || ((CollectionPredicate)this.pages.get()).test((Iterable)var1.pages());
      }
   }

   public Optional<CollectionPredicate<Filterable<Component>, WrittenBookPredicate.PagePredicate>> pages() {
      return this.pages;
   }

   public Optional<String> author() {
      return this.author;
   }

   public Optional<String> title() {
      return this.title;
   }

   public MinMaxBounds.Ints generation() {
      return this.generation;
   }

   public Optional<Boolean> resolved() {
      return this.resolved;
   }

   public static record PagePredicate(Component contents) implements Predicate<Filterable<Component>> {
      public static final Codec<WrittenBookPredicate.PagePredicate> CODEC;

      public PagePredicate(Component param1) {
         super();
         this.contents = var1;
      }

      public boolean test(Filterable<Component> var1) {
         return ((Component)var1.raw()).equals(this.contents);
      }

      public Component contents() {
         return this.contents;
      }

      // $FF: synthetic method
      public boolean test(final Object param1) {
         return this.test((Filterable)var1);
      }

      static {
         CODEC = ComponentSerialization.CODEC.xmap(WrittenBookPredicate.PagePredicate::new, WrittenBookPredicate.PagePredicate::contents);
      }
   }
}
