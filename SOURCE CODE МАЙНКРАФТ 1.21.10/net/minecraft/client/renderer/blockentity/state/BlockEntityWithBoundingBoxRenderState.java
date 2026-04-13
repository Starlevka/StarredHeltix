package net.minecraft.client.renderer.blockentity.state;

import javax.annotation.Nullable;
import net.minecraft.world.level.block.entity.BoundingBoxRenderable;

public class BlockEntityWithBoundingBoxRenderState extends BlockEntityRenderState {
   public boolean isVisible;
   public BoundingBoxRenderable.Mode mode;
   public BoundingBoxRenderable.RenderableBox box;
   @Nullable
   public BlockEntityWithBoundingBoxRenderState.InvisibleBlockType[] invisibleBlocks;
   @Nullable
   public boolean[] structureVoids;

   public BlockEntityWithBoundingBoxRenderState() {
      super();
   }

   public static enum InvisibleBlockType {
      AIR,
      BARRIER,
      LIGHT,
      STRUCUTRE_VOID;

      private InvisibleBlockType() {
      }

      // $FF: synthetic method
      private static BlockEntityWithBoundingBoxRenderState.InvisibleBlockType[] $values() {
         return new BlockEntityWithBoundingBoxRenderState.InvisibleBlockType[]{AIR, BARRIER, LIGHT, STRUCUTRE_VOID};
      }
   }
}
