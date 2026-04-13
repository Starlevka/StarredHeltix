package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;

public class ContextNbtProvider implements NbtProvider {
   private static final ExtraCodecs.LateBoundIdMapper<String, ContextNbtProvider.Source<?>> SOURCES = new ExtraCodecs.LateBoundIdMapper();
   private static final Codec<ContextNbtProvider.Source<?>> GETTER_CODEC;
   public static final MapCodec<ContextNbtProvider> MAP_CODEC;
   public static final Codec<ContextNbtProvider> INLINE_CODEC;
   private final ContextNbtProvider.Source<?> source;

   private ContextNbtProvider(ContextNbtProvider.Source<?> var1) {
      super();
      this.source = var1;
   }

   public LootNbtProviderType getType() {
      return NbtProviders.CONTEXT;
   }

   @Nullable
   public Tag get(LootContext var1) {
      return this.source.get(var1);
   }

   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(this.source.contextParam());
   }

   public static NbtProvider forContextEntity(LootContext.EntityTarget var0) {
      return new ContextNbtProvider(new ContextNbtProvider.EntitySource(var0.getParam()));
   }

   static {
      LootContext.EntityTarget[] var0 = LootContext.EntityTarget.values();
      int var1 = var0.length;

      int var2;
      for(var2 = 0; var2 < var1; ++var2) {
         LootContext.EntityTarget var3 = var0[var2];
         SOURCES.put(var3.getSerializedName(), new ContextNbtProvider.EntitySource(var3.getParam()));
      }

      LootContext.BlockEntityTarget[] var4 = LootContext.BlockEntityTarget.values();
      var1 = var4.length;

      for(var2 = 0; var2 < var1; ++var2) {
         LootContext.BlockEntityTarget var5 = var4[var2];
         SOURCES.put(var5.getSerializedName(), new ContextNbtProvider.BlockEntitySource(var5.getParam()));
      }

      GETTER_CODEC = SOURCES.codec(Codec.STRING);
      MAP_CODEC = RecordCodecBuilder.mapCodec((var0x) -> {
         return var0x.group(GETTER_CODEC.fieldOf("target").forGetter((var0xx) -> {
            return var0xx.source;
         })).apply(var0x, ContextNbtProvider::new);
      });
      INLINE_CODEC = GETTER_CODEC.xmap(ContextNbtProvider::new, (var0x) -> {
         return var0x.source;
      });
   }

   private interface Source<T> {
      ContextKey<? extends T> contextParam();

      @Nullable
      Tag get(T var1);

      @Nullable
      default Tag get(LootContext var1) {
         Object var2 = var1.getOptionalParameter(this.contextParam());
         return var2 != null ? this.get(var2) : null;
      }
   }

   private static record EntitySource(ContextKey<? extends Entity> contextParam) implements ContextNbtProvider.Source<Entity> {
      EntitySource(ContextKey<? extends Entity> param1) {
         super();
         this.contextParam = var1;
      }

      public Tag get(Entity var1) {
         return NbtPredicate.getEntityTagToCompare(var1);
      }

      public ContextKey<? extends Entity> contextParam() {
         return this.contextParam;
      }
   }

   private static record BlockEntitySource(ContextKey<? extends BlockEntity> contextParam) implements ContextNbtProvider.Source<BlockEntity> {
      BlockEntitySource(ContextKey<? extends BlockEntity> param1) {
         super();
         this.contextParam = var1;
      }

      public Tag get(BlockEntity var1) {
         return var1.saveWithFullMetadata((HolderLookup.Provider)var1.getLevel().registryAccess());
      }

      public ContextKey<? extends BlockEntity> contextParam() {
         return this.contextParam;
      }
   }
}
