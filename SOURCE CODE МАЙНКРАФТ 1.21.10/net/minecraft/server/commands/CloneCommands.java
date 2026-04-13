package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

public class CloneCommands {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType(Component.translatable("commands.clone.overlap"));
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((var0, var1) -> {
      return Component.translatableEscape("commands.clone.toobig", var0, var1);
   });
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.clone.failed"));
   public static final Predicate<BlockInWorld> FILTER_AIR = (var0) -> {
      return !var0.getState().isAir();
   };

   public CloneCommands() {
      super();
   }

   public static void register(CommandDispatcher<CommandSourceStack> var0, CommandBuildContext var1) {
      var0.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("clone").requires(Commands.hasPermission(2))).then(beginEndDestinationAndModeSuffix(var1, (var0x) -> {
         return ((CommandSourceStack)var0x.getSource()).getLevel();
      }))).then(Commands.literal("from").then(Commands.argument("sourceDimension", DimensionArgument.dimension()).then(beginEndDestinationAndModeSuffix(var1, (var0x) -> {
         return DimensionArgument.getDimension(var0x, "sourceDimension");
      })))));
   }

   private static ArgumentBuilder<CommandSourceStack, ?> beginEndDestinationAndModeSuffix(CommandBuildContext var0, InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> var1) {
      return Commands.argument("begin", BlockPosArgument.blockPos()).then(((RequiredArgumentBuilder)Commands.argument("end", BlockPosArgument.blockPos()).then(destinationAndStrictSuffix(var0, var1, (var0x) -> {
         return ((CommandSourceStack)var0x.getSource()).getLevel();
      }))).then(Commands.literal("to").then(Commands.argument("targetDimension", DimensionArgument.dimension()).then(destinationAndStrictSuffix(var0, var1, (var0x) -> {
         return DimensionArgument.getDimension(var0x, "targetDimension");
      })))));
   }

   private static CloneCommands.DimensionAndPosition getLoadedDimensionAndPosition(CommandContext<CommandSourceStack> var0, ServerLevel var1, String var2) throws CommandSyntaxException {
      BlockPos var3 = BlockPosArgument.getLoadedBlockPos(var0, var1, var2);
      return new CloneCommands.DimensionAndPosition(var1, var3);
   }

   private static ArgumentBuilder<CommandSourceStack, ?> destinationAndStrictSuffix(CommandBuildContext var0, InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> var1, InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> var2) {
      InCommandFunction var3 = (var1x) -> {
         return getLoadedDimensionAndPosition(var1x, (ServerLevel)var1.apply(var1x), "begin");
      };
      InCommandFunction var4 = (var1x) -> {
         return getLoadedDimensionAndPosition(var1x, (ServerLevel)var1.apply(var1x), "end");
      };
      InCommandFunction var5 = (var1x) -> {
         return getLoadedDimensionAndPosition(var1x, (ServerLevel)var2.apply(var1x), "destination");
      };
      return modeSuffix(var0, var3, var4, var5, false, Commands.argument("destination", BlockPosArgument.blockPos())).then(modeSuffix(var0, var3, var4, var5, true, Commands.literal("strict")));
   }

   private static ArgumentBuilder<CommandSourceStack, ?> modeSuffix(CommandBuildContext var0, InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> var1, InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> var2, InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> var3, boolean var4, ArgumentBuilder<CommandSourceStack, ?> var5) {
      return var5.executes((var4x) -> {
         return clone((CommandSourceStack)var4x.getSource(), (CloneCommands.DimensionAndPosition)var1.apply(var4x), (CloneCommands.DimensionAndPosition)var2.apply(var4x), (CloneCommands.DimensionAndPosition)var3.apply(var4x), (var0) -> {
            return true;
         }, CloneCommands.Mode.NORMAL, var4);
      }).then(wrapWithCloneMode(var1, var2, var3, (var0x) -> {
         return (var0) -> {
            return true;
         };
      }, var4, Commands.literal("replace"))).then(wrapWithCloneMode(var1, var2, var3, (var0x) -> {
         return FILTER_AIR;
      }, var4, Commands.literal("masked"))).then(Commands.literal("filtered").then(wrapWithCloneMode(var1, var2, var3, (var0x) -> {
         return BlockPredicateArgument.getBlockPredicate(var0x, "filter");
      }, var4, Commands.argument("filter", BlockPredicateArgument.blockPredicate(var0)))));
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapWithCloneMode(InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> var0, InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> var1, InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> var2, InCommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> var3, boolean var4, ArgumentBuilder<CommandSourceStack, ?> var5) {
      return var5.executes((var5x) -> {
         return clone((CommandSourceStack)var5x.getSource(), (CloneCommands.DimensionAndPosition)var0.apply(var5x), (CloneCommands.DimensionAndPosition)var1.apply(var5x), (CloneCommands.DimensionAndPosition)var2.apply(var5x), (Predicate)var3.apply(var5x), CloneCommands.Mode.NORMAL, var4);
      }).then(Commands.literal("force").executes((var5x) -> {
         return clone((CommandSourceStack)var5x.getSource(), (CloneCommands.DimensionAndPosition)var0.apply(var5x), (CloneCommands.DimensionAndPosition)var1.apply(var5x), (CloneCommands.DimensionAndPosition)var2.apply(var5x), (Predicate)var3.apply(var5x), CloneCommands.Mode.FORCE, var4);
      })).then(Commands.literal("move").executes((var5x) -> {
         return clone((CommandSourceStack)var5x.getSource(), (CloneCommands.DimensionAndPosition)var0.apply(var5x), (CloneCommands.DimensionAndPosition)var1.apply(var5x), (CloneCommands.DimensionAndPosition)var2.apply(var5x), (Predicate)var3.apply(var5x), CloneCommands.Mode.MOVE, var4);
      })).then(Commands.literal("normal").executes((var5x) -> {
         return clone((CommandSourceStack)var5x.getSource(), (CloneCommands.DimensionAndPosition)var0.apply(var5x), (CloneCommands.DimensionAndPosition)var1.apply(var5x), (CloneCommands.DimensionAndPosition)var2.apply(var5x), (Predicate)var3.apply(var5x), CloneCommands.Mode.NORMAL, var4);
      }));
   }

   private static int clone(CommandSourceStack var0, CloneCommands.DimensionAndPosition var1, CloneCommands.DimensionAndPosition var2, CloneCommands.DimensionAndPosition var3, Predicate<BlockInWorld> var4, CloneCommands.Mode var5, boolean var6) throws CommandSyntaxException {
      BlockPos var7 = var1.position();
      BlockPos var8 = var2.position();
      BoundingBox var9 = BoundingBox.fromCorners(var7, var8);
      BlockPos var10 = var3.position();
      BlockPos var11 = var10.offset(var9.getLength());
      BoundingBox var12 = BoundingBox.fromCorners(var10, var11);
      ServerLevel var13 = var1.dimension();
      ServerLevel var14 = var3.dimension();
      if (!var5.canOverlap() && var13 == var14 && var12.intersects(var9)) {
         throw ERROR_OVERLAP.create();
      } else {
         int var15 = var9.getXSpan() * var9.getYSpan() * var9.getZSpan();
         int var16 = var0.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
         if (var15 > var16) {
            throw ERROR_AREA_TOO_LARGE.create(var16, var15);
         } else if (var13.hasChunksAt(var7, var8) && var14.hasChunksAt(var10, var11)) {
            if (var14.isDebug()) {
               throw ERROR_FAILED.create();
            } else {
               ArrayList var17 = Lists.newArrayList();
               ArrayList var18 = Lists.newArrayList();
               ArrayList var19 = Lists.newArrayList();
               LinkedList var20 = Lists.newLinkedList();
               int var21 = 0;
               ProblemReporter.ScopedCollector var22 = new ProblemReporter.ScopedCollector(LOGGER);

               try {
                  BlockPos var23 = new BlockPos(var12.minX() - var9.minX(), var12.minY() - var9.minY(), var12.minZ() - var9.minZ());

                  int var24;
                  int var25;
                  BlockPos var27;
                  for(var24 = var9.minZ(); var24 <= var9.maxZ(); ++var24) {
                     for(var25 = var9.minY(); var25 <= var9.maxY(); ++var25) {
                        for(int var26 = var9.minX(); var26 <= var9.maxX(); ++var26) {
                           var27 = new BlockPos(var26, var25, var24);
                           BlockPos var28 = var27.offset(var23);
                           BlockInWorld var29 = new BlockInWorld(var13, var27, false);
                           BlockState var30 = var29.getState();
                           if (var4.test(var29)) {
                              BlockEntity var31 = var13.getBlockEntity(var27);
                              if (var31 != null) {
                                 TagValueOutput var32 = TagValueOutput.createWithContext(var22.forChild(var31.problemPath()), var0.registryAccess());
                                 var31.saveCustomOnly((ValueOutput)var32);
                                 CloneCommands.CloneBlockEntityInfo var33 = new CloneCommands.CloneBlockEntityInfo(var32.buildResult(), var31.components());
                                 var18.add(new CloneCommands.CloneBlockInfo(var28, var30, var33, var14.getBlockState(var28)));
                                 var20.addLast(var27);
                              } else if (!var30.isSolidRender() && !var30.isCollisionShapeFullBlock(var13, var27)) {
                                 var19.add(new CloneCommands.CloneBlockInfo(var28, var30, (CloneCommands.CloneBlockEntityInfo)null, var14.getBlockState(var28)));
                                 var20.addFirst(var27);
                              } else {
                                 var17.add(new CloneCommands.CloneBlockInfo(var28, var30, (CloneCommands.CloneBlockEntityInfo)null, var14.getBlockState(var28)));
                                 var20.addLast(var27);
                              }
                           }
                        }
                     }
                  }

                  var24 = 2 | (var6 ? 816 : 0);
                  if (var5 == CloneCommands.Mode.MOVE) {
                     Iterator var36 = var20.iterator();

                     while(var36.hasNext()) {
                        BlockPos var38 = (BlockPos)var36.next();
                        var13.setBlock(var38, Blocks.BARRIER.defaultBlockState(), var24 | 816);
                     }

                     var25 = var6 ? var24 : 3;
                     Iterator var39 = var20.iterator();

                     while(var39.hasNext()) {
                        var27 = (BlockPos)var39.next();
                        var13.setBlock(var27, Blocks.AIR.defaultBlockState(), var25);
                     }
                  }

                  ArrayList var37 = Lists.newArrayList();
                  var37.addAll(var17);
                  var37.addAll(var18);
                  var37.addAll(var19);
                  List var40 = Lists.reverse(var37);
                  Iterator var41 = var40.iterator();

                  CloneCommands.CloneBlockInfo var42;
                  while(var41.hasNext()) {
                     var42 = (CloneCommands.CloneBlockInfo)var41.next();
                     var14.setBlock(var42.pos, Blocks.BARRIER.defaultBlockState(), var24 | 816);
                  }

                  var41 = var37.iterator();

                  while(var41.hasNext()) {
                     var42 = (CloneCommands.CloneBlockInfo)var41.next();
                     if (var14.setBlock(var42.pos, var42.state, var24)) {
                        ++var21;
                     }
                  }

                  for(var41 = var18.iterator(); var41.hasNext(); var14.setBlock(var42.pos, var42.state, var24)) {
                     var42 = (CloneCommands.CloneBlockInfo)var41.next();
                     BlockEntity var43 = var14.getBlockEntity(var42.pos);
                     if (var42.blockEntityInfo != null && var43 != null) {
                        var43.loadCustomOnly(TagValueInput.create(var22.forChild(var43.problemPath()), var14.registryAccess(), (CompoundTag)var42.blockEntityInfo.tag));
                        var43.setComponents(var42.blockEntityInfo.components);
                        var43.setChanged();
                     }
                  }

                  if (!var6) {
                     var41 = var40.iterator();

                     while(var41.hasNext()) {
                        var42 = (CloneCommands.CloneBlockInfo)var41.next();
                        var14.updateNeighboursOnBlockSet(var42.pos, var42.previousStateAtDestination);
                     }
                  }

                  var14.getBlockTicks().copyAreaFrom(var13.getBlockTicks(), var9, var23);
               } catch (Throwable var35) {
                  try {
                     var22.close();
                  } catch (Throwable var34) {
                     var35.addSuppressed(var34);
                  }

                  throw var35;
               }

               var22.close();
               if (var21 == 0) {
                  throw ERROR_FAILED.create();
               } else {
                  var0.sendSuccess(() -> {
                     return Component.translatable("commands.clone.success", var21);
                  }, true);
                  return var21;
               }
            }
         } else {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
         }
      }
   }

   private static record DimensionAndPosition(ServerLevel dimension, BlockPos position) {
      DimensionAndPosition(ServerLevel param1, BlockPos param2) {
         super();
         this.dimension = var1;
         this.position = var2;
      }

      public ServerLevel dimension() {
         return this.dimension;
      }

      public BlockPos position() {
         return this.position;
      }
   }

   private static enum Mode {
      FORCE(true),
      MOVE(true),
      NORMAL(false);

      private final boolean canOverlap;

      private Mode(final boolean param3) {
         this.canOverlap = var3;
      }

      public boolean canOverlap() {
         return this.canOverlap;
      }

      // $FF: synthetic method
      private static CloneCommands.Mode[] $values() {
         return new CloneCommands.Mode[]{FORCE, MOVE, NORMAL};
      }
   }

   private static record CloneBlockEntityInfo(CompoundTag tag, DataComponentMap components) {
      final CompoundTag tag;
      final DataComponentMap components;

      CloneBlockEntityInfo(CompoundTag param1, DataComponentMap param2) {
         super();
         this.tag = var1;
         this.components = var2;
      }

      public CompoundTag tag() {
         return this.tag;
      }

      public DataComponentMap components() {
         return this.components;
      }
   }

   static record CloneBlockInfo(BlockPos pos, BlockState state, @Nullable CloneCommands.CloneBlockEntityInfo blockEntityInfo, BlockState previousStateAtDestination) {
      final BlockPos pos;
      final BlockState state;
      @Nullable
      final CloneCommands.CloneBlockEntityInfo blockEntityInfo;
      final BlockState previousStateAtDestination;

      CloneBlockInfo(BlockPos param1, BlockState param2, @Nullable CloneCommands.CloneBlockEntityInfo param3, BlockState param4) {
         super();
         this.pos = var1;
         this.state = var2;
         this.blockEntityInfo = var3;
         this.previousStateAtDestination = var4;
      }

      public BlockPos pos() {
         return this.pos;
      }

      public BlockState state() {
         return this.state;
      }

      @Nullable
      public CloneCommands.CloneBlockEntityInfo blockEntityInfo() {
         return this.blockEntityInfo;
      }

      public BlockState previousStateAtDestination() {
         return this.previousStateAtDestination;
      }
   }
}
