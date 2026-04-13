package net.minecraft.world.item;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

public class AdventureModePredicate {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<AdventureModePredicate> CODEC;
   public static final StreamCodec<RegistryFriendlyByteBuf, AdventureModePredicate> STREAM_CODEC;
   public static final Component CAN_BREAK_HEADER;
   public static final Component CAN_PLACE_HEADER;
   private static final Component UNKNOWN_USE;
   private final List<BlockPredicate> predicates;
   @Nullable
   private List<Component> cachedTooltip;
   @Nullable
   private BlockInWorld lastCheckedBlock;
   private boolean lastResult;
   private boolean checksBlockEntity;

   public AdventureModePredicate(List<BlockPredicate> var1) {
      super();
      this.predicates = var1;
   }

   private static boolean areSameBlocks(BlockInWorld var0, @Nullable BlockInWorld var1, boolean var2) {
      if (var1 != null && var0.getState() == var1.getState()) {
         if (!var2) {
            return true;
         } else if (var0.getEntity() == null && var1.getEntity() == null) {
            return true;
         } else if (var0.getEntity() != null && var1.getEntity() != null) {
            ProblemReporter.ScopedCollector var3 = new ProblemReporter.ScopedCollector(LOGGER);

            boolean var7;
            try {
               RegistryAccess var4 = var0.getLevel().registryAccess();
               CompoundTag var5 = saveBlockEntity(var0.getEntity(), var4, var3);
               CompoundTag var6 = saveBlockEntity(var1.getEntity(), var4, var3);
               var7 = Objects.equals(var5, var6);
            } catch (Throwable var9) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            var3.close();
            return var7;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static CompoundTag saveBlockEntity(BlockEntity var0, RegistryAccess var1, ProblemReporter var2) {
      TagValueOutput var3 = TagValueOutput.createWithContext(var2.forChild(var0.problemPath()), var1);
      var0.saveWithId(var3);
      return var3.buildResult();
   }

   public boolean test(BlockInWorld var1) {
      if (areSameBlocks(var1, this.lastCheckedBlock, this.checksBlockEntity)) {
         return this.lastResult;
      } else {
         this.lastCheckedBlock = var1;
         this.checksBlockEntity = false;
         Iterator var2 = this.predicates.iterator();

         BlockPredicate var3;
         do {
            if (!var2.hasNext()) {
               this.lastResult = false;
               return false;
            }

            var3 = (BlockPredicate)var2.next();
         } while(!var3.matches(var1));

         this.checksBlockEntity |= var3.requiresNbt();
         this.lastResult = true;
         return true;
      }
   }

   private List<Component> tooltip() {
      if (this.cachedTooltip == null) {
         this.cachedTooltip = computeTooltip(this.predicates);
      }

      return this.cachedTooltip;
   }

   public void addToTooltip(Consumer<Component> var1) {
      this.tooltip().forEach(var1);
   }

   private static List<Component> computeTooltip(List<BlockPredicate> var0) {
      Iterator var1 = var0.iterator();

      BlockPredicate var2;
      do {
         if (!var1.hasNext()) {
            return var0.stream().flatMap((var0x) -> {
               return ((HolderSet)var0x.blocks().orElseThrow()).stream();
            }).distinct().map((var0x) -> {
               return ((Block)var0x.value()).getName().withStyle(ChatFormatting.DARK_GRAY);
            }).toList();
         }

         var2 = (BlockPredicate)var1.next();
      } while(!var2.blocks().isEmpty());

      return List.of(UNKNOWN_USE);
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 instanceof AdventureModePredicate) {
         AdventureModePredicate var2 = (AdventureModePredicate)var1;
         return this.predicates.equals(var2.predicates);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.predicates.hashCode();
   }

   public String toString() {
      return "AdventureModePredicate{predicates=" + String.valueOf(this.predicates) + "}";
   }

   static {
      CODEC = ExtraCodecs.compactListCodec(BlockPredicate.CODEC, ExtraCodecs.nonEmptyList(BlockPredicate.CODEC.listOf())).xmap(AdventureModePredicate::new, (var0) -> {
         return var0.predicates;
      });
      STREAM_CODEC = StreamCodec.composite(BlockPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()), (var0) -> {
         return var0.predicates;
      }, AdventureModePredicate::new);
      CAN_BREAK_HEADER = Component.translatable("item.canBreak").withStyle(ChatFormatting.GRAY);
      CAN_PLACE_HEADER = Component.translatable("item.canPlace").withStyle(ChatFormatting.GRAY);
      UNKNOWN_USE = Component.translatable("item.canUse.unknown").withStyle(ChatFormatting.GRAY);
   }
}
