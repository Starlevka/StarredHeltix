package net.minecraft.commands.arguments.blocks;

import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

public class BlockInput implements Predicate<BlockInWorld> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final BlockState state;
   private final Set<Property<?>> properties;
   @Nullable
   private final CompoundTag tag;

   public BlockInput(BlockState var1, Set<Property<?>> var2, @Nullable CompoundTag var3) {
      super();
      this.state = var1;
      this.properties = var2;
      this.tag = var3;
   }

   public BlockState getState() {
      return this.state;
   }

   public Set<Property<?>> getDefinedProperties() {
      return this.properties;
   }

   public boolean test(BlockInWorld var1) {
      BlockState var2 = var1.getState();
      if (!var2.is(this.state.getBlock())) {
         return false;
      } else {
         Iterator var3 = this.properties.iterator();

         while(var3.hasNext()) {
            Property var4 = (Property)var3.next();
            if (var2.getValue(var4) != this.state.getValue(var4)) {
               return false;
            }
         }

         if (this.tag == null) {
            return true;
         } else {
            BlockEntity var5 = var1.getEntity();
            return var5 != null && NbtUtils.compareNbt(this.tag, var5.saveWithFullMetadata((HolderLookup.Provider)var1.getLevel().registryAccess()), true);
         }
      }
   }

   public boolean test(ServerLevel var1, BlockPos var2) {
      return this.test(new BlockInWorld(var1, var2, false));
   }

   public boolean place(ServerLevel var1, BlockPos var2, int var3) {
      BlockState var4 = (var3 & 16) != 0 ? this.state : Block.updateFromNeighbourShapes(this.state, var1, var2);
      if (var4.isAir()) {
         var4 = this.state;
      }

      var4 = this.overwriteWithDefinedProperties(var4);
      boolean var5 = false;
      if (var1.setBlock(var2, var4, var3)) {
         var5 = true;
      }

      if (this.tag != null) {
         BlockEntity var6 = var1.getBlockEntity(var2);
         if (var6 != null) {
            ProblemReporter.ScopedCollector var7 = new ProblemReporter.ScopedCollector(LOGGER);

            try {
               RegistryAccess var8 = var1.registryAccess();
               ProblemReporter var9 = var7.forChild(var6.problemPath());
               TagValueOutput var10 = TagValueOutput.createWithContext(var9.forChild(() -> {
                  return "(before)";
               }), var8);
               var6.saveWithoutMetadata((ValueOutput)var10);
               CompoundTag var11 = var10.buildResult();
               var6.loadWithComponents(TagValueInput.create(var7, var8, (CompoundTag)this.tag));
               TagValueOutput var12 = TagValueOutput.createWithContext(var9.forChild(() -> {
                  return "(after)";
               }), var8);
               var6.saveWithoutMetadata((ValueOutput)var12);
               CompoundTag var13 = var12.buildResult();
               if (!var13.equals(var11)) {
                  var5 = true;
                  var6.setChanged();
                  var1.getChunkSource().blockChanged(var2);
               }
            } catch (Throwable var15) {
               try {
                  var7.close();
               } catch (Throwable var14) {
                  var15.addSuppressed(var14);
               }

               throw var15;
            }

            var7.close();
         }
      }

      return var5;
   }

   private BlockState overwriteWithDefinedProperties(BlockState var1) {
      if (var1 == this.state) {
         return var1;
      } else {
         Property var3;
         for(Iterator var2 = this.properties.iterator(); var2.hasNext(); var1 = copyProperty(var1, this.state, var3)) {
            var3 = (Property)var2.next();
         }

         return var1;
      }
   }

   private static <T extends Comparable<T>> BlockState copyProperty(BlockState var0, BlockState var1, Property<T> var2) {
      return (BlockState)var0.trySetValue(var2, var1.getValue(var2));
   }

   // $FF: synthetic method
   public boolean test(final Object param1) {
      return this.test((BlockInWorld)var1);
   }
}
