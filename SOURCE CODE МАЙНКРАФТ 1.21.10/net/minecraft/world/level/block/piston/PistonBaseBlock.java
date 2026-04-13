package net.minecraft.world.level.block.piston;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PistonBaseBlock extends DirectionalBlock {
   public static final MapCodec<PistonBaseBlock> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(Codec.BOOL.fieldOf("sticky").forGetter((var0x) -> {
         return var0x.isSticky;
      }), propertiesCodec()).apply(var0, PistonBaseBlock::new);
   });
   public static final BooleanProperty EXTENDED;
   public static final int TRIGGER_EXTEND = 0;
   public static final int TRIGGER_CONTRACT = 1;
   public static final int TRIGGER_DROP = 2;
   public static final int PLATFORM_THICKNESS = 4;
   private static final Map<Direction, VoxelShape> SHAPES;
   private final boolean isSticky;

   public MapCodec<PistonBaseBlock> codec() {
      return CODEC;
   }

   public PistonBaseBlock(boolean var1, BlockBehaviour.Properties var2) {
      super(var2);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(EXTENDED, false));
      this.isSticky = var1;
   }

   protected VoxelShape getShape(BlockState var1, BlockGetter var2, BlockPos var3, CollisionContext var4) {
      return (Boolean)var1.getValue(EXTENDED) ? (VoxelShape)SHAPES.get(var1.getValue(FACING)) : Shapes.block();
   }

   public void setPlacedBy(Level var1, BlockPos var2, BlockState var3, LivingEntity var4, ItemStack var5) {
      if (!var1.isClientSide()) {
         this.checkIfExtend(var1, var2, var3);
      }

   }

   protected void neighborChanged(BlockState var1, Level var2, BlockPos var3, Block var4, @Nullable Orientation var5, boolean var6) {
      if (!var2.isClientSide()) {
         this.checkIfExtend(var2, var3, var1);
      }

   }

   protected void onPlace(BlockState var1, Level var2, BlockPos var3, BlockState var4, boolean var5) {
      if (!var4.is(var1.getBlock())) {
         if (!var2.isClientSide() && var2.getBlockEntity(var3) == null) {
            this.checkIfExtend(var2, var3, var1);
         }

      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext var1) {
      return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, var1.getNearestLookingDirection().getOpposite())).setValue(EXTENDED, false);
   }

   private void checkIfExtend(Level var1, BlockPos var2, BlockState var3) {
      Direction var4 = (Direction)var3.getValue(FACING);
      boolean var5 = this.getNeighborSignal(var1, var2, var4);
      if (var5 && !(Boolean)var3.getValue(EXTENDED)) {
         if ((new PistonStructureResolver(var1, var2, var4, true)).resolve()) {
            var1.blockEvent(var2, this, 0, var4.get3DDataValue());
         }
      } else if (!var5 && (Boolean)var3.getValue(EXTENDED)) {
         BlockPos var6 = var2.relative((Direction)var4, 2);
         BlockState var7 = var1.getBlockState(var6);
         byte var8 = 1;
         if (var7.is(Blocks.MOVING_PISTON) && var7.getValue(FACING) == var4) {
            BlockEntity var9 = var1.getBlockEntity(var6);
            if (var9 instanceof PistonMovingBlockEntity) {
               PistonMovingBlockEntity var10 = (PistonMovingBlockEntity)var9;
               if (var10.isExtending() && (var10.getProgress(0.0F) < 0.5F || var1.getGameTime() == var10.getLastTicked() || ((ServerLevel)var1).isHandlingTick())) {
                  var8 = 2;
               }
            }
         }

         var1.blockEvent(var2, this, var8, var4.get3DDataValue());
      }

   }

   private boolean getNeighborSignal(SignalGetter var1, BlockPos var2, Direction var3) {
      Direction[] var4 = Direction.values();
      int var5 = var4.length;

      int var6;
      for(var6 = 0; var6 < var5; ++var6) {
         Direction var7 = var4[var6];
         if (var7 != var3 && var1.hasSignal(var2.relative(var7), var7)) {
            return true;
         }
      }

      if (var1.hasSignal(var2, Direction.DOWN)) {
         return true;
      } else {
         BlockPos var9 = var2.above();
         Direction[] var10 = Direction.values();
         var6 = var10.length;

         for(int var11 = 0; var11 < var6; ++var11) {
            Direction var8 = var10[var11];
            if (var8 != Direction.DOWN && var1.hasSignal(var9.relative(var8), var8)) {
               return true;
            }
         }

         return false;
      }
   }

   protected boolean triggerEvent(BlockState var1, Level var2, BlockPos var3, int var4, int var5) {
      Direction var6 = (Direction)var1.getValue(FACING);
      BlockState var7 = (BlockState)var1.setValue(EXTENDED, true);
      if (!var2.isClientSide()) {
         boolean var8 = this.getNeighborSignal(var2, var3, var6);
         if (var8 && (var4 == 1 || var4 == 2)) {
            var2.setBlock(var3, var7, 2);
            return false;
         }

         if (!var8 && var4 == 0) {
            return false;
         }
      }

      if (var4 == 0) {
         if (!this.moveBlocks(var2, var3, var6, true)) {
            return false;
         }

         var2.setBlock(var3, var7, 67);
         var2.playSound((Entity)null, (BlockPos)var3, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, var2.random.nextFloat() * 0.25F + 0.6F);
         var2.gameEvent(GameEvent.BLOCK_ACTIVATE, var3, GameEvent.Context.of(var7));
      } else if (var4 == 1 || var4 == 2) {
         BlockEntity var15 = var2.getBlockEntity(var3.relative(var6));
         if (var15 instanceof PistonMovingBlockEntity) {
            ((PistonMovingBlockEntity)var15).finalTick();
         }

         BlockState var9 = (BlockState)((BlockState)Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, var6)).setValue(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
         var2.setBlock(var3, var9, 276);
         var2.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(var3, var9, (BlockState)this.defaultBlockState().setValue(FACING, Direction.from3DDataValue(var5 & 7)), var6, false, true));
         var2.updateNeighborsAt(var3, var9.getBlock());
         var9.updateNeighbourShapes(var2, var3, 2);
         if (this.isSticky) {
            BlockPos var10 = var3.offset(var6.getStepX() * 2, var6.getStepY() * 2, var6.getStepZ() * 2);
            BlockState var11 = var2.getBlockState(var10);
            boolean var12 = false;
            if (var11.is(Blocks.MOVING_PISTON)) {
               BlockEntity var13 = var2.getBlockEntity(var10);
               if (var13 instanceof PistonMovingBlockEntity) {
                  PistonMovingBlockEntity var14 = (PistonMovingBlockEntity)var13;
                  if (var14.getDirection() == var6 && var14.isExtending()) {
                     var14.finalTick();
                     var12 = true;
                  }
               }
            }

            if (!var12) {
               if (var4 != 1 || var11.isAir() || !isPushable(var11, var2, var10, var6.getOpposite(), false, var6) || var11.getPistonPushReaction() != PushReaction.NORMAL && !var11.is(Blocks.PISTON) && !var11.is(Blocks.STICKY_PISTON)) {
                  var2.removeBlock(var3.relative(var6), false);
               } else {
                  this.moveBlocks(var2, var3, var6, false);
               }
            }
         } else {
            var2.removeBlock(var3.relative(var6), false);
         }

         var2.playSound((Entity)null, (BlockPos)var3, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, var2.random.nextFloat() * 0.15F + 0.6F);
         var2.gameEvent(GameEvent.BLOCK_DEACTIVATE, var3, GameEvent.Context.of(var9));
      }

      return true;
   }

   public static boolean isPushable(BlockState var0, Level var1, BlockPos var2, Direction var3, boolean var4, Direction var5) {
      if (var2.getY() >= var1.getMinY() && var2.getY() <= var1.getMaxY() && var1.getWorldBorder().isWithinBounds(var2)) {
         if (var0.isAir()) {
            return true;
         } else if (!var0.is(Blocks.OBSIDIAN) && !var0.is(Blocks.CRYING_OBSIDIAN) && !var0.is(Blocks.RESPAWN_ANCHOR) && !var0.is(Blocks.REINFORCED_DEEPSLATE)) {
            if (var3 == Direction.DOWN && var2.getY() == var1.getMinY()) {
               return false;
            } else if (var3 == Direction.UP && var2.getY() == var1.getMaxY()) {
               return false;
            } else {
               if (!var0.is(Blocks.PISTON) && !var0.is(Blocks.STICKY_PISTON)) {
                  if (var0.getDestroySpeed(var1, var2) == -1.0F) {
                     return false;
                  }

                  switch(var0.getPistonPushReaction()) {
                  case BLOCK:
                     return false;
                  case DESTROY:
                     return var4;
                  case PUSH_ONLY:
                     return var3 == var5;
                  }
               } else if ((Boolean)var0.getValue(EXTENDED)) {
                  return false;
               }

               return !var0.hasBlockEntity();
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean moveBlocks(Level var1, BlockPos var2, Direction var3, boolean var4) {
      BlockPos var5 = var2.relative(var3);
      if (!var4 && var1.getBlockState(var5).is(Blocks.PISTON_HEAD)) {
         var1.setBlock(var5, Blocks.AIR.defaultBlockState(), 276);
      }

      PistonStructureResolver var6 = new PistonStructureResolver(var1, var2, var3, var4);
      if (!var6.resolve()) {
         return false;
      } else {
         HashMap var7 = Maps.newHashMap();
         List var8 = var6.getToPush();
         ArrayList var9 = Lists.newArrayList();
         Iterator var10 = var8.iterator();

         while(var10.hasNext()) {
            BlockPos var11 = (BlockPos)var10.next();
            BlockState var12 = var1.getBlockState(var11);
            var9.add(var12);
            var7.put(var11, var12);
         }

         List var20 = var6.getToDestroy();
         BlockState[] var21 = new BlockState[var8.size() + var20.size()];
         Direction var22 = var4 ? var3 : var3.getOpposite();
         int var13 = 0;

         int var14;
         BlockPos var15;
         BlockState var16;
         for(var14 = var20.size() - 1; var14 >= 0; --var14) {
            var15 = (BlockPos)var20.get(var14);
            var16 = var1.getBlockState(var15);
            BlockEntity var17 = var16.hasBlockEntity() ? var1.getBlockEntity(var15) : null;
            dropResources(var16, var1, var15, var17);
            if (!var16.is(BlockTags.FIRE) && var1.isClientSide()) {
               var1.levelEvent(2001, var15, getId(var16));
            }

            var1.setBlock(var15, Blocks.AIR.defaultBlockState(), 18);
            var1.gameEvent(GameEvent.BLOCK_DESTROY, var15, GameEvent.Context.of(var16));
            var21[var13++] = var16;
         }

         BlockState var29;
         for(var14 = var8.size() - 1; var14 >= 0; --var14) {
            var15 = (BlockPos)var8.get(var14);
            var16 = var1.getBlockState(var15);
            var15 = var15.relative(var22);
            var7.remove(var15);
            var29 = (BlockState)Blocks.MOVING_PISTON.defaultBlockState().setValue(FACING, var3);
            var1.setBlock(var15, var29, 324);
            var1.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(var15, var29, (BlockState)var9.get(var14), var3, var4, false));
            var21[var13++] = var16;
         }

         if (var4) {
            PistonType var23 = this.isSticky ? PistonType.STICKY : PistonType.DEFAULT;
            BlockState var25 = (BlockState)((BlockState)Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING, var3)).setValue(PistonHeadBlock.TYPE, var23);
            var16 = (BlockState)((BlockState)Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, var3)).setValue(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
            var7.remove(var5);
            var1.setBlock(var5, var16, 324);
            var1.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(var5, var16, var25, var3, true, true));
         }

         BlockState var24 = Blocks.AIR.defaultBlockState();
         Iterator var26 = var7.keySet().iterator();

         while(var26.hasNext()) {
            BlockPos var27 = (BlockPos)var26.next();
            var1.setBlock(var27, var24, 82);
         }

         var26 = var7.entrySet().iterator();

         while(var26.hasNext()) {
            Entry var30 = (Entry)var26.next();
            BlockPos var32 = (BlockPos)var30.getKey();
            BlockState var18 = (BlockState)var30.getValue();
            var18.updateIndirectNeighbourShapes(var1, var32, 2);
            var24.updateNeighbourShapes(var1, var32, 2);
            var24.updateIndirectNeighbourShapes(var1, var32, 2);
         }

         Orientation var28 = ExperimentalRedstoneUtils.initialOrientation(var1, var6.getPushDirection(), (Direction)null);
         var13 = 0;

         int var31;
         for(var31 = var20.size() - 1; var31 >= 0; --var31) {
            var29 = var21[var13++];
            BlockPos var34 = (BlockPos)var20.get(var31);
            if (var1 instanceof ServerLevel) {
               ServerLevel var19 = (ServerLevel)var1;
               var29.affectNeighborsAfterRemoval(var19, var34, false);
            }

            var29.updateIndirectNeighbourShapes(var1, var34, 2);
            var1.updateNeighborsAt(var34, var29.getBlock(), var28);
         }

         for(var31 = var8.size() - 1; var31 >= 0; --var31) {
            var1.updateNeighborsAt((BlockPos)var8.get(var31), var21[var13++].getBlock(), var28);
         }

         if (var4) {
            var1.updateNeighborsAt(var5, Blocks.PISTON_HEAD, var28);
         }

         return true;
      }
   }

   protected BlockState rotate(BlockState var1, Rotation var2) {
      return (BlockState)var1.setValue(FACING, var2.rotate((Direction)var1.getValue(FACING)));
   }

   protected BlockState mirror(BlockState var1, Mirror var2) {
      return var1.rotate(var2.getRotation((Direction)var1.getValue(FACING)));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> var1) {
      var1.add(FACING, EXTENDED);
   }

   protected boolean useShapeForLightOcclusion(BlockState var1) {
      return (Boolean)var1.getValue(EXTENDED);
   }

   protected boolean isPathfindable(BlockState var1, PathComputationType var2) {
      return false;
   }

   static {
      EXTENDED = BlockStateProperties.EXTENDED;
      SHAPES = Shapes.rotateAll(Block.boxZ(16.0D, 4.0D, 16.0D));
   }
}
